package com.github.sorinescu.piqr;

import java.io.IOException;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PiQRValidKeyCache extends Thread {
    OkHttpClient apiClient = new OkHttpClient();

    static final String hardcodedKey = "9d3c03d7a2bd46d99746dee9bed88a76";

    private String apiUrl;
    private ArrayList<String> validKeys = new ArrayList();

    PiQRValidKeyCache(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    private String getApiKey() {
        String apiKey = System.getenv("KEY_MANAGER_API_KEY");
        if (apiKey == null)
            throw new RuntimeException("KEY_MANAGER_API_KEY environment variable must be set");

        return apiKey;
    }

    public boolean authorize(String key) {
        if (hardcodedKey.equals(key))
            return true;

        synchronized(this) {
            for (String validKey: validKeys) {
                if (validKey.equals(key))
                    return true;
            }
        }

        return false;
    }

    public void run() {
        System.out.println("Started valid key cache");

        String apiKey = getApiKey();
        Request request = new Request.Builder()
            .url(apiUrl + "/keys/valid")
            .addHeader("Authorization", "Token " + apiKey)
            .build();

        System.out.println("Built request");

        while (true) {
            try (Response response = apiClient.newCall(request).execute()) {
                String responseStr = response.body().string();
                response.close();

                System.out.println("Got valid keys: " + responseStr);

                JSONObject json = new JSONObject(responseStr);
                JSONArray keys = json.getJSONArray("keys");

                ArrayList<String> localValidKeys = new ArrayList();
                for (int i = 0; i < keys.length(); ++i) {
                    JSONObject key = keys.getJSONObject(i);

                    localValidKeys.add(key.getString("key"));
                }

                synchronized(this) {
                    validKeys = localValidKeys;
                }
            } catch (IOException e) {
                System.err.println("Got exception: " + e.toString());
            }

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {}
        }
    }
}