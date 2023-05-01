package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetworkHandler;

public class PacketMessage implements Packet {
    public String contactName;
    public String sender;
    public String message;

    public PacketMessage() { }
    
    public PacketMessage(String contactName, String sender, String message) {
    	this.contactName = contactName;
    	this.sender = sender;
    	this.message = message;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.contactName = Utils.readPintoStringFromStream(stream, NetworkHandler.USERNAME_MAX);
		this.sender = Utils.readPintoStringFromStream(stream, NetworkHandler.USERNAME_MAX);
		this.message = Utils.readPintoStringFromStream(stream, 512);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writePintoStringToStream(stream, this.contactName, NetworkHandler.USERNAME_MAX);
		Utils.writePintoStringToStream(stream, this.sender, 16);
		Utils.writePintoStringToStream(stream, this.message, 512);
	}

	@Override
	public int getID() {
		return 3;
	}

	@Override
	public void handle(NetworkHandler netHandler) {
		netHandler.handleMessagePacket(this);
	}
}
