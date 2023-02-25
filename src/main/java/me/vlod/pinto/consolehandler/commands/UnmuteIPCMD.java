package me.vlod.pinto.consolehandler.commands;

import me.vlod.pinto.PintoServer;
import me.vlod.pinto.consolehandler.ConsoleCaller;
import me.vlod.pinto.consolehandler.ConsoleCommand;

public class UnmuteIPCMD implements ConsoleCommand {
	@Override
	public String getName() {
		return "unmuteip";
	}

	@Override
	public String getDescription() {
		return "Unmutes the specified IP address";
	}

	@Override
	public String getUsage() {
		return "unmuteip <ip>";
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
		server.unmuteUser(target, true);
		caller.sendMessage("Unmuted the IP " + target + "!");
	}
}
