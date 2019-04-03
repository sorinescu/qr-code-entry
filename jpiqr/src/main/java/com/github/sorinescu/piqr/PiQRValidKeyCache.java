package com.github.sorinescu.piqr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import io.sentry.Sentry;

public class PiQRValidKeyCache extends Thread {
    private static final Logger logger = Logging.getLogger(PiQRValidKeyCache.class.getName());

    OkHttpClient apiClient = new OkHttpClient();

    private String apiUrl;
    private String apiKey;
    private String hardcodedKey;
    private ArrayList<String> validKeys = new ArrayList();

    PiQRValidKeyCache(String apiUrl, String apiKey, String hardcodedKey) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.hardcodedKey = hardcodedKey;
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
        logger.info("Started valid key cache");

        Request request = new Request.Builder()
            .url(apiUrl + "/keys/valid")
            .addHeader("Authorization", "Bearer " + apiKey)
            .build();

        logger.info("Built request");

        while (true) {
            try (Response response = apiClient.newCall(request).execute()) {
                String responseStr = response.body().string();
                response.close();

                logger.info("Refreshed valid keys");

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
                logger.severe("Got exception: " + e.toString());
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