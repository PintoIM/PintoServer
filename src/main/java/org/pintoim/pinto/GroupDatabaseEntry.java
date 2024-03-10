package org.pintoim.pinto;

import java.util.LinkedHashSet;

public class GroupDatabaseEntry {
	private PintoServer server;
	private String id;
	public LinkedHashSet<String> members = new LinkedHashSet<String>();
	
	public GroupDatabaseEntry(PintoServer server, String id) {
		this.server = server;
		this.id = id;
	}
	
	public String getDatabaseSelector() {
		return String.format("id = \"%s\"", this.id);
	}
	
	public void save() {
		String membersEncoded = null;
		if (this.members.size() > 0) {
			membersEncoded = "";
			for (String participant : this.members) {
				membersEncoded += String.format("%s,", participant);
			}
			membersEncoded = membersEncoded.substring(0, membersEncoded.length() - 1);	
		}

		try {
			this.server.database.changeRows(PintoServer.GROUPS_TABLE_NAME, this.getDatabaseSelector(), 
					new String[] { 
							"id", 
							"members"
					}, 
					new String[] { 
							String.format("\"%s\"", this.id),
							(membersEncoded == null ? "NULL" : String.format("\"%s\"", membersEncoded)),
					});
		} catch (Exception ex) {
			PintoServer.logger.throwable(ex);
		}
	}
	
	public void load() {
		try {
			String[] row = this.server.database.getRows(
					PintoServer.GROUPS_TABLE_NAME, this.getDatabaseSelector()).get(0);
			String membersRaw = row[1];
			
			if (!membersRaw.equalsIgnoreCase("null")) {
				for (String member : membersRaw.split(",")) {
					if (this.members.contains(member)) continue;
					this.members.add(member);
				}
			}

			this.save();
		} catch (Exception ex) {
			PintoServer.logger.throwable(ex);
		}
	}
	
	public static GroupDatabaseEntry createAndReturnEntry(PintoServer server, String id) {
		try {
			server.database.createRow(PintoServer.GROUPS_TABLE_NAME,
					new String[] { 
							String.format("\"%s\"", id),
							"NULL"
					});
			GroupDatabaseEntry groupDatabaseEntry = new GroupDatabaseEntry(server, id);
			groupDatabaseEntry.load();
			return groupDatabaseEntry;
		} catch (Exception ex) {
			PintoServer.logger.throwable(ex);
			return null;
		}
	}
	
	public static boolean exists(PintoServer server, String id) {
		try {
			return server.database.getRows(PintoServer.GROUPS_TABLE_NAME, new GroupDatabaseEntry(server,
					id).getDatabaseSelector()).size() > 0;
		} catch (Exception ex) {
			PintoServer.logger.throwable(ex);
			return false;
		}
	}
}
