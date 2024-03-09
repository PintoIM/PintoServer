package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class PacketNotification extends Packet {
	public int type;
	public int autoCloseDelay;
	public String title;
	public String body;
    
    public PacketNotification() { }
    
    /**
     * Types:<br>
     * - 0 -> In Window Pop-up (Warning)<br>
     * - 1 -> In Window Pop-up (Information)<br>
     * - 2 -> Pop-up (Notification)<br>
     * 
     * @param type the type
     * @param autoCloseDelay the delay before closing the notification
     * @param title the title
     * @param body the body
     */
    public PacketNotification(int type, int autoCloseDelay, String title, String body) {
    	this.type = type;
    	this.autoCloseDelay = autoCloseDelay;
    	this.title = title;
    	this.body = body;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.type = stream.read();
		this.autoCloseDelay = stream.readInt();
		this.title = Packet.readString(stream, 32);
		this.body = Packet.readString(stream, 1024);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		stream.write(this.type);
		stream.writeInt(this.autoCloseDelay);
		Packet.writeString(stream, this.title, 32);
		Packet.writeString(stream, this.body, 1024);
	}

	@Override
	public int getID() {
		return 4;
	}

	@Override
	public int getPacketSize() {
		return 1 + 4 + 32 + 1024;
	}
}
