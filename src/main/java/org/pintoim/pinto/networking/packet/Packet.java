package org.pintoim.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class Packet {
	public abstract void read(DataInputStream stream) throws IOException;
    public abstract void write(DataOutputStream stream) throws IOException;
    public abstract int getID();
    public abstract int getPacketSize();

	public static String readString(DataInputStream stream, int maxLength) throws IOException {
	    int length = stream.readInt();
	    if (length < 0) 
	    	throw new IOException("Weird string, the length is less than 0!");
	    if (length < 1) return "";
	    
	    byte[] buffer = new byte[length];
	    stream.read(buffer);
	    
	    String str = new String(buffer, StandardCharsets.UTF_16BE);
	    if (str.length() > maxLength)
	    	throw new IllegalArgumentException(String.format(
	    			"Read more data than allowed! (%d > %d)", 
	    			str.length(), maxLength));
	    
	    return str;
	}
	
	public static void writeString(DataOutputStream stream, String str, int maxLength) throws IOException {
	    if (str.length() > maxLength)
	    	str = str.substring(0, maxLength - 1);
	    byte[] stringData = str.getBytes(StandardCharsets.UTF_16BE);
	    
		stream.writeInt(stringData.length);
	    if (stringData.length < 1) return;
	    
	    stream.write(stringData);
	}

    @Override
    public final String toString() {
    	return String.format("%s (%d)", this.getClass().getSimpleName(), this.getID());
    }
}