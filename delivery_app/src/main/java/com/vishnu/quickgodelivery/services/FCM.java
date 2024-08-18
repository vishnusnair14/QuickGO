package com.vishnu.quickgodelivery.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.vishnu.quickgodelivery.R;
import com.vishnu.quickgodelivery.miscellaneous.FCMNotificationStorageHelper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class FCM extends FirebaseMessagingService {
    private final String LOG_TAG = "FCM";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    public FCM() {
    }

    private void showFCMNotification(String title, String body) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        long[] vibrationPattern = {0, 500, 1000, 500};

        NotificationChannel channel = new NotificationChannel(
                "FCM_CHANNEL",
                "FCM Notification",
                NotificationManager.IMPORTANCE_HIGH);

        channel.setDescription("This channel is used for fcm message notifications.");
        channel.setVibrationPattern(vibrationPattern);
        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "FCM_CHANNEL")
                .setSmallIcon(R.drawable.ic_launcher_delivery_app_icon_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(vibrationPattern)
                .setAutoCancel(true);

        Notification notification = builder.build();
        notificationManager.notify(0, notification);
    }

    private String generateTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss"));
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        /* Update new token in database */
        if (user != null) {
            updateFCMTokenToDB(token, user.getUid());
            Log.i(LOG_TAG, "Refreshed token: " + token);
        } else {
            Log.d(LOG_TAG, "user null @FCM");
        }

    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
    }

    @Override
    public void onMessageSent(@NonNull String msgId) {
        super.onMessageSent(msgId);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (!remoteMessage.getData().isEmpty()) {
            FCMNotificationStorageHelper.saveNotificationPayload(getApplicationContext(),
                    remoteMessage.getData().get("user_id"), remoteMessage.getData().get("shop_id"));

            // Log data payload
            Log.i(LOG_TAG, "Data Payload: " + remoteMessage.getData());
        }

        if (remoteMessage.getNotification() != null) {
            // Handle notification payload
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            Log.i(LOG_TAG, "Notification Title: " + title);
            Log.i(LOG_TAG, "Notification Body: " + body);

            // Display notification if app is in the foreground
            showFCMNotification(title, body);
        }
    }


    private void updateFCMTokenToDB(String updatedToken, String clientID) {
        try {
            DocumentReference FCMTokenBucketRef = db.document("FCMTokenMapping/DeliveryAppClient");

            Map<String, Object> subAttributes = new HashMap<>();
            subAttributes.put("delivery_client_id", clientID);
            subAttributes.put("fcm_token", updatedToken);
            subAttributes.put("token_creation_date", generateTimestamp());

            Map<String, Object> keyMain = new HashMap<>();
            keyMain.put(clientID, subAttributes);

            FCMTokenBucketRef.get().addOnCompleteListener(task23 -> {
                if (task23.isSuccessful()) {
                    DocumentSnapshot documentSnapshot = task23.getResult();
                    if (documentSnapshot.exists()) {
                        if (!keyMain.isEmpty()) {
                            FCMTokenBucketRef.update(keyMain).addOnSuccessListener(var -> {
                                        Log.d(LOG_TAG, "FCM Token updated to db: SUCCESS");
                                    }
                            ).addOnFailureListener(e ->
                                    Log.d(LOG_TAG, "FCM Token updated to db: FAILED!"));
                        }
                    } else {
                        if (!keyMain.isEmpty()) {
                            FCMTokenBucketRef.set(keyMain).addOnSuccessListener(var -> {
                                        Log.d(LOG_TAG, "FCM Token added to db: SUCCESS");
                                    }
                            ).addOnFailureListener(e -> {
                                Log.d(LOG_TAG, "FCM Token added to db: FAILED!");
                            });
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
    }


}
