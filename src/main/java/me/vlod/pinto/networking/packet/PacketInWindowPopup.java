package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetworkHandler;

public class PacketInWindowPopup implements Packet {
    public String message;
    
    public PacketInWindowPopup() { }
    
    public PacketInWindowPopup(String message) {
    	this.message = message;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.message = Utils.readPintoStringFromStream(stream, 256);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writePintoStringToStream(stream, this.message, 256);
	}

	@Override
	public int getID() {
		return 5;
	}

	@Override
	public void handle(NetworkHandler netHandler) {
	}

	@Override
	public int getSize() {
		return Utils.getPintoStringSize(message);
	}
}
