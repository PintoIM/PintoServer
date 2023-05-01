package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetworkHandler;

public class PacketRegister implements Packet {
    public byte protocolVersion;
    public String clientVersion;
    public String name;
    public String passwordHash;
	
    public PacketRegister() { }
    
    public PacketRegister(byte protocolVersion, String name, String passwordHash) {
    	this.protocolVersion = protocolVersion;
    	this.name = name;
    	this.passwordHash = passwordHash;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.protocolVersion = (byte) stream.read();
		this.clientVersion = Utils.readUTF16StringFromStream(stream);
		this.name = Utils.readUTF16StringFromStream(stream);
		this.passwordHash = Utils.readUTF16StringFromStream(stream);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		stream.write(this.protocolVersion);
		Utils.writeUTF16StringToStream(stream, this.clientVersion);
		Utils.writeUTF16StringToStream(stream, this.name);
		Utils.writeUTF16StringToStream(stream, this.passwordHash);
	}

	@Override
	public int getID() {
		return 1;
	}

	@Override
	public void handle(NetworkHandler netHandler) {
		netHandler.handleRegisterPacket(this);
	}
}
