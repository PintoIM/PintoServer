package org.pintoim.pinto.event;

import org.pintoim.pinto.networking.NetServerHandler;

import me.vlod.hottyevents.Event;

/**
 * Event called when a client disconnects
 */
public class ClientDisconnectedEvent extends Event {
	public final NetServerHandler client;
	public final String reason;
	
	public ClientDisconnectedEvent(NetServerHandler client, String reason) {
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
