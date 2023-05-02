package me.vlod.pinto;

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
	
	public static boolean isLatest(String version) {
		try {
			HttpURLConnection httpConnection = (HttpURLConnection) new URL(UPDATE_URL).openConnection();
			httpConnection.setRequestMethod("GET");
			httpConnection.setRequestProperty("Content-Type", "application/json");
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
			
			return version.equalsIgnoreCase(response.getString("latest"));
		} catch (Exception ex) {
			return false;
		}
	}
	
	public static boolean isSupported(String version) {
		try {
			HttpURLConnection httpConnection = (HttpURLConnection) new URL(UPDATE_URL).openConnection();
			httpConnection.setRequestMethod("GET");
			httpConnection.setRequestProperty("Content-Type", "application/json");
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
			
			return Arrays.asList(response.getJSONArray("supported")
					.toList()
					.toArray(new String[0]))
					.contains(version);
		} catch (Exception ex) {
			return false;
		}
	}
}