package me.vlod.pinto.consolehandler.commands;

import me.vlod.pinto.PintoServer;
import me.vlod.pinto.consolehandler.ConsoleCaller;
import me.vlod.pinto.consolehandler.ConsoleCommand;

public class UnmuteCMD implements ConsoleCommand {
	@Override
	public String getName() {
		return "unmute";
	}

	@Override
	public String getDescription() {
		return "Unmutes the specified user";
	}

	@Override
	public String getUsage() {
		return "unmute <user>";
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
		server.unmuteUser(target, false);
		caller.sendMessage("Unmuted the user " + target + "!");
	}
}
