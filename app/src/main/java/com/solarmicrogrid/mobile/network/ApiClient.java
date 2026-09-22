package com.solarmicrogrid.mobile.network;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Native Android REST API Client for Solar Microgrid backend services.
 */
public class ApiClient {

    // `adb reverse tcp:5298 tcp:5298` exposes the PC's local API to a USB device.
    public static final String BASE_URL = "http://127.0.0.1:5298/api";

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }

    public static void get(String endpoint, ApiCallback callback) {
        sendRequest("GET", endpoint, null, callback);
    }

    public static void post(String endpoint, String jsonBody, ApiCallback callback) {
        sendRequest("POST", endpoint, jsonBody, callback);
    }

    public static void put(String endpoint, String jsonBody, ApiCallback callback) {
        sendRequest("PUT", endpoint, jsonBody, callback);
    }

    public static void delete(String endpoint, ApiCallback callback) {
        sendRequest("DELETE", endpoint, null, callback);
    }

    private static void sendRequest(String method, String endpoint, String jsonBody, ApiCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BASE_URL + endpoint);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);

                if (jsonBody != null && (method.equals("POST") || method.equals("PUT") || method.equals("PATCH"))) {
                    conn.setDoOutput(true);
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(input, 0, input.length);
                    }
                }

                int statusCode = conn.getResponseCode();
                InputStream is = (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream();

                StringBuilder response = new StringBuilder();
                if (is != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line.trim());
                        }
                    }
                }

                String responseStr = response.toString();
                if (statusCode >= 200 && statusCode < 300) {
                    mainHandler.post(() -> callback.onSuccess(responseStr));
                } else {
                    String errorMsg = "HTTP " + statusCode;
                    try {
                        JSONObject errJson = new JSONObject(responseStr);
                        if (errJson.has("message")) {
                            errorMsg = errJson.getString("message");
                        }
                    } catch (Exception ignored) {}
                    String finalError = errorMsg;
                    mainHandler.post(() -> callback.onError(finalError));
                }

            } catch (Exception e) {
                String msg = "Connection error: " + e.getMessage();
                mainHandler.post(() -> callback.onError(msg));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }
}
