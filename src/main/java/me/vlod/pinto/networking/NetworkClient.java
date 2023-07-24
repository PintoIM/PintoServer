package me.vlod.pinto.networking;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import me.vlod.pinto.Delegate;
import me.vlod.pinto.PintoServer;
import me.vlod.pinto.Utils;
import me.vlod.pinto.networking.packet.Packet;
import me.vlod.pinto.networking.packet.Packets;

public class NetworkClient {
    public boolean isConnected;
    private Socket socket;
    public DataInputStream inputStream;
    public DataOutputStream outputStream;
    private Thread readThread;
    public Delegate disconnected = Delegate.empty;
    public Delegate receivedPacket = Delegate.empty;
    public Delegate receivesPTAPIdentifier = Delegate.empty;
    private Object sendLock = new Object();
    
    public NetworkClient(Socket socket) {
    	this.socket = socket;
    }
    
    public void start() {
        try {
            this.isConnected = true;

            this.inputStream = new DataInputStream(this.socket.getInputStream());
            this.outputStream = new DataOutputStream(this.socket.getOutputStream());
            
            this.readThread = new Thread("Client-Read-Thread") {
            	@Override
            	public void run() {
            		readThread_Func();
            	}
            };
            this.readThread.start();
        } catch (Exception ex) {
            this.disconnect(null);
        }
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

    	ByteArrayOutputStream packetData = new ByteArrayOutputStream();
    	DataOutputStream packetDataOS = new DataOutputStream(packetData);
    	
    	try {
        	synchronized (this.sendLock) {
        		packetDataOS.write("PMSG".getBytes(StandardCharsets.US_ASCII));        		
        		packetDataOS.writeInt(packet.getSize());
        		packetDataOS.writeInt(packet.getID());
        		if (packet.getSize() > 0) {
            		packet.write(packetDataOS);
        		}
        		
        		packetData.flush();
        		packetData.writeTo(this.outputStream);
        		packetData.close();
        		packetDataOS.close();
        		
        		this.outputStream.flush();
        	}
    	} catch (Exception ex) {
            this.disconnect(String.format("Internal error (%s)", ex.getMessage()));
            PintoServer.logger.throwable(ex);
    	}
    }

    private void readThread_Func() {
    	while (this.isConnected) {
    		try {
                int headerPart0 = inputStream.read();
                
                if (headerPart0 == 0xDF) {
                	this.receivesPTAPIdentifier.call();
                	return;
                }
                
                int headerPart1 = inputStream.read();
                int headerPart2 = inputStream.read();
                int headerPart3 = inputStream.read();
                
                if (headerPart0 == -1 || 
                	headerPart1 == -1 || 
                	headerPart2 == -1 || 
                	headerPart3 == -1) 
                	throw new SocketException("Client disconnect");
                
                // PMSG
                if (headerPart0 != 0x50 || 
                	headerPart1 != 0x4d || 
                	headerPart2 != 0x53 || 
                	headerPart3 != 0x47) {
                	throw new SocketException("Bad packet header!");
                }

                // TODO: Remove the size as it's useless
                int size = inputStream.readInt();
                int id = inputStream.readInt();
                Packet packet = Packets.getPacketByID(id);

                if (packet == null) {
                	throw new SocketException(String.format("Bad packet ID: %d", id));
                }

                if (size > 0) {
                    byte[] data = Utils.readNBytes(inputStream, size);
                    DataInputStream tempReader = new DataInputStream(new ByteArrayInputStream(data));
                    packet.read(tempReader);
                    tempReader.close();
                }

                this.receivedPacket.call(packet);
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
