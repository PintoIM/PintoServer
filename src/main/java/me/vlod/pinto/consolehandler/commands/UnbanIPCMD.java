package me.vlod.pinto.consolehandler.commands;

import me.vlod.pinto.PintoServer;
import me.vlod.pinto.consolehandler.ConsoleCaller;
import me.vlod.pinto.consolehandler.ConsoleCommand;

public class UnbanIPCMD implements ConsoleCommand {
	@Override
	public String getName() {
		return "unbanip";
	}

	@Override
	public String getDescription() {
		return "Unbans the specified IP address";
	}

	@Override
	public String getUsage() {
		return "unbanip <ip>";
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
		server.unbanUser(target, true);
		caller.sendMessage("Unbanned the IP " + target + "!");
	}
}
