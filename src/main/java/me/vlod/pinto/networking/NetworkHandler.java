package me.vlod.pinto.networking;

import java.util.ArrayList;

import me.vlod.pinto.Delegate;
import me.vlod.pinto.PintoServer;
import me.vlod.pinto.UserStatus;
import me.vlod.pinto.configuration.BannedConfig;
import me.vlod.pinto.configuration.MainConfig;
import me.vlod.pinto.configuration.WhitelistConfig;
import me.vlod.pinto.consolehandler.ConsoleCaller;
import me.vlod.pinto.consolehandler.ConsoleHandler;

public class NetworkHandler {
	private PintoServer server;
	public NetworkAddress networkAddress;
	public NetworkClient networkClient;
	public ConsoleHandler consoleHandler;
	public int noLoginKickTicks;
	public byte protocolVersion;
	public boolean loggedIn;
	public String userName;
	public String passwordHash;
	public UserStatus status;
	public ArrayList<String> contacts = new ArrayList<String>();
	
	public NetworkHandler(PintoServer server, NetworkAddress address, NetworkClient client) {
		this.server = server;
		this.networkAddress = address;
		this.networkClient = client;
		this.consoleHandler = new ConsoleHandler(this.server, new ConsoleCaller(this));
		
		this.networkClient.receivedPacket = new Delegate() {
			@Override
			public void call(Object... args) {
				onReceivedPacket((Packet)args[0]);
			}
		};
		
		this.networkClient.disconnected = new Delegate() {
			@Override
			public void call(Object... args) {
				onDisconnect((String)args[0]);
			}
		};
		
		PintoServer.logger.info(this.networkAddress + " has connected");
	}
	
	private void onReceivedPacket(Packet packet) {
		this.handlePacket(packet);
	}
	
	private void onDisconnect(String reason) {
		this.changeStatus(UserStatus.OFFLINE, true);
		this.server.clients.remove(this);
		PintoServer.logger.info(this.networkAddress + " has disconnected: " + reason);
	}
	
	public void onTick() {
		this.noLoginKickTicks++;

		if (this.noLoginKickTicks > 10 && this.userName == null) {
			this.kick("No login packet received in an acceptable time frame!");
			return;
		}
	}

	private String getDatabaseSelector() {
		return String.format("name = \"%s\"", this.userName);
	}
	
	@SuppressWarnings("unused")
	private void setDatabaseEntry() {
		String contactsEncoded = "";
		for (String contact : this.contacts) {
			contactsEncoded += String.format("%s,", contact);
		}
		contactsEncoded = contactsEncoded.substring(0, contactsEncoded.length() - 1);
		
		try {
			this.server.database.changeRows(PintoServer.TABLE_NAME, this.getDatabaseSelector(), 
					new String[] { 
							"name", 
							"passwordHash",
							"laststatus",
							"contacts"
					}, 
					new String[] { 
							String.format("\"%s\"", this.userName),
							String.format("\"%s\"", this.passwordHash),
							"" + this.status.getIndex(),
							String.format("\"%s\"", contactsEncoded)
					});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void loadDatabaseEntry() {
		try {
			String[] row = this.server.database.getRows(
					PintoServer.TABLE_NAME, this.getDatabaseSelector()).get(0);
			
			this.passwordHash = row[1];
			this.status = UserStatus.fromIndex(Integer.valueOf(row[2]));
			
			String contactsRaw = row[3];
			
			if (!contactsRaw.equalsIgnoreCase("null")) {
				for (String contact : contactsRaw.split(",")) {
					this.contacts.add(contact);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void performSync() {
		this.networkClient.sendPacket(new PacketStatus("", this.status));
		
		for (String contact : this.contacts) {
			NetworkHandler netHandler = this.server.getHandlerByName(contact);
			this.networkClient.sendPacket(new PacketAddContact(contact));
			if (netHandler != null) {
				this.networkClient.sendPacket(new PacketStatus(contact, netHandler.status));
				netHandler.networkClient.sendPacket(new PacketStatus(this.userName, this.status));
			}
		}
	}
	
    public void kick(String reason) {
    	PintoServer.logger.info("Kicking " + this.networkAddress + 
    			(this.userName != null ? " (" + this.userName + ")" : "") + ": " + reason);
    	this.networkClient.sendPacket(new PacketLogout(reason));
    	this.networkClient.disconnect("Kicked -> " + reason);
    }
    
    public void changeStatus(UserStatus status, boolean noSelfUpdate) {
    	this.status = status;
    	if (!noSelfUpdate) {
    		this.networkClient.sendPacket(new PacketStatus("", status));
    		this.setDatabaseEntry();
    	}

		for (String contact : this.contacts) {
			NetworkHandler netHandler = this.server.getHandlerByName(contact);
			if (netHandler == null) continue;
			netHandler.networkClient.sendPacket(new PacketStatus(this.userName, 
					(this.status == UserStatus.INVISIBLE ? UserStatus.OFFLINE : this.status)));
		}
    }
    
	public void handlePacket(Packet packet) {
		switch (packet.getID()) {
		case 0:
			this.handleLoginPacket((PacketLogin)packet);
			break;
		case 3:
			this.handleMessagePacket((PacketMessage)packet);
			break;
		case 6:
			this.handleAddContactPacket((PacketAddContact)packet);
			break;
		case 7:
			this.handleRemoveContactPacket((PacketRemoveContact)packet);
			break;
		case 8:
			this.handleStatusPacket((PacketStatus)packet);
			break;
		}
	}

	private void handleLoginPacket(PacketLogin packet) {
    	// Check if either the user name or IP are not white-listed
    	if (MainConfig.instance.useWhiteList && 
    		!WhitelistConfig.instance.ips.contains(this.networkAddress.ip) &&
    		!WhitelistConfig.instance.users.contains(packet.name)) {
    		this.kick("You are not white-listed!");
    		return;
    	}
    	
		String bannedReason = BannedConfig.instance.users.get(packet.name);
		String bannedReasonIP = BannedConfig.instance.ips.get(this.networkAddress.ip);
    	
		// Check if either the user name or IP are banned
    	if (bannedReason != null) {
    		this.kick("You are banned from this chat!\nReason: " + bannedReason);
    		return;
    	} else if (bannedReasonIP != null) {
    		this.kick("You are banned from this chat based on your IP address!\nReason: " + bannedReasonIP);
    		return;
    	}
		
    	// Check if the client protocol is not 11
    	if (packet.protocolVersion != 11) {
    		this.kick("Illegal protocol version!\nMust be 11, but got " + packet.protocolVersion);
    		return;
    	}
    	this.protocolVersion = packet.protocolVersion;
    	
    	// Check if the user name is already used
    	if (this.server.getHandlerByName(packet.name) != null) {
    		this.kick("Someone with this username is already connected!");
    		return;
    	}
    	
    	// Check if the user name is legal
    	if (!packet.name.matches("^(?=.{3,15}$)[a-z0-9._]+$")) {
    		this.kick("Illegal username!\n"
    				+ "Legal usernames must have a length of at least 3 and at most 16\n"
    				+ "Legal usernames may only contain lowercase alphanumeric characters, underscores and dots");
    		return;
    	}
    	this.userName = packet.name;

    	// Check if the client is registered
    	boolean noSQLEntry = false;
    	try {
			if (this.server.database.getRows(PintoServer.TABLE_NAME, 
					String.format("name = \"%s\"", this.userName)).size() < 1) {
				noSQLEntry = true;
			}
		} catch (Exception ex) {
			noSQLEntry = true;
			// TODO: Proper SQL error handler
			PintoServer.logger.throwable(ex);
		}
    	
    	if (noSQLEntry) {
    		this.kick("This account doesn't exist! Please create one and try again.");
    		return;
    	}
    	
    	// Load the database entry
    	this.loadDatabaseEntry();
    	
    	if (!this.passwordHash.equalsIgnoreCase(packet.passwordHash)) {
    		this.kick("Invalid password!");
    		return;
    	}

    	// Mark the client as logged in
    	this.loggedIn = true;
    	
    	// Send the login packet to let the client know they have logged in
    	this.networkClient.sendPacket(new PacketLogin(this.protocolVersion, "", ""));
    	
    	// Sync the database to the user
    	this.performSync();
    }

    private void handleMessagePacket(PacketMessage packet) {
    	if (!this.contacts.contains(packet.contactName)) {
    		this.kick("Protocol violation!");
    		return;
    	}
    	
		NetworkHandler netHandler = this.server.getHandlerByName(packet.contactName);
		
		if (netHandler == null || netHandler.status == UserStatus.INVISIBLE) {
			this.networkClient.sendPacket(new PacketInWindowPopup(String.format(
					"%s is offline and may not receive messages", packet.contactName)));
			return;
		}
		
		String message = String.format("%s: %s", this.userName, packet.message);
		netHandler.networkClient.sendPacket(new PacketMessage(this.userName, message));
		this.networkClient.sendPacket(new PacketMessage(packet.contactName, message));
    }
    
	private void handleAddContactPacket(PacketAddContact packet) {
	}
    
	private void handleRemoveContactPacket(PacketRemoveContact packet) {
	}
	
    private void handleStatusPacket(PacketStatus packet) {
    	if (packet.status == UserStatus.OFFLINE) {
    		this.kick("Protocol violation!");
    		return;
    	}
    	this.changeStatus(packet.status, false);
	}
}
