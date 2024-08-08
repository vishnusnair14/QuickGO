package com.vishnu.quickgoorder.server.sapi;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.vishnu.quickgoorder.ui.settings.PreferencesManager;

public class ApiServiceGenerator {

    private static APIService apiService;
    private static final String LOG_TAG = "PreferenceManager";

    public static APIService getApiService(Context context) {
        if (apiService != null) {
            Log.d(LOG_TAG, "apiService not NULL");
            return apiService;
        }

        String baseUrl = PreferencesManager.getBaseUrl(context);
        Log.d(LOG_TAG, "apiService NULL: creating");
//        Toast.makeText(context, baseUrl, Toast.LENGTH_SHORT).show();
        apiService = ApiClient.getClient(baseUrl).create(APIService.class);
        return apiService;
    }

    public static void resetApiService() {
        apiService = null;
        ApiClient.resetRetrofitClient();
        Log.d(LOG_TAG, "apiService set to NULL");
    }
}
