package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketServerInfo extends Packet {
    public String serverID;
    public String serverSoftware;
    
    public PacketServerInfo() { }
    
    public PacketServerInfo(String serverID, String serverSoftware) {
    	this.serverID = serverID;
    	this.serverSoftware = serverSoftware;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.serverID = Packet.readString(stream, 36);
		this.serverSoftware = Packet.readString(stream, 128);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Packet.writeString(stream, this.serverID, 36);
		Packet.writeString(stream, this.serverSoftware, 128);
	}

	@Override
	public int getID() {
		return 17;
	}

	@Override
	public int getPacketSize() {
		return 36 + 128;
	}
}
