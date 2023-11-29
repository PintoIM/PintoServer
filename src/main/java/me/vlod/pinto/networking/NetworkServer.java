package me.vlod.pinto.networking;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import me.vlod.pinto.PintoServer;
import me.vlod.pinto.Utils;
import me.vlod.pinto.logger.Logger;

public class NetworkServer {
	public static Logger logger = PintoServer.logger;
	private ServerSocket serverTCPSocket;
	//private DatagramSocket serverUDPSocket;
	private Thread listenThreadTCP;
	//private Thread listenThreadUDP;
	public volatile boolean isListening;
	private int connectionNumber;
	private ArrayList<NetLoginHandler> loginHandlers = new ArrayList<NetLoginHandler>();
	private ArrayList<NetServerHandler> serverHandlers = new ArrayList<NetServerHandler>();

	public NetworkServer(PintoServer instance, InetAddress address, int port) throws IOException {
		this.serverTCPSocket = new ServerSocket(port, 0, address);
		this.serverTCPSocket.setPerformancePreferences(0, 2, 1);
		//this.serverUDPSocket = new DatagramSocket(port, address);
		this.isListening = true;
		this.listenThreadTCP = new Thread("Network-TCP-Server") {
			@Override
			public void run() {
				while (NetworkServer.this.isListening) {
					try {
						Socket clientConnection = NetworkServer.this.serverTCPSocket.accept();

						if (clientConnection != null) {
							String threadName = "NetConn-" + NetworkServer.this.connectionNumber++;
							NetworkTCPManager netManager = new NetworkTCPManager(clientConnection, threadName, null);
							NetLoginHandler loginHandler = new NetLoginHandler(instance, netManager, threadName);
							NetworkServer.this.addLoginHandler(loginHandler);
						}
						
						Thread.sleep(1);
					} catch (Exception ex) {
						logger.severe("Failed to accept a connection: " + 
								Utils.getThrowableStackTraceAsStr(ex));
					}
				}
			}
		};
		/*this.listenThreadUDP = new Thread("Network-UDP-Server") {
			@Override
			public void run() {
				while (NetworkServer.this.isListening) {
					try {
						DatagramSocket server = NetworkServer.this.serverUDPSocket;
						
						// HEADER + SIZE
						DatagramPacket dgPacket = new DatagramPacket(new byte[8], 0, 8);
						server.receive(dgPacket);
						
						byte[] dgPacketData = dgPacket.getData();
						int headerPart0 = dgPacketData[0];
			            int headerPart1 = dgPacketData[1];
			            int headerPart2 = dgPacketData[2];
			            int headerPart3 = dgPacketData[3];

			            if (headerPart0 != 'P' || 
			            	headerPart1 != 'M' || 
			            	headerPart2 != 'S' || 
			            	headerPart3 != 'G') {
			            	logger.warn("Ignoring bad UDP packet!");
			            	continue;
			            }

			            int size = Utils.bytesToInt(new byte[] { 
			            	dgPacketData[4],
			            	dgPacketData[5],
			            	dgPacketData[6],
			            	dgPacketData[7] 
			            });
			            
			            dgPacket = new DatagramPacket(new byte[size], 0, size);
			            server.receive(dgPacket);
			            dgPacketData = dgPacket.getData();

			            NetworkUDPManager netManager = NetworkServer.this.getUDPManager(
			            		dgPacket.getAddress(), dgPacket.getPort());
			            
			            if (netManager == null) {
							String threadName = "NetConn-" + NetworkServer.this.connectionNumber++;
							netManager = new NetworkUDPManager(NetworkServer.this.serverUDPSocket, 
									dgPacket.getAddress(), dgPacket.getPort(), threadName, null);
							NetLoginHandler loginHandler = new NetLoginHandler(instance, netManager, threadName);
							NetworkServer.this.addLoginHandler(loginHandler);	
			            }
			            
			            netManager.handlePacket(dgPacketData);
					} catch (Exception ex) {
						logger.severe("Failed to accept a connection: " + 
								Utils.getThrowableStackTraceAsStr(ex));
					}
				}
			}
		};*/
		this.listenThreadTCP.start();
		//this.listenThreadUDP.start();
	}

	public void addServerHandler(NetServerHandler serverHandler) {
		if (serverHandler == null) {
			throw new IllegalArgumentException("Got NULL for server handler!");
		} else {
			this.serverHandlers.add(serverHandler);
		}
	}

	private void addLoginHandler(NetLoginHandler loginHandler) {
		if (loginHandler == null) {
			throw new IllegalArgumentException("Got NULL for login handler!");
		} else {
			this.loginHandlers.add(loginHandler);
		}
	}

	/*
	private NetworkUDPManager getUDPManager(InetAddress address, int port) {
		for (NetLoginHandler loginHandler : this.loginHandlers.toArray(new NetLoginHandler[0])) {
			if (loginHandler.netManager instanceof NetworkUDPManager) {
				NetworkAddress netAddress = loginHandler.netManager.getAddress();
				
				if (netAddress.ip.equals(address.getHostAddress()) && netAddress.port == port) {
					return (NetworkUDPManager) loginHandler.netManager;
				}
			}
		}

		for (NetServerHandler serverHandler : this.serverHandlers.toArray(new NetServerHandler[0])) {
			if (serverHandler.netManager instanceof NetworkUDPManager) {
				NetworkAddress netAddress = serverHandler.netManager.getAddress();
				
				if (netAddress.ip.equals(address.getHostAddress()) && netAddress.port == port) {
					return (NetworkUDPManager) serverHandler.netManager;
				}
			}
		}
		
		return null;
	}*/
	
	public void update() throws IOException {
		for (NetLoginHandler loginHandler : this.loginHandlers.toArray(new NetLoginHandler[0])) {
			loginHandler.onUpdate();

			if (loginHandler.connectionClosed) {
				this.loginHandlers.remove(loginHandler);
			}
		}

		for (NetServerHandler serverHandler : this.serverHandlers.toArray(new NetServerHandler[0])) {
			serverHandler.onUpdate();

			if (serverHandler.connectionClosed) {
				this.serverHandlers.remove(serverHandler);
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void stop() {
		this.isListening = false;
		this.listenThreadTCP.stop();
		try {
			this.serverTCPSocket.close();
		} catch (Exception ex) {
		}
	}
}
