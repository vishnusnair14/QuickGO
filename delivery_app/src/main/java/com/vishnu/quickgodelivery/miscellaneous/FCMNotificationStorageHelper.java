package com.vishnu.quickgodelivery.miscellaneous;

import android.content.Context;
import android.content.SharedPreferences;

public class FCMNotificationStorageHelper {

    private static final String PREF_NAME = "NotificationPrefs";
    private static final String KEY_NOTIFICATION_PAYLOAD = "fcm_notification_payload";

    public static void saveNotificationPayload(Context context, String user_id, String shop_id) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_NOTIFICATION_PAYLOAD, shop_id);
        editor.putString(KEY_NOTIFICATION_PAYLOAD, user_id);
        editor.apply();
    }

    public static String getShopID(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getString(KEY_NOTIFICATION_PAYLOAD, "EMPTY");
    }

    public static String getUserID(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return preferences.getString(KEY_NOTIFICATION_PAYLOAD, "EMPTY");
    }
}
