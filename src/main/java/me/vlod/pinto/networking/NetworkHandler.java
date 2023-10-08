package me.vlod.pinto.networking;

import me.vlod.pinto.CallStatus;
import me.vlod.pinto.ClientUpdateCheck;
import me.vlod.pinto.Coloring;
import me.vlod.pinto.Delegate;
import me.vlod.pinto.GroupDatabaseEntry;
import me.vlod.pinto.PintoServer;
import me.vlod.pinto.UserDatabaseEntry;
import me.vlod.pinto.UserStatus;
import me.vlod.pinto.Utils;
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
import me.vlod.pinto.networking.packet.PacketCallChangeStatus;
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
import me.vlod.pinto.networking.packet.PacketSetOption;
import me.vlod.pinto.networking.packet.PacketStatus;
import me.vlod.pinto.networking.packet.PacketTyping;

public class NetworkHandler {
	public static final int PROTOCOL_VERSION = 3;
	public static final int USERNAME_MAX = 16;
	public static final int MESSAGE_RATE_LIMIT_TIME = 1;
	private PintoServer server;
	public NetworkClient networkClient;
	public NetworkAddress networkAddress;
	public int noLoginKickTicks;
	public int ticksTillNextKeepAlive;
	public int noKeepAlivePacketTicks;
	public int messageRateLimitTicks;
	public byte protocolVersion;
	public boolean hasDisconnected;
	public UserDatabaseEntry databaseEntry;
	public String userName;
	public String motd = "";
	public String clientVersion;
	public ConsoleHandler consoleHandler;
	public boolean inCall;
	public String inCallWith;
	public int callHostPort;

	public NetworkHandler(PintoServer server, NetworkClient client) {
		this.server = server;
		this.networkClient = client;
		this.networkAddress = this.networkClient.networkAddress;
		
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
		this.hasDisconnected = true;
		this.changeStatus(UserStatus.OFFLINE, "", true);
		this.server.clients.remove(this);
		PintoServer.logger.info("%s has disconnected: %s", this.networkAddress, reason.replace("\n", "\\n"));
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
		if (this.hasDisconnected) {
			PintoServer.logger.warn("Still ticking disconnected client! (%s)", this.networkAddress);
			this.server.clients.remove(this);
			return;
		}
		
		this.noLoginKickTicks++;
		if (this.noLoginKickTicks > 6 && this.userName == null) {
			this.kick("No login packet received in an acceptable time frame!");
			return;
		}
		
		if (this.noKeepAlivePacketTicks > 2) {
			this.kick("Timed out");
			return;
		}
		
		this.ticksTillNextKeepAlive++;
		if (this.ticksTillNextKeepAlive >= 5) {
			this.sendPacket(new PacketKeepAlive());
			this.noKeepAlivePacketTicks++;	
			this.ticksTillNextKeepAlive = 0;
		}
		
		if (this.messageRateLimitTicks > 0) {
			this.messageRateLimitTicks--;
		}
	}

	private void performSync() {
		this.sendPacket(new PacketClearContacts());
		this.sendPacket(new PacketStatus("", this.databaseEntry.status, ""));

		for (String contact : this.databaseEntry.contacts) {
			if (NetHandlerUtils.isUserGroup(contact)) {
				this.sendPacket(new PacketAddContact(contact, UserStatus.ONLINE, "Pinto! Group"));
				continue;
			}
			
			NetworkHandler netHandler = this.server.getHandlerByName(contact);
			this.sendPacket(new PacketAddContact(contact, 
					netHandler == null ? UserStatus.OFFLINE : netHandler.getStatus(),
					netHandler == null ? "" : 
						netHandler.isOnline() ? netHandler.motd : ""));
			
			if (netHandler != null && 
				NetHandlerUtils.getToOthersStatus(this.databaseEntry.status) != UserStatus.OFFLINE) {
				netHandler.sendPacket(new PacketStatus(this.userName, this.getStatus(), ""));
			}
		}
		
		for (String contactRequest : this.databaseEntry.contactRequests) {
			this.sendPacket(new PacketContactRequest(contactRequest));
		}
	}

    public void kick(String reason) {
    	PintoServer.logger.info("Kicking %s (%s): %s", this.networkAddress,
    			(this.userName != null ? this.userName : "** UNAUTHENTICATED **"), 
    			reason.replace("\n", "\\n"));
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
			if (NetHandlerUtils.isUserGroup(contact)) continue;
			NetworkHandler netHandler = this.server.getHandlerByName(contact);
			if (netHandler == null) continue;
			netHandler.sendPacket(new PacketStatus(this.userName,
					this.getStatus(), 
					this.isOnline() ? this.motd : ""));
		}
    }

    private void finishLogin() {
    	// Send the login packet to let the client know they have logged in
    	this.sendPacket(new PacketLogin(this.protocolVersion, "", ""));
    	PintoServer.logger.info("%s has logged in (Client version %s)", 
    			this.userName, this.clientVersion);
    	
    	// Send the user our server ID
    	this.sendPacket(new PacketServerID(MainConfig.instance.serverID));
    	
    	// Sync the database to the user
    	this.performSync();
    	
    	// Create the console handler for the user
    	this.consoleHandler = new ConsoleHandler(this.server, false);
    	
    	// Check if the client is not the latest to send a notice
    	if (!MainConfig.instance.ignoreClientVersion &&
    		!ClientUpdateCheck.isLatest(this.clientVersion)) {
    		this.sendPacket(new PacketInWindowPopup("Your client version is not the latest,"
    				+ " upgrade to the latest version to get the most recent features and bug fixes!", true));
    		this.sendPacket(new PacketPopup("Client outdated", "Your client version is not the latest,"
    				+ " upgrade to the latest version to get the most recent features and bug fixes!"));
        	PintoServer.logger.warn("%s has an older client than the latest!", 
        			this.userName, this.clientVersion);
    	}
    	
    	// Experimental features
    	if (MainConfig.instance.enableExperimentsToUsers.contains(this.userName)) {
    		this.sendPacket(new PacketSetOption("exp_calls", "1"));
    	}
    	
    	// Send a heart beat with the updated users count
    	this.server.sendHeartbeat();
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
    	
    	// Finish logging in the client
    	this.finishLogin();
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
    	
    	// Finish logging in the client
    	this.finishLogin();
    }
	
	public void handleMessagePacket(PacketMessage packet) {
		String msg = packet.message.trim();
		boolean receiverIsGroup = NetHandlerUtils.isUserGroup(packet.contactName);
		
		if (msg.isEmpty()) {
			this.sendPacket(new PacketMessage(packet.contactName, "",
					Coloring.translateAlternativeColoringCodes(
							"&8[&5!&8]&4 You may not send empty messages!")));
			return;
		}
		
    	if (!this.databaseEntry.contacts.contains(packet.contactName) || this.getStatus() == UserStatus.OFFLINE) {
			this.sendPacket(new PacketMessage(packet.contactName, "", 
					Coloring.translateAlternativeColoringCodes(String.format(
							"&8[&5!&8]&4 You may not send messages to %s", packet.contactName))));
    		return;
    	}
    	
		if (this.messageRateLimitTicks > 0) {
			this.kick("Illegal operation!");
			return;
		}
    	
		if (!receiverIsGroup) {
			NetworkHandler netHandler = this.server.getHandlerByName(packet.contactName);
			if (netHandler == null || netHandler.getStatus() == UserStatus.OFFLINE) {
				this.sendPacket(new PacketMessage(packet.contactName, "", 
						Coloring.translateAlternativeColoringCodes(String.format(
								"&8[&5!&8]&4 %s is offline and may not receive messages", packet.contactName))));
				return;
			}

			netHandler.sendPacket(new PacketMessage(this.userName, this.userName, msg));
			this.sendPacket(new PacketMessage(packet.contactName, this.userName, msg));
		} else {
			if (msg.startsWith("/")) {
				msg = msg.replaceFirst("\\/", "");
				this.consoleHandler.handleInput(msg, new ConsoleCaller(this, packet.contactName));
			} else {
				this.server.sendMessageInGroup(packet.contactName, this.userName, msg);
			}
		}
		
		this.messageRateLimitTicks = NetworkHandler.MESSAGE_RATE_LIMIT_TIME;
    }
    
	public void handleAddContactPacket(PacketAddContact packet) {
		if (this.databaseEntry.contacts.size() + 1 > 500) {
			this.sendPacket(new PacketInWindowPopup("You have reached the limit of 500 contacts"));
			return;
		}
		
		if (packet.contactName.equalsIgnoreCase("G:new")) {
			String groupID = Utils.getPintoGroupID();
			
			GroupDatabaseEntry groupDatabaseEntry = GroupDatabaseEntry.createAndReturnEntry(this.server, groupID);
			groupDatabaseEntry.members.add(this.userName);
			groupDatabaseEntry.save();
			
			this.databaseEntry.contacts.add(groupID);
			this.databaseEntry.save();
			this.sendPacket(new PacketAddContact(groupID, UserStatus.ONLINE, "Pinto! Group"));	
		} else {
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
						"%s is already on your contact list", packet.contactName), true));
				return;
			}
			
			if (!UserDatabaseEntry.isRegistered(this.server, packet.contactName)) {
				this.sendPacket(new PacketInWindowPopup(String.format(
						"%s is not a registered user", packet.contactName)));
				return;
			}
			
			UserDatabaseEntry userDatabaseEntry = new UserDatabaseEntry(this.server, packet.contactName);
			userDatabaseEntry.load();
			
			if (userDatabaseEntry.contacts.size() + 1 > 500) {
				this.sendPacket(new PacketInWindowPopup(
						String.format("%s has reached the 500 contacts limit", packet.contactName)));
				return;
			}
			
			if (userDatabaseEntry.contactRequests.contains(this.userName)) {
				this.sendPacket(new PacketInWindowPopup(
						String.format("You have already sent %s a contact request", packet.contactName), true));
				return;
			}
			
			userDatabaseEntry.contactRequests.add(this.userName);
			userDatabaseEntry.save();

			NetworkHandler netHandler = this.server.getHandlerByName(packet.contactName);
			if (netHandler != null) {
				netHandler.databaseEntry.load();
				netHandler.sendPacket(new PacketContactRequest(this.userName));
			}

			this.sendPacket(new PacketInWindowPopup(String.format(
					"%s has been sent a request to be added on your contact list", packet.contactName), true));
		}
	}
    
	public void handleRemoveContactPacket(PacketRemoveContact packet) {
    	if (!this.databaseEntry.contacts.contains(packet.contactName)) {
    		this.kick("Illegal operation!");
    		return;
    	}
    	
		this.databaseEntry.contacts.remove(packet.contactName);
		this.databaseEntry.save();
		this.sendPacket(new PacketRemoveContact(packet.contactName));
		
		if (NetHandlerUtils.isUserGroup(packet.contactName)) {
			GroupDatabaseEntry groupDatabaseEntry = new GroupDatabaseEntry(this.server, packet.contactName);
			groupDatabaseEntry.load();
			groupDatabaseEntry.members.remove(this.userName);
			groupDatabaseEntry.save();
		} else {
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
	}
	
	public void handleStatusPacket(PacketStatus packet) {
    	if (packet.status == UserStatus.OFFLINE) {
    		this.kick("Illegal operation!");
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
		
		if (!this.databaseEntry.contactRequests.contains(requester)) {
			this.kick("Protocol violation!");
			return;
		}
		
		this.databaseEntry.contactRequests.remove(requester);
		this.databaseEntry.save();
		
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
			this.sendPacket(new PacketInWindowPopup(
					String.format("You are now contacts with %s", requester), true));
			
			requesterNotification = String.format("%s has accepted your request", this.userName, true);
		} else {
			requesterNotification = String.format("%s has declined your request", this.userName);
		}
		
		if (requesterNetHandler != null) {
			if (answer.equalsIgnoreCase("yes")) {
				requesterNetHandler.databaseEntry.load();
				requesterNetHandler.sendPacket(new PacketAddContact(this.userName, this.getStatus(), 
						this.isOnline() ? this.motd : ""));
			}
			
			requesterNetHandler.sendPacket(new PacketInWindowPopup(requesterNotification, true));
		}
	}
	
	public void handleTypingPacket(PacketTyping packet) {
		if (NetHandlerUtils.isUserGroup(packet.contactName)) {
			return;
		}
		
		if (packet.contactName == this.userName) {
			this.kick("Illegal operation!");
			return;
		}
		
		NetworkHandler netHandler = this.server.getHandlerByName(packet.contactName);
		if (netHandler == null || netHandler.getStatus() == UserStatus.OFFLINE) {
			return;
		}
		
    	if (!this.databaseEntry.contacts.contains(packet.contactName)) {
    		return;
    	}
		
		netHandler.sendPacket(new PacketTyping(this.userName, packet.state));
	}
	
	public void handleKeepAlivePacket() {
		this.noKeepAlivePacketTicks--;
	}
	
	public void handleCallChangeStatusPacket(PacketCallChangeStatus packet) {
		NetworkHandler otherUser = null;

		PintoServer.logger.info("%s changed their call status to %s (%s)", 
				this.userName, packet.callStatus, packet.details);
		
		switch (packet.callStatus) {
		case CONNECTING:
			if (!MainConfig.instance.enableExperimentsToUsers.contains(this.userName)) {
				this.sendPacket(new PacketCallChangeStatus(CallStatus.ERROR, 
						"Feature unavailable"));
				return;
			}
			
			if (!packet.details.contains("@")) {
				this.kick("Protocol violation!");
				return;
			}

			String[] detailsSplit = packet.details.split("@");
			String callTarget = detailsSplit[0];
			String upnpIP = detailsSplit[1];
			String portRaw = detailsSplit[2];
			otherUser = this.server.getHandlerByName(callTarget);
			
			if (NetHandlerUtils.isUserGroup(callTarget)) {
				this.sendPacket(new PacketCallChangeStatus(CallStatus.ERROR, 
						"You may not call groups!"));
				return;
			}
			
			if (otherUser == null) {
				this.sendPacket(new PacketCallChangeStatus(CallStatus.ERROR, 
						"The specified user is offline"));
				return;
			}
			
			if (otherUser.inCall) {
				this.sendPacket(new PacketCallChangeStatus(CallStatus.ERROR, 
						"The specified user is already in a call"));
				return;
			}
			
			this.inCall = true;
			this.inCallWith = callTarget;
			try {				
				this.callHostPort = Integer.valueOf(portRaw);
			} catch (Exception ex) {
				this.kick("Protocol violation!");
				return;
			}
			
			otherUser.inCall = true;
			otherUser.inCallWith = this.userName;
			otherUser.sendPacket(new PacketCallChangeStatus(CallStatus.CONNECTING, 
					String.format("%s@%s@%d", this.userName, upnpIP, this.callHostPort)));
			
			break;
		case ENDED:
			otherUser = this.server.getHandlerByName(this.inCallWith);
			
			this.inCall = false;
			this.inCallWith = null;
			this.callHostPort = 0;
			
			if (otherUser == null) {
				PintoServer.logger.warn("%s attempted to end call with offline user!", this.userName);
				return;
			}
			
			if (!otherUser.inCall) {
				return;
			}
			
			otherUser.inCall = false;
			otherUser.inCallWith = null;
			otherUser.callHostPort = 0;
			otherUser.sendPacket(new PacketCallChangeStatus(CallStatus.ENDED, ""));

			break;
		default:
			break;
		}
	}
	
	public UserStatus getStatus() {
		return NetHandlerUtils.getToOthersStatus(this.databaseEntry.status);
	}
	
	public UserStatus getActualStatus() {
		return this.databaseEntry.status;
	}
	
	public boolean isOnline() {
		return this.getStatus() != UserStatus.OFFLINE;
	}
}
