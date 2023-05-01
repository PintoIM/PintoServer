package me.vlod.pinto.networking;

import me.vlod.pinto.Delegate;
import me.vlod.pinto.PintoServer;
import me.vlod.pinto.UserDatabaseEntry;
import me.vlod.pinto.UserStatus;
import me.vlod.pinto.consolehandler.ConsoleCaller;
import me.vlod.pinto.consolehandler.ConsoleHandler;
import me.vlod.pinto.networking.packet.Packet;
import me.vlod.pinto.networking.packet.PacketAddContact;
import me.vlod.pinto.networking.packet.PacketClearContacts;
import me.vlod.pinto.networking.packet.PacketContactRequest;
import me.vlod.pinto.networking.packet.PacketInWindowPopup;
import me.vlod.pinto.networking.packet.PacketLogin;
import me.vlod.pinto.networking.packet.PacketLogout;
import me.vlod.pinto.networking.packet.PacketMessage;
import me.vlod.pinto.networking.packet.PacketRegister;
import me.vlod.pinto.networking.packet.PacketRemoveContact;
import me.vlod.pinto.networking.packet.PacketStatus;

public class NetworkHandler {
	public static final int PROTOCOL_VERSION = 12;
	public static final int USERNAME_MAX = 16;
	private PintoServer server;
	public NetworkAddress networkAddress;
	public NetworkClient networkClient;
	public ConsoleHandler consoleHandler;
	public int noLoginKickTicks;
	public byte protocolVersion;
	public boolean loggedIn;
	public UserDatabaseEntry databaseEntry;
	public String userName;
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
	}
	
	private void onReceivedPacket(Packet packet) {
		this.handlePacket(packet);
	}
	
	private void onDisconnect(String reason) {
		this.changeStatus(UserStatus.OFFLINE, true);
		this.server.clients.remove(this);
		PintoServer.logger.info("%s has disconnected: %s", this.networkAddress, reason);
	}
	
	public void addToSendQueue(Packet packet) {
		if (packet.getID() != 255) {
			PintoServer.logger.info("Adding packet %s (%d) to %s (%s)'s send queue", 
					packet.getClass().getSimpleName().toUpperCase(),
					packet.getID(), this.networkAddress,
	    			(this.userName != null ? this.userName : "** UNAUTHENTICATED **"));	
		}
		this.networkClient.addToSendQueue(packet);
	}
	
	public void onTick() {
		this.noLoginKickTicks++;
		
		if (this.noLoginKickTicks > 10 && this.userName == null) {
			this.kick("No login packet received in an acceptable time frame!");
			return;
		}
		
		//this.addToSendQueue(new PacketShrimp());
	}

	private void performSync() {
		this.addToSendQueue(new PacketClearContacts());
		this.addToSendQueue(new PacketStatus("", this.databaseEntry.status));

		for (String contact : this.databaseEntry.contacts) {
			NetworkHandler netHandler = this.server.getHandlerByName(contact);
			this.addToSendQueue(new PacketAddContact(contact));
			
			if (netHandler != null) {
				this.addToSendQueue(new PacketStatus(contact, 
						NetHandlerUtils.getToOthersStatus(netHandler.databaseEntry.status)));
				netHandler.addToSendQueue(new PacketStatus(this.userName, this.databaseEntry.status));
			}
		}
	}
	
    public void kick(String reason) {
    	PintoServer.logger.info("Kicking %s (%s): %s", this.networkAddress,
    			(this.userName != null ? this.userName : "** UNAUTHENTICATED **"), reason);
    	this.networkClient.clearSendQueue();
    	this.addToSendQueue(new PacketLogout(reason));
    	this.networkClient.flushSendQueue();
    	this.networkClient.disconnect(String.format("Kicked (%s)", reason));
    }
    
    public void changeStatus(UserStatus status, boolean noSelfUpdate) {
    	if (this.databaseEntry == null) return;
    	
    	this.databaseEntry.status = status;
    	if (!noSelfUpdate) {
    		this.addToSendQueue(new PacketStatus("", status));
    		this.databaseEntry.save();
    	}

		for (String contact : this.databaseEntry.contacts) {
			NetworkHandler netHandler = this.server.getHandlerByName(contact);
			if (netHandler == null) continue;
			netHandler.addToSendQueue(new PacketStatus(this.userName,
					NetHandlerUtils.getToOthersStatus(status)));
		}
    }
    
	public void handlePacket(Packet packet) {
		PintoServer.logger.info("Received packet %s (%d) from %s (%s)", 
				packet.getClass().getSimpleName().toUpperCase(),
				packet.getID(), this.networkAddress,
    			(this.userName != null ? this.userName : "** UNAUTHENTICATED **"));
		packet.handle(this);
	}

	public void handleLoginPacket(PacketLogin packet) {
		NetHandlerUtils.performModerationChecks(this, packet.name);
		NetHandlerUtils.performProtocolCheck(this, packet.protocolVersion);
		NetHandlerUtils.performNameVerification(this, packet.name);
		
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
    		this.kick("This account doesn't exist! Please create one and try again.");
    		return;
    	}
    	
    	// Load the database entry
    	this.databaseEntry = new UserDatabaseEntry(this.server, this.userName);
    	this.databaseEntry.load();
    	
    	if (!this.databaseEntry.passwordHash.equalsIgnoreCase(packet.passwordHash)) {
    		this.kick("Invalid password!");
    		return;
    	}

    	// Mark the client as logged in
    	this.loggedIn = true;
    	
    	// Send the login packet to let the client know they have logged in
    	this.addToSendQueue(new PacketLogin(this.protocolVersion, "", ""));
    	PintoServer.logger.info("%s has logged in (Client version %s)", 
    			this.userName, this.clientVersion);
    	
    	// Sync the database to the user
    	this.performSync();
    }

	public void handleRegisterPacket(PacketRegister packet) {
		NetHandlerUtils.performModerationChecks(this, packet.name);
		NetHandlerUtils.performProtocolCheck(this, packet.protocolVersion);
		NetHandlerUtils.performNameVerification(this, packet.name);
		
    	this.protocolVersion = packet.protocolVersion;
    	this.clientVersion = packet.clientVersion;
    	this.userName = packet.name;

    	if (UserDatabaseEntry.isRegistered(this.server, userName)) {
    		this.kick("This account already exists!");
    		return;
    	}
    	
    	// Create the database entry
    	this.databaseEntry = UserDatabaseEntry.registerAndReturnEntry(this.server, packet.name,
    			packet.passwordHash, UserStatus.ONLINE);
    	
    	// Mark the client as logged in
    	this.loggedIn = true;
    	
    	// Send the login packet to let the client know they have logged in
    	this.addToSendQueue(new PacketLogin(this.protocolVersion, "", ""));
    	PintoServer.logger.info("%s has been registered and has logged in (Client version %s)", 
    			this.userName, this.clientVersion);
    }
	
	public void handleMessagePacket(PacketMessage packet) {
    	if (!this.databaseEntry.contacts.contains(packet.contactName)) {
			this.addToSendQueue(new PacketMessage(packet.contactName, "", String.format(
					"You may not send messages to %s", packet.contactName)));
    		return;
    	}
    	
		NetworkHandler netHandler = this.server.getHandlerByName(packet.contactName);
		if (netHandler == null || netHandler.databaseEntry.status == UserStatus.INVISIBLE) {
			this.addToSendQueue(new PacketMessage(packet.contactName, "", String.format(
					"%s is offline and may not receive messages", packet.contactName)));
			return;
		}
		
		netHandler.addToSendQueue(new PacketMessage(this.userName, this.userName, packet.message));
		this.addToSendQueue(new PacketMessage(packet.contactName, this.userName, packet.message));
    }
    
	public void handleAddContactPacket(PacketAddContact packet) {
		if (packet.contactName.equals(this.userName)) {
			this.addToSendQueue(new PacketInWindowPopup("You may not add yourself to your contact list"));
			return;
		}
		
		if (this.databaseEntry.contacts.contains(packet.contactName)) {
			this.addToSendQueue(new PacketInWindowPopup(String.format(
					"%s is already on your contact list", packet.contactName)));
			return;
		}
		
		NetworkHandler netHandler = this.server.getHandlerByName(packet.contactName);
		if (netHandler == null || netHandler.databaseEntry.status == UserStatus.INVISIBLE) {
			this.addToSendQueue(new PacketInWindowPopup(String.format(
					"%s is offline and may not be added to your contact list", packet.contactName)));
			return;
		}
		
		netHandler.addToSendQueue(new PacketContactRequest(this.userName));
		this.addToSendQueue(new PacketInWindowPopup(String.format(
				"%s has been sent a request to be added on your contact list", packet.contactName)));
	}
    
	public void handleRemoveContactPacket(PacketRemoveContact packet) {
    	if (!this.databaseEntry.contacts.contains(packet.contactName)) {
    		this.kick("Protocol violation!");
    		return;
    	}
    	
		this.databaseEntry.contacts.remove(packet.contactName);
		this.databaseEntry.save();
		this.performSync();
		
		UserDatabaseEntry contactDatabaseEntry = new UserDatabaseEntry(this.server, packet.contactName);
		contactDatabaseEntry.load();
		contactDatabaseEntry.contacts.remove(this.userName);
		contactDatabaseEntry.save();
		
		NetworkHandler contactNetHandler = this.server.getHandlerByName(packet.contactName);
		if (contactNetHandler == null) {
			return;
		}
		contactNetHandler.databaseEntry = contactDatabaseEntry;
		contactNetHandler.performSync();
	}
	
	public void handleStatusPacket(PacketStatus packet) {
    	if (packet.status == UserStatus.OFFLINE) {
    		this.kick("Protocol violation!");
    		return;
    	}
    	this.changeStatus(packet.status, false);
	}
    
	public void handleContactRequestPacket(PacketContactRequest packet) {
		String[] contactNameSplitted = packet.contactName.split(":");
		String requester = contactNameSplitted[0];
		String answer = contactNameSplitted[1];
		String requesterNotification = "";
		
		if (answer.equalsIgnoreCase("yes")) {
			this.databaseEntry.contacts.add(requester);
			this.databaseEntry.save();
			this.performSync();
			
			UserDatabaseEntry requesterDatabaseEntry = new UserDatabaseEntry(this.server, requester);
			requesterDatabaseEntry.load();
			requesterDatabaseEntry.contacts.add(this.userName);
			requesterDatabaseEntry.save();
			
			requesterNotification = String.format("%s has accepted your request", this.userName);
		} else {
			requesterNotification = String.format("%s has declined your request", this.userName);
		}
		
		NetworkHandler requesterNetHandler = this.server.getHandlerByName(requester);
		if (requesterNetHandler == null) {
			return;
		}
		requesterNetHandler.databaseEntry.load();
		requesterNetHandler.performSync();
		requesterNetHandler.addToSendQueue(new PacketInWindowPopup(requesterNotification));
	}
	
	/*
	public void handleCallStartPacket(PacketCallStart packet) {
		NetworkHandler contactNetHandler = this.server.getHandlerByName(packet.contactName);
		
		if (contactNetHandler == null) {
			this.addToSendQueue(new PacketInWindowPopup(String.format(
					"%s is offline and may not receive calls", packet.contactName)));
			this.addToSendQueue(new PacketCallEnd());
			return;
		}
		
		contactNetHandler.addToSendQueue(new PacketCallRequest(this.userName));
	}
	
	public void handleCallRequestPacket(PacketCallRequest packet) {
		String[] contactNameSplitted = packet.contactName.split(":");
		String target = contactNameSplitted[0];
		String answer = contactNameSplitted[1];
		NetworkHandler targetNetHandler = this.server.getHandlerByName(target);

		if (answer.equalsIgnoreCase("yes")) {
			this.inCall = true;
			this.inCallWith = target;
			targetNetHandler.inCall = true;
			targetNetHandler.inCallWith = this.userName;
		} else if (answer.equalsIgnoreCase("no")) {
			targetNetHandler.addToSendQueue(new PacketCallEnd());
		}
	}

	public void handleCallPartyInfoPacket(PacketCallPartyInfo packet) {
		// FIXME: Make this actually work
		NetworkHandler targetNetHandler = this.server.getHandlerByName(this.inCallWith);
		if (targetNetHandler == null) {
			return;
		}
		targetNetHandler.addToSendQueue(new PacketCallPartyInfo(this.networkAddress.ip, packet.port));
	}

	public void handleCallEndPacket(PacketCallEnd packet) {
		NetworkHandler targetNetHandler = this.server.getHandlerByName(this.inCallWith);
		
		this.inCall = false;
		this.inCallWith = null;
		
		if (targetNetHandler == null) {
			return;
		}
		targetNetHandler.addToSendQueue(new PacketCallEnd());
		targetNetHandler.inCall = false;
		targetNetHandler.inCallWith = null;
	}*/
}
