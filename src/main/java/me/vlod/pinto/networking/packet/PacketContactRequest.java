package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.networking.NetServerHandler;

public class PacketContactRequest extends Packet {
    public String contactName;

    public PacketContactRequest() { }
    
    public PacketContactRequest(String contactName) {
    	this.contactName = contactName;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.contactName = Packet.readString(stream, NetServerHandler.USERNAME_MAX + 4);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Packet.writeString(stream, this.contactName, NetServerHandler.USERNAME_MAX + 4);
	}

	@Override
	public int getID() {
		return 9;
	}

	@Override
	public int getPacketSize() {
		return NetServerHandler.USERNAME_MAX + 4;
	}
}
