package org.pintoim.pinto.consolehandler.commands;

import org.pintoim.pinto.PintoServer;
import org.pintoim.pinto.UserDatabaseEntry;
import org.pintoim.pinto.consolehandler.ConsoleCaller;
import org.pintoim.pinto.consolehandler.ConsoleCommand;

public class CreateAccount implements ConsoleCommand {
	@Override
	public String getName() {
		return "create-account";
	}

	@Override
	public String getDescription() {
		return "Creates an account on the server";
	}

	@Override
	public String getUsage() {
		return "create-account <user> <password>";
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
		String user = args[0];
		String password = args[1];
		
		if (UserDatabaseEntry.isRegistered(server, user)) {
			PintoServer.logger.error("The account %s is already registered", user);
			return;
		}
		
		UserDatabaseEntry.registerHelper(server, user, password);
		PintoServer.logger.info("Successfully registered the account %s", user);
	}
}
