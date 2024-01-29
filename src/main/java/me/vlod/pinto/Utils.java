package me.vlod.pinto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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
	 * @param salt the salt to use (can be empty)
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
	 * Gets a salted SHA256 hash from the specified string
	 * 
	 * @param salt the salt to use (can be empty)
	 * @param content the string to hash
	 * @return the SHA256 hash or empty on error
	 */
	public static String getSHA256HashFromStr(String salt, String content) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
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
                if ((Integer.MAX_VALUE - 8) - total < nread) {
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
    
    /**
     * Writes the specified RSA public or private key
     *  into the specified FileOutputStream in PEM format
     * 
     * @param key the key to write
     * @param stream the stream to write to
     * @throws IOException if an I/O error has occurred
     * @throws IllegalArgumentException if the key is not a public or private RSA key
     */
    public static void writeRSAPublicPrivateKeyPEM(Key key, FileOutputStream stream) throws IOException {
    	if (!(key instanceof RSAPublicKey) && !(key instanceof RSAPrivateKey)) {
    		throw new IllegalArgumentException("The key is not a public or private RSA key!");
    	}
    	
    	Base64.Encoder base64Encoder = Base64.getEncoder();
    	String base64Data = new String(base64Encoder.encode(key.getEncoded()));
    	String splittedBase64Data = String.join("\n", Utils.splitStringIntoChunks(base64Data, 64));
    	String keyType = key instanceof PublicKey ? "PUBLIC" : "PRIVATE";
    	
    	stream.write(String.format("-----BEGIN %s KEY-----\n", keyType).getBytes(StandardCharsets.US_ASCII));
    	stream.write(splittedBase64Data.getBytes(StandardCharsets.US_ASCII));
    	stream.write(String.format("\n-----END %s KEY-----", keyType).getBytes(StandardCharsets.US_ASCII));
		stream.flush();
    }
    
    /**
     * Reads the specified PEM file that contains an RSA public key
     * 
     * @param file the file to read
     * @return the decoded RSA public key
     * @throws IOException
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    public static RSAPublicKey readRSAPublicKeyPEM(File file) 
    		throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
    	String fileData = new String(Files.readAllBytes(file.toPath()), StandardCharsets.US_ASCII);
    	Base64.Decoder decoder = Base64.getDecoder();
    	byte[] publicKey = decoder.decode(
    		fileData
    		.replace("-----BEGIN PUBLIC KEY-----", "")
    		.replace("\n", "")
    		.replace("\r", "")
    		.replace("-----END PUBLIC KEY-----", "")
    		.trim()
    	);
    	return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
    }
    
    /**
     * Reads the specified PEM file that contains an RSA private key
     * 
     * @param file the file to read
     * @return the decoded RSA private key
     * @throws IOException
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    public static RSAPrivateKey readRSAPrivateKeyPEM(File file) 
    		throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
    	String fileData = new String(Files.readAllBytes(file.toPath()), StandardCharsets.US_ASCII);
    	Base64.Decoder decoder = Base64.getDecoder();
    	byte[] privateKey = decoder.decode(
    		fileData
    		.replace("-----BEGIN PRIVATE KEY-----", "")
    		.replace("\n", "")
    		.replace("\r", "")
    		.replace("-----END PRIVATE KEY-----", "")
    		.trim()
    	);
    	return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKey));
    }
    
    /**
     * Converts a big endian byte array representation of an integer to an integer
     * 
     * @param bytes the byte array
     * @return the integer
     * @throws IllegalArgumentException if the bytes are signed
     */
    public static int bytesToInt(byte[] bytes) {
        int ch1 = bytes[0] & 0xFF;
        int ch2 = bytes[1] & 0xFF;
        int ch3 = bytes[2] & 0xFF;
        int ch4 = bytes[3] & 0xFF;
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new IllegalArgumentException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }
    
    /**
     * Converts an integer to a big endian byte array representation
     * 
     * @param i the integer
     * @return the byte array representation
     */
    public static byte[] intToBytes(int i) {
    	byte[] bytes = new byte[4];
    	bytes[0] = (byte)((i >>> 24) & 0xFF);
    	bytes[1] = (byte)((i >>> 16) & 0xFF);
    	bytes[2] = (byte)((i >>> 8) & 0xFF);
    	bytes[3] = (byte)((i >>> 0) & 0xFF);
        return bytes;
    }
    
    /**
     * Returns a unique Pinto! group ID
     * 
     * @return the group ID
     */
    public static String getPintoGroupID() {
    	// 7 bytes can have 72057594037927936 combinations
    	byte[] groupIDBytes = new byte[7];
    	SecureRandom secureRandom = new SecureRandom();
    	secureRandom.nextBytes(groupIDBytes);
    	return String.format("G:%s", Utils.bytesToHex(groupIDBytes).toLowerCase());
    }
    
    /**
     * Kills other java instances<br>
     * This is some jank code, do not use in production<br>
     * Uses hacky JVM shit as well
     */
    public static void killOtherJavaInstances() {
		try {
			int currentPID = Integer.valueOf(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
			Process taskList = Runtime.getRuntime()
					.exec("tasklist /FI \"imagename eq javaw.exe\" /FO CSV /NH");
			taskList.waitFor();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(taskList.getInputStream()));
			ArrayList<String> processes = new ArrayList<String>();
			
			String s;
			while ((s = reader.readLine()) != null) {
				processes.add(s);
			}
			
			for (String process : processes) {
				int processPID = Integer.valueOf(process.split(",")[1].replace("\"", ""));
				if (processPID == currentPID) continue;
				Runtime.getRuntime().exec("taskkill /f /pid " + processPID);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
    }
}

