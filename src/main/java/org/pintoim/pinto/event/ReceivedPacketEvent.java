package org.pintoim.pinto.event;

import org.pintoim.pinto.networking.NetServerHandler;
import org.pintoim.pinto.networking.packet.Packet;

import me.vlod.hottyevents.Event;

/**
 * Event called when receiving a packet
 */
public class ReceivedPacketEvent extends Event {
	private boolean cancelled;
	public final NetServerHandler client;
	public final Packet packet;
	
	public ReceivedPacketEvent(NetServerHandler client, Packet packet) {
		this.client = client;
		this.packet = packet;
	}
	
	@Override
	public boolean getCancelled() {
		return this.cancelled;
	}

	@Override
	public void setCancelled(boolean cancelState) {
		this.cancelled = cancelState;
	}
}
