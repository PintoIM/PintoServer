package me.vlod.pinto.event;

import me.vlod.hottyevents.Event;
import me.vlod.pinto.networking.NetworkHandler;

/**
 * Event called when a client disconnects
 */
public class ClientDisconnectedEvent extends Event {
	public final NetworkHandler client;
	public final String reason;
	
	public ClientDisconnectedEvent(NetworkHandler client, String reason) {
		this.client = client;
		this.reason = reason;
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
