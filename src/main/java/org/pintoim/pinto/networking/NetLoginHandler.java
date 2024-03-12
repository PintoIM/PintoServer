package org.pintoim.pinto.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.pintoim.pinto.PintoServer;
import org.pintoim.pinto.UserDatabaseEntry;
import org.pintoim.pinto.UserStatus;
import org.pintoim.pinto.Utils;
import org.pintoim.pinto.configuration.MainConfig;
import org.pintoim.pinto.networking.packet.Packet;
import org.pintoim.pinto.networking.packet.PacketLogin;
import org.pintoim.pinto.networking.packet.PacketRegister;

public class NetLoginHandler extends NetBaseHandler {
	private PintoServer instance;
	private int notLoggedInTimer;
	private boolean doingLogin;
	private boolean handshaking;
	private UserDatabaseEntry databaseEntry;

	public NetLoginHandler(PintoServer instance, NetworkManager netManager, String threadName) throws IOException {
		this.instance = instance;
		this.netManager = netManager;
		this.netManager.setNetHandler(this);
		this.logger.info(String.format("%s has connected", this.netManager.getAddress()));
		if (!(this.netManager instanceof NetworkUDPManager)) {
			new Thread(threadName + "-Handshaker") {
				@Override
				public void run() {
					try {
						NetLoginHandler.this.handshaking = true;
						NetLoginHandler.this.handshakeAESKey(instance.rsaPublicKey, instance.rsaPrivateKey);
					} catch (Exception ex) {
						PintoServer.logger.error("Failed to handshake with %s: %s", 
								NetLoginHandler.this, Utils.getThrowableStackTraceAsStr(ex));
						NetLoginHandler.this.netManager.close();
					} finally {
						NetLoginHandler.this.handshaking = false;
					}
				}
			}.start();	
		}
	}

    public void handshakeAESKey(RSAPublicKey rsaPublicKey, RSAPrivateKey rsaPrivateKey) throws Exception {
    	DataInputStream dataInputStream = this.netManager.getInputStream();
    	DataOutputStream dataOutputStream = this.netManager.getOutputStream();
    	PintoServer.logger.info("Handshaking AES key with %s...", this);

    	// Send our public key
    	byte[] rsaPublicKeyBytes = rsaPublicKey.getEncoded();
    	// Use this instead of writeInt to not send 4 different TCP packets
    	dataOutputStream.write(Utils.intToBytes(rsaPublicKeyBytes.length));
    	dataOutputStream.write(rsaPublicKeyBytes);
    	
    	// Get a cipher using our private key
    	Cipher cipher = Cipher.getInstance("RSA");
    	cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
    	
    	// Receive the encrypted AES details and decrypt them
    	int encryptedAESKeySize = dataInputStream.readInt();
    	while (encryptedAESKeySize == 0x7FFFFFFF) {
    		this.notLoggedInTimer = 0;
    		encryptedAESKeySize = dataInputStream.readInt();
    		Thread.sleep(100);
    	}
    	
    	byte[] aesKey = cipher.doFinal(Utils.readNBytes(dataInputStream, encryptedAESKeySize));
    	PintoServer.logger.verbose("%s's AES key: %s", this, Utils.bytesToHex(aesKey));

    	// Finish handshaking
    	SecretKey secretKey = new SecretKeySpec(aesKey, "AES");
    	this.netManager.onHandshaked(secretKey);
    	PintoServer.logger.info("Done handshaking with %s", this);
    }
	
    @Override
	public void onUpdate() throws IOException {
		if (this.notLoggedInTimer++ == 50) { // 5 seconds
			this.kick("Took too long to log in");
		} else if (!this.handshaking) {
			this.netManager.processReceivedPackets();
			this.netManager.interrupt();
		}
	}

	@Override
	public void handlePacket(Packet packet) {
		logger.verbose("Received %s from %s", packet, this);
		
		if (packet instanceof PacketLogin) {
			this.handleLoginPacket((PacketLogin)packet);
		} else if (packet instanceof PacketRegister) {
			this.handleRegisterPacket((PacketRegister)packet);
		} else {
			super.handlePacket(packet);
		}
	}
	
	public void handleLoginPacket(PacketLogin packet) {
		if (this.doingLogin) {
			return;
		}
		this.doingLogin = true;
		
		// Perform all checks on the user's input
		if (!NetUtils.performModerationChecks(this, packet.name) || 
			!NetUtils.performProtocolCheck(this, packet.protocolVersion, packet.clientVersion) ||
			!NetUtils.performNameVerification(this, packet.name)) {
			return;
		}

    	if (this.instance.clients.size() + 1 > MainConfig.instance.maxUsers) {
    		this.kick("The server is full!");
    		return;
    	}
		
    	// Check if the user name is already online
		this.userName = packet.name;
    	if (this.instance.getHandlerByName(this.userName) != null) {
    		this.kick("Someone with this username is already connected!");
    		return;
    	}
    	
    	// Check if the client is not registered
    	if (!UserDatabaseEntry.isRegistered(this.instance, this.userName)) {
    		this.kick("Invalid username or password!");
    		return;
    	}
    	
    	// Load the database entry
    	this.databaseEntry = new UserDatabaseEntry(this.instance, this.userName);
    	this.databaseEntry.load();
    	
    	if (!this.databaseEntry.passwordHash.equalsIgnoreCase(packet.passwordHash)) {
    		this.kick("Invalid username or password!");
    		return;
    	}
		this.doLogin(packet.clientVersion);
	}

	public void handleRegisterPacket(PacketRegister packet) {
		if (this.doingLogin) {
			return;
		}
		this.doingLogin = true;
		
		// Perform all checks on the user's input
		if (!NetUtils.performModerationChecks(this, packet.name) || 
			!NetUtils.performProtocolCheck(this, packet.protocolVersion, packet.clientVersion) ||
			!NetUtils.performNameVerification(this, packet.name)) {
			return;
		}

    	this.userName = packet.name;
    	if (UserDatabaseEntry.isRegistered(this.instance, userName)) {
    		this.kick("This account already exists!");
    		return;
    	}
    	
    	if (!packet.passwordHash.matches(NetUtils.PASSWORD_REGEX_CHECK)) {
    		this.kick("Illegal password hash! Attempted SQL injection?");
    		return;
    	}
    	
    	if (MainConfig.instance.noClientRegistrations) {
    		this.kick("Sorry! Registrations are currently disabled!");
    		return;
    	}
    	
    	// Create the database entry
    	this.databaseEntry = UserDatabaseEntry.registerAndReturnEntry(this.instance, packet.name,
    			packet.passwordHash, UserStatus.ONLINE);
    	this.doLogin(packet.clientVersion);
    }
	
	
	private void doLogin(String clientVersion) {
    	NetServerHandler netHandler = new NetServerHandler(this.instance, this.netManager, 
    			this.userName, this.databaseEntry, clientVersion);
    	this.instance.networkServer.addServerHandler(netHandler);
		this.connectionClosed = true;
	}
}
