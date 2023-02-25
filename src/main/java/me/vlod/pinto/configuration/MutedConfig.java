package me.vlod.pinto.configuration;

import java.util.HashMap;

public class MutedConfig implements Config {
	public static MutedConfig instance;
	public HashMap<String, String> users = new HashMap<String, String>();
	public HashMap<String, String> ips = new HashMap<String, String>();
}
