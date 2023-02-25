package me.vlod.pinto.consolehandler;

public class ConsoleVariableTypeTools  {
    public static Object getStringAsVarType(String str) {
    	return getStringAsVarType(str, ConsoleVariableType.STRING);
    }
	
    public static Object getStringAsVarType(String str, ConsoleVariableType type) {
        try {
            if (type == ConsoleVariableType.INT)
                return Integer.valueOf(str);
            else if (type == ConsoleVariableType.BOOL)
                return Boolean.valueOf(str);
            else
                return str;
        } catch (Exception ex) {
            return null;
        }
    }
}