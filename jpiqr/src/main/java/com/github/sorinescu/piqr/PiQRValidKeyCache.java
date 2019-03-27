package com.github.sorinescu.piqr;

import java.io.IOException;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import io.sentry.Sentry;

public class PiQRValidKeyCache extends Thread {
    OkHttpClient apiClient = new OkHttpClient();

    static final String hardcodedKey = System.getenv("PIQR_HARDCODED_KEY");

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
        if (hardcodedKey != null && hardcodedKey.equals(key))
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

                System.out.println("Refreshed valid keys");

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
                Sentry.capture(e);
            }

            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                Sentry.capture(e);
            }
        }
    }
}