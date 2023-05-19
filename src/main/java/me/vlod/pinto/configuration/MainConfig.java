package me.vlod.pinto.configuration;

public class MainConfig implements Config {
	public static MainConfig instance;
	public int listenPort = 2407;
	public String listenIP = "0.0.0.0";
	public String databaseFile = "pintoserver.db";
	public boolean useWhiteList = false;
	public int maxUsers = 128;
	public String pluginsDir = "plugins";
	public String heartbeatName = "My Cool Pinto! Server";
	public String heartbeatURL = "http://api.fieme.net:8880/pinto-server-list/heartbeat.php";
	public String heartbeatTags = "";
}
