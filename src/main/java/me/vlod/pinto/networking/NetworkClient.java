package me.vlod.pinto.networking;

import java.io.BufferedOutputStream;
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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
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

            this.socket.setSoTimeout(20000);
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
    	
    	// Receive the encrypted AES details and decrypt them
    	int encryptedAESKeySize = dataInputStream.readInt();
    	byte[] aesKey = cipher.doFinal(Utils.readNBytes(dataInputStream, encryptedAESKeySize));
    	PintoServer.logger.verbose("%s's AES key: %s", this.networkAddress, Utils.bytesToHex(aesKey));

    	// Finish handshaking
    	this.secretKey = new SecretKeySpec(aesKey, "AES");
    	this.cipherDecryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
    	this.cipherEncryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
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

    public IvParameterSpec getIV() throws NoSuchAlgorithmException, NoSuchPaddingException {
        SecureRandom secureRandom = SecureRandom.getInstanceStrong();
        byte[] iv = new byte[16];
        secureRandom.nextBytes(iv);
        return new IvParameterSpec(iv);
    }
    
    public void sendPacket(Packet packet) {
    	if (!this.isConnected) return;

    	ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    	DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
    	BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(this.outputStream, 4096);
    	
    	try {
        	synchronized (this.sendLock) {
        		dataOutputStream.writeInt(packet.getID()); // ID
            	packet.write(dataOutputStream); // Data
        		dataOutputStream.flush();
        	}
        	
        	IvParameterSpec iv = this.getIV();
        	this.cipherEncryptor.init(Cipher.ENCRYPT_MODE, this.secretKey, iv);
        	byte[] packetData = byteArrayOutputStream.toByteArray();
        	byte[] encryptedPacketData = this.cipherEncryptor.doFinal(packetData);
        	byteArrayOutputStream.close();
        	
        	bufferedOutputStream.write("PMSG".getBytes(StandardCharsets.US_ASCII));
        	bufferedOutputStream.write(iv.getIV());
        	bufferedOutputStream.write(Utils.intToBytes(encryptedPacketData.length));
        	bufferedOutputStream.write(encryptedPacketData);
        	bufferedOutputStream.flush();
    	} catch (Exception ex) {
            this.disconnect(String.format("Internal error (%s)", ex.getMessage()));
            PintoServer.logger.throwable(ex);
    	}
    }

    private void processReceivedEncryptedData(byte[] encryptedData, byte[] iv) throws Exception {
    	this.cipherDecryptor.init(Cipher.DECRYPT_MODE, this.secretKey, new IvParameterSpec(iv));
		byte[] decryptedData = this.cipherDecryptor.doFinal(encryptedData);
		DataInputStream dataInputStream = 
				new DataInputStream(new ByteArrayInputStream(decryptedData));

        int id = dataInputStream.readInt();
        Packet packet = Packets.getPacketByID(id);

        if (packet == null) {
        	throw new SocketException(String.format("Bad packet ID: %d", id));
        }

        packet.read(dataInputStream);
        receivedPacket.call(packet);
    }
    
    private void readThread_Func() {
    	while (this.isConnected) {
    		try {
                int headerPart0 = this.inputStream.read();
                int headerPart1 = this.inputStream.read();
                int headerPart2 = this.inputStream.read();
                int headerPart3 = this.inputStream.read();

                if (headerPart0 == -1 || 
                    headerPart1 == -1 || 
                    headerPart2 == -1 || 
                    headerPart3 == -1) {
                	throw new SocketException("Client disconnect");
                }
                
                if (headerPart0 != 'P' || 
                	headerPart1 != 'M' || 
                	headerPart2 != 'S' || 
                	headerPart3 != 'G') {
                	throw new SocketException("Bad packet header!");
                }
    			
    			byte[] iv = new byte[16];
    			this.inputStream.read(iv);
    			
    			byte[] encryptedDataSize = new byte[4];
    			this.inputStream.read(encryptedDataSize);
    			
    			byte[] encryptedData = new byte[Utils.bytesToInt(encryptedDataSize)];
    			int readAmount = this.inputStream.read(encryptedData);
    			if (readAmount == -1) throw new SocketException("Client disconnect");
    			
    			this.processReceivedEncryptedData(encryptedData, iv);
                
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
