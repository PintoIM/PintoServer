package me.vlod.pinto;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import org.json.JSONObject;

import me.vlod.hottyevents.Event;
import me.vlod.hottyevents.EventSender;
import me.vlod.pinto.configuration.BannedConfig;
import me.vlod.pinto.configuration.ConfigLoaderSaver;
import me.vlod.pinto.configuration.MainConfig;
import me.vlod.pinto.configuration.WhitelistConfig;
import me.vlod.pinto.console.Console;
import me.vlod.pinto.consolehandler.ConsoleCaller;
import me.vlod.pinto.consolehandler.ConsoleHandler;
import me.vlod.pinto.logger.LogLevel;
import me.vlod.pinto.logger.Logger;
import me.vlod.pinto.networking.NetHandlerUtils;
import me.vlod.pinto.networking.NetworkAddress;
import me.vlod.pinto.networking.NetworkClient;
import me.vlod.pinto.networking.NetworkHandler;
import me.vlod.pinto.networking.packet.Packet;
import me.vlod.pinto.plugin.PluginManager;
import me.vlod.sql.SQLInterface;
import me.vlod.sql.SQLiteInterface;

public class PintoServer implements Runnable {
	public static final String VERSION_STRING = "exp1";
	public static final String ASCII_LOGO = ""
			+ " _____ _       _        _____                          \n"
			+ "|  __ (_)     | |      / ____|                         \n"
			+ "| |__) | _ __ | |_ ___| (___   ___ _ ____   _____ _ __ \n"
			+ "|  ___/ | '_ \\| __/ _ \\\\___ \\ / _ \\ '__\\ \\ / / _ \\ '__|\n"
			+ "| |   | | | | | || (_) |___) |  __/ |   \\ V /  __/ |   \n"
			+ "|_|   |_|_| |_|\\__\\___/_____/ \\___|_|    \\_/ \\___|_|";
	public static final String TABLE_NAME = "pinto";
	public static PintoServer instance;
	public static Logger logger;
	public boolean running;
	public Console console;
	public Scanner cliScanner;
	public ConsoleHandler consoleHandler;
	public SQLInterface database;
	public ServerSocket serverSocket;
	public final ArrayList<NetworkHandler> clients = new ArrayList<NetworkHandler>();
	public PluginManager pluginManager;
	public final EventSender<Event> eventSender = new EventSender<Event>();
	
	static {
		// Logger setup
		logger = new Logger();
		
		// Console target
		logger.targets.add(new Delegate() {
			@Override
			public void call(Object... args) {
				String str = (String) args[0];
				Color color = (Color) args[1];
				
				if (instance != null && instance.console != null) {
					instance.console.write(str, color);
				}
			}
		});
		
		// Stdout and Stderr target
		logger.targets.add(new Delegate() {
			@Override
			public void call(Object... args) {
				String str = (String) args[0];
				Color color = (Color) args[1];
				
				if (color != Color.red) {
					System.out.println(str);
				} else {
					System.err.println(str);
				}
			}
		});
	}
	
	@Override
	public void run() {
		try {
			logger.levelMultiLine(LogLevel.INFO, ASCII_LOGO);
			
			// Load configuration
			logger.info("Loading configuration...");
			this.initConfig();
			
			if (MainConfig.instance.serverID.trim().isEmpty()) {
				logger.warn("No valid server ID found, generating one for you...");
				MainConfig.instance.serverID = UUID.randomUUID().toString();
			}
			logger.info("The ID of this server is %s", MainConfig.instance.serverID);
			
			// Load database
			logger.info("Loading database...");
			this.database = new SQLiteInterface(MainConfig.instance.databaseFile);
			
			if (!this.database.doesTableExist(TABLE_NAME)) {
				LinkedHashMap<String, String> columns = new LinkedHashMap<String, String>();
				columns.put("name", "varchar(16)");
				columns.put("passwordhash", "varchar(32)");
				columns.put("laststatus", "int");
				columns.put("contacts", "text");
				this.database.createTable(TABLE_NAME, columns);
			}
			
			// Clean database
			logger.info("Cleaning database...");
			for (String[] row : this.database.getRows(TABLE_NAME)) {
				new UserDatabaseEntry(this, row[0]).load();
			}
			
			// Initialization
			logger.info("Initializing...");
			logger.info("The server will listen on %s:%d, you can change this in the configuration",
					MainConfig.instance.listenIP, MainConfig.instance.listenPort);
			
			// Set runnable to true, which will allow the loops to work
			this.running = true;
			// Create the server socket, this might throw an exception, 
			// usually when it can't bind to the port
			this.serverSocket = new ServerSocket(MainConfig.instance.listenPort, 50, 
					InetAddress.getByName(MainConfig.instance.listenIP));
			// Create the CLI scanner only when the CLI is present or when it is forced
			if (System.console() != null || System.getProperty("pinto.forceConsole") != null) {
				this.cliScanner = new Scanner(System.in);
			}
			// Create the console handler that will handle console commands
			this.consoleHandler = new ConsoleHandler(this, new ConsoleCaller(null));
			
			logger.info("Loading plugins...");
			this.pluginManager = new PluginManager();
			this.pluginManager.loadPlugins(new File(MainConfig.instance.pluginsDir));
			
			logger.info("Initialized! Listening on %s:%d...", 
					MainConfig.instance.listenIP, MainConfig.instance.listenPort);
		} catch (Throwable throwable) {
			logger.fatal("!!! STARTUP FAILURE !!!");
			logger.fatal("The server wasn't able to start! Is there another server running?");
			logger.throwable(throwable);
			if (this.console != null) {
				logger.fatal("You may close this window to quit");
			}

			if (this.console != null) {
				// Loop forever to allow the user to read the logs
				while (true) {
					// Make the loop sleep to prevent high CPU usage
		    		try {
						Thread.sleep(1);
					} catch (Exception ex) {
						PintoServer.logger.throwable(ex);
					}
				}
			} else {
				this.stop();
				return;
			}
		}
		
		// Network accept thread
		new Thread("Network-Accept-Thread") {
			public void run() {
				while (running) {
					try {
						Socket socket = serverSocket.accept();
						NetworkAddress address = new NetworkAddress(socket);
						NetworkClient client = new NetworkClient(socket);
						clients.add(new NetworkHandler(PintoServer.instance, address, client));
						
						// Make the loop sleep to prevent high CPU usage
			    		try {
							Thread.sleep(1);
						} catch (Exception ex) {
							PintoServer.logger.throwable(ex);
						}
					} catch (Exception ex) {
						if (running) {
							logger.throwable(ex);
						}
					}
				}
			}
		}.start();
		
		// Tick thread
		new Thread("Tick-Thread") {
			public void run() {
				long lastTime = 0;
				
				while (running) {
					if (System.currentTimeMillis() - lastTime > 1000) {
						try {
							for (NetworkHandler handler : clients.toArray(new NetworkHandler[0])) {
								handler.onTick();
							}
						} catch (Exception ex) {
							if (running) {
								logger.throwable(ex);
							}
						}
						lastTime = System.currentTimeMillis();
					}
					
					// Make the loop sleep to prevent high CPU usage
		    		try {
						Thread.sleep(1);
					} catch (Exception ex) {
						PintoServer.logger.throwable(ex);
					}
				}
			}
		}.start();
		
		// Heart beat thread
		new Thread("Heartbeat-Thread") {
			public void run() {
				long lastTime = 0;
				
				while (running) {
					if (System.currentTimeMillis() - lastTime > 45 * 1000) {
						sendHeartbeat();
						lastTime = System.currentTimeMillis();
					}
					
					// Make the loop sleep to prevent high CPU usage
		    		try {
						Thread.sleep(1);
					} catch (Exception ex) {
						PintoServer.logger.throwable(ex);
					}
				}
			}
		}.start();
		
		// Main loop (code past this point wont ever be ran)
		while (this.running) {
			// Handler CLI input
			if (this.cliScanner != null) {
				System.out.print("> ");
				String cliInput = this.cliScanner.nextLine();
				this.onConsoleSubmit(cliInput);	
			}
			
			// Make the loop sleep to prevent high CPU usage
    		try {
				Thread.sleep(1);
			} catch (Exception ex) {
				PintoServer.logger.throwable(ex);
			}
		}
	}
	
	public void stop() {
		this.stop(null);
	}
	
	public void stop(String kickReason) {
		logger.info("Stopping...");
		
		// Kick everyone online
		if (kickReason == null || kickReason.isEmpty()) {
			kickReason = "Server shutting down!";
		} else {
			kickReason = String.format("Server shutting down: %s", kickReason);
		}
		this.disconnectAllClients(kickReason);
		
		// Set running to false to tell every permanent loop to stop
		this.running = false;
		
		// Close the CLI scanner
		if (this.cliScanner != null) {
			this.cliScanner.close();
		}
		
		// Shutdown the server socket
		if (this.serverSocket != null) {
			try {
				this.serverSocket.close();
			} catch (Exception ex) {
			}
		}
		
		// Close the console window
		if (this.console != null) {
			this.console.hide();
		}

		// Exit forcefully in case of a hard failure
		System.exit(0);
	}

	public void initConfig() {
		this.loadConfig();
		this.saveConfig();
	}
	
	public void loadConfig() {
		ConfigLoaderSaver cls = new ConfigLoaderSaver(MainConfig.instance = new MainConfig(), 
				new File("main.yml"));
		cls.load();
		
		cls = new ConfigLoaderSaver(WhitelistConfig.instance = new WhitelistConfig(), 
				new File("whitelist.yml"));
		cls.load();
		
		cls = new ConfigLoaderSaver(BannedConfig.instance = new BannedConfig(), 
				new File("banned.yml"));
		cls.load();
	}
	
	public void saveConfig() {
		ConfigLoaderSaver cls = new ConfigLoaderSaver(MainConfig.instance, new File("main.yml"));
		cls.save();
		
		cls = new ConfigLoaderSaver(WhitelistConfig.instance, new File("whitelist.yml"));
		cls.save();
		
		cls = new ConfigLoaderSaver(BannedConfig.instance, new File("banned.yml"));
		cls.save();
	}
	
	public void onConsoleSubmit(String input) {
		this.consoleHandler.handleInput(input);
	}
	
	public void sendHeartbeat() {
		if (MainConfig.instance.heartbeatURL.trim().isEmpty()) {
			return;
		}
		
		HttpURLConnection httpConnection = null;
		
		try {
			httpConnection = (HttpURLConnection) new URL(MainConfig.instance.heartbeatURL)
					.openConnection();
			httpConnection.setRequestMethod("POST");
			httpConnection.setRequestProperty("User-Agent", "PintoServer");
			httpConnection.setDoOutput(true);
			httpConnection.setDoInput(true);
			httpConnection.connect();

			OutputStream outputStream = httpConnection.getOutputStream();
			
			int onlineClients = 0;
			for (NetworkHandler client : this.clients) {
				if (client.loggedIn && NetHandlerUtils
						.getToOthersStatus(client.databaseEntry.status) != UserStatus.OFFLINE) {
					onlineClients++;
				}
			}
			
			JSONObject payload = new JSONObject();
			payload.put("name", MainConfig.instance.heartbeatName);
			payload.put("port", MainConfig.instance.listenPort);
			payload.put("users", onlineClients);
			payload.put("maxUsers", MainConfig.instance.maxUsers);
			payload.put("tags", MainConfig.instance.heartbeatTags);

			PrintWriter printWriter = new PrintWriter(outputStream);
			printWriter.println(payload.toString());
			printWriter.flush();

			InputStream inputStream = null;
			if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				inputStream = httpConnection.getInputStream();
			} else {
				inputStream = httpConnection.getErrorStream();
			}
			
			BufferedReader bufferedReader = new BufferedReader(
            		new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String responseline;
            
            while ((responseline = bufferedReader.readLine()) != null) {
                stringBuilder.append(String.format("%s\n", responseline));
            }
            
            String responseRaw = stringBuilder.toString();
            try {
                JSONObject response = new JSONObject(responseRaw);
    			
                if (!response.getString("status").equalsIgnoreCase("Error")) {
                	logger.info("Successfully sent a heart beat: %s", response.getString("status"));
                } else {
                	logger.warn("Sent a heart beat, but the server replied with error \"%s\"",
                			response.getString("error"));
                }
            } catch (Exception ex) {
            	throw new IOException(String.format("Illegal heartbeat response: %s", responseRaw));
            }
            
            printWriter.close();
            bufferedReader.close();
		} catch (Exception ex) {
			logger.error("Unable to send a heart beat!");
			logger.throwable(ex);
		} finally {
			if (httpConnection != null) {
				httpConnection.disconnect();
			}
		}
	}
	
	public void banUser(String target, String reason, boolean ip) {
		if (ip) {
			BannedConfig.instance.ips.put(target, reason);
			PintoServer.logger.log("Moderation", "Banned the IP \"" + target + "\" for \"" + reason + "\"");
			
			NetworkHandler[] handlers = this.getHandlersByAddress(target);
			for (NetworkHandler handler : handlers) {
				handler.kick("You have been IP banned from this chat!\nReason: " + reason);
			}
		} else {
			BannedConfig.instance.users.put(target, reason);
			PintoServer.logger.log("Moderation", "Banned the user \"" + target + "\" for \"" + reason + "\"");
			
			NetworkHandler handler = this.getHandlerByName(target);
			if (handler != null) {
				handler.kick("You have been banned from this chat!\nReason: " + reason);
			}
		}
		
		this.saveConfig();
	}
	
	public void unbanUser(String target, boolean ip) {
		if (ip) {
			BannedConfig.instance.ips.remove(target);
			PintoServer.logger.log("Moderation", "Unbanned the IP \"" + target + "\"");
		} else {
			BannedConfig.instance.users.remove(target);
			PintoServer.logger.log("Moderation", "Unbanned the user \"" + target + "\"");
		}
		this.saveConfig();
	}	

	public NetworkHandler getHandlerByName(String name) {
		for (NetworkHandler handler : this.clients.toArray(new NetworkHandler[0])) {
			if (handler.userName != null && handler.userName.equals(name)) {
				return handler;
			}
		}

		return null;
	}
	
	public NetworkHandler[] getHandlersByAddress(String address) {
		ArrayList<NetworkHandler> handlers = new ArrayList<NetworkHandler>();
		
		for (NetworkHandler handler : this.clients.toArray(new NetworkHandler[0])) {
			// Using String.equals instead of == to compensate for any weird cases
			if (handler.networkAddress.ip.equals(address)) {
				handlers.add(handler);
			}
		}

		return handlers.toArray(new NetworkHandler[0]);
	}
	
	public NetworkHandler getHandlerByAddress(NetworkAddress address) {
		for (NetworkHandler handler : this.clients.toArray(new NetworkHandler[0])) {
			// Using String.equals instead of == to compensate for any weird cases
			if (handler.networkAddress.ip.equals(address.ip) && 
				handler.networkAddress.port == address.port) {
				return handler;
			}
		}

		return null;
	}

	public void sendGlobalPacket(Packet packet, NetworkAddress... exclusionList) {
		List<NetworkAddress> exclusionListAsList = Arrays.asList(exclusionList);
		for (NetworkHandler handler : this.clients.toArray(new NetworkHandler[0])) {
			if (exclusionListAsList.contains(handler.networkAddress)) continue;
			handler.sendPacket(packet);
		}
	}

	public void disconnectAllClients(String reason, NetworkAddress... exclusionList) {
		List<NetworkAddress> exclusionListAsList = Arrays.asList(exclusionList);
		for (NetworkHandler handler : this.clients.toArray(new NetworkHandler[0])) {
			if (exclusionListAsList.contains(handler.networkAddress)) continue;
			handler.kick(reason);
		}
	}
	
	public static void main(String[] args) {
		PintoServer pintoServer = new PintoServer();
		
		// Create the graphical console if we aren't in a headless environment
		if (!GraphicsEnvironment.isHeadless() && System.getProperty("pinto.noGUI") == null) {
			pintoServer.console = new Console();
			pintoServer.console.onSubmit = new Delegate() {
				@Override
				public void call(Object... args) {
					pintoServer.onConsoleSubmit((String)args[0]);
				}
			};
			pintoServer.console.onClose = new Delegate() {
				@Override
				public void call(Object... args) {
					pintoServer.stop();
				}
			};
			pintoServer.console.show();
		}

		(new Thread(PintoServer.instance = pintoServer, "Pinto-Main-Thread")).start();
	}
}
