package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketKeepAlive extends Packet {
	@Override
	public void read(DataInputStream stream) throws IOException {
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
	}

	@Override
	public int getID() {
		return 255;
	}

	@Override
	public int getPacketSize() {
		return 0;
	}
}
