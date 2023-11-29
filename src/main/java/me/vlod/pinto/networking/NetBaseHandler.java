package me.vlod.pinto.networking;

import java.io.IOException;

import me.vlod.pinto.PintoServer;
import me.vlod.pinto.logger.Logger;
import me.vlod.pinto.networking.packet.Packet;
import me.vlod.pinto.networking.packet.PacketKeepAlive;
import me.vlod.pinto.networking.packet.PacketLogout;

public class NetBaseHandler {
	public static final byte PROTOCOL_VERSION = 10;
	public static final int USERNAME_MAX = 16;
	public static final int MESSAGE_RATE_LIMIT_TIME = 10;
	protected Logger logger = PintoServer.logger;
	public NetworkManager netManager;
	public String userName;
	public boolean connectionClosed;

	public void sendPacket(Packet packet) {
		if (!(packet instanceof PacketKeepAlive)) {			
			logger.verbose("Added %s to the send queue of %s", packet, this);
		}
		this.netManager.addToQueue(packet);
		this.netManager.interrupt();
	}
	
	public void kick(String reason) {
		this.logger.info(String.format("Kicking %s: %s", this, reason));
		this.sendPacket(new PacketLogout(reason));
		this.netManager.close();
		this.connectionClosed = true;
		this.onDisconnect();
	}

	public void handleTermination(String reason) {
		this.logger.info(String.format("%s has disconnected: %s", this, reason));
		this.connectionClosed = true;
		this.onDisconnect();
	}

	public void handlePacket(Packet packet) {
		this.onBadPacket();
	}
	
	protected void onUpdate() throws IOException {
	}

	protected void onBadPacket() {
		this.kick("Illegal packet during session type!");
	}
	
	protected void onDisconnect() {
	}

	@Override
	public String toString() {
		return this.userName != null ? String.format("%s [%s]", this.userName, this.netManager.getAddress())
				: this.netManager.getAddress().toString();
	}
}
