package me.vlod.pinto.configuration;

public class MainConfig implements Config {
	public static MainConfig instance;
	public int listenPort = 2407;
	public String listenIP = "0.0.0.0";
	public String databaseFile = "pintoserver.db";
	public String serverName = "My Pinto! server";
	public String serverMOTD = "Welcome to my exciting Pinto! server! Have fun! :D";
	public boolean useWhiteList = false;
	public boolean autoMute = true;
}
