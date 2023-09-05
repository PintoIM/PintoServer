package me.vlod.pinto.consolehandler.commands;

import me.vlod.pinto.PintoServer;
import me.vlod.pinto.consolehandler.ConsoleCaller;
import me.vlod.pinto.consolehandler.ConsoleCommand;

public class Ban implements ConsoleCommand {
	@Override
	public String getName() {
		return "ban";
	}

	@Override
	public String getDescription() {
		return "Bans the specified user";
	}

	@Override
	public String getUsage() {
		return "ban <user> <reason>";
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
		server.banUser(target, reason, false);
		caller.sendMessage("Banned the user " + target + "!");
	}
}
