package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.networking.NetworkHandler;

public class PacketCallInit implements Packet {
	@Override
	public void read(DataInputStream stream) throws IOException {
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
	}

	@Override
	public int getID() {
		return 13;
	}

	@Override
	public void handle(NetworkHandler netHandler) {
	}

	@Override
	public int getSize() {
		return 0;
	}
}
