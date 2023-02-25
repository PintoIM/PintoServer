package me.vlod.pinto.networking;

import java.util.ArrayList;
import java.util.Arrays;

import me.vlod.pinto.Delegate;
import me.vlod.pinto.PintoServer;
import me.vlod.pinto.Utils;
import me.vlod.pinto.configuration.BannedConfig;
import me.vlod.pinto.configuration.MainConfig;
import me.vlod.pinto.configuration.MutedConfig;
import me.vlod.pinto.configuration.OperatorConfig;
import me.vlod.pinto.configuration.WhitelistConfig;
import me.vlod.pinto.consolehandler.ConsoleCaller;
import me.vlod.pinto.consolehandler.ConsoleHandler;

public class NetworkHandler {
	public static final String[] illegalUsernames = new String[] {
			"system",
			"admin",
			"administrator",
			"staff",
			"console",
			"owner",
			"servermanager",
			"manager",
			"boss",
			"moderation",
			"pinto",
			"support",
			"officialpinto",
			"pintoofficial"
	};
	private PintoServer server;
	public NetworkAddress networkAddress;
	public NetworkClient networkClient;
	public ConsoleHandler consoleHandler;
	public int noLoginKickTicks;
	public int autoMuteTicks;
	public int autoUnmuteTicks;
	public int amountOfChattingBeforeCooldownReset;
	public byte protocolVersion;
	public String userName;
	
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
		this.server.clients.remove(this);
		if (this.userName != null) {
			// Remove this user from the typing list
			this.server.removeUserFromTypingList(this.userName);
			this.server.sendGlobalMessage(this.userName + " has left");
		}
		PintoServer.logger.info(this.networkAddress + " has disconnected: " + reason);
	}
	
	public void onTick() {
		this.noLoginKickTicks++;
		if (MainConfig.instance.autoMute) {
			this.autoMuteTicks++;
			this.autoUnmuteTicks++;	
		} else {
			this.autoMuteTicks = 0;
			this.autoUnmuteTicks = 0;
			this.amountOfChattingBeforeCooldownReset = 0;
		}
		
		if (this.noLoginKickTicks > 10 && this.userName == null) {
			this.kick("No login packet received in an acceptable time frame!");
			return;
		}

		if (MainConfig.instance.autoMute && this.autoMuteTicks >= 3) {
			if (this.amountOfChattingBeforeCooldownReset <= 3) {
				this.amountOfChattingBeforeCooldownReset = 0;
			} else {
				this.server.muteUser(this.userName, "You are chatting too much!"
						+ " You will be unmuted in 15 seconds!", false);
				this.amountOfChattingBeforeCooldownReset = 0;
				this.autoUnmuteTicks = 0;
			}
			this.autoMuteTicks = 0;
		}
		
		String mutedReason = MutedConfig.instance.users.get(this.userName);
		if (MainConfig.instance.autoMute && this.autoUnmuteTicks >= 15 &&
			mutedReason != null && 
			mutedReason.equals("You are chatting too much! You will be unmuted in 15 seconds!")) {
			this.server.unmuteUser(this.userName, false);
		}
	}
	
	public void sendMessage(String message) {
		this.networkClient.sendPacket(new PacketMessage((byte)255, message));
	}
	
    public void kick(String reason) {
    	PintoServer.logger.info("Kicking " + this.networkAddress + 
    			(this.userName != null ? " (" + this.userName + ")" : "") + ": " + reason);
    	this.networkClient.sendPacket(new PacketLogout(reason));
    	this.networkClient.disconnect("Kicked -> " + reason);
    }
	
    public void syncTypingList(ArrayList<String> typingUsers) {
		String typingUsersStr = "";

		for (String user : typingUsers.toArray(new String[0])) {
			if (!user.equals(this.userName)) {
				typingUsersStr += user + ", ";
			}
		}
		
		typingUsersStr = Utils.replaceLast(typingUsersStr, ",", "").trim();
		this.networkClient.sendPacket(new PacketTyping(typingUsersStr));
    }
    
	public void handlePacket(Packet packet) {
        if (packet.getID() == 0) {
        	this.handleLoginPacket((PacketLogin)packet);
        } else if (packet.getID() == 2) {
            this.handleMessagePacket((PacketMessage)packet);
        } else if (packet.getID() == 3) {
        	this.handleTypingPacket((PacketTyping)packet);
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
    	if (!packet.name.matches("^(?=.{3,15}$)[a-z0-9._]+$") || 
    		Arrays.asList(illegalUsernames).contains(packet.name.toLowerCase()
    				.replace("_", "").replace(".", ""))) {
    		this.kick("Illegal username!\n"
    				+ "Legal usernames must have a length of at least 3 and at most 16\n"
    				+ "Legal usernames may only contain lowercase alphanumeric characters, underscores and dots");
    		return;
    	}
    	this.userName = packet.name;

    	// Send the MOTD packet
    	this.networkClient.sendPacket(new PacketLogin(this.protocolVersion, 
    			MainConfig.instance.serverName, MainConfig.instance.serverMOTD));
    	// Send the joined message
    	this.server.sendGlobalMessage(this.userName + " has joined");
    	
    	// Synchronize typing list
    	this.syncTypingList(this.server.typingUsers);
    }

    private void handleMessagePacket(PacketMessage packet) {
    	boolean isOperator = OperatorConfig.instance.users.contains(this.userName);
    	
    	// Check if the message is a command
		if (packet.message.startsWith("/")) {
			// If so, attempt to handle it
			if (isOperator) {
				PintoServer.logger.info(this.userName + " used " + packet.message);
				this.consoleHandler.handleInput(packet.message.replaceFirst("/", ""));
			} else {
				this.sendMessage("Only operators and the console may use commands!");
			}
		} else {
			// If not, attempt to send the message to everyone
			String mutedReason = MutedConfig.instance.users.get(this.userName);
			String mutedReasonIP = MutedConfig.instance.ips.get(this.networkAddress.ip); 
			
			// Prefer user name mute
			if (mutedReason != null) {
				this.sendMessage("You are muted in this chat!");
				this.sendMessage("Reason: " + mutedReason);
			} else if (mutedReasonIP != null) {
				this.sendMessage("You are muted in this chat based on your IP address!");
				this.sendMessage("Reason: " + mutedReasonIP);
			} else {
				this.server.sendGlobalMessage((isOperator ? "*" : "") + this.userName + " > " + packet.message);
				if (MainConfig.instance.autoMute) {
					this.amountOfChattingBeforeCooldownReset++;
				}
			}
		}
    }
    
    private void handleTypingPacket(PacketTyping packet) {
    	if (packet.usernames.length() > 0) {
    		this.server.addUserToTypingList(userName);
    	} else {
    		this.server.removeUserFromTypingList(userName);
    	}
    }
}
