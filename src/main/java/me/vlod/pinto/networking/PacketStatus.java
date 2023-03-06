package me.vlod.pinto.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.UserStatus;
import me.vlod.pinto.Utils;

public class PacketStatus implements Packet {
    public String contactName;
    public UserStatus status;

    public PacketStatus() { }
    
    public PacketStatus(String contactName, UserStatus status) {
    	this.contactName = contactName;
    	this.status = status;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.contactName = Utils.readUTF8StringFromStream(stream);
		this.status = UserStatus.fromIndex(stream.readInt());
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writeUTF8StringToStream(stream, this.contactName);
		stream.writeInt(this.status.getIndex());
	}

	@Override
	public int getID() {
		return 8;
	}
}
