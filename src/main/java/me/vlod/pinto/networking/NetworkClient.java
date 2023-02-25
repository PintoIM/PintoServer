package me.vlod.pinto.networking;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import me.vlod.pinto.Delegate;
import me.vlod.pinto.PintoServer;

public class NetworkClient {
    private boolean noDisconnectEvent;
    public boolean isConnected;
    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Thread readThread;
    public Delegate disconnected = Delegate.empty;
    public Delegate receivedPacket = Delegate.empty;
    
    public NetworkClient(Socket socket) {
        try {
            this.socket = socket;
            this.isConnected = true;

            this.inputStream = new DataInputStream(this.socket.getInputStream());
            this.outputStream = new DataOutputStream(this.socket.getOutputStream());
            
            this.readThread = new Thread() {
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

        if (this.isConnected && !noDisconnectEventValue) {
            this.disconnected.call(reason);
        }
        this.isConnected = false;
    }

    public void sendPacket(Packet packet) {
    	try {
    		if (!this.isConnected) return;
    		this.outputStream.write(packet.getID());
			packet.write(this.outputStream);
			this.outputStream.flush();
		} catch (Exception ex) {
			PintoServer.logger.throwable(ex);
		}
    }

    public void sendData(byte[] data) {
    	try {
    		if (!this.isConnected) return;
			this.outputStream.write(data);
			this.outputStream.flush();
		} catch (Exception ex) {
			PintoServer.logger.throwable(ex);
		}
    }
    
    private void readThread_Func() {
    	while (this.isConnected) {
    		try {
				int packetID = this.inputStream.read();
				Packet packet = Packets.getPacketByID(packetID);
				
				if (packetID != -1) {
					if (packet != null) {
	                    int packetSize = packet.getLength();
	                    byte[] buffer = new byte[packetSize];

	                    int readBytesTotal = 0;
	                    int readBytes = readBytesTotal = this.inputStream.read(buffer, 0, packetSize);

	                    while (readBytesTotal < packetSize) {
	                        readBytes = this.inputStream.read(buffer, readBytesTotal, packetSize - readBytesTotal);
	                        readBytesTotal += readBytes;
	                    }
	                    
	                    DataInputStream bufferStream = new DataInputStream(new ByteArrayInputStream(buffer));
	                    packet.read(bufferStream);
	                    bufferStream.close();
	                    this.receivedPacket.call(packet);
					} else {
						throw new SocketException("Received invalid packet -> " + packetID);
					}
				} else {
					throw new SocketException("Client disconnect");
    			}
    		} catch (Exception ex) {
                if (!(ex instanceof IOException || ex instanceof SocketException)) {
                    this.disconnect("Internal error -> " + ex.getMessage());
                    PintoServer.logger.throwable(ex);
                } else {
                    this.disconnect(ex.getMessage());
                }
                return;
    		}
    	}
    }
}
