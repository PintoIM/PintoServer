package org.pintoim.pinto.event;

import org.pintoim.pinto.networking.NetServerHandler;

import me.vlod.hottyevents.Event;

/**
 * Event called when a client connects
 */
public class ClientConnectedEvent extends Event {
	private boolean cancelled;
	public final NetServerHandler client;
	
	public ClientConnectedEvent(NetServerHandler client) {
		this.client = client;
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
