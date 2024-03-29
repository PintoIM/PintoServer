package org.pintoim.pinto.networking;

import java.nio.charset.StandardCharsets;

import org.pintoim.pinto.Coloring;
import org.pintoim.pinto.GroupDatabaseEntry;
import org.pintoim.pinto.PintoServer;
import org.pintoim.pinto.UserDatabaseEntry;
import org.pintoim.pinto.UserStatus;
import org.pintoim.pinto.Utils;
import org.pintoim.pinto.consolehandler.ConsoleCaller;
import org.pintoim.pinto.networking.packet.PacketAddContact;
import org.pintoim.pinto.networking.packet.PacketCallChangeStatus;
import org.pintoim.pinto.networking.packet.PacketContactRequest;
import org.pintoim.pinto.networking.packet.PacketKeepAlive;
import org.pintoim.pinto.networking.packet.PacketLogout;
import org.pintoim.pinto.networking.packet.PacketMessage;
import org.pintoim.pinto.networking.packet.PacketNotification;
import org.pintoim.pinto.networking.packet.PacketRemoveContact;
import org.pintoim.pinto.networking.packet.PacketStatus;
import org.pintoim.pinto.networking.packet.PacketTyping;

public class NetServerPacketsHandler {
	private PintoServer instance;
	private NetServerHandler netHandler;
	
	public NetServerPacketsHandler(PintoServer instance, NetServerHandler netHandler) {
		this.instance = instance;
		this.netHandler = netHandler;
	}
	
	public void handleMessagePacket(PacketMessage packet) {
		PMSGMessage msg = packet.payload;
		String msgText = msg.headers.get("Content-Type").equals("pinto/text") ? new String(msg.data, StandardCharsets.UTF_16BE) : null;
		boolean receiverIsGroup = NetUtils.isUserGroup(packet.contactName);

    	if (!this.netHandler.databaseEntry.contacts.contains(packet.contactName) || this.netHandler.getStatus() == UserStatus.OFFLINE) {
			this.netHandler.sendPacket(new PacketMessage(packet.contactName, "", 
					new PMSGMessage(Coloring.translateAlternativeColoringCodes(String.format(
							"&8[&5!&8]&4 You may not send messages to %s", packet.contactName)))));
    		return;
    	}
    	
		if (this.netHandler.messageRateLimitTicks > 0) {
			this.netHandler.kick("Illegal operation!");
			return;
		}
    	
		if (!receiverIsGroup) {
			NetServerHandler netHandler = this.instance.getHandlerByName(packet.contactName);
			if (netHandler == null || netHandler.getStatus() == UserStatus.OFFLINE) {
				this.netHandler.sendPacket(new PacketMessage(packet.contactName, "", 
						new PMSGMessage(Coloring.translateAlternativeColoringCodes(String.format(
								"&8[&5!&8]&4 %s is offline and may not receive messages", packet.contactName)))));
				return;
			}

			netHandler.sendPacket(new PacketMessage(this.netHandler.userName, this.netHandler.userName, msg));
			this.netHandler.sendPacket(new PacketMessage(packet.contactName, this.netHandler.userName, msg));
		} else {
			if (msgText != null && msgText.startsWith("/")) {
				msgText = msgText.replaceFirst("\\/", "");
				this.netHandler.consoleHandler.handleInput(msgText, new ConsoleCaller(this.netHandler, packet.contactName));
			} else {
				this.instance.sendMessageInGroup(packet.contactName, this.netHandler.userName, msg);
			}
		}
		
		this.netHandler.messageRateLimitTicks = NetServerHandler.MESSAGE_RATE_LIMIT_TIME;
    }
    
	public void handleAddContactPacket(PacketAddContact packet) {
		if (this.netHandler.databaseEntry.contacts.size() + 1 > 500) {
			this.netHandler.sendPacket(new PacketNotification(0, -1, "", "You have reached the limit of 500 contacts"));
			return;
		}
		
		if (packet.contactName.equalsIgnoreCase("G:new")) {
			String groupID = Utils.getPintoGroupID();
			
			GroupDatabaseEntry groupDatabaseEntry = GroupDatabaseEntry.createAndReturnEntry(this.instance, groupID);
			groupDatabaseEntry.members.add(this.netHandler.userName);
			groupDatabaseEntry.save();
			
			this.netHandler.databaseEntry.contacts.add(groupID);
			this.netHandler.databaseEntry.save();
			this.netHandler.sendPacket(new PacketAddContact(groupID, UserStatus.ONLINE, "Pinto! Group"));	
		} else {
			if (!packet.contactName.matches(NetUtils.USERNAME_REGEX_CHECK)) {
				this.netHandler.sendPacket(new PacketNotification(0, -1, "", "Invalid contact name specified"));
				return;
			}
			
			if (packet.contactName.equals(this.netHandler.userName)) {
				this.netHandler.sendPacket(new PacketNotification(0, -1, "", "You may not add yourself to your contact list"));
				return;
			}
			
			if (this.netHandler.databaseEntry.contacts.contains(packet.contactName)) {
				this.netHandler.sendPacket(new PacketNotification(1, -1, "", String.format(
						"%s is already on your contact list", packet.contactName)));
				return;
			}
			
			if (!UserDatabaseEntry.isRegistered(this.instance, packet.contactName)) {
				this.netHandler.sendPacket(new PacketNotification(0, -1, "", String.format(
						"%s is not a registered user", packet.contactName)));
				return;
			}
			
			UserDatabaseEntry userDatabaseEntry = new UserDatabaseEntry(this.instance, packet.contactName);
			userDatabaseEntry.load();
			
			if (userDatabaseEntry.contacts.size() + 1 > 500) {
				this.netHandler.sendPacket(new PacketNotification(0, -1, "", 
						String.format("%s has reached the 500 contacts limit", packet.contactName)));
				return;
			}
			
			if (userDatabaseEntry.contactRequests.contains(this.netHandler.userName)) {
				this.netHandler.sendPacket(new PacketNotification(1, -1, "",  
						String.format("You have already sent %s a contact request", packet.contactName)));
				return;
			}
			
			userDatabaseEntry.contactRequests.add(this.netHandler.userName);
			userDatabaseEntry.save();

			NetServerHandler netHandler = this.instance.getHandlerByName(packet.contactName);
			if (netHandler != null) {
				netHandler.databaseEntry.load();
				netHandler.sendPacket(new PacketContactRequest(this.netHandler.userName));
			}

			this.netHandler.sendPacket(new PacketNotification(1, -1, "", String.format( 
					"%s has been sent a request to be added on your contact list", packet.contactName)));
		}
	}
    
	public void handleRemoveContactPacket(PacketRemoveContact packet) {
    	if (!this.netHandler.databaseEntry.contacts.contains(packet.contactName)) {
    		this.netHandler.kick("Illegal operation!");
    		return;
    	}
    	
		this.netHandler.databaseEntry.contacts.remove(packet.contactName);
		this.netHandler.databaseEntry.save();
		this.netHandler.sendPacket(new PacketRemoveContact(packet.contactName));
		
		if (NetUtils.isUserGroup(packet.contactName)) {
			GroupDatabaseEntry groupDatabaseEntry = new GroupDatabaseEntry(this.instance, packet.contactName);
			groupDatabaseEntry.load();
			groupDatabaseEntry.members.remove(this.netHandler.userName);
			groupDatabaseEntry.save();
		} else {
			UserDatabaseEntry contactDatabaseEntry = new UserDatabaseEntry(this.instance, packet.contactName);
			contactDatabaseEntry.load();
			contactDatabaseEntry.contacts.remove(this.netHandler.userName);
			contactDatabaseEntry.save();
			
			NetServerHandler contactNetHandler = this.instance.getHandlerByName(packet.contactName);
			if (contactNetHandler == null) {
				return;
			}
			contactNetHandler.databaseEntry = contactDatabaseEntry;
			contactNetHandler.sendPacket(new PacketRemoveContact(this.netHandler.userName));
		}
	}
	
	public void handleStatusPacket(PacketStatus packet) {
    	if (packet.status == UserStatus.OFFLINE) {
    		this.netHandler.kick("Illegal operation!");
    		return;
    	}
    	this.netHandler.changeStatus(packet.status, packet.motd, false);
	}
    
	public void handleContactRequestPacket(PacketContactRequest packet) {
		String[] contactNameSplitted = packet.contactName.split(":");
		String requester = contactNameSplitted[0];
		String answer = contactNameSplitted[1];
		String requesterNotification = "";
		NetServerHandler requesterNetHandler = this.instance.getHandlerByName(requester);
		
		if (!this.netHandler.databaseEntry.contactRequests.contains(requester)) {
			this.netHandler.kick("Protocol violation!");
			return;
		}
		
		this.netHandler.databaseEntry.contactRequests.remove(requester);
		this.netHandler.databaseEntry.save();
		
		if (answer.equalsIgnoreCase("yes")) {
			if (this.netHandler.databaseEntry.contacts.contains(requester)) {
				return;
			}
			
			this.netHandler.databaseEntry.contacts.add(requester);
			this.netHandler.databaseEntry.save();

			UserDatabaseEntry requesterDatabaseEntry = new UserDatabaseEntry(this.instance, requester);
			requesterDatabaseEntry.load();
			requesterDatabaseEntry.contacts.add(this.netHandler.userName);
			requesterDatabaseEntry.save();
			
			this.netHandler.sendPacket(new PacketAddContact(requester, 
					requesterNetHandler == null ? UserStatus.OFFLINE : requesterNetHandler.getStatus(), 
					 requesterNetHandler == null ? "" : 
						 requesterNetHandler.isOnline() ? requesterNetHandler.motd : ""));	
			this.netHandler.sendPacket(new PacketNotification(1, -1, "", 
					String.format("You are now contacts with %s", requester)));
			
			requesterNotification = String.format("%s has accepted your request", this.netHandler.userName, true);
		} else {
			requesterNotification = String.format("%s has declined your request", this.netHandler.userName);
		}
		
		if (requesterNetHandler != null) {
			if (answer.equalsIgnoreCase("yes")) {
				requesterNetHandler.databaseEntry.load();
				requesterNetHandler.sendPacket(new PacketAddContact(this.netHandler.userName, this.netHandler.getStatus(), 
						this.netHandler.isOnline() ? this.netHandler.motd : ""));
			}
			
			requesterNetHandler.sendPacket(new PacketNotification(1, -1, "", requesterNotification));
		}
	}
	
	public void handleTypingPacket(PacketTyping packet) {
		if (NetUtils.isUserGroup(packet.contactName)) {
			return;
		}
		
		if (packet.contactName == this.netHandler.userName) {
			this.netHandler.kick("Illegal operation!");
			return;
		}
		
		NetServerHandler netHandler = this.instance.getHandlerByName(packet.contactName);
		if (netHandler == null || netHandler.getStatus() == UserStatus.OFFLINE) {
			return;
		}
		
    	if (!this.netHandler.databaseEntry.contacts.contains(packet.contactName)) {
    		return;
    	}
		
		netHandler.sendPacket(new PacketTyping(this.netHandler.userName, packet.state));
	}
	
	public void handleKeepAlivePacket(PacketKeepAlive packet) {
		this.netHandler.noKeepAlivePacketTicks--;
	}
	
	public void handleCallChangeStatusPacket(PacketCallChangeStatus packet) {
	}
	
	public void handleLogoutPacket(PacketLogout packet) {
		this.netHandler.netManager.shutdown("Leaving...");
	}
}
