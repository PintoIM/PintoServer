package me.vlod.pinto.configuration;

import java.util.HashMap;

public class MainConfig implements Config {
	public static MainConfig instance;
	public int listenPort = 2407;
	public String listenIP = "0.0.0.0";
	public String databaseFile = "pintoserver.db";
	public boolean useWhiteList = false;
	public boolean ignoreClientVersion = false;
	public boolean showVerboseLogs = false;
	public int maxUsers = 128;
	public String pluginsDir = "plugins";
	public String heartbeatName = "My Cool Pinto! Server";
	public String heartbeatURL = "http://ponso00.com:8880/pinto-server-list/heartbeat.php";
	public String heartbeatTags = "";
	public String serverID = "";
	@SuppressWarnings("serial")
	public HashMap<String, String> filesToServeOnHTTP = new HashMap<String, String>() {{
		put("welcome.html", "text/html");
	}};
}
