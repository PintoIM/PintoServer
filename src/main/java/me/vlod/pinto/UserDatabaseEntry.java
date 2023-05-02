package me.vlod.pinto;

import java.util.LinkedHashSet;

public class UserDatabaseEntry {
	private PintoServer server;
	private String userName;
	public String passwordHash;
	public UserStatus status = UserStatus.ONLINE;
	public LinkedHashSet<String> contacts = new LinkedHashSet<String>();
	
	public UserDatabaseEntry(PintoServer server, String userName) {
		this.server = server;
		this.userName = userName;
	}
	
	public String getDatabaseSelector() {
		return String.format("name = \"%s\"", this.userName);
	}
	
	public void save() {
		String contactsEncoded = null;
		if (this.contacts.size() > 0) {
			contactsEncoded = "";
			for (String contact : this.contacts) {
				contactsEncoded += String.format("%s,", contact);
			}
			contactsEncoded = contactsEncoded.substring(0, contactsEncoded.length() - 1);	
		}

		try {
			this.server.database.changeRows(PintoServer.TABLE_NAME, this.getDatabaseSelector(), 
					new String[] { 
							"name", 
							"passwordHash",
							"laststatus",
							"contacts"
					}, 
					new String[] { 
							String.format("\"%s\"", this.userName),
							String.format("\"%s\"", this.passwordHash),
							"" + this.status.getIndex(),
							(contactsEncoded == null ? "NULL" : String.format("\"%s\"", contactsEncoded))
					});
		} catch (Exception ex) {
			PintoServer.logger.throwable(ex);
		}
	}
	
	public void load() {
		try {
			String[] row = this.server.database.getRows(
					PintoServer.TABLE_NAME, this.getDatabaseSelector()).get(0);
			
			this.passwordHash = row[1];
			this.status = UserStatus.fromIndex(Integer.valueOf(row[2]));
			
			String contactsRaw = row[3];
			
			if (!contactsRaw.equalsIgnoreCase("null")) {
				for (String contact : contactsRaw.split(",")) {
					if (this.contacts.contains(contact)) continue;
					this.contacts.add(contact);
				}
				this.save();
			}
		} catch (Exception ex) {
			PintoServer.logger.throwable(ex);
		}
	}
	
	public static UserDatabaseEntry registerAndReturnEntry(PintoServer server, String userName, 
			String passwordHash, UserStatus status) {
		try {
			server.database.createRow(PintoServer.TABLE_NAME,
					new String[] { 
							String.format("\"%s\"", userName),
							String.format("\"%s\"", passwordHash),
							"" + status.getIndex(),
							"NULL"
					});
			UserDatabaseEntry userDatabaseEntry = new UserDatabaseEntry(server, userName);
			userDatabaseEntry.load();
			return userDatabaseEntry;
		} catch (Exception ex) {
			PintoServer.logger.throwable(ex);
			return null;
		}
	}
	
	public static boolean isRegistered(PintoServer server, String userName) {
		try {
			return server.database.getRows(PintoServer.TABLE_NAME, new UserDatabaseEntry(server,
					userName).getDatabaseSelector()).size() > 0;
		} catch (Exception ex) {
			PintoServer.logger.throwable(ex);
			return false;
		}
	}
}