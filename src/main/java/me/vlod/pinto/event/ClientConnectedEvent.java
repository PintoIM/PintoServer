package me.vlod.pinto.event;

import me.vlod.hottyevents.Event;
import me.vlod.pinto.networking.NetworkHandler;

/**
 * Event called when a client connects
 */
public class ClientConnectedEvent extends Event {
	private boolean cancelled;
	public final NetworkHandler client;
	
	public ClientConnectedEvent(NetworkHandler client) {
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
