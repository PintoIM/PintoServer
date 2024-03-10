package org.pintoim.pinto.consolehandler;

public interface ConsoleCallable {
    public String getName();
    public default String[] getAliases() {
    	return new String[0];
    }
    public String getDescription();
    public default String getRequiredPermission() {
    	return "none";
    }
}