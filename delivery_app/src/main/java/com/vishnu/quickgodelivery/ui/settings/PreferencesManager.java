package com.vishnu.quickgodelivery.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

public   class PreferencesManager {

    private static final String LOG_TAG = "PreferenceManager";
    private static final String PREFERENCES_FILE = "com.vishnu.intellicart.ui.settings";
    private static final String BASE_URL_KEY = "base_url";
    private static final String TEST_SERVER_URL = "https://big-terminally-lacewing.ngrok-free.app";
    private static final String PRODUCTION_SERVER_URL = "https://obedient-josi-voigo-cae47768.koyeb.app";
//    private static final String PRODUCTION_SERVER_URL = "https://wild-marna-quickgo-f8d67c90.koyeb.app";

    public static void setBaseUrl(@NonNull Context context, boolean useTestServer) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(BASE_URL_KEY, useTestServer ? TEST_SERVER_URL : PRODUCTION_SERVER_URL);
        editor.apply();
    }

    public static String getBaseUrl(@NonNull Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        Log.d(LOG_TAG, "Current using url: " + sharedPreferences.getString(BASE_URL_KEY, PRODUCTION_SERVER_URL));
        return sharedPreferences.getString(BASE_URL_KEY, TEST_SERVER_URL); // Default to test URL
    }
}
