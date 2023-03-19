package me.vlod.pinto.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;

public class PacketCallPartyInfo implements Packet {
    public String ipAddress;
    public int port;

    public PacketCallPartyInfo() { }
    
    public PacketCallPartyInfo(String ipAddress, int port) {
    	this.ipAddress = ipAddress;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.ipAddress = Utils.readUTF8StringFromStream(stream);
		this.port = stream.readInt();
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writeUTF8StringToStream(stream, this.ipAddress);
		stream.writeInt(this.port);
	}

	@Override
	public int getID() {
		return 13;
	}
}
