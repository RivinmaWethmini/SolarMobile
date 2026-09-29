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

    private static void sendRequest(String method, String endpoint, String jsonBody, ApiCallback callback, boolean attachToken) {
        executor.execute(() -> {
            String primaryBase = BASE_URL;
            String secondaryBase = primaryBase.equals(USB_ADB_URL) ? LAN_WIFI_URL : USB_ADB_URL;

            try {
                executeHttp(primaryBase, method, endpoint, jsonBody, callback, attachToken);
            } catch (Exception primaryEx) {
                // If primary endpoint is unreachable, auto-failover to secondary
                try {
                    executeHttp(secondaryBase, method, endpoint, jsonBody, callback, attachToken);
                    BASE_URL = secondaryBase;
                } catch (Exception secondaryEx) {
                    try {
                        // Failover 3: Emulator loopback 10.0.2.2
                        executeHttp(EMULATOR_URL, method, endpoint, jsonBody, callback, attachToken);
                        BASE_URL = EMULATOR_URL;
                    } catch (Exception tertiaryEx) {
                        String msg = "Connection error: " + primaryEx.getMessage();
                        mainHandler.post(() -> callback.onError(msg));
                    }
                }
            }
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
            conn.setConnectTimeout(4000);
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
