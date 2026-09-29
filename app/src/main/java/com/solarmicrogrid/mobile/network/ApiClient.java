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

    // USB ADB Reverse (127.0.0.1:5298) and LAN Wi-Fi (192.168.1.5:5298)
    public static final String USB_ADB_URL = "http://127.0.0.1:5298/api";
    public static final String LAN_WIFI_URL = "http://192.168.1.5:5298/api";
    public static volatile String BASE_URL = USB_ADB_URL;

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
    public static void patch(String endpoint, String jsonBody, ApiCallback callback) {
        sendRequest("PATCH", endpoint, jsonBody, callback);
    }
    public static void delete(String endpoint, ApiCallback callback) {
        sendRequest("DELETE", endpoint, null, callback);
    }

    private static void sendRequest(String method, String endpoint, String jsonBody, ApiCallback callback) {
        executor.execute(() -> {
            String primaryBase = BASE_URL;
            String secondaryBase = primaryBase.equals(USB_ADB_URL) ? LAN_WIFI_URL : USB_ADB_URL;

            try {
                executeHttp(primaryBase, method, endpoint, jsonBody, callback);
            } catch (Exception primaryEx) {
                // If primary endpoint is unreachable, auto-failover to secondary
                try {
                    executeHttp(secondaryBase, method, endpoint, jsonBody, callback);
                    BASE_URL = secondaryBase;
                } catch (Exception secondaryEx) {
                    String msg = "Connection error: " + primaryEx.getMessage();
                    mainHandler.post(() -> callback.onError(msg));
                }
            }
        });
    }

    private static void executeHttp(String baseUrl, String method, String endpoint, String jsonBody, ApiCallback callback) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(5000);

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
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
