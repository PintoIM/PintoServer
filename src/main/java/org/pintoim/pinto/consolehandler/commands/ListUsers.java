package org.pintoim.pinto.consolehandler.commands;

import org.pintoim.pinto.PintoServer;
import org.pintoim.pinto.configuration.MainConfig;
import org.pintoim.pinto.consolehandler.ConsoleCaller;
import org.pintoim.pinto.consolehandler.ConsoleCommand;
import org.pintoim.pinto.networking.NetServerHandler;

public class ListUsers implements ConsoleCommand {
	@Override
	public String getName() {
		return "list-users";
	}

	@Override
	public String getDescription() {
		return "Lists all connected users";
	}

	@Override
	public String getUsage() {
		return "list-users";
	}

	@Override
	public int getMinArgsCount() {
		return 0;
	}

	@Override
	public int getMaxArgsCount() {
		return 0;
	}

	@Override
	public void execute(PintoServer server, ConsoleCaller caller, String[] args) throws Exception {
		PintoServer.logger.info("There are %d clients out of the max of %d connected%s", 
				server.clients.size(), MainConfig.instance.maxUsers, server.clients.size() > 0 ? ":" : "");
		
		for (NetServerHandler client : server.clients.toArray(new NetServerHandler[0])) {
			PintoServer.logger.info("- %s (%s)", client.netManager.getAddress(),
					client.userName != null ? client.userName : "** UNAUTHENTICATED **");
		}
	}
}
