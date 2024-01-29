package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.networking.NetServerHandler;

public class PacketTyping extends Packet {
    public String contactName;
    public boolean state;

    public PacketTyping() { }
    
    public PacketTyping(String contactName, boolean state) {
    	this.contactName = contactName;
    	this.state = state;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.contactName = Packet.readString(stream, NetServerHandler.USERNAME_MAX);
		this.state = stream.read() == 0x01;
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Packet.writeString(stream, this.contactName, NetServerHandler.USERNAME_MAX);
		stream.write(this.state ? 0x01 : 0x00);
	}

	@Override
	public int getID() {
		return 18;
	}

	@Override
	public int getPacketSize() {
		return NetServerHandler.USERNAME_MAX + 1;
	}
	
	@Override
	public String getDataAsStr() {
		return this.contactName + "," + this.state;
	}
}
