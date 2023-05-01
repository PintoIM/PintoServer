package me.vlod.pinto.configuration;

public class MainConfig implements Config {
	public static MainConfig instance;
	public int listenPort = 2407;
	public String listenIP = "0.0.0.0";
	public String databaseFile = "pintoserver.db";
	public boolean useWhiteList = false;
}
