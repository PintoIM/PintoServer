package me.vlod.pinto.networking.packet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import me.vlod.pinto.UserStatus;
import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.NetServerHandler;

public class PacketAddContact extends Packet {
    public String contactName;
    public UserStatus status;
    public String motd;
    
    public PacketAddContact() { }
    
    public PacketAddContact(String contactName, UserStatus status, String motd) {
    	this.contactName = contactName;
    	this.status = status;
    	this.motd = motd;
    }
    
	@Override
	public void read(DataInputStream stream) throws IOException {
		this.contactName = Utils.readPintoStringFromStream(stream, NetServerHandler.USERNAME_MAX);
		this.status = UserStatus.fromIndex(stream.readInt());
		this.motd = Utils.readPintoStringFromStream(stream, 64);
	}
	
	@Override
	public void write(DataOutputStream stream) throws IOException {
		Utils.writePintoStringToStream(stream, this.contactName, NetServerHandler.USERNAME_MAX);
		stream.writeInt(this.status.getIndex());
		Utils.writePintoStringToStream(stream, this.motd, 64);
	}

	@Override
	public int getID() {
		return 6;
	}

	@Override
	public int getPacketSize() {
		return NetServerHandler.USERNAME_MAX + 4 + 64;
	}
	
	@Override
	public String getDataAsStr() {
		return this.contactName + "," + this.status + "," + this.motd;
	}
}
