package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetworkHandler;

public class PacketLogout implements Packet {
    public String reason;
    
    public PacketLogout() { }
    
    public PacketLogout(String reason) {
    	this.reason = reason;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.reason = Utils.readUTF8StringFromStream(stream);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writeUTF8StringToStream(stream, this.reason);
	}

	@Override
	public int getID() {
		return 2;
	}

	@Override
	public void handle(NetworkHandler netHandler) {
	}
}
