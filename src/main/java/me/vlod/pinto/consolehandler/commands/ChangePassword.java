package me.vlod.pinto.consolehandler.commands;

import me.vlod.pinto.PintoServer;
import me.vlod.pinto.UserDatabaseEntry;
import me.vlod.pinto.Utils;
import me.vlod.pinto.consolehandler.ConsoleCaller;
import me.vlod.pinto.consolehandler.ConsoleCommand;
import me.vlod.pinto.networking.NetServerHandler;

public class ChangePassword implements ConsoleCommand {
	@Override
	public String getName() {
		return "change-password";
	}

	@Override
	public String getDescription() {
		return "Changes a user's password, they will be kicked";
	}

	@Override
	public String getUsage() {
		return "change-password <user> <new password>";
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
		
		if (!UserDatabaseEntry.isRegistered(server, user)) {
			PintoServer.logger.error("The account %s is not registered", user);
			return;
		}
		
		UserDatabaseEntry userDatabaseEntry = new UserDatabaseEntry(server, user);
		userDatabaseEntry.load();
		userDatabaseEntry.passwordHash = Utils.getSHA256HashFromStr("", password);
		userDatabaseEntry.save();
		PintoServer.logger.info("Successfully changed %s's password", user);
		
		NetServerHandler netHandler = server.getHandlerByName(user);
		if (netHandler != null) {
			netHandler.kick("Your password has been modified!");
		}
	}
}
