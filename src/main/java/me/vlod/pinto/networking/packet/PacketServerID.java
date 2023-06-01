package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetworkHandler;

public class PacketServerID implements Packet {
    public String serverID;

    public PacketServerID() { }
    
    public PacketServerID(String serverID) {
    	this.serverID = serverID;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.serverID = Utils.readPintoStringFromStream(stream, 36);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writePintoStringToStream(stream, this.serverID, 36);
	}

	@Override
	public int getID() {
		return 17;
	}

	@Override
	public void handle(NetworkHandler netHandler) {
	}

	@Override
	public int getSize() {
		return Utils.getPintoStringSize(this.serverID);
	}
}
