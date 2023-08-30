package me.vlod.pinto.consolehandler;

import me.vlod.pinto.PintoServer;
import me.vlod.pinto.networking.NetworkHandler;

public class ConsoleCaller {
	public boolean isClient;
	public NetworkHandler client;
	
	public ConsoleCaller(NetworkHandler client) {
		this.isClient = client != null;
		this.client = client;
	}
	
	public void sendMessage(String message) {
		if (this.isClient) {
			// TODO: Client command handler
		} else {
			PintoServer.logger.info(message);
		}
	}
}
