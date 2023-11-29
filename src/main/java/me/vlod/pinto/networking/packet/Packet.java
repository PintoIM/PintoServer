package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public abstract class Packet {
	public abstract void read(DataInputStream stream) throws IOException;
    public abstract void write(DataOutputStream stream) throws IOException;
    public abstract int getID();
    public abstract int getPacketSize();

    public String getDataAsStr() {
    	return null;
    }
    
    @Override
    public final String toString() {
    	String dataAsStr = this.getDataAsStr();
    	dataAsStr = dataAsStr != null ? String.format("{%s}", dataAsStr) : "";
    	return String.format("%s(%d)%s", this.getClass().getSimpleName(), this.getID(), dataAsStr);
    }
}