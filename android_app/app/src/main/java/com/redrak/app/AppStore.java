package com.redrak.app;

import android.content.Context;
import android.content.SharedPreferences;

public class AppStore {
    private static AppStore instance;
    private SharedPreferences prefs;

    private AppStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE);
    }

    public static synchronized AppStore getInstance(Context context) {
        if (instance == null) {
            instance = new AppStore(context);
        }
        return instance;
    }

    public void setToken(String token) {
        prefs.edit().putString("AUTH_TOKEN", token).apply();
    }

    public String getToken() {
        return prefs.getString("AUTH_TOKEN", null);
    }

    public void setProfile(String profileJson) {
        prefs.edit().putString("USER_PROFILE", profileJson).apply();
    }

    public String getProfile() {
        return prefs.getString("USER_PROFILE", null);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}