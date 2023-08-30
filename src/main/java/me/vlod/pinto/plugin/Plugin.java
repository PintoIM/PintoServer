package me.vlod.pinto.plugin;

public interface Plugin {
	public PluginInformation getInfo();
	public void onLoad();
}