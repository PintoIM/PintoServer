package org.pintoim.pinto.networking;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.pintoim.pinto.ClientUpdateCheck;
import org.pintoim.pinto.PintoServer;
import org.pintoim.pinto.UserDatabaseEntry;
import org.pintoim.pinto.UserStatus;
import org.pintoim.pinto.Utils;
import org.pintoim.pinto.configuration.MainConfig;
import org.pintoim.pinto.consolehandler.ConsoleHandler;
import org.pintoim.pinto.networking.packet.Packet;
import org.pintoim.pinto.networking.packet.PacketAddContact;
import org.pintoim.pinto.networking.packet.PacketClearContacts;
import org.pintoim.pinto.networking.packet.PacketContactRequest;
import org.pintoim.pinto.networking.packet.PacketKeepAlive;
import org.pintoim.pinto.networking.packet.PacketLogin;
import org.pintoim.pinto.networking.packet.PacketNotification;
import org.pintoim.pinto.networking.packet.PacketServerInfo;
import org.pintoim.pinto.networking.packet.PacketStatus;

public class NetServerHandler extends NetBaseHandler {
	private PintoServer instance;
	private NetServerPacketsHandler packetsHandler;
	public UserDatabaseEntry databaseEntry;
	public String clientVersion;
	public String motd = "";
	protected int ticksTillNextKeepAlive;
	protected int noKeepAlivePacketTicks;
	protected int messageRateLimitTicks;
	protected ConsoleHandler consoleHandler;
	
	public NetServerHandler(PintoServer instance, NetworkManager netManager, 
			String userName, UserDatabaseEntry databaseEntry, String clientVersion) {
		this.instance = instance;
		this.netManager = netManager;
		this.userName = userName;
		this.databaseEntry = databaseEntry;
		this.clientVersion = clientVersion;
		this.netManager.setNetHandler(this);
		this.instance.clients.add(this);
		this.finishLogin();
	}
	
	public UserStatus getStatus() {
		return NetUtils.getToOthersStatus(this.databaseEntry.status);
	}
	
	public UserStatus getActualStatus() {
		return this.databaseEntry.status;
	}
	
	public boolean isOnline() {
		return this.getStatus() != UserStatus.OFFLINE;
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
			if (NetUtils.isUserGroup(contact)) continue;
			NetServerHandler netHandler = this.instance.getHandlerByName(contact);
			if (netHandler == null) continue;
			netHandler.sendPacket(new PacketStatus(this.userName,
					this.getStatus(), 
					this.isOnline() ? this.motd : ""));
		}
    }
	
	public void synchronize() {
		this.sendPacket(new PacketClearContacts());
		this.sendPacket(new PacketStatus("", this.databaseEntry.status, ""));

		for (String contact : this.databaseEntry.contacts) {
			if (NetUtils.isUserGroup(contact)) {
				this.sendPacket(new PacketAddContact(contact, UserStatus.ONLINE, "Pinto! Group"));
				continue;
			}
			
			NetServerHandler netHandler = this.instance.getHandlerByName(contact);
			this.sendPacket(new PacketAddContact(contact, 
					netHandler == null ? UserStatus.OFFLINE : netHandler.getStatus(),
					netHandler == null ? "" : 
						netHandler.isOnline() ? netHandler.motd : ""));
			
			if (netHandler != null && 
				NetUtils.getToOthersStatus(this.databaseEntry.status) != UserStatus.OFFLINE) {
				netHandler.sendPacket(new PacketStatus(this.userName, this.getStatus(), ""));
			}
		}
		
		for (String contactRequest : this.databaseEntry.contactRequests) {
			this.sendPacket(new PacketContactRequest(contactRequest));
		}
	}
	
    private void finishLogin() {
    	this.packetsHandler = new NetServerPacketsHandler(this.instance, this);
    	this.consoleHandler = new ConsoleHandler(this.instance, false);
    	
    	this.sendPacket(new PacketLogin(NetBaseHandler.PROTOCOL_VERSION, "", ""));
    	this.sendPacket(new PacketServerInfo(MainConfig.instance.serverID, PintoServer.SOFTWARE_INFO));
    	this.synchronize();

    	this.instance.sendHeartbeat();
    	PintoServer.logger.info("%s has logged in (Client version %s)", 
    			this.userName, this.clientVersion);
    	
    	if (!MainConfig.instance.ignoreClientVersion &&
    		!ClientUpdateCheck.isLatest(this.clientVersion)) {
    		this.sendPacket(new PacketNotification(1, 0, "", "Your client version is not the latest,"
    				+ " upgrade to the latest version to get the most recent features and bug fixes!"));
    		this.sendPacket(new PacketNotification(2, 0, "Client outdated", "Your client version is not the latest,"
    				+ " upgrade to the latest version to get the most recent features and bug fixes!"));
        	PintoServer.logger.warn("%s has an older client than the latest!", 
        			this.userName, this.clientVersion);
    	}
    }
    
    @Override
    protected void onUpdate() throws IOException {
    	this.netManager.processReceivedPackets();
    	this.netManager.interrupt();
		if (this.noKeepAlivePacketTicks > 2) {
			this.kick("Timed out");
			return;
		}
		
		this.ticksTillNextKeepAlive++;
		if (this.ticksTillNextKeepAlive >= 25) { // 2.5 seconds
			this.sendPacket(new PacketKeepAlive());
			if (this.noKeepAlivePacketTicks > 0) {
				logger.warn("%s has missed x%d keep alive packets", this, this.noKeepAlivePacketTicks);
			}
			this.noKeepAlivePacketTicks++;	
			this.ticksTillNextKeepAlive = 0;
		}
		
		if (this.messageRateLimitTicks > 0) {
			this.messageRateLimitTicks--;
		}
    }
    
    @Override
    protected void onDisconnect() {
		this.changeStatus(UserStatus.OFFLINE, "", true);
		this.instance.clients.remove(this);
		this.instance.sendHeartbeat();
    }
    
    @Override
    public void handlePacket(Packet packet) {
    	try {
    		if (!(packet instanceof PacketKeepAlive)) {    			
    			logger.verbose("Received %s from %s", packet, this);
    		}
    		
    		if (this.packetsHandler == null) {
    			this.onBadPacket();
    			return;
    		}
    		
    		String packetName = packet.getClass().getSimpleName().replace("Packet", "");
			Method handler = this.packetsHandler.getClass().getMethod(
					String.format("handle%sPacket", packetName), packet.getClass());
			handler.invoke(this.packetsHandler, packet);
		} catch (InvocationTargetException ex) {
			logger.error("The packet handler for %s has encountered an error: %s", 
					this, Utils.getThrowableStackTraceAsStr(ex.getCause()));
			this.netManager.shutdown("Internal Server Error");
		} catch (Exception ex) {
			this.onBadPacket();
		}
    }
}
