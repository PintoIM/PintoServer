package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetworkHandler;

public class PacketMessage implements Packet {
    public String contactName;
    public String message;

    public PacketMessage() { }
    
    public PacketMessage(String contactName, String message) {
    	this.contactName = contactName;
    	this.message = message;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.contactName = Utils.readUTF8StringFromStream(stream);
		this.message = Utils.readUTF8StringFromStream(stream);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writeUTF8StringToStream(stream, this.contactName);
		Utils.writeUTF8StringToStream(stream, this.message);
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
