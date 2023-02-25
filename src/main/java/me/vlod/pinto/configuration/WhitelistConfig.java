package me.vlod.pinto.configuration;

import java.util.ArrayList;

public class WhitelistConfig implements Config {
	public static WhitelistConfig instance;
	public ArrayList<String> users = new ArrayList<String>();
	public ArrayList<String> ips = new ArrayList<String>();
}
