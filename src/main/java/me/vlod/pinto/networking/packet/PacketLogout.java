package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketLogout extends Packet {
    public String reason;
    
    public PacketLogout() { }
    
    public PacketLogout(String reason) {
    	this.reason = reason;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.reason = Packet.readString(stream, 256);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Packet.writeString(stream, this.reason, 256);
	}

	@Override
	public int getID() {
		return 2;
	}

	@Override
	public int getPacketSize() {
		return 256;
	}
}
