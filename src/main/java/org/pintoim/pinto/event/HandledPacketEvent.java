package org.pintoim.pinto.event;

import org.pintoim.pinto.networking.NetServerHandler;
import org.pintoim.pinto.networking.packet.Packet;

import me.vlod.hottyevents.Event;

/**
 * Event called after handling a packet (can't be cancelled)
 */
public class HandledPacketEvent extends Event {
	public final NetServerHandler client;
	public final Packet packet;
	
	public HandledPacketEvent(NetServerHandler client, Packet packet) {
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
