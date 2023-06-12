package me.vlod.pinto;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * General purpose utilities
 */
public class Utils {
	/** 
	 * Converts a byte array to a hex string<br>
	 * From https://stackoverflow.com/a/9855338
	 * 
	 * @param bytes the byte array
	 * @return the hex string or "DEADBEEF" on invalid input
	 */
	public static String bytesToHex(byte[] bytes) {
		if (bytes == null || bytes.length < 1) return "DEADBEEF";
		
		char[] hexArray = "0123456789ABCDEF".toCharArray();
	    char[] hexChars = new char[bytes.length * 2];
	    
	    for (int j = 0; j < bytes.length; j++) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    
	    return new String(hexChars);
	}
	
	/**
	 * Gets an throwable's stacktrace as a string
	 * 
	 * @param throwable the exception
	 * @return the throwable's stacktrace or null
	 */
	public static String getThrowableStackTraceAsStr(Throwable throwable) {
		if (throwable == null) return null;
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		throwable.printStackTrace(printWriter);
		return stringWriter.toString();
	}
	
	/**
	 * Checks if the specified string is a numeric value
	 * 
	 * @param str the string
	 * @param checkForDouble check if the string is a valid double value
	 * @return the check status
	 */
	public static boolean isNumeric(String str, boolean checkForDouble) {
	    if (str == null || 
	    	(!checkForDouble && str.contains(".")) || 
	    	(checkForDouble && !str.contains(".")))
	        return false;
	    
	    try {
	    	if (!checkForDouble)
	    		Integer.valueOf(str);
	    	else
	    		Double.valueOf(str);
	    } catch (NumberFormatException ex) {
	        return false;
	    }
	    
	    return true;
	}
	
	/**
	 * Checks if the specified string is a boolean value
	 * 
	 * @param str the string
	 * @return the check status
	 */
	public static boolean isBoolean(String str) {
		return str != null && (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false"));
	}
	
	/**
	 * Replaces the last match in the specified string
	 * 
	 * @param str the string
	 * @param match the match
	 * @param replaceWith what to the replace the match with
	 * @return the replaced string or the string if no match
	 */
	public static String replaceLast(String str, String match, String replaceWith) {
		return new StringBuilder(
				new StringBuilder(str)
					.reverse()
					.toString()
					.replaceFirst(match, replaceWith))
				.reverse()
				.toString();
	}
	
	/** 
	 * Splits a string by spaces, ignoring quotes<br>
	 * From <a href="https://stackoverflow.com/a/366532">https://stackoverflow.com/a/366532</a>
	 * 
	 * @param str the string to split
	 * @return the result or empty array
	 */
	public static String[] splitBySpace(String str) {
		try {
			List<String> matchList = new ArrayList<String>();
			Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
			Matcher regexMatcher = regex.matcher(str);
			
			while (regexMatcher.find()) {
			    if (regexMatcher.group(1) != null) {
			        // Add double-quoted string without the quotes
			        matchList.add(regexMatcher.group(1));
			    } else if (regexMatcher.group(2) != null) {
			        // Add single-quoted string without the quotes
			        matchList.add(regexMatcher.group(2));
			    } else {
			        // Add unquoted word
			        matchList.add(regexMatcher.group());
			    }
			}	
			
			return matchList.toArray(new String[0]);
		} catch (Exception ex) {
			ex.printStackTrace();
			return new String[0];
		}
	}

	/**
	 * Gets a salted MD5 hash from the specified string
	 * 
	 * @param salt the salt to use
	 * @param content the string to hash
	 * @return the MD5 hash or empty on error
	 */
	public static String getMD5HashFromStr(String salt, String content) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.update((salt + content).getBytes());
			return (new BigInteger(1, messageDigest.digest())).toString(16);	
		} catch (Exception ex) {
			return "";
		}
	}
	
	/**
	 * Splits the specified string into chunks<br>
	 * From <a href="https://stackoverflow.com/a/3760193">https://stackoverflow.com/a/3760193</a>
	 * 
	 * @param str the string to split
	 * @param chunkSize the chunk size
	 * @return the chunks
	 */
	public static String[] splitStringIntoChunks(String str, int chunkSize) {
	    List<String> chunks = new ArrayList<String>((str.length() + chunkSize - 1) / chunkSize);

	    for (int chunkIndex = 0; chunkIndex < str.length(); chunkIndex += chunkSize) {
	        chunks.add(str.substring(chunkIndex, Math.min(str.length(), chunkIndex + chunkSize)));
	    }
	    
	    return chunks.toArray(new String[0]);
	}

	/**
	 * Checks if the specified string is a valid IP address
	 * 
	 * @param str the string
	 * @return true if it is a valid address, false if otherwise
	 */
	public static boolean isIPv4Address(String str) {
	    if (str.isEmpty()) {
	        return false;
	    }
	    
	    try {
	        InetAddress inetAddress = InetAddress.getByName(str);
	        return inetAddress instanceof Inet4Address;
	    } catch (UnknownHostException ex) {
	        return false;
	    }
	}
	
	/**
	 * Re-maps the specified value in a range to a new range<br>
	 * From <a href="https://stackoverflow.com/a/929107">https://stackoverflow.com/a/929107</a>
	 * 
	 * @param oldValue the value
	 * @param oldMin the old minimum
	 * @param oldMax the old maximum
	 * @param min the new minimum
	 * @param max the new maximum
	 * @return the re-mapped value
	 */
	public static int remapToRange(int oldValue, int oldMin, int oldMax, int min, int max) {
		int oldRange = (oldMax - oldMin);  
		int range = (max - min);
		int value = (((oldValue - oldMin) * range) / oldRange) + min;
		return value;
	}
	

	/**
	 * Reads 4 bytes (size header) + a string from the specified stream (encoded in UTF-16)
	 * 
	 * @param stream the stream to read from
	 * @param maxLength the maximum length of the string
	 * @return the string
	 * @throws IOException if an IO error occurred
	 */
	public static String readPintoStringFromStream(DataInputStream stream, int maxLength) throws IOException {
	    int length = stream.readInt();
	    if (length < 0) 
	    	throw new IOException("Weird string, the length is less than 0!");
	    if (length < 1) return "";
	    
	    byte[] buffer = new byte[length];
	    stream.read(buffer);
	    
	    String str = new String(buffer, StandardCharsets.UTF_16BE);
	    if (str.length() > maxLength)
	    	throw new IllegalArgumentException(String.format(
	    			"Read more data than allowed! (%d > %d)", 
	    			str.length(), maxLength));
	    
	    return str;
	}
	
	/**
	 * Writes the specified string (encoded in UTF-16) with a size header to specified stream
	 * 
	 * @param stream the stream to write to
	 * @param str the string to write
	 * @param maxLength the maximum length of the string
	 * @throws IOException if an IO error occurred
	 */
	public static void writePintoStringToStream(DataOutputStream stream, 
			String str, int maxLength) throws IOException {
	    if (str.length() > maxLength)
	    	str = str.substring(0, maxLength - 1);
	    byte[] stringData = str.getBytes(StandardCharsets.UTF_16BE);
	    
		stream.writeInt(stringData.length);
	    if (stringData.length < 1) return;
	    
	    stream.write(stringData);
	}
	
	/**
	 * Gets the size of the specified string (encoded in UTF-16) along side a size header
	 * 
	 * @param str the string
	 * @return the size
	 */
	public static int getPintoStringSize(String str) {
		return 4 + str.getBytes(StandardCharsets.UTF_16BE).length;
	}
	
	public static final int INPUT_STREAM_MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;
	
	/**
	 * The readNBytes from Java 9 ported over to Java 8
	 * 
	 * @param stream the stream to read the bytes from
	 * @param len the amount of bytes to read
	 * @return the bytes
	 * @throws IOException if an IO error occurred
	 */
	public static byte[] readNBytes(InputStream stream, int len) throws IOException {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }

        List<byte[]> bufs = null;
        byte[] result = null;
        int total = 0;
        int remaining = len;
        int n;
        do {
            byte[] buf = new byte[Math.min(remaining, 8192)];
            int nread = 0;

            // read to EOF which may read more or less than buffer size
            while ((n = stream.read(buf, nread,
                    Math.min(buf.length - nread, remaining))) > 0) {
                nread += n;
                remaining -= n;
            }

            if (nread > 0) {
                if (INPUT_STREAM_MAX_BUFFER_SIZE - total < nread) {
                    throw new OutOfMemoryError("Required array size too large");
                }
                if (nread < buf.length) {
                    buf = Arrays.copyOfRange(buf, 0, nread);
                }
                total += nread;
                if (result == null) {
                    result = buf;
                } else {
                    if (bufs == null) {
                        bufs = new ArrayList<>();
                        bufs.add(result);
                    }
                    bufs.add(buf);
                }
            }
            // if the last call to read returned -1 or the number of bytes
            // requested have been read then break
        } while (n >= 0 && remaining > 0);

        if (bufs == null) {
            if (result == null) {
                return new byte[0];
            }
            return result.length == total ?
                result : Arrays.copyOf(result, total);
        }

        result = new byte[total];
        int offset = 0;
        remaining = total;
        for (byte[] b : bufs) {
            int count = Math.min(b.length, remaining);
            System.arraycopy(b, 0, result, offset, count);
            offset += count;
            remaining -= count;
        }

        return result;
    }
}

