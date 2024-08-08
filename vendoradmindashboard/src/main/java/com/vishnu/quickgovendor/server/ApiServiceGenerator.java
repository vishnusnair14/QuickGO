package com.vishnu.quickgovendor.server;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.vishnu.quickgovendor.ui.settings.PreferencesManager;


public class ApiServiceGenerator {

    private static APISerivce apiService;
    private static final String LOG_TAG = "PreferenceManager";

    public static APISerivce getApiService(Context context) {
        if (apiService != null) {
            Log.d(LOG_TAG, "apiService not NULL");
            return apiService;
        }

        String baseUrl = PreferencesManager.getBaseUrl(context);
        Log.d(LOG_TAG, "apiService NULL: creating");
        Toast.makeText(context, baseUrl, Toast.LENGTH_SHORT).show();
        apiService = ApiClient.getClient(baseUrl).create(APISerivce.class);
        return apiService;
    }

    public static void resetApiService() {
        apiService = null;
        ApiClient.resetRetrofitClient();
        Log.d(LOG_TAG, "apiService set to NULL");
    }
}
