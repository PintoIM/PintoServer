package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetServerHandler;

public class PacketMessage extends Packet {
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
		this.contactName = Utils.readPintoStringFromStream(stream, NetServerHandler.USERNAME_MAX);
		this.sender = Utils.readPintoStringFromStream(stream, NetServerHandler.USERNAME_MAX);
		this.message = Utils.readPintoStringFromStream(stream, 512);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writePintoStringToStream(stream, this.contactName, NetServerHandler.USERNAME_MAX);
		Utils.writePintoStringToStream(stream, this.sender, NetServerHandler.USERNAME_MAX);
		Utils.writePintoStringToStream(stream, this.message, 512);
	}

	@Override
	public int getID() {
		return 3;
	}

	@Override
	public int getPacketSize() {
		return NetServerHandler.USERNAME_MAX * 2 + 512;
	}
}
