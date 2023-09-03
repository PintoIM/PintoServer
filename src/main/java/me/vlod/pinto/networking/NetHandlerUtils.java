package me.vlod.pinto.networking;

import me.vlod.pinto.ClientUpdateCheck;
import me.vlod.pinto.UserStatus;
import me.vlod.pinto.configuration.BannedConfig;
import me.vlod.pinto.configuration.MainConfig;
import me.vlod.pinto.configuration.WhitelistConfig;

public class NetHandlerUtils {
	public static final String USERNAME_REGEX_CHECK = "^(?=.{3,15}$)[a-zA-Z0-9._]+$";
	public static final String PASSWORD_REGEX_CHECK = "^(?=.{64}$)[a-zA-Z0-9._]+$";
	
	public static boolean performModerationChecks(NetworkHandler handler, String username) {
		// Check if either the user name or IP are not white-listed
    	if (MainConfig.instance.useWhiteList && 
    		!WhitelistConfig.instance.ips.contains(handler.networkAddress.ip) &&
    		!WhitelistConfig.instance.users.contains(username)) {
    		handler.kick("You are not white-listed!");
    	}
    	
		String bannedReason = BannedConfig.instance.users.get(username);
		String bannedReasonIP = BannedConfig.instance.ips.get(handler.networkAddress.ip);
    	
		// Check if either the user name or IP are banned
    	if (bannedReason != null) {
    		handler.kick("You are banned!\nReason: " + bannedReason);
    		return false;
    	} else if (bannedReasonIP != null) {
    		handler.kick("You are banned!\nReason: " + bannedReasonIP);
    		return false;
    	}
    	
    	return true;
	}
	
	public static boolean performNameVerification(NetworkHandler handler, String username) {
    	if (!username.matches(USERNAME_REGEX_CHECK)) {
    		handler.kick("Illegal username!\n"
    				+ "Legal usernames must have a length of at least 3 and at most 16\n"
    				+ "Legal usernames may only contain alphanumeric characters,"
    				+ " underscores and dots");
    		return false;
    	}
    	
    	return true;
	}
	
	public static boolean performProtocolCheck(NetworkHandler handler, byte protocol, String clientVersion) {
		// Check if the client protocol is not PROTOCOL_VERSION
    	if (protocol != NetworkHandler.PROTOCOL_VERSION) {
    		handler.kick(String.format("Illegal protocol version!\nMust be %d, but got %d!\n"
    				+ "Are you using a compatible Pinto! version?", 
    				NetworkHandler.PROTOCOL_VERSION, protocol));
    		return false;
    	}
    	
    	// Check if the client version is not supported
    	if (!MainConfig.instance.ignoreClientVersion && 
    		!ClientUpdateCheck.isSupported(clientVersion)) {
    		handler.kick(String.format("Your client version is unsupported!\n"
    				+ "Please update to the latest version!"));
    		return false;
    	}
    	
    	return true;
	}
	
	public static UserStatus getToOthersStatus(UserStatus status) {
		return status == UserStatus.INVISIBLE ? UserStatus.OFFLINE : status;
	}
}
