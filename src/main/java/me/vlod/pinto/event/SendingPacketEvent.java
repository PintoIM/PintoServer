package me.vlod.pinto.event;

import me.vlod.hottyevents.Event;
import me.vlod.pinto.networking.NetServerHandler;
import me.vlod.pinto.networking.packet.Packet;

/**
 * Event called before sending a packet
 */
public class SendingPacketEvent extends Event {
	private boolean cancelled;
	public final NetServerHandler client;
	public final Packet packet;
	
	public SendingPacketEvent(NetServerHandler client, Packet packet) {
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
