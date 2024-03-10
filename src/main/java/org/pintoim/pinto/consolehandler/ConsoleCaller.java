package org.pintoim.pinto.consolehandler;

import org.pintoim.pinto.PintoServer;
import org.pintoim.pinto.networking.NetServerHandler;
import org.pintoim.pinto.networking.PMSGMessage;
import org.pintoim.pinto.networking.packet.PacketMessage;

public class ConsoleCaller {
	public boolean isClient;
	public NetServerHandler client;
	public String clientChat;
	
	public ConsoleCaller(NetServerHandler client, String clientChat) {
		this.isClient = client != null;
		this.client = client;
		this.clientChat = clientChat;
	}
	
	public void sendMessage(String message) {
		if (this.isClient) {
			this.client.sendPacket(new PacketMessage(this.clientChat, "", new PMSGMessage(message)));
		} else {
			PintoServer.logger.info(message);
		}
	}
}
