package me.vlod.pinto.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import me.vlod.pinto.Utils;

public class PacketMessage implements Packet {
    public byte userID;
    public String message;

    public PacketMessage() { }
    
    public PacketMessage(byte userID, String message) {
    	this.userID = userID;
    	this.message = message;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.userID = (byte) stream.read();
		this.message = Utils.readUTF8StringFromStream(stream).trim();
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		stream.write(this.userID);
		Utils.writeUTF8StringToStream(stream, StringUtils.rightPad(this.message, 512));
	}

	@Override
	public int getID() {
		return 2;
	}

	@Override
	public int getLength() {
		// User ID + Message
		return 1 + (512 + 2);
	}
}
