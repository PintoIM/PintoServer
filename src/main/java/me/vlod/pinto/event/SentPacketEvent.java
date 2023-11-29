package me.vlod.pinto.event;

import me.vlod.hottyevents.Event;
import me.vlod.pinto.networking.NetServerHandler;
import me.vlod.pinto.networking.packet.Packet;

/**
 * Event called after sending a packet (can't be cancelled)
 */
public class SentPacketEvent extends Event {
	public final NetServerHandler client;
	public final Packet packet;
	
	public SentPacketEvent(NetServerHandler client, Packet packet) {
		this.client = client;
		this.packet = packet;
	}
	
	/**
	 * This always returns false
	 */
	@Override
	public boolean getCancelled() {
		return false;
	}

	/**
	 * Doesn't do anything
	 */
	@Override
	public void setCancelled(boolean cancelState) {
	}
}
