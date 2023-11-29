package me.vlod.pinto.consolehandler.commands.client;

import me.vlod.pinto.Coloring;
import me.vlod.pinto.GroupDatabaseEntry;
import me.vlod.pinto.PintoServer;
import me.vlod.pinto.UserDatabaseEntry;
import me.vlod.pinto.consolehandler.ConsoleCaller;
import me.vlod.pinto.consolehandler.ConsoleCommand;
import me.vlod.pinto.networking.NetServerHandler;
import me.vlod.pinto.networking.packet.PacketRemoveContact;

public class Remove implements ConsoleCommand {
	@Override
	public String getName() {
		return "remove";
	}

	@Override
	public String getDescription() {
		return "Removes an user from this group chat";
	}

	@Override
	public String getUsage() {
		return "remove <name>";
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
		String groupID = caller.clientChat;
		String user = args[0];
		GroupDatabaseEntry groupDatabaseEntry = new GroupDatabaseEntry(server, groupID);
		groupDatabaseEntry.load();
		NetServerHandler userClient = server.getHandlerByName(user);
		
		if (!groupDatabaseEntry.members.contains(user)) {
			caller.sendMessage(Coloring.translateAlternativeColoringCodes(
					String.format("&8[&5!&8]&4 %s is not in the group", user)));
			return;
		}
		
		UserDatabaseEntry userDatabaseEntry = new UserDatabaseEntry(server, user);
		userDatabaseEntry.load();
		
		groupDatabaseEntry.members.remove(user);
		groupDatabaseEntry.save();
		userDatabaseEntry.contacts.remove(groupID);
		userDatabaseEntry.save();
		
		if (userClient != null) {
			userClient.databaseEntry.load();
			userClient.sendPacket(new PacketRemoveContact(groupID));	
		}
		caller.sendMessage(Coloring.translateAlternativeColoringCodes(
				String.format("&8[&5i&8]&4 You have removed %s from this group", user)));
		server.sendMessageInGroup(groupID, "", Coloring.translateAlternativeColoringCodes(
				String.format("&8[&5i&8]&4 %s has removed %s from this group", 
						caller.client.userName, user)));
	}
}
