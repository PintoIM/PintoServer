package me.vlod.pinto.networking;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.packet.Packet;
import me.vlod.pinto.networking.packet.PacketFactory;

public class NetworkTCPManager implements NetworkManager {
	// HEADER + IV + ENCRYPTED_SIZE + ID
	private static final int PACKET_SIZE_MODIFIER = 4 + 16 + 4 + 4;
	private Socket socket;
	private NetworkAddress address;
	private DataInputStream inputStream;
	private DataOutputStream outputStream;
    private Cipher cipherDecryptor;
    private Cipher cipherEncryptor;
    private SecretKey secretKey;
	private boolean connected;
	private boolean isTerminating;
	private boolean isClosing;
	private String terminationReason;
	private Object sendQueueLock = new Object();
	private int sendQueueByteLength;
	private List<Packet> readPackets = Collections.synchronizedList(new LinkedList<Packet>());
	private List<Packet> sendPackets = Collections.synchronizedList(new LinkedList<Packet>());
	private Thread writeThread;
	private Thread readThread;
	private NetBaseHandler netHandler;
	private int timeSinceLastRead;

	public NetworkTCPManager(Socket socket, String threadName, NetBaseHandler netHandler) throws IOException {
		if (socket == null) {
			throw new IllegalArgumentException("No socket specified");
		}
		this.socket = socket;
		this.netHandler = netHandler;
		socket.setTrafficClass(24); // IPTOS_THROUGHPUT + IPTOS_LOWDELAY
		this.inputStream = new DataInputStream(socket.getInputStream());
		this.outputStream = new DataOutputStream(socket.getOutputStream());
		this.address = new NetworkAddress(socket);
		this.connected = true;

		this.readThread = new Thread(threadName + "-Reader") {
			@Override
			public void run() {
				while (true) {
					if (!NetworkTCPManager.this.connected || NetworkTCPManager.this.isClosing) {
						break;
					}
					NetworkTCPManager.this.readPacket();
				}
			}
		};

		this.writeThread = new Thread(threadName + "-Writer") {
			@Override
			public void run() {
				while (true) {
					if (!NetworkTCPManager.this.connected) {
						break;
					}
					NetworkTCPManager.this.sendPacket();
				}
			}
		};
	}

	@Override
	public void onHandshaked(SecretKey secretKey) throws Exception {
		if (secretKey == null) {
			throw new IllegalArgumentException("No secret key specified");
		}
		this.secretKey = secretKey;
    	this.cipherDecryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
    	this.cipherEncryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
		this.readThread.start();
		this.writeThread.start();
	}
	
    public IvParameterSpec getIV() throws NoSuchAlgorithmException, NoSuchPaddingException {
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        byte[] iv = new byte[16];
        secureRandom.nextBytes(iv);
        return new IvParameterSpec(iv);
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
			// XXX: Should be fine to use the un-encrypted size
			this.sendQueueByteLength += PACKET_SIZE_MODIFIER + packet.getPacketSize();
			this.sendPackets.add(packet);
		}
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
	    	BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(this.outputStream, 4096);
			
			synchronized (this.sendQueueLock) {
				packet = this.sendPackets.remove(0);
				// XXX: Should be fine to use the un-encrypted size
				this.sendQueueByteLength -= PACKET_SIZE_MODIFIER + packet.getPacketSize();
			}

			// Write the packet
			dataOutputStream.writeInt(packet.getID());
			packet.write(dataOutputStream);
			
			IvParameterSpec iv = this.getIV();
        	this.cipherEncryptor.init(Cipher.ENCRYPT_MODE, this.secretKey, iv);
        	byte[] packetData = byteArrayOutputStream.toByteArray();
        	byte[] encryptedPacketData = this.cipherEncryptor.doFinal(packetData);
        	byteArrayOutputStream.close();
        	
        	bufferedOutputStream.write("PMSG".getBytes(StandardCharsets.US_ASCII));
        	bufferedOutputStream.write(Utils.intToBytes(encryptedPacketData.length));
        	bufferedOutputStream.write(iv.getIV());
        	bufferedOutputStream.write(encryptedPacketData);
        	bufferedOutputStream.flush();
		} catch (InterruptedException ex) {
		} catch (Exception ex) {
			if (this.isTerminating || this.isClosing) {
				return;
			}
			this.handleNetworkError(ex);
		}
	}

	private void readPacket() {
		try {
			int headerPart0 = this.inputStream.read();
            int headerPart1 = this.inputStream.read();
            int headerPart2 = this.inputStream.read();
            int headerPart3 = this.inputStream.read();

            if (headerPart0 == -1 || 
                headerPart1 == -1 || 
                headerPart2 == -1 || 
                headerPart3 == -1) {
            	this.shutdown("Client disconnect");
            	return;
            }
            
            if (headerPart0 != 'P' || 
            	headerPart1 != 'M' || 
            	headerPart2 != 'S' || 
            	headerPart3 != 'G') {
            	this.shutdown("Bad packet header");
            	return;
            }
			
			byte[] encryptedDataSize = new byte[4];
			this.inputStream.read(encryptedDataSize);
            
			byte[] iv = new byte[16];
			this.inputStream.read(iv);
			
			byte[] encryptedData = new byte[Utils.bytesToInt(encryptedDataSize)];
			this.inputStream.readFully(encryptedData);

			this.cipherDecryptor.init(Cipher.DECRYPT_MODE, this.secretKey, new IvParameterSpec(iv));
			byte[] decryptedData = this.cipherDecryptor.doFinal(encryptedData);
			DataInputStream dataInputStream = 
					new DataInputStream(new ByteArrayInputStream(decryptedData));
			
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
			
			if (ex instanceof SocketException || ex instanceof EOFException) {
				this.shutdown("Client disconnect");
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
					if (NetworkTCPManager.this.readThread.isAlive()) {
						try {
							NetworkTCPManager.this.readThread.stop();
						} catch (Exception ex) {
						}
					}

					if (NetworkTCPManager.this.writeThread.isAlive()) {
						try {
							NetworkTCPManager.this.writeThread.stop();
						} catch (Exception ex) {
						}
					}
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
		}).start();
		this.connected = false;

		try {
			this.inputStream.close();
		} catch (Exception ex) {
		}

		try {
			this.outputStream.close();
		} catch (Exception ex) {
		}

		try {
			this.socket.close();
		} catch (Exception ex) {
		}
	}

	@Override
	public void processReceivedPackets() throws IOException {
		if (this.sendQueueByteLength > 0x1_000_000) { // 16 megabytes
			this.shutdown("Send buffer overflow");
		}

		if (this.readPackets.isEmpty()) {
			if (this.timeSinceLastRead++ == 300) { // 30 seconds
				this.shutdown("No packet read within 30 seconds");
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
		return (InetSocketAddress) this.socket.getRemoteSocketAddress();
	}

	@Override
	public NetworkAddress getAddress() {
		return this.address;
	}
	
	@Override
	public DataInputStream getInputStream() {
		return this.inputStream;
	}

	@Override
	public DataOutputStream getOutputStream() {
		return this.outputStream;
	}

	@Override
	public void interrupt() {
		this.readThread.interrupt();
		this.writeThread.interrupt();
	}
	
	@Override
	public void close() {
		this.isClosing = true;
		this.readThread.interrupt();

		(new Thread("Network-Closer") {
			@Override
			public void run() {
				try {
					Thread.sleep(2000);
					if (NetworkTCPManager.this.connected) {
						NetworkTCPManager.this.writeThread.interrupt();
						NetworkTCPManager.this.shutdown("Connection closed");
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}).start();
	}
}
