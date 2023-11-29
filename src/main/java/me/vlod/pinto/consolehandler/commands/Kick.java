package me.vlod.pinto.consolehandler.commands;

import me.vlod.pinto.PintoServer;
import me.vlod.pinto.consolehandler.ConsoleCaller;
import me.vlod.pinto.consolehandler.ConsoleCommand;
import me.vlod.pinto.networking.NetServerHandler;

public class Kick implements ConsoleCommand {
	@Override
	public String getName() {
		return "kick";
	}

	@Override
	public String getDescription() {
		return "Kicks the specified user";
	}

	@Override
	public String getUsage() {
		return "kick <user> <reason>";
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
		
		NetServerHandler handler = server.getHandlerByName(target);
		if (handler != null) {
			handler.kick("You have been kicked!\nReason: " + reason);
			caller.sendMessage("Kicked the user " + target + "!");
			PintoServer.logger.log("Moderation", "Kicked the user \"" + target + "\" for \"" + reason + "\"");
		} else {
			caller.sendMessage("Unable to find " + target + "!");
		}
	}
}
