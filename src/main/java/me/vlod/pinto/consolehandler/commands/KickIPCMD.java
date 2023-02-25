package me.vlod.pinto.consolehandler.commands;

import me.vlod.pinto.PintoServer;
import me.vlod.pinto.consolehandler.ConsoleCaller;
import me.vlod.pinto.consolehandler.ConsoleCommand;
import me.vlod.pinto.networking.NetworkHandler;

public class KickIPCMD implements ConsoleCommand {
	@Override
	public String getName() {
		return "kickip";
	}

	@Override
	public String getDescription() {
		return "Kicks the specified IP address";
	}

	@Override
	public String getUsage() {
		return "kickip <user> <reason>";
	}

	@Override
	public int getMinArgsCount() {
		return 2;
	}

	@Override
	public int getMaxArgsCount() {
		return 2;
	}

	@Override
	public void execute(PintoServer server, ConsoleCaller caller, String[] args) throws Exception {
		String target = args[0];
		String reason = args[1];
		
		NetworkHandler[] handlers = server.getHandlersByAddress(target);
		for (NetworkHandler handler : handlers) {
			handler.kick("You have been kicked!\nReason: " + reason);
		}
		
		caller.sendMessage("Kicked the IP " + target + "!");
		PintoServer.logger.log("Moderation", "Kicked the IP \"" + target + "\" for \"" + reason + "\"");
	}
}
