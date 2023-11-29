package me.vlod.pinto.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

import javax.crypto.SecretKey;

import me.vlod.pinto.networking.packet.Packet;

public interface NetworkManager {
	/**
	 * Notifies that handshaking has ended
	 * 
	 * @param secretKey the secret key that was handshaked
	 */
	void onHandshaked(SecretKey secretKey) throws Exception;
	
	/**
	 * Sets the network handler
	 * 
	 * @param netHandler the network handler
	 */
	void setNetHandler(NetBaseHandler netHandler);
	
	/**
	 * Adds a packet to the send queue
	 * 
	 * @param packet the packet
	 */
	void addToQueue(Packet packet);
	
	/**
	 * Shuts down the connection with the specified reason
	 * Typically used for errors or other non-user requested closures
	 * 
	 * @param reason the reason
	 */
	void shutdown(String reason);
	
	/**
	 * Processes read packets
	 */
	void processReceivedPackets() throws IOException;
	
	/**
	 * Gets the remote socket address
	 * 
	 * @return the remote socket address
	 */
	InetSocketAddress getSocketAddress();
	
	/**
	 * Gets the network address
	 * 
	 * @return the network address
	 */
	NetworkAddress getAddress();
	
	/**
	 * Returns the data input stream used for reading
	 * NEVER use this after handshaking
	 * 
	 * @return the input stream
	 */
	DataInputStream getInputStream();
	
	/**
	 * Returns the data output stream used for writing
	 * NEVER use this after handshaking
	 * 
	 * @return the output stream
	 */
	DataOutputStream getOutputStream();
	
	/**
	 * Interrupts the read and write threads
	 */
	void interrupt();
	
	/**
	 * Closes the connection
	 * Typically used for user requested closure
	 */
	void close();
}
