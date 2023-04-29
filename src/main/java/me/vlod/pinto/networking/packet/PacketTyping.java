package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetworkHandler;

public class PacketTyping implements Packet {
    public String usernames;
	
    public PacketTyping() { }
    
    public PacketTyping(String usernames) {
    	this.usernames = usernames;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.usernames = Utils.readUTF8StringFromStream(stream);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writeUTF8StringToStream(stream, this.usernames);
	}

	@Override
	public int getID() {
		return 4;
	}

	@Override
	public void handle(NetworkHandler netHandler) {
	}
}
