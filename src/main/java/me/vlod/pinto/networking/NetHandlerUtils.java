package me.vlod.pinto.networking;

import me.vlod.pinto.UserStatus;
import me.vlod.pinto.configuration.BannedConfig;
import me.vlod.pinto.configuration.MainConfig;
import me.vlod.pinto.configuration.WhitelistConfig;

public class NetHandlerUtils {
	public static void performModerationChecks(NetworkHandler handler, String username) {
		// Check if either the user name or IP are not white-listed
    	if (MainConfig.instance.useWhiteList && 
    		!WhitelistConfig.instance.ips.contains(handler.networkAddress.ip) &&
    		!WhitelistConfig.instance.users.contains(username)) {
    		handler.kick("You are not white-listed!");
    		return;
    	}
    	
		String bannedReason = BannedConfig.instance.users.get(username);
		String bannedReasonIP = BannedConfig.instance.ips.get(handler.networkAddress.ip);
    	
		// Check if either the user name or IP are banned
    	if (bannedReason != null) {
    		handler.kick("You are banned from this chat!\nReason: " + bannedReason);
    		return;
    	} else if (bannedReasonIP != null) {
    		handler.kick("You are banned from this chat based on your IP address!\nReason: " + bannedReasonIP);
    		return;
    	}
	}
	
	public static void performNameVerification(NetworkHandler handler, String username) {
    	if (!username.matches("^(?=.{3,15}$)[a-z0-9._]+$")) {
    		handler.kick("Illegal username!\n"
    				+ "Legal usernames must have a length of at least 3 and at most 16\n"
    				+ "Legal usernames may only contain lowercase alphanumeric characters, underscores and dots");
    		return;
    	}
	}
	
	public static void performProtocolCheck(NetworkHandler handler, byte protocol) {
    	// Check if the client protocol is not PROTOCOL_VERSION
    	if (protocol != NetworkHandler.PROTOCOL_VERSION) {
    		handler.kick(String.format("Illegal protocol version!\nMust be %d/, but got %d", 
    				NetworkHandler.PROTOCOL_VERSION, protocol));
    		return;
    	}
	}
	
	public static UserStatus getToOthersStatus(UserStatus status) {
		return status == UserStatus.INVISIBLE ? UserStatus.OFFLINE : status;
	}
}
