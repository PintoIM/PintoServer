package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetworkHandler;

public class PacketLogin implements Packet {
    public byte protocolVersion;
    public String clientVersion;
    public String token;
	
    public PacketLogin() { }
    
    public PacketLogin(byte protocolVersion, String token) {
    	this.protocolVersion = protocolVersion;
    	this.clientVersion = "";
    	this.token = token;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.protocolVersion = (byte) stream.read();
		this.clientVersion = Utils.readPintoStringFromStream(stream, 32);
		this.token = Utils.readPintoStringFromStream(stream, Integer.MAX_VALUE);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		stream.write(this.protocolVersion);
		Utils.writePintoStringToStream(stream, this.clientVersion, 32);
		Utils.writePintoStringToStream(stream, this.token, Integer.MAX_VALUE);
	}

	@Override
	public int getID() {
		return 0;
	}

	@Override
	public void handle(NetworkHandler netHandler) {
		netHandler.handleLoginPacket(this);
	}

	@Override
	public int getSize() {
		return 1 + Utils.getPintoStringSize(this.clientVersion) + Utils.getPintoStringSize(this.token);
	}
}
