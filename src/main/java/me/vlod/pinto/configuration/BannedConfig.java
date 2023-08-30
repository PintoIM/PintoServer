package me.vlod.pinto.configuration;

import java.util.HashMap;

public class BannedConfig implements Config {
	public static BannedConfig instance;
	public HashMap<String, String> users = new HashMap<String, String>();
	public HashMap<String, String> ips = new HashMap<String, String>();
}
