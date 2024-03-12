package org.pintoim.pinto;

import java.util.LinkedHashSet;

public class UserDatabaseEntry {
	private PintoServer server;
	private String userName;
	public String passwordHash;
	public UserStatus status = UserStatus.ONLINE;
	public LinkedHashSet<String> contacts = new LinkedHashSet<String>();
	public LinkedHashSet<String> contactRequests = new LinkedHashSet<String>();
	
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

		String contactRequestsEncoded = null;
		if (this.contactRequests.size() > 0) {
			contactRequestsEncoded = "";
			for (String contact : this.contactRequests) {
				contactRequestsEncoded += String.format("%s,", contact);
			}
			contactRequestsEncoded = contactRequestsEncoded.substring(0, contactRequestsEncoded.length() - 1);	
		}
		
		try {
			this.server.database.changeRows(PintoServer.USERS_TABLE_NAME, this.getDatabaseSelector(), 
					new String[] { 
							"name", 
							"passwordHash",
							"laststatus",
							"contacts",
							"contactrequests"
					}, 
					new String[] { 
							String.format("\"%s\"", this.userName),
							String.format("\"%s\"", this.passwordHash),
							"" + this.status.getIndex(),
							(contactsEncoded == null ? "NULL" : String.format("\"%s\"", contactsEncoded)),
							(contactRequestsEncoded == null ? "NULL" : 
								String.format("\"%s\"", contactRequestsEncoded))
					});
		} catch (Exception ex) {
			PintoServer.logger.throwable(ex);
		}
	}
	
	public void load() {
		try {
			String[] row = this.server.database.getRows(
					PintoServer.USERS_TABLE_NAME, this.getDatabaseSelector()).get(0);
			
			this.passwordHash = row[1];
			this.status = UserStatus.fromIndex(Integer.valueOf(row[2]));
			this.contacts.clear();
			this.contactRequests.clear();
			String contactsRaw = row[3];
			String contactRequestsRaw = row[4];
			
			if (!contactsRaw.equalsIgnoreCase("null")) {
				for (String contact : contactsRaw.split(",")) { 
					if (this.contacts.contains(contact)) continue;
					this.contacts.add(contact);
				}
			}
			
			if (!contactRequestsRaw.equalsIgnoreCase("null")) {
				for (String contact : contactRequestsRaw.split(",")) {
					if (this.contactRequests.contains(contact)) continue;
					this.contactRequests.add(contact);
				}
			}
			
			this.save();
		} catch (Exception ex) {
			PintoServer.logger.throwable(ex);
		}
	}
	
	public static void registerHelper(PintoServer server, String userName, String password) {
		String passwordHash = Utils.getSHA256HashFromStr("", password).toUpperCase();
		registerAndReturnEntry(server, userName, passwordHash, UserStatus.ONLINE);
	}
	
	public static UserDatabaseEntry registerAndReturnEntry(PintoServer server, String userName, 
			String passwordHash, UserStatus status) {
		try {
			server.database.createRow(PintoServer.USERS_TABLE_NAME,
					new String[] { 
							String.format("\"%s\"", userName),
							String.format("\"%s\"", passwordHash),
							"" + status.getIndex(),
							"NULL",
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
			return server.database.getRows(PintoServer.USERS_TABLE_NAME, new UserDatabaseEntry(server,
					userName).getDatabaseSelector()).size() > 0;
		} catch (Exception ex) {
			PintoServer.logger.throwable(ex);
			return false;
		}
	}
}