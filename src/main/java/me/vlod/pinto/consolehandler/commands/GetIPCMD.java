package me.vlod.pinto.consolehandler.commands;

import me.vlod.pinto.PintoServer;
import me.vlod.pinto.consolehandler.ConsoleCaller;
import me.vlod.pinto.consolehandler.ConsoleCommand;
import me.vlod.pinto.networking.NetworkHandler;

public class GetIPCMD implements ConsoleCommand {
	@Override
	public String getName() {
		return "getip";
	}

	@Override
	public String getDescription() {
		return "Gets the IP of the specified user";
	}

	@Override
	public String getUsage() {
		return "getip <user>";
	}

	@Override
	public int getMinArgsCount() {
		return 1;
	}

	@Override
	public int getMaxArgsCount() {
		return 1;
	}

	@Override
	public void execute(PintoServer server, ConsoleCaller caller, String[] args) throws Exception {
		String target = args[0];
		NetworkHandler handler = server.getHandlerByName(target);
		
		if (handler != null) {
			caller.sendMessage(target + "'s IP address: " + handler.networkAddress.ip);
		} else {
			caller.sendMessage("Unable to find " + target + "!");
		}
	}
}
