package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetworkHandler;

public class PacketSetOption implements Packet {
    public String option;
    public String value;
    
    public PacketSetOption() { }
    
    public PacketSetOption(String option, String value) {
    	this.option = option;
    	this.value = value;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.option = Utils.readPintoStringFromStream(stream, 64);
		this.value = Utils.readPintoStringFromStream(stream, 128);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writePintoStringToStream(stream, this.option, 64);
		Utils.writePintoStringToStream(stream, this.value, 128);
	}

	@Override
	public int getID() {
		return 12;
	}

	@Override
	public void handle(NetworkHandler netHandler) {
	}
}
