package me.vlod.pinto.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import me.vlod.pinto.Utils;

public class PacketLogin implements Packet {
    public byte protocolVersion;
    public String name;
    public String sessionID;
	
    public PacketLogin() { }
    
    public PacketLogin(byte protocolVersion, 
    		String name, String sessionID) {
    	this.protocolVersion = protocolVersion;
    	this.name = name;
    	this.sessionID = sessionID;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.protocolVersion = (byte) stream.read();
		this.name = Utils.readUTF8StringFromStream(stream).trim();
		this.sessionID = Utils.readUTF8StringFromStream(stream).trim();
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		stream.write(this.protocolVersion);
		Utils.writeUTF8StringToStream(stream, StringUtils.rightPad(this.name, 128));
		Utils.writeUTF8StringToStream(stream, StringUtils.rightPad(this.sessionID, 128));
	}

	@Override
	public int getID() {
		return 0;
	}

	@Override
	public int getLength() {
		// Protocol version + Name + Session ID
		return 1 + (128 + 2) + (128 + 2);
	}
}
