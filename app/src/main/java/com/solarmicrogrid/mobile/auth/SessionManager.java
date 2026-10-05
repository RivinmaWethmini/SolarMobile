package com.solarmicrogrid.mobile.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.solarmicrogrid.mobile.models.AuthUser;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Manages JWT tokens, authenticated user profile, and active session state
 * across all SolarMobile components.
 */
public class SessionManager {

    private static final String PREF_NAME = "solar_auth_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_JSON = "user_json";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    // Legacy preference keys for compatibility with existing views
    private static final String LEGACY_PREF_NAME = "solar_session";
    private static final String KEY_LEGACY_PROSUMER_NIC = "prosumer_nic";

    private static SessionManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final SharedPreferences legacyPrefs;

    private SessionManager(Context context) {
        Context appCtx = context.getApplicationContext();
        this.context = appCtx;
        this.prefs = appCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.legacyPrefs = appCtx.getSharedPreferences(LEGACY_PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    /**
     * Saves new session upon successful login or registration.
     */
    public void saveSession(String accessToken, String refreshToken, AuthUser user) {
        SharedPreferences.Editor editor = prefs.edit();
        if (accessToken != null) {
            editor.putString(KEY_ACCESS_TOKEN, accessToken);
        }
        if (refreshToken != null) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        }
        if (user != null) {
            editor.putString(KEY_USER_JSON, user.toJson().toString());
            // Sync with legacy prosumer_nic
            if (user.getNic() != null && !user.getNic().isEmpty()) {
                legacyPrefs.edit().putString(KEY_LEGACY_PROSUMER_NIC, user.getNic()).apply();
            } else if (user.getUsername() != null) {
                legacyPrefs.edit().putString(KEY_LEGACY_PROSUMER_NIC, user.getUsername()).apply();
            }

            // Sync user session with SQLite database
            try {
                com.solarmicrogrid.mobile.database.DatabaseHelper db = new com.solarmicrogrid.mobile.database.DatabaseHelper(context);
                db.saveUserSession(user.getNic(), user.getFullName(), user.getRole());
            } catch (Exception ignored) {
            }
        }
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * Updates tokens after a successful silent refresh.
     */
    public void updateTokens(String accessToken, String refreshToken) {
        SharedPreferences.Editor editor = prefs.edit();
        if (accessToken != null) {
            editor.putString(KEY_ACCESS_TOKEN, accessToken);
        }
        if (refreshToken != null) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        }
        editor.apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getAccessToken() != null;
    }

    public AuthUser getUser() {
        String jsonStr = prefs.getString(KEY_USER_JSON, null);
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return null;
        }
        try {
            return AuthUser.fromJson(new JSONObject(jsonStr));
        } catch (JSONException e) {
            return null;
        }
    }

    public String getProsumerNic() {
        AuthUser user = getUser();
        if (user != null && user.getNic() != null && !user.getNic().trim().isEmpty()) {
            return user.getNic();
        }
        return legacyPrefs.getString(KEY_LEGACY_PROSUMER_NIC, "200012345678");
    }

    /**
     * Revokes session and clears all cached tokens and user credentials.
     */
    public void clearSession() {
        prefs.edit().clear().apply();
        legacyPrefs.edit().clear().apply();
        try {
            com.solarmicrogrid.mobile.database.DatabaseHelper db = new com.solarmicrogrid.mobile.database.DatabaseHelper(context);
            db.clearUserSession();
        } catch (Exception ignored) {
        }
    }

    public void logout() {
        clearSession();
    }
}
