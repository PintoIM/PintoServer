package org.pintoim.pinto.plugin;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.pintoim.pinto.PintoServer;
import org.pintoim.pinto.consolehandler.ConsoleCommand;
import org.pintoim.pinto.consolehandler.ConsoleVariable;

public class PluginManager {
	private final ArrayList<Plugin> loadedPlugins = new ArrayList<Plugin>();
	private final HashMap<Plugin, ArrayList<ConsoleCommand>> pluginCommands = 
			new HashMap<Plugin, ArrayList<ConsoleCommand>>(); 
	private final HashMap<Plugin, ArrayList<ConsoleVariable>> pluginVariables = 
			new HashMap<Plugin, ArrayList<ConsoleVariable>>();
	
	/**
	 * Gets a {@link Plugin} instance from the specified file
	 * 
	 * @param file the file to try to load a plugin instance
	 * @return the plugin instance, or null on failure
	 */
	public static Plugin getPluginFromFile(File file) {
		PintoServer.logger.info("Instantiating %s as a plugin...", file.getAbsolutePath());
		
		try {
			URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { file.toURI().toURL() });
			InputStream mainStream = classLoader.getResourceAsStream("main.txt");
			String main = IOUtils.toString(mainStream, StandardCharsets.US_ASCII);
			Class<?> pluginMain = classLoader.loadClass(main);
			Object plugin = pluginMain.getConstructor().newInstance();
			
			if (!(plugin instanceof Plugin)) {
				throw new Exception("Plugin main is not implementing the Plugin interface!");
			}
			 
			return (Plugin)plugin;
		} catch (Exception ex) {
			PintoServer.logger.info("Unable to instantiate %s as a plugin!", file.getAbsolutePath());
			PintoServer.logger.throwable(ex);
			return null;
		}
	}
	
	/**
	 * Attempts to load a plugin
	 * 
	 * @param plugin the plugin to load, to get an instance, refer to {@link #getPluginFromFile}
	 */
	public void loadPlugin(Plugin plugin) {
		if (plugin == null) {
			return;
		}
		
		try {
			PluginInformation pluginInformation = plugin.getInfo();
			PintoServer.logger.info("Loading plugin \"%s\" by %s (ver %s)...", 
					pluginInformation.getName(), 
					pluginInformation.getAuthor(), 
					pluginInformation.getVersion());
			plugin.onLoad();
			PintoServer.logger.info("Loaded plugin \"%s\" by %s (ver %s)!", 
					pluginInformation.getName(), 
					pluginInformation.getAuthor(), 
					pluginInformation.getVersion());
		} catch (Exception ex) {
			PintoServer.logger.error("Unable to load a plugin!");
			PintoServer.logger.throwable(ex);
		}
		
		this.loadedPlugins.add(plugin);
		this.pluginCommands.put(plugin, new ArrayList<ConsoleCommand>());
		this.pluginVariables.put(plugin, new ArrayList<ConsoleVariable>());
	}
	
	/**
	 * Loads all the plugins that are .jar files in the specified folder
	 * 
	 * @param folder the folder to load from, if it doesn't exist, it is created
	 */
	public void loadPlugins(File folder) {
		if (!folder.isDirectory()) {
			folder.delete();
		}
		
		if (!folder.exists()) {
			folder.mkdir();
			return;
		}
		
		for (File plugin : folder.listFiles()) {
			if (!plugin.getName().endsWith(".jar")) continue;
			this.loadPlugin(PluginManager.getPluginFromFile(plugin));
		}
	}
	
	/**
	 * Gets the current server instance
	 * 
	 * @return the instance
	 */
	public static PintoServer getServer() {
		return PintoServer.instance;
	}
	
	/**
	 * Gets the current instance of PluginManager
	 * 
	 * @return the instance
	 */
	public static PluginManager getInstance() {
		return PluginManager.getServer().pluginManager;
	}
	
	/**
	 * Registers a console command from the specified plugin
	 * 
	 * @param command the command to register
	 * @param plugin the plugin
	 */
	public void registerConsoleCommand(ConsoleCommand command, Plugin plugin) {
		ArrayList<ConsoleCommand> commands = this.pluginCommands.get(plugin);
		if (commands.contains(command)) return;
		getServer().consoleHandler.commands.add(command);
		commands.add(command);
	}
	
	/**
	 * Registers a console variable from the specified plugin
	 * 
	 * @param variable the variable to register
	 * @param plugin the plugin
	 */
	public void registerConsoleVariable(ConsoleVariable variable, Plugin plugin) {
		ArrayList<ConsoleVariable> variables = this.pluginVariables.get(plugin);
		if (variables.contains(variable)) return;
		getServer().consoleHandler.variables.add(variable);
		variables.add(variable);
	}
}
