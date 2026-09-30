package com.solarmicrogrid.mobile.network;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.solarmicrogrid.mobile.auth.SessionManager;

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
 * Supports complete Authentication, Node Management, Reservations, and Prosumer operations.
 */
public class ApiClient {

    // USB ADB Reverse (127.0.0.1:5298), Android Emulator (10.0.2.2:5298), and LAN Wi-Fi
    public static final String USB_ADB_URL = "http://127.0.0.1:5298/api";
    public static final String EMULATOR_URL = "http://10.0.2.2:5298/api";
    public static final String LAN_WIFI_URL = "http://192.168.1.5:5298/api";
    public static volatile String BASE_URL = USB_ADB_URL;

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static Context appContext;

    public static void init(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }

    public static void get(String endpoint, ApiCallback callback) {
        sendRequest("GET", endpoint, null, callback, true);
    }

    public static void post(String endpoint, String jsonBody, ApiCallback callback) {
        sendRequest("POST", endpoint, jsonBody, callback, true);
    }

    public static void put(String endpoint, String jsonBody, ApiCallback callback) {
        sendRequest("PUT", endpoint, jsonBody, callback, true);
    }

    public static void patch(String endpoint, ApiCallback callback) {
        sendRequest("PATCH", endpoint, null, callback, true);
    }

    public static void patch(String endpoint, String jsonBody, ApiCallback callback) {
        sendRequest("PATCH", endpoint, jsonBody, callback, true);
    }

    public static void delete(String endpoint, ApiCallback callback) {
        sendRequest("DELETE", endpoint, null, callback, true);
    }

    // Unauthenticated variants (for login/registration without sending stale tokens)
    public static void postUnauthenticated(String endpoint, String jsonBody, ApiCallback callback) {
        sendRequest("POST", endpoint, jsonBody, callback, false);
    }

    // ─── AUTHENTICATION HELPERS (MATCHING WEB FRONTEND) ─────────────────────────

    public static void login(String identifier, String password, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("identifier", identifier);
            body.put("password", password);
            body.put("deviceInfo", "SolarMobile Android Client");
            postUnauthenticated("/auth/login", body.toString(), callback);
        } catch (Exception e) {
            callback.onError("Failed to build login request: " + e.getMessage());
        }
    }

    public static void sendLoginOtp(String identifier, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("identifier", identifier);
            postUnauthenticated("/auth/otp/send-login", body.toString(), callback);
        } catch (Exception e) {
            callback.onError("Failed to build OTP request: " + e.getMessage());
        }
    }

    public static void verifyOtp(String identifier, String otp, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("identifier", identifier);
            if (identifier != null && identifier.contains("@")) {
                body.put("email", identifier);
            }
            body.put("otp", otp);
            body.put("deviceInfo", "SolarMobile Android Client");
            postUnauthenticated("/auth/otp/verify", body.toString(), callback);
        } catch (Exception e) {
            callback.onError("Failed to build verification request: " + e.getMessage());
        }
    }

    public static void sendRegistrationOtp(String email, String role, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("email", email);
            body.put("role", role != null ? role : "Consumer");
            postUnauthenticated("/auth/otp/send", body.toString(), callback);
        } catch (Exception e) {
            callback.onError("Failed to build registration OTP request: " + e.getMessage());
        }
    }

    public static void register(JSONObject payload, ApiCallback callback) {
        try {
            if (!payload.has("deviceInfo")) {
                payload.put("deviceInfo", "SolarMobile Android Client");
            }
            postUnauthenticated("/auth/register", payload.toString(), callback);
        } catch (Exception e) {
            callback.onError("Failed to build registration request: " + e.getMessage());
        }
    }

    public static void logout(String refreshToken, ApiCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("refreshToken", refreshToken != null ? refreshToken : "");
            post("/auth/logout", body.toString(), callback);
        } catch (Exception e) {
            callback.onError("Failed to build logout request: " + e.getMessage());
        }
    }

    public static void getMe(ApiCallback callback) {
        get("/auth/me", callback);
    }

    // ─── INTERNAL HTTP EXECUTION ────────────────────────────────────────────────

    private static final String[] CANDIDATES = new String[] {
            USB_ADB_URL,
            LAN_WIFI_URL,
            EMULATOR_URL
    };

    private static volatile String activeBaseUrl = null;

    private static void sendRequest(String method, String endpoint, String jsonBody, ApiCallback callback, boolean attachToken) {
        executor.execute(() -> {
            // 1. Try cached working URL first if available
            String cached = activeBaseUrl;
            if (cached != null) {
                try {
                    executeHttp(cached, method, endpoint, jsonBody, callback, attachToken);
                    return;
                } catch (Exception ex) {
                    activeBaseUrl = null; // Cache invalidated, probe candidates
                }
            }

            // 2. Fast probe across all candidates
            for (String candidate : CANDIDATES) {
                try {
                    executeHttp(candidate, method, endpoint, jsonBody, callback, attachToken);
                    activeBaseUrl = candidate;
                    return;
                } catch (Exception ignored) {
                    // Try next candidate
                }
            }

            // 3. If all candidates fail to establish connection
            mainHandler.post(() -> callback.onError("Unable to connect to server. Please check your network or USB connection."));
        });
    }

    private static void executeHttp(String baseUrl, String method, String endpoint, String jsonBody, ApiCallback callback, boolean attachToken) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(baseUrl + endpoint);
            conn = (HttpURLConnection) url.openConnection();

            if ("PATCH".equalsIgnoreCase(method)) {
                try {
                    conn.setRequestMethod("PATCH");
                } catch (java.net.ProtocolException pe) {
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
                }
            } else {
                conn.setRequestMethod(method);
            }

            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(5000);

            // Automatically inject JWT Bearer token if session exists
            if (attachToken && appContext != null) {
                String token = SessionManager.getInstance(appContext).getAccessToken();
                if (token != null && !token.trim().isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }
            }

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
                String friendlyMsg = parseFriendlyErrorMessage(statusCode, responseStr);
                mainHandler.post(() -> callback.onError(friendlyMsg));
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String parseFriendlyErrorMessage(int statusCode, String responseBody) {
        String serverMessage = null;
        if (responseBody != null && !responseBody.trim().isEmpty()) {
            try {
                JSONObject json = new JSONObject(responseBody);
                if (json.has("message") && !json.isNull("message")) {
                    serverMessage = json.getString("message");
                } else if (json.has("errors") && !json.isNull("errors")) {
                    JSONObject errorsObj = json.getJSONObject("errors");
                    java.util.Iterator<String> keys = errorsObj.keys();
                    if (keys.hasNext()) {
                        String key = keys.next();
                        JSONArray arr = errorsObj.optJSONArray(key);
                        if (arr != null && arr.length() > 0) {
                            serverMessage = arr.getString(0);
                        } else {
                            serverMessage = errorsObj.optString(key, null);
                        }
                    }
                } else if (json.has("error") && !json.isNull("error")) {
                    serverMessage = json.getString("error");
                } else if (json.has("title") && !json.isNull("title")) {
                    String title = json.getString("title");
                    if (!"One or more validation errors occurred.".equalsIgnoreCase(title)) {
                        serverMessage = title;
                    }
                }
            } catch (Exception ignored) {}
        }

        if (serverMessage != null && !serverMessage.trim().isEmpty()) {
            serverMessage = serverMessage.replace('_', ' ');
            return serverMessage;
        }

        switch (statusCode) {
            case 400:
                return "Please check your entered details and try again.";
            case 401:
                return "Incorrect email or password. Please try again.";
            case 403:
                return "Access restricted. Your account may be pending approval.";
            case 404:
                return "Requested account or service was not found.";
            case 429:
                return "Too many requests. Please wait a moment.";
            case 500:
            case 502:
            case 503:
                return "Server is currently unavailable. Please try again in a few moments.";
            default:
                return "Request failed (Error " + statusCode + "). Please try again.";
        }
    }
}
