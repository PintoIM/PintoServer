package org.pintoim.pinto.consolehandler.commands;

import org.pintoim.pinto.PintoServer;
import org.pintoim.pinto.consolehandler.ConsoleCaller;
import org.pintoim.pinto.consolehandler.ConsoleCommand;
import org.pintoim.pinto.networking.NetServerHandler;

public class GetIP implements ConsoleCommand {
	@Override
	public String getName() {
		return "get-ip";
	}

	@Override
	public String getDescription() {
		return "Gets the IP of the specified user";
	}

	@Override
	public String getUsage() {
		return "get-ip <user>";
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
		NetServerHandler handler = server.getHandlerByName(target);
		
		if (handler != null) {
			caller.sendMessage(target + "'s IP address: " + handler.netManager.getAddress().ip);
		} else {
			caller.sendMessage("Unable to find " + target + "!");
		}
	}
}
