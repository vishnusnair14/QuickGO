package com.vishnu.quickgoorder.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vishnu.quickgoorder.R;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LocationService extends Service {

    private final String LOG_TAG = "LocationService";
    private static final String CHANNEL_ID = "LocationServiceChannel";
    public static final String ACTION_LOCATION_BROADCAST = "LocationService.LocationBroadcast";
    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String ACTION_ENABLE_BROADCAST = "ACTION_ENABLE_BROADCAST";
    public static final String ACTION_DISABLE_BROADCAST = "ACTION_DISABLE_BROADCAST";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;
    private DocumentReference inactivePartnersRef;
    private FirebaseUser user;
    private boolean isBroadcastingEnabled = true;
    private final DecimalFormat coordinateFormat = new DecimalFormat("0.0000000000");

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        // TODO
        inactivePartnersRef = db.collection("DeliveryPartnersData")
                .document("availablePartnersForDelivery")
                .collection("mysore").document(getPresentDate());

//        createNotificationChannel();
//        startForeground(1, createNotification());

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
//                    uploadDPDataToDB(location);
                    if (isBroadcastingEnabled) {
                        sendLocationBroadcast(location);
                    }
                }
            }
        };

        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2500)
                .setMinUpdateIntervalMillis(2500)
                .setMaxUpdateDelayMillis(3000)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service")
                .setContentText("Tracking location in the background")
                .setSmallIcon(R.drawable.baseline_my_location_24)
                .build();
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @NonNull
    private Map<String, Object> getDPData(double lat, double lon) {
        Map<String, Object> deliveryPartnerData = new HashMap<>();
        deliveryPartnerData.put("dp_name", user.getDisplayName());
        deliveryPartnerData.put("dp_id", user.getUid());
        deliveryPartnerData.put("dp_lat", lat);
        deliveryPartnerData.put("dp_lon", lon);
        deliveryPartnerData.put("dp_tst", FieldValue.serverTimestamp());

        Map<String, Object> deliveryPartnerInfo = new HashMap<>();
        deliveryPartnerInfo.put(user.getUid(), deliveryPartnerData);
        return deliveryPartnerInfo;
    }

    public void uploadDPDataToDB(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        Map<String, Object> deliveryPartnerInfo = getDPData(lat, lon);

        inactivePartnersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot documentSnapshot = task.getResult();
                if (documentSnapshot.exists()) {
                    // Document exists, update the array and user data
                    inactivePartnersRef.update(deliveryPartnerInfo)
                            .addOnSuccessListener(aVoid -> {
                                // Document updated successfully
                                Log.i(LOG_TAG, "Delivery partner's co-ordinates updated to db!");
                            })
                            .addOnFailureListener(e -> {
                                // Handle errors here
                                Log.i(LOG_TAG, "Error updating co-ordinates!");
                            });
                } else {
                    // Document doesn't exist, set the array and user data
                    inactivePartnersRef.set(deliveryPartnerInfo)
                            .addOnSuccessListener(aVoid -> {
                                // Document updated successfully
                                Log.i(LOG_TAG, "Delivery partner co-ordinates updated!");
                            })
                            .addOnFailureListener(e -> {
                                // Handle errors here
                                Log.i(LOG_TAG, "Error updating co-ordinates!");
                            });
                }
            }
        });

    }

    private void sendLocationBroadcast(Location location) {
        Intent intent = new Intent(ACTION_LOCATION_BROADCAST);
        intent.putExtra(EXTRA_LATITUDE, location.getLatitude());
        intent.putExtra(EXTRA_LONGITUDE, location.getLongitude());
        sendBroadcast(intent);
    }

    private String getPresentDate() {
        return new SimpleDateFormat("ddMMMyyyy", Locale.getDefault()).format(new Date()).toUpperCase();
    }

    private String getCurrentDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_ENABLE_BROADCAST.equals(action)) {
                isBroadcastingEnabled = true;
            } else if (ACTION_DISABLE_BROADCAST.equals(action)) {
                isBroadcastingEnabled = false;
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        stopLocationService();
    }

    public void stopLocationService() {
        stopForeground(true);
        stopSelf();
    }

}
