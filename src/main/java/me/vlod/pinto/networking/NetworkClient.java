package me.vlod.pinto.networking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

import me.vlod.pinto.Delegate;
import me.vlod.pinto.PintoServer;
import me.vlod.pinto.networking.packet.Packet;
import me.vlod.pinto.networking.packet.Packets;

public class NetworkClient {
    private boolean noDisconnectEvent;
    public boolean isConnected;
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Thread readThread;
    private Thread sendThread;
    public Delegate disconnected = Delegate.empty;
    public Delegate receivedPacket = Delegate.empty;
    private LinkedList<Packet> packetSendQueue = new LinkedList<Packet>();
    
    public NetworkClient(Socket socket) {
        try {
            this.socket = socket;
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
            
            this.sendThread = new Thread("Client-Send-Thread") {
            	@Override
            	public void run() {
            		sendThread_Func();
            	}
            };
            this.sendThread.start();
        } catch (Exception ex) {
            this.disconnect(null);
        }
    }
    
    public void disconnect(String reason) {
        boolean noDisconnectEventValue = this.noDisconnectEvent;
        this.noDisconnectEvent = true;
        
        if (this.inputStream != null) {
			try {
				this.inputStream.close();
			} catch (Exception ex) {
			}
        }
        
        if (this.outputStream != null) {
			try {
				this.outputStream.close();
			} catch (Exception ex) {
			}
        }

        if (this.socket != null) {
			try {
				this.socket.close();
			} catch (Exception ex) {
			}	
        }

        this.socket = null;
        this.inputStream = null;
        this.outputStream = null;
        this.readThread = null;
        this.sendThread = null;

        if (this.isConnected && !noDisconnectEventValue) {
            this.disconnected.call(reason);
        }
        this.isConnected = false;
    }

    public void addToSendQueue(Packet packet) {
    	if (!this.isConnected) return;
    	this.packetSendQueue.add(packet);
    }

    public void clearSendQueue() {
    	this.packetSendQueue.clear();
    }
    
    public void flushSendQueue() {
    	if (!this.isConnected) return;
    	for (Packet packet : this.packetSendQueue.toArray(new Packet[0])) {
        	try {
        		if (!this.isConnected) return;
        		if (packet == null) continue;
        		this.outputStream.write((byte)packet.getID());
    			packet.write(this.outputStream);
    			this.outputStream.flush();
    		} catch (Exception ex) {
    			PintoServer.logger.throwable(ex);
    		}
        	this.packetSendQueue.remove(packet);
    	}
    }
    
    private void readThread_Func() {
    	while (this.isConnected) {
    		try {
				int packetID = this.inputStream.read();
				Packet packet = Packets.getPacketByID(packetID);
				
				if (packetID != -1) {
					if (packet != null) {
	                    packet.read(this.inputStream);
	                    this.receivedPacket.call(packet);
					} else {
						throw new SocketException(String.format("Received invalid packet (%d)", packetID));
					}
				} else {
					throw new SocketException("Client disconnect");
    			}
				
				// Make the loop sleep to prevent high CPU usage
	    		try {
					Thread.sleep(1);
				} catch (Exception ex) {
					PintoServer.logger.throwable(ex);
				}
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
    
    private void sendThread_Func() {
    	while (this.isConnected) {
    		this.flushSendQueue();
    		
			// Make the loop sleep to prevent high CPU usage
    		// Also here to allow the send queue to work
    		try {
				Thread.sleep(1);
			} catch (Exception ex) {
				PintoServer.logger.throwable(ex);
			}
    	}
    }
}
