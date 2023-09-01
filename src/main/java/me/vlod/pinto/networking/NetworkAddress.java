package me.vlod.pinto.networking;

import java.net.Socket;

public class NetworkAddress {
	public String ip;
	public int port;
	
	public NetworkAddress(String ip, int port) {
		this.ip = ip;
		this.port = port;
	}
	
	public NetworkAddress(String str) {
		String[] stringSplitted = str.split(":");
		this.ip = stringSplitted[0];
		this.port = Integer.valueOf(stringSplitted[1]);
	}
	
	public NetworkAddress(Socket socket) {
		this.ip = socket.getInetAddress().getHostAddress();
		this.port = socket.getPort();
	}
	
	@Override
	public String toString() {
		return this.ip + ":" + this.port;
	}
}
