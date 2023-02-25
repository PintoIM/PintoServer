package me.vlod.pinto.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import me.vlod.pinto.Utils;

public class PacketTyping implements Packet {
    public String usernames;
	
    public PacketTyping() { }
    
    public PacketTyping(String usernames) {
    	this.usernames = usernames;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.usernames = Utils.readUTF8StringFromStream(stream).trim();
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writeUTF8StringToStream(stream, StringUtils.rightPad(this.usernames, 512));
	}

	@Override
	public int getID() {
		return 3;
	}

	@Override
	public int getLength() {
		// User names
		return (512 + 2);
	}
}
