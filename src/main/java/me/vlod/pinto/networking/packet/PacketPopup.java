package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetworkHandler;

public class PacketPopup implements Packet {
	public String title;
	public String body;
    
    public PacketPopup() { }
    
    public PacketPopup(String title, String body) {
    	this.title = title;
    	this.body = body;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.title = Utils.readPintoStringFromStream(stream, 32);
		this.body = Utils.readPintoStringFromStream(stream, 1024);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writePintoStringToStream(stream, this.title, 32);
		Utils.writePintoStringToStream(stream, this.body, 1024);
	}

	@Override
	public int getID() {
		return 4;
	}

	@Override
	public void handle(NetworkHandler netHandler) {
	}

	@Override
	public int getSize() {
		return Utils.getPintoStringSize(title) + Utils.getPintoStringSize(body);
	}
}
