package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetworkHandler;

public class PacketInWindowPopup implements Packet {
    public String message;
    public boolean isInfo;
    
    public PacketInWindowPopup() { }
    
    public PacketInWindowPopup(String message) {
    	this.message = message;
    	this.isInfo = false;
    }
    
    public PacketInWindowPopup(String message, boolean isInfo) {
    	this.message = message;
    	this.isInfo = isInfo;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.message = Utils.readPintoStringFromStream(stream, 256);
		this.isInfo = stream.read() == 0x01;
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writePintoStringToStream(stream, this.message, 256);
		stream.write(this.isInfo ? 0x01 : 0x00);
	}

	@Override
	public int getID() {
		return 5;
	}

	@Override
	public void handle(NetworkHandler netHandler) {
	}
}
