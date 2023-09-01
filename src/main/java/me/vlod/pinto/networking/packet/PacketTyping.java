package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetworkHandler;

public class PacketTyping implements Packet {
    public String contactName;
    public boolean state;

    public PacketTyping() { }
    
    public PacketTyping(String contactName, boolean state) {
    	this.contactName = contactName;
    	this.state = state;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.contactName = Utils.readPintoStringFromStream(stream, NetworkHandler.USERNAME_MAX);
		this.state = stream.read() == 0x01;
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writePintoStringToStream(stream, this.contactName, NetworkHandler.USERNAME_MAX);
		stream.write(this.state ? 0x01 : 0x00);
	}

	@Override
	public int getID() {
		return 18;
	}

	@Override
	public void handle(NetworkHandler netHandler) {
		netHandler.handleTypingPacket(this);
	}
}
