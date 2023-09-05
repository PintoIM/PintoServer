package me.vlod.pinto.consolehandler.commands;

import me.vlod.pinto.PintoServer;
import me.vlod.pinto.consolehandler.ConsoleCaller;
import me.vlod.pinto.consolehandler.ConsoleCommand;
import me.vlod.pinto.networking.packet.PacketPopup;

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
		server.sendGlobalPacket(new PacketPopup(args[1], args[0]));
	}
}
