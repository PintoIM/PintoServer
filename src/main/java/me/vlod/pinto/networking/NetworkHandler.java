package me.vlod.pinto.networking;

import me.vlod.pinto.ClientUpdateCheck;
import me.vlod.pinto.Delegate;
import me.vlod.pinto.PintoServer;
import me.vlod.pinto.UserDatabaseEntry;
import me.vlod.pinto.UserStatus;
import me.vlod.pinto.configuration.MainConfig;
import me.vlod.pinto.consolehandler.ConsoleCaller;
import me.vlod.pinto.consolehandler.ConsoleHandler;
import me.vlod.pinto.event.ClientConnectedEvent;
import me.vlod.pinto.event.ClientDisconnectedEvent;
import me.vlod.pinto.event.HandledPacketEvent;
import me.vlod.pinto.event.ReceivedPacketEvent;
import me.vlod.pinto.event.SendingPacketEvent;
import me.vlod.pinto.event.SentPacketEvent;
import me.vlod.pinto.networking.packet.Packet;
import me.vlod.pinto.networking.packet.PacketAddContact;
import me.vlod.pinto.networking.packet.PacketClearContacts;
import me.vlod.pinto.networking.packet.PacketContactRequest;
import me.vlod.pinto.networking.packet.PacketInWindowPopup;
import me.vlod.pinto.networking.packet.PacketKeepAlive;
import me.vlod.pinto.networking.packet.PacketLogin;
import me.vlod.pinto.networking.packet.PacketLogout;
import me.vlod.pinto.networking.packet.PacketMessage;
import me.vlod.pinto.networking.packet.PacketPopup;
import me.vlod.pinto.networking.packet.PacketRegister;
import me.vlod.pinto.networking.packet.PacketRemoveContact;
import me.vlod.pinto.networking.packet.PacketServerID;
import me.vlod.pinto.networking.packet.PacketStatus;

public class NetworkHandler {
	public static final int PROTOCOL_VERSION = 1;
	public static final int USERNAME_MAX = 16;
	private PintoServer server;
	public NetworkAddress networkAddress;
	public NetworkClient networkClient;
	public ConsoleHandler consoleHandler;
	public int noLoginKickTicks;
	public int noKeepAlivePacketTicks;
	public byte protocolVersion;
	public boolean loggedIn;
	public UserDatabaseEntry databaseEntry;
	public String userName;
	public String motd = "";
	public String clientVersion;
	public boolean inCall;
	public String inCallWith;

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
		
		PintoServer.logger.info("%s has connected", this.networkAddress);
		
		if (this.server.clients.size() > MainConfig.instance.maxUsers) {
			this.kick("The server is full!");
			return;
		}
		
		ClientConnectedEvent event = new ClientConnectedEvent(this);
		this.server.eventSender.send(event);
		if (event.getCancelled()) {
			this.networkClient.disconnect("Connected event cancelled");
		}
	}
	
	private void onReceivedPacket(Packet packet) {
		ReceivedPacketEvent event = new ReceivedPacketEvent(this, packet);
		this.server.eventSender.send(event);
		if (event.getCancelled()) {
			return;
		}
		
		if (packet.getID() != 255) {
			PintoServer.logger.verbose("Received packet %s (%d) from %s (%s)", 
					packet.getClass().getSimpleName().toUpperCase(),
					packet.getID(), this.networkAddress,
	    			(this.userName != null ? this.userName : "** UNAUTHENTICATED **"));	
		}
		
		packet.handle(this);
		this.server.eventSender.send(new HandledPacketEvent(this, packet));
	}
	
	private void onDisconnect(String reason) {
		this.changeStatus(UserStatus.OFFLINE, "", true);
		this.server.clients.remove(this);
		PintoServer.logger.info("%s has disconnected: %s", this.networkAddress, reason);
		this.server.eventSender.send(new ClientDisconnectedEvent(this, reason));
		this.server.sendHeartbeat();
	}
	
	public void sendPacket(Packet packet) {
		SendingPacketEvent event = new SendingPacketEvent(this, packet);
		this.server.eventSender.send(event);
		if (event.getCancelled()) {
			return;
		}
		
		if (packet.getID() != 255) {
			PintoServer.logger.verbose("Sent packet %s (%d) to %s (%s)", 
					packet.getClass().getSimpleName().toUpperCase(),
					packet.getID(), this.networkAddress,
	    			(this.userName != null ? this.userName : "** UNAUTHENTICATED **"));	
		}
		this.networkClient.sendPacket(packet);
		this.server.eventSender.send(new SentPacketEvent(this, packet));
	}
	
	public void onTick() {
		this.noLoginKickTicks++;
		
		if (this.noLoginKickTicks > 6 && this.userName == null) {
			this.kick("No login packet received in an acceptable time frame!");
			return;
		}
		
		if (this.noKeepAlivePacketTicks > 5) {
			this.kick("Timed out");
			return;
		}
		
		this.sendPacket(new PacketKeepAlive());
		this.noKeepAlivePacketTicks++;
	}

	private void performSync() {
		this.sendPacket(new PacketClearContacts());
		this.sendPacket(new PacketStatus("", this.databaseEntry.status, ""));

		for (String contact : this .databaseEntry.contacts) {
			NetworkHandler netHandler = this.server.getHandlerByName(contact);
			this.sendPacket(new PacketAddContact(contact, 
					netHandler == null ? UserStatus.OFFLINE : this.getStatus(),
					netHandler == null ? "" : 
						this.isOnline() ? netHandler.motd : ""));
			
			if (netHandler != null && 
				NetHandlerUtils.getToOthersStatus(this.databaseEntry.status) != UserStatus.OFFLINE) {
				netHandler.sendPacket(new PacketStatus(this.userName, this.databaseEntry.status, ""));
			}
		}
	}
	
    public void kick(String reason) {
    	PintoServer.logger.info("Kicking %s (%s): %s", this.networkAddress,
    			(this.userName != null ? this.userName : "** UNAUTHENTICATED **"), reason);
    	this.sendPacket(new PacketLogout(reason));
    	this.networkClient.disconnect(null, true);
    	this.onDisconnect(String.format("Kicked (%s)", reason));
    }
    
    public void changeStatus(UserStatus status, String motd, boolean noSelfUpdate) {
    	if (this.databaseEntry == null) return;
    	
    	this.databaseEntry.status = status;
    	this.motd = motd;
    	
    	if (!noSelfUpdate) {
    		this.sendPacket(new PacketStatus("", status, this.motd));
    		this.databaseEntry.save();
    	}

		for (String contact : this.databaseEntry.contacts) {
			NetworkHandler netHandler = this.server.getHandlerByName(contact);
			if (netHandler == null) continue;
			netHandler.sendPacket(new PacketStatus(this.userName,
					this.getStatus(), 
					this.isOnline() ? this.motd : ""));
		}
    }

	public void handleLoginPacket(PacketLogin packet) {
		if (!NetHandlerUtils.performModerationChecks(this, packet.name) || 
			!NetHandlerUtils.performProtocolCheck(this, packet.protocolVersion, packet.clientVersion) ||
			!NetHandlerUtils.performNameVerification(this, packet.name)) {
			return;
		}
		
    	// Check if the user name is already used
    	if (this.server.getHandlerByName(packet.name) != null) {
    		this.kick("Someone with this username is already connected!");
    		return;
    	}

    	this.protocolVersion = packet.protocolVersion;
    	this.clientVersion = packet.clientVersion;
    	this.userName = packet.name;

    	// Check if the client is not registered
    	if (!UserDatabaseEntry.isRegistered(this.server, userName)) {
    		this.kick("Invalid username or password!");
    		return;
    	}
    	
    	// Load the database entry
    	this.databaseEntry = new UserDatabaseEntry(this.server, this.userName);
    	this.databaseEntry.load();
    	
    	if (!this.databaseEntry.passwordHash.equalsIgnoreCase(packet.passwordHash)) {
    		this.kick("Invalid username or password!");
    		return;
    	}
    	
    	// Mark the client as logged in
    	this.loggedIn = true;
    	
    	// Send the login packet to let the client know they have logged in
    	this.sendPacket(new PacketLogin(this.protocolVersion, "", ""));
    	PintoServer.logger.info("%s has logged in (Client version %s)", 
    			this.userName, this.clientVersion);
    	
    	// Send the user our server ID
    	this.sendPacket(new PacketServerID(MainConfig.instance.serverID));
    	
    	// Sync the database to the user
    	this.performSync();
    	
    	// Check if the client is not the latest to send a notice
    	if (!MainConfig.instance.ignoreClientVersion &&
    		!ClientUpdateCheck.isLatest(this.clientVersion)) {
    		this.sendPacket(new PacketInWindowPopup("Your client version is not the latest,"
    				+ " upgrade to the latest version to get the most recent features and bug fixes!"));
    		this.sendPacket(new PacketPopup("Client outdated", "Your client version is not the latest,"
    				+ " upgrade to the latest version to get the most recent features and bug fixes!"));
        	PintoServer.logger.warn("%s has an older client than the latest!", 
        			this.userName, this.clientVersion);
    	}
    	
    	// Send a heart beat with the updated users count
    	this.server.sendHeartbeat();
    }

	public void handleRegisterPacket(PacketRegister packet) {
		if (!NetHandlerUtils.performModerationChecks(this, packet.name) || 
			!NetHandlerUtils.performProtocolCheck(this, packet.protocolVersion, packet.clientVersion) ||
			!NetHandlerUtils.performNameVerification(this, packet.name)) {
			return;
		}
		
    	this.protocolVersion = packet.protocolVersion;
    	this.clientVersion = packet.clientVersion;
    	this.userName = packet.name;

    	if (UserDatabaseEntry.isRegistered(this.server, userName)) {
    		this.kick("This account already exists!");
    		return;
    	}
    	
    	if (!packet.passwordHash.matches(NetHandlerUtils.PASSWORD_REGEX_CHECK)) {
    		this.kick("Illegal password hash! Attempted SQL injection?");
    		return;
    	}
    	
    	// Create the database entry
    	this.databaseEntry = UserDatabaseEntry.registerAndReturnEntry(this.server, packet.name,
    			packet.passwordHash, UserStatus.ONLINE);
    	
    	// Mark the client as logged in
    	this.loggedIn = true;
    	
    	// Send the login packet to let the client know they have logged in
    	this.sendPacket(new PacketLogin(this.protocolVersion, "", ""));
    	PintoServer.logger.info("%s has been registered and has logged in (Client version %s)", 
    			this.userName, this.clientVersion);
    	
    	// Send first time message
    	this.sendPacket(new PacketPopup("Welcome!", "Welcome to Pinto!,"
    			+ " enjoy your new account,"
    			+ " you can start talking with people by clicking on File > Add Contact"));
    }
	
	public void handleMessagePacket(PacketMessage packet) {
		String msg = packet.message.trim();
		
		if (msg.isEmpty()) {
			this.sendPacket(new PacketMessage(packet.contactName, "",
					"You may not send empty messages!"));
			return;
		}
		
    	if (!this.databaseEntry.contacts.contains(packet.contactName)) {
			this.sendPacket(new PacketMessage(packet.contactName, "", String.format(
					"You may not send messages to %s", packet.contactName)));
    		return;
    	}
    	
		NetworkHandler netHandler = this.server.getHandlerByName(packet.contactName);
		if (netHandler == null || netHandler.databaseEntry.status == UserStatus.INVISIBLE) {
			this.sendPacket(new PacketMessage(packet.contactName, "", String.format(
					"%s is offline and may not receive messages", packet.contactName)));
			return;
		}
		
		netHandler.sendPacket(new PacketMessage(this.userName, this.userName, msg));
		this.sendPacket(new PacketMessage(packet.contactName, this.userName, msg));
    }
    
	public void handleAddContactPacket(PacketAddContact packet) {
		if (!packet.contactName.matches(NetHandlerUtils.USERNAME_REGEX_CHECK)) {
			this.sendPacket(new PacketInWindowPopup("Invalid contact name specified"));
			return;
		}
		
		if (packet.contactName.equals(this.userName)) {
			this.sendPacket(new PacketInWindowPopup("You may not add yourself to your contact list"));
			return;
		}
		
		if (this.databaseEntry.contacts.contains(packet.contactName)) {
			this.sendPacket(new PacketInWindowPopup(String.format(
					"%s is already on your contact list", packet.contactName)));
			return;
		}
		
		NetworkHandler netHandler = this.server.getHandlerByName(packet.contactName);
		if (netHandler == null || netHandler.databaseEntry.status == UserStatus.INVISIBLE) {
			this.sendPacket(new PacketInWindowPopup(String.format(
					"%s is offline and may not be added to your contact list", packet.contactName)));
			return;
		}
		
		netHandler.sendPacket(new PacketContactRequest(this.userName));
		this.sendPacket(new PacketInWindowPopup(String.format(
				"%s has been sent a request to be added on your contact list", packet.contactName)));
	}
    
	public void handleRemoveContactPacket(PacketRemoveContact packet) {
    	if (!this.databaseEntry.contacts.contains(packet.contactName)) {
    		this.kick("Protocol violation!");
    		return;
    	}
    	
		this.databaseEntry.contacts.remove(packet.contactName);
		this.databaseEntry.save();
		this.sendPacket(new PacketRemoveContact(packet.contactName));
		
		UserDatabaseEntry contactDatabaseEntry = new UserDatabaseEntry(this.server, packet.contactName);
		contactDatabaseEntry.load();
		contactDatabaseEntry.contacts.remove(this.userName);
		contactDatabaseEntry.save();
		
		NetworkHandler contactNetHandler = this.server.getHandlerByName(packet.contactName);
		if (contactNetHandler == null) {
			return;
		}
		contactNetHandler.databaseEntry = contactDatabaseEntry;
		contactNetHandler.sendPacket(new PacketRemoveContact(this.userName));
	}
	
	public void handleStatusPacket(PacketStatus packet) {
    	if (packet.status == UserStatus.OFFLINE) {
    		this.kick("Protocol violation!");
    		return;
    	}
    	this.changeStatus(packet.status, packet.motd, false);
	}
    
	public void handleContactRequestPacket(PacketContactRequest packet) {
		String[] contactNameSplitted = packet.contactName.split(":");
		String requester = contactNameSplitted[0];
		String answer = contactNameSplitted[1];
		String requesterNotification = "";
		NetworkHandler requesterNetHandler = this.server.getHandlerByName(requester);
		
		if (answer.equalsIgnoreCase("yes")) {
			if (this.databaseEntry.contacts.contains(requester)) {
				return;
			}
			
			this.databaseEntry.contacts.add(requester);
			this.databaseEntry.save();

			UserDatabaseEntry requesterDatabaseEntry = new UserDatabaseEntry(this.server, requester);
			requesterDatabaseEntry.load();
			requesterDatabaseEntry.contacts.add(this.userName);
			requesterDatabaseEntry.save();
			
			this.sendPacket(new PacketAddContact(requester, 
					requesterNetHandler == null ? UserStatus.OFFLINE : requesterNetHandler.getStatus(), 
					 requesterNetHandler == null ? "" : 
						 requesterNetHandler.isOnline() ? requesterNetHandler.motd : ""));	
			
			requesterNotification = String.format("%s has accepted your request", this.userName);
		} else {
			requesterNotification = String.format("%s has declined your request", this.userName);
		}

		if (requesterNetHandler != null) {
			if (answer.equalsIgnoreCase("yes")) {
				requesterNetHandler.databaseEntry.load();
				requesterNetHandler.sendPacket(new PacketAddContact(this.userName, this.getStatus(), 
						this.isOnline() ? this.motd : ""));
			}
			
			requesterNetHandler.sendPacket(new PacketInWindowPopup(requesterNotification));
		}
	}
	
	public void handleKeepAlivePacket() {
		this.noKeepAlivePacketTicks--;
	}
	
	public UserStatus getStatus() {
		return NetHandlerUtils.getToOthersStatus(this.databaseEntry.status);
	}
	
	public boolean isOnline() {
		return this.getStatus() != UserStatus.OFFLINE;
	}
}
