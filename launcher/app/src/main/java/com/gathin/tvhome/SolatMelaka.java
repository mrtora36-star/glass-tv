package com.gathin.tvhome;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SolatMelaka {
    public interface Callback {
        void onResult(JSONObject data);
        void onError();
    }

    private static final String API =
        "https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=today&zone=MLK01";

    private static final ExecutorService EXECUTOR =
        Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private SolatMelaka() {}

    public static void fetch(Callback callback) {
        EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(API).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("Accept", "application/json");

                try (InputStream input = connection.getInputStream();
                     BufferedReader reader = new BufferedReader(
                         new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    StringBuilder body = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                    }

                    JSONObject response = new JSONObject(body.toString());
                    JSONArray prayerTimes = response.optJSONArray("prayerTime");
                    if (prayerTimes == null || prayerTimes.length() == 0) {
                        throw new IllegalStateException("Tiada data waktu solat");
                    }

                    JSONObject today = prayerTimes.getJSONObject(0);
                    MAIN.post(() -> callback.onResult(today));
                }
            } catch (Exception e) {
                MAIN.post(callback::onError);
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }
}
