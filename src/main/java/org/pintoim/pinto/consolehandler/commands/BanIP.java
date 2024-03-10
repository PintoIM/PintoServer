package org.pintoim.pinto.consolehandler.commands;

import org.pintoim.pinto.PintoServer;
import org.pintoim.pinto.consolehandler.ConsoleCaller;
import org.pintoim.pinto.consolehandler.ConsoleCommand;

public class BanIP implements ConsoleCommand {
	@Override
	public String getName() {
		return "ban-ip";
	}

	@Override
	public String getDescription() {
		return "Bans the specified IP address";
	}

	@Override
	public String getUsage() {
		return "ban-ip <ip> <reason>";
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
		server.banUser(target, reason, true);
		caller.sendMessage("Banned the IP " + target + "!");
	}
}
