package me.vlod.pinto.consolehandler;

import me.vlod.pinto.PintoServer;
import me.vlod.pinto.networking.NetworkHandler;
import me.vlod.pinto.networking.packet.PacketMessage;

public class ConsoleCaller {
	public boolean isClient;
	public NetworkHandler client;
	public String clientChat;
	
	public ConsoleCaller(NetworkHandler client, String clientChat) {
		this.isClient = client != null;
		this.client = client;
		this.clientChat = clientChat;
	}
	
	public void sendMessage(String message) {
		if (this.isClient) {
			this.client.sendPacket(new PacketMessage(this.clientChat, "", message));
		} else {
			PintoServer.logger.info(message);
		}
	}
}
