package com.vishnu.quickgodelivery.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.DocumentSnapshot;
import com.vishnu.quickgodelivery.miscellaneous.Utils;

import java.util.HashMap;
import java.util.Map;

public class LocationUpdater {

    private static final long UPDATE_INTERVAL_MS = 8000; // 8 seconds
    private static final String LOG_TAG = "LocationUpdater";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateTask;

    private final String partnerID;
    private double latitude;
    private double longitude;
    private final Context context;
    private SharedPreferences preferences;

    public LocationUpdater(Context context, String partnerID, SharedPreferences preferences) {
        this.context = context;
        this.partnerID = partnerID;
        this.preferences = preferences;
    }

    // BroadcastReceiver to receive location updates
    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocationService.ACTION_LOCATION_BROADCAST.equals(intent.getAction())) {
                latitude = intent.getDoubleExtra(LocationService.EXTRA_LATITUDE, 0.00);
                longitude = intent.getDoubleExtra(LocationService.EXTRA_LONGITUDE, 0.00);
            }
        }
    };

    // Method to start location updates
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void startLocationUpdates() {
        // Register the receiver for location updates
        IntentFilter filter = new IntentFilter(LocationService.ACTION_LOCATION_BROADCAST);
        context.registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        updateTask = new Runnable() {
            @Override
            public void run() {
                // Update location to Firestore
                if (preferences.getBoolean("isOnDuty", false)) {
                    updatePartnerLocationToDB(partnerID, latitude, longitude);
                }
                // Schedule the next update after 20 seconds
                handler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };
        handler.post(updateTask);
    }

    // Method to stop location updates
    public void stopLocationUpdates() {
        if (updateTask != null) {
            handler.removeCallbacks(updateTask);
        }
        context.unregisterReceiver(locationReceiver);
    }

    public static void updatePartnerLocationToDB(String partnerID, double latitude, double longitude) {
        try {
            DocumentReference partnerLocationRef = FirebaseFirestore.getInstance().document("DeliveryPartners/" + partnerID);

            // Create a GeoPoint with latitude and longitude
            GeoPoint geoPoint = new GeoPoint(latitude, longitude);

            Map<String, Object> locationData = new HashMap<>();
            locationData.put("dp_loc_coordinates", geoPoint);
            locationData.put("last_location_updated", FieldValue.serverTimestamp());

            partnerLocationRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot documentSnapshot = task.getResult();
                    if (documentSnapshot.exists()) {
                        partnerLocationRef.update(locationData).addOnSuccessListener(aVoid -> {
                            Log.d(LOG_TAG, "Partner location updated in DB: SUCCESS");
                        }).addOnFailureListener(e -> {
                            Log.d(LOG_TAG, "Partner location updated in DB: FAILED");
                        });
                    } else {
                        partnerLocationRef.set(locationData).addOnSuccessListener(aVoid -> {
                            Log.d(LOG_TAG, "Partner location added to DB: SUCCESS");
                        }).addOnFailureListener(e -> {
                            Log.d(LOG_TAG, "Partner location added to DB: FAILED");
                        });
                    }
                } else {
                    Log.d(LOG_TAG, "Failed to check document existence: " + task.getException());
                }
            });


        } catch (Exception e) {
            Log.e(LOG_TAG, "Error updating partner location: " + e.toString());
        }
    }
}
