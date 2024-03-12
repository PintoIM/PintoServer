package org.pintoim.pinto;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.json.JSONObject;

public class ClientUpdateCheck {
	public static final String UPDATE_URL = 
			"https://github.com/PintoIM/Pinto/raw/main/version.json";
	
	public static JSONObject getVersionInformation() {
		try {
			HttpURLConnection httpConnection = (HttpURLConnection) new URL(UPDATE_URL).openConnection();
			httpConnection.setRequestMethod("GET");
			httpConnection.setRequestProperty("User-Agent", "PintoServer");
			httpConnection.setDoInput(true);
			httpConnection.connect();
			
			InputStream inputStream = httpConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(
            		new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String responseline;
            
            while ((responseline = bufferedReader.readLine()) != null) {
                stringBuilder.append(String.format("%s\n", responseline));
            }
            
            String responseRaw = stringBuilder.toString();
            JSONObject response = new JSONObject(responseRaw);

            bufferedReader.close();
			httpConnection.disconnect();
			
			return response;
		} catch (Exception ex) {
			PintoServer.logger.error("Unable to get version information!");
			PintoServer.logger.throwable(ex);
			return null;
		}
	}
	
	public static boolean isLatest(String version) {
		if (PintoServer.IS_RELEASE_CANDIDATE) {
			return true;
		}
		
		try {
			JSONObject information = getVersionInformation();
			return version.equalsIgnoreCase(information.getString("latest"));
		} catch (Exception ex) {
			PintoServer.logger.error("Unable to determine if version is latest!");
			PintoServer.logger.throwable(ex);
			return true;
		}
	}
	
	public static boolean isSupported(String version) {
		if (PintoServer.IS_RELEASE_CANDIDATE) {
			return true;
		}
		
		try {
			JSONObject information = getVersionInformation();
			return Arrays.asList(information.getJSONArray("supported")
					.toList()
					.toArray(new String[0]))
					.contains(version);
		} catch (Exception ex) {
			PintoServer.logger.error("Unable to determine if version is supported!");
			PintoServer.logger.throwable(ex);
			return true;
		}
	}
}