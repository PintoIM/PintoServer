package me.vlod.pinto.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import me.vlod.pinto.Utils;

public class PacketLogout implements Packet {
    public String reason;
    
    public PacketLogout() { }
    
    public PacketLogout(String reason) {
    	this.reason = reason;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.reason = Utils.readUTF8StringFromStream(stream).trim();
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writeUTF8StringToStream(stream, StringUtils.rightPad(this.reason, 256));
	}

	@Override
	public int getID() {
		return 1;
	}

	@Override
	public int getLength() {
		// Reason
		return (256 + 2);
	}
}
