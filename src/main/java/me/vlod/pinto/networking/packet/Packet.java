package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.networking.NetworkHandler;

public interface Packet {
	public void read(DataInputStream stream) throws IOException;
    public void write(DataOutputStream stream) throws IOException;
    public void handle(NetworkHandler netHandler);
    public int getID();
    public int getSize();
}