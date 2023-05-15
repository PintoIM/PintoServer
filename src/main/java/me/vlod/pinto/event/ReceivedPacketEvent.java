package me.vlod.pinto.event;

import me.vlod.hottyevents.Event;
import me.vlod.pinto.networking.NetworkHandler;
import me.vlod.pinto.networking.packet.Packet;

/**
 * Event called when receiving a packet
 */
public class ReceivedPacketEvent extends Event {
	private boolean cancelled;
	public final NetworkHandler client;
	public final Packet packet;
	
	public ReceivedPacketEvent(NetworkHandler client, Packet packet) {
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