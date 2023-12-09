package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;

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
		this.serverID = Utils.readPintoStringFromStream(stream, 36);
		this.serverSoftware = Utils.readPintoStringFromStream(stream, 128);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writePintoStringToStream(stream, this.serverID, 36);
		Utils.writePintoStringToStream(stream, this.serverSoftware, 128);
	}

	@Override
	public int getID() {
		return 17;
	}

	@Override
	public int getPacketSize() {
		return 36 + 128;
	}
	
	@Override
	public String getDataAsStr() {
		return this.serverID + "," + this.serverSoftware;
	}
}
