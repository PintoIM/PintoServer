package me.vlod.pinto.networking;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import me.vlod.pinto.Delegate;
import me.vlod.pinto.PintoServer;
import me.vlod.pinto.Utils;
import me.vlod.pinto.logger.LogLevel;
import me.vlod.pinto.networking.packet.Packet;
import me.vlod.pinto.networking.packet.Packets;

public class NetworkClient {
    public boolean isConnected;
    private Socket socket;
    public NetworkAddress networkAddress;
    private InputStream inputStream;
    private OutputStream outputStream;
    private Thread readThread;
    private Cipher cipherDecryptor;
    private Cipher cipherEncryptor;
    public Delegate disconnected = Delegate.empty;
    public Delegate receivedPacket = Delegate.empty;
    private Object sendLock = new Object();
    private SecretKey secretKey;
    
    public NetworkClient(RSAPublicKey rsaPublicKey, RSAPrivateKey rsaPrivateKey, Socket socket) {
        try {
            this.socket = socket;
            this.networkAddress = new NetworkAddress(this.socket);
            this.isConnected = true;

            this.socket.setSoTimeout(10000);
            this.inputStream = this.socket.getInputStream();
            this.outputStream = this.socket.getOutputStream();
            
            this.readThread = new Thread("Client-Read-Thread") {
            	@Override
            	public void run() {
            		readThread_Func();
            	}
            };
            
            try {
            	this.handshake(rsaPublicKey, rsaPrivateKey);
            } catch (Exception ex) {
            	PintoServer.logger.levelMultiLine(LogLevel.WARN, "Failed to handshake with %s: %s", 
            			this.networkAddress, Utils.getThrowableStackTraceAsStr(ex));
            	this.disconnect("Handshaking failed");
            }
        } catch (Exception ex) {
            this.disconnect(null);
        }
    }
    
    private void handshake(RSAPublicKey rsaPublicKey, RSAPrivateKey rsaPrivateKey) throws Exception {
    	DataInputStream dataInputStream = new DataInputStream(this.inputStream);
    	DataOutputStream dataOutputStream = new DataOutputStream(this.outputStream);
    	PintoServer.logger.info("Handshaking AES key with %s...", this.networkAddress);

    	// Send our public key
    	byte[] rsaPublicKeyBytes = rsaPublicKey.getEncoded();
    	dataOutputStream.writeInt(rsaPublicKeyBytes.length);
    	dataOutputStream.write(rsaPublicKeyBytes);
    	
    	// Get a cipher using our private key
    	Cipher cipher = Cipher.getInstance("RSA");
    	cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
    	
    	// Receive the encrypted AES key and decrypt it
    	int encryptedAESKeySize = dataInputStream.readInt();
    	byte[] aesKey = cipher.doFinal(Utils.readNBytes(dataInputStream, encryptedAESKeySize));
    	PintoServer.logger.verbose("%s's AES key: %s", this.networkAddress, Utils.bytesToHex(aesKey));

    	// Finish handshaking
    	this.secretKey = new SecretKeySpec(aesKey, "AES");
    	this.cipherDecryptor = Cipher.getInstance("AES/ECB/PKCS5Padding");
    	this.cipherEncryptor = Cipher.getInstance("AES/ECB/PKCS5Padding");
    	
    	this.cipherDecryptor.init(Cipher.DECRYPT_MODE, this.secretKey);
    	this.cipherEncryptor.init(Cipher.ENCRYPT_MODE, this.secretKey);
    	PintoServer.logger.info("Done handshaking with %s", this.networkAddress);
    	
    	this.readThread.start();
    }

    public void disconnect(String reason) {
    	this.disconnect(reason, false);
    }
    
    public void disconnect(String reason, boolean noDisconnectEvent) {
    	boolean sendEvent = this.isConnected && !noDisconnectEvent;
    	this.isConnected = false;

        if (this.socket != null) {
			try {
				this.socket.close();
			} catch (Exception ex) {
				// Ignore any close exceptions, as we are cleaning up
			}	
        }
        
        this.socket = null;
        this.inputStream = null;
        this.outputStream = null;
        this.readThread = null;
        
        if (sendEvent)
        	this.disconnected.call(reason);
    }

    public void sendPacket(Packet packet) {
    	if (!this.isConnected) return;

    	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    	DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
    	
    	try {
        	synchronized (this.sendLock) {
        		dataOutputStream.write("PMSG".getBytes(StandardCharsets.US_ASCII)); // Header
        		dataOutputStream.writeInt(packet.getID()); // ID
            	packet.write(dataOutputStream); // Data
        		dataOutputStream.flush();
        	}
        	
        	byte[] packetData = byteArrayOutputStream.toByteArray();
        	byte[] encryptedPacketData = this.cipherEncryptor.doFinal(packetData);
        	byteArrayOutputStream.close();
        	
        	this.outputStream.write(Utils.intToBytes(encryptedPacketData.length));
        	this.outputStream.write(encryptedPacketData);
        	this.outputStream.flush();
    	} catch (Exception ex) {
            this.disconnect(String.format("Internal error (%s)", ex.getMessage()));
            PintoServer.logger.throwable(ex);
    	}
    }

    private void readThread_Func() {
    	while (this.isConnected) {
    		try {
    			byte[] encryptedDataSize = new byte[4];
    			this.inputStream.read(encryptedDataSize);
    			
    			byte[] encryptedData = new byte[Utils.bytesToInt(encryptedDataSize)];
    			int readAmount = this.inputStream.read(encryptedData);
    			if (readAmount == -1) throw new SocketException("Client disconnect");
    			
    			byte[] decryptedData = this.cipherDecryptor.doFinal(encryptedData);
    			DataInputStream dataInputStream = 
    					new DataInputStream(new ByteArrayInputStream(decryptedData));
    			
                int headerPart0 = dataInputStream.read();
                int headerPart1 = dataInputStream.read();
                int headerPart2 = dataInputStream.read();
                int headerPart3 = dataInputStream.read();
                
                // PMSG
                if (headerPart0 != 'P' || 
                	headerPart1 != 'M' || 
                	headerPart2 != 'S' || 
                	headerPart3 != 'G') {
                	throw new SocketException("Bad packet header!");
                }

                int id = dataInputStream.readInt();
                Packet packet = Packets.getPacketByID(id);

                if (packet == null) {
                	throw new SocketException(String.format("Bad packet ID: %d", id));
                }

                packet.read(dataInputStream);
                receivedPacket.call(packet);
                
                Thread.sleep(1);
    		} catch (Exception ex) {
                if (!(ex instanceof IOException || ex instanceof SocketException)) {
                    this.disconnect(String.format("Internal error (%s)", ex.getMessage()));
                    PintoServer.logger.throwable(ex);
                } else {
                    this.disconnect(ex.getMessage());
                }
                return;
    		}
    	}
    }
}
