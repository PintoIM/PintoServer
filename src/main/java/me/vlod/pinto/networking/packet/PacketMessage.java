package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.networking.NetServerHandler;
import me.vlod.pinto.networking.PMSGMessage;

public class PacketMessage extends Packet {
	public static final int PAYLOAD_MAX_LENGTH = 8388608;
    public String contactName;
    public String sender;
    public PMSGMessage payload;

    public PacketMessage() { }
    
    public PacketMessage(String contactName, String sender, PMSGMessage payload) {
    	this.contactName = contactName;
    	this.sender = sender;
    	this.payload = payload;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.contactName = Packet.readString(stream, NetServerHandler.USERNAME_MAX);
		this.sender = Packet.readString(stream, NetServerHandler.USERNAME_MAX);
		int payloadLength = stream.readInt();
		
		if (payloadLength > PAYLOAD_MAX_LENGTH) {
			System.out.printf("%d > %d\n", payloadLength, PAYLOAD_MAX_LENGTH);
			return;
		}
		
		byte[] payload = new byte[payloadLength];
		stream.readFully(payload);
		this.payload = PMSGMessage.decode(payload);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Packet.writeString(stream, this.contactName, NetServerHandler.USERNAME_MAX);
		Packet.writeString(stream, this.sender, NetServerHandler.USERNAME_MAX);
		byte[] payload = this.payload.encode();
		stream.writeInt(payload.length);
		stream.write(payload);
	}

	@Override
	public int getID() {
		return 3;
	}

	@Override
	public int getPacketSize() {
		// TODO: Figure this crap out
		return NetServerHandler.USERNAME_MAX * 2 /*+ PAYLOAD_MAX_LENGTH*/;
	}
}
