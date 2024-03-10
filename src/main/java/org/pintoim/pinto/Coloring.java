package org.pintoim.pinto;

import java.awt.Color;
import java.util.HashMap;

public class Coloring {
	public static final char COLORING_PREFIX = 0xA7;
	public static final String COLORING_BLACK = COLORING_PREFIX + "000000";
	public static final String COLORING_BLUE = COLORING_PREFIX + "0000FF";
	public static final String COLORING_GRAY = COLORING_PREFIX + "808080";
	public static final String COLORING_GREEN = COLORING_PREFIX + "008000";
	public static final String COLORING_PURPLE = COLORING_PREFIX + "800080";
	public static final String COLORING_RED = COLORING_PREFIX + "FF0000";
	public static final String COLORING_WHITE = COLORING_PREFIX + "FFFFFF";
	public static final HashMap<String, String> COLORING_CODES_MAP = new HashMap<String, String>();
	
	static {
		COLORING_CODES_MAP.put("0", COLORING_BLACK);
		COLORING_CODES_MAP.put("1", COLORING_BLUE);
		COLORING_CODES_MAP.put("2", COLORING_GREEN);
		COLORING_CODES_MAP.put("4", COLORING_RED);
		COLORING_CODES_MAP.put("5", COLORING_PURPLE);
		COLORING_CODES_MAP.put("8", COLORING_GRAY);
		COLORING_CODES_MAP.put("f", COLORING_WHITE);
	}
	
	public static String getColoringFromRGB(int r, int g, int b) {
		return String.format("%s%02x%02x%02x", COLORING_PREFIX,r, g, b);
	}
	
	public static String getColoringFromAWTColor(Color color) {
		return Coloring.getColoringFromRGB(color.getRed(), color.getGreen(), color.getBlue());
	}
	
	public static String translateColoringCodes(String originalStr) {
		char[] str = originalStr.toCharArray();
		String newString = "";
		
		for (int i = 0; i <= str.length - 1; i++) {
            if (str[i] == COLORING_PREFIX && (i + 1 <= str.length - 1) && "012458f".indexOf(str[i + 1]) > -1) {
            	newString += COLORING_CODES_MAP.get("" + str[i + 1]);
            	i++;
            } else {
            	newString += str[i];
            }
        }
		
		return newString;
	}
	
    public static String translateAlternativeColoringCodes(String originalStr) {
        char[] str = originalStr.toCharArray();
        
        for (int i = 0; i < str.length - 1; i++) {
            if (str[i] == '&' && "012458fF".indexOf(str[i + 1]) > -1) {
                str[i] = COLORING_PREFIX;
                str[i + 1] = Character.toLowerCase(str[i + 1]);
            }
        }
        
        return Coloring.translateColoringCodes(new String(str));
    }
}
