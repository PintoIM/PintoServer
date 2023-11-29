package me.vlod.pinto.networking;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.SecretKey;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.packet.Packet;
import me.vlod.pinto.networking.packet.PacketFactory;

public class NetworkUDPManager implements NetworkManager {
	// HEADER + SIZE + ID
	private static final int PACKET_SIZE_MODIFIER = 4 + 4 + 4;
	private DatagramSocket server;
	private NetworkAddress address;
	private InetAddress remoteIP;
	private int remotePort;
	private boolean connected;
	private boolean isTerminating;
	private boolean isClosing;
	private String terminationReason;
	private Object sendQueueLock = new Object();
	private int sendQueueByteLength;
	private List<Packet> readPackets = Collections.synchronizedList(new LinkedList<Packet>());
	private List<Packet> sendPackets = Collections.synchronizedList(new LinkedList<Packet>());
	private Thread writeThread;
	private NetBaseHandler netHandler;
	private int timeSinceLastRead;

	public NetworkUDPManager(DatagramSocket server, InetAddress remoteIP, int remotePort, String threadName, 
			NetBaseHandler netHandler) throws IOException {
		this.server = server;
		this.netHandler = netHandler;
		this.remoteIP = remoteIP;
		this.remotePort = remotePort;
		this.address = new NetworkAddress(remoteIP.getHostAddress(), remotePort);
		this.connected = true;

		this.writeThread = new Thread(threadName + "-Writer") {
			@Override
			public void run() {
				while (true) {
					try {
						if (!NetworkUDPManager.this.connected) {
							break;
						}

						NetworkUDPManager.this.sendPacket();
					} finally {
					}
				}
			}
		};
	}

	@Override
	public void onHandshaked(SecretKey secretKey) throws Exception {
		this.writeThread.start();
	}

    @Override
	public void setNetHandler(NetBaseHandler netHandler) {
		this.netHandler = netHandler;
	}

	@Override
	public void addToQueue(Packet packet) {
		if (this.isClosing) {
			return;
		}
		
		synchronized (this.sendQueueLock) {
			this.sendQueueByteLength += PACKET_SIZE_MODIFIER + packet.getPacketSize();
			this.sendPackets.add(packet);
		}
	}

	private DatagramPacket getDGPacket(byte[] data) {
		return new DatagramPacket(data, data.length, this.remoteIP, this.remotePort);
	}
	
	private void sendPacket() {
		try {
			if (this.sendPackets.isEmpty()) {
				Thread.sleep(10L);
				return;
			}
			
			Packet packet;
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	    	DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
			
			synchronized (this.sendQueueLock) {
				packet = this.sendPackets.remove(0);
				this.sendQueueByteLength -= PACKET_SIZE_MODIFIER + packet.getPacketSize();
			}

			dataOutputStream.writeInt(packet.getID());
			packet.write(dataOutputStream);
			byte[] data = byteArrayOutputStream.toByteArray();
			byteArrayOutputStream.close();
			dataOutputStream.close();
			
			byteArrayOutputStream = new ByteArrayOutputStream();
			dataOutputStream = new DataOutputStream(byteArrayOutputStream);
			
			dataOutputStream.write("PMSG".getBytes(StandardCharsets.US_ASCII));
			dataOutputStream.writeInt(data.length);
			dataOutputStream.write(data);
        	dataOutputStream.flush();
        	
        	DatagramPacket dgPacket = this.getDGPacket(byteArrayOutputStream.toByteArray());
        	this.server.send(dgPacket);
		} catch (InterruptedException ex) {
		} catch (Exception ex) {
			if (this.isTerminating || this.isClosing) {
				return;
			}
			this.handleNetworkError(ex);
		}
	}

	public void handlePacket(byte[] dgPacket) {
		try {
			DataInputStream dataInputStream = 
					new DataInputStream(new ByteArrayInputStream(dgPacket));
			
	        int packetID = dataInputStream.readInt();
			Packet packet = PacketFactory.getPacketByID(packetID);
			
			if (packet == null) {
				this.shutdown("Bad packet ID " + packetID);
				return;
			}
			
			packet.read(dataInputStream);
			dataInputStream.close();
			this.readPackets.add(packet);
		} catch (Exception ex) {
			if (this.isTerminating || this.isClosing) {
				return;
			}
			this.handleNetworkError(ex);
		}
	}

	private void handleNetworkError(Exception ex) {
		this.shutdown(Utils.getThrowableStackTraceAsStr(ex));
	}

	@Override
	public void shutdown(String reason) {
		if (!this.connected) {
			return;
		}
		this.isTerminating = true;
		this.terminationReason = reason;

		(new Thread("Network-Terminator") {
			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				try {
					Thread.sleep(5000);

					if (NetworkUDPManager.this.writeThread.isAlive()) {
						try {
							NetworkUDPManager.this.writeThread.stop();
						} catch (Exception ex) {
						}
					}
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
		}).start();
		this.connected = false;
	}

	@Override
	public void processReceivedPackets() throws IOException {
		if (this.sendQueueByteLength > 0x100000) { // A megabyte
			this.shutdown("Send buffer overflow");
		}

		if (this.readPackets.isEmpty()) {
			if (this.timeSinceLastRead++ == 100) { // 10 seconds
				this.shutdown("No packet read within 10 seconds");
			}
		} else {
			this.timeSinceLastRead = 0;
		}

		int packetsLimit = 100;
		while (!this.readPackets.isEmpty() && packetsLimit-- >= 0) {
			Packet packet = this.readPackets.remove(0);
			this.netHandler.handlePacket(packet);
		}

		if (this.isTerminating && this.readPackets.isEmpty()) {
			this.netHandler.handleTermination(this.terminationReason);
		}
	}

	@Override
	public InetSocketAddress getSocketAddress() {
		return null;
	}

	@Override
	public NetworkAddress getAddress() {
		return this.address;
	}
	
	@Override
	public DataInputStream getInputStream() {
		return null;
	}

	@Override
	public DataOutputStream getOutputStream() {
		return null;
	}

	@Override
	public void interrupt() {
	}
	
	@Override
	public void close() {
		this.isClosing = true;

		(new Thread("Network-Closer") {
			@Override
			public void run() {
				try {
					Thread.sleep(2000);
					if (NetworkUDPManager.this.connected) {
						NetworkUDPManager.this.writeThread.interrupt();
						NetworkUDPManager.this.shutdown("Connection closed");
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}).start();
	}
}
