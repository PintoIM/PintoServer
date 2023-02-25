package me.vlod.pinto.consolehandler.commands;

import me.vlod.pinto.PintoServer;
import me.vlod.pinto.consolehandler.ConsoleCaller;
import me.vlod.pinto.consolehandler.ConsoleCommand;

public class MuteIPCMD implements ConsoleCommand {
	@Override
	public String getName() {
		return "muteip";
	}

	@Override
	public String getDescription() {
		return "Mutes the specified IP address";
	}

	@Override
	public String getUsage() {
		return "muteip <ip> <reason>";
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
		server.muteUser(target, reason, true);
		caller.sendMessage("Muted the IP " + target + "!");
	}
}
