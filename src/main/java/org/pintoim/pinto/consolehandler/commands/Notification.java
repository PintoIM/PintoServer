package org.pintoim.pinto.consolehandler.commands;

import org.pintoim.pinto.PintoServer;
import org.pintoim.pinto.consolehandler.ConsoleCaller;
import org.pintoim.pinto.consolehandler.ConsoleCommand;
import org.pintoim.pinto.networking.packet.PacketNotification;

public class Notification implements ConsoleCommand {
	@Override
	public String getName() {
		return "notification";
	}

	@Override
	public String getDescription() {
		return "Sends a notification to everyone online";
	}

	@Override
	public String getUsage() {
		return "notification <message> <title>";
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
		PintoServer.logger.log("Notification", "%s: %s", args[1], args[0]);
		server.sendGlobalPacket(new PacketNotification(2, -1, args[1], args[0]));
	}
}
