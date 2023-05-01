package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetworkHandler;

public class PacketCallStart implements Packet {
    public String contactName;

    public PacketCallStart() { }
    
    public PacketCallStart(String contactName) {
    	this.contactName = contactName;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.contactName = Utils.readUTF16StringFromStream(stream);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writeUTF16StringToStream(stream, this.contactName);
	}

	@Override
	public int getID() {
		return 11;
	}

	@Override
	public void handle(NetworkHandler netHandler) {
		//netHandler.handleCallStartPacket(this);
	}
}
