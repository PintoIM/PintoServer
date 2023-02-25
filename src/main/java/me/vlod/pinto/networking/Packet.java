package me.vlod.pinto.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Packet {
	public void read(DataInputStream stream) throws IOException;
    public void write(DataOutputStream stream) throws IOException;
    public int getID();
    public int getLength();
}