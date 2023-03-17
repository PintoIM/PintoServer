package me.vlod.pinto.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketClearContacts implements Packet {
	@Override
	public void read(DataInputStream stream) throws IOException {
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
	}

	@Override
	public int getID() {
		return 10;
	}
}
