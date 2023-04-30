package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetworkHandler;

public class PacketRegister implements Packet {
    public String name;
    public String passwordHash;
	
    public PacketRegister() { }
    
    public PacketRegister(String name, String passwordHash) {
    	this.name = name;
    	this.passwordHash = passwordHash;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.name = Utils.readASCIIStringFromStream(stream);
		this.passwordHash = Utils.readASCIIStringFromStream(stream);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writeASCIIStringToStream(stream, this.name);
		Utils.writeASCIIStringToStream(stream, this.passwordHash);
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
