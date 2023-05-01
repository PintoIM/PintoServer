package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetworkHandler;

public class PacketCallPartyInfo implements Packet {
    public String ipAddress;
    public int port;

    public PacketCallPartyInfo() { }
    
    public PacketCallPartyInfo(String ipAddress, int port) {
    	this.ipAddress = ipAddress;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.ipAddress = Utils.readUTF16StringFromStream(stream);
		this.port = stream.readInt();
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writeUTF16StringToStream(stream, this.ipAddress);
		stream.writeInt(this.port);
	}

	@Override
	public int getID() {
		return 13;
	}

	@Override
	public void handle(NetworkHandler netHandler) {
		//netHandler.handleCallPartyInfoPacket(this);
	}
}
