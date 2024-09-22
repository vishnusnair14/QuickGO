package com.vishnu.quickgoorder.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.vishnu.quickgoorder.MainActivity;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.cloud.DbHandler;
import com.vishnu.quickgoorder.ui.track.OrderTrackActivity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class FCM extends FirebaseMessagingService {
    private final String LOG_TAG = "FCM";
    private String trackOrderID;

    public FCM() {
    }

    private void showForegroundFCMNotification(String title, String body) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        long[] vibrationPattern = {0, 500, 1000, 500};

        NotificationChannel channel = new NotificationChannel(
                "FCM_CHANNEL",
                "FCM Notification",
                NotificationManager.IMPORTANCE_HIGH);

        channel.setDescription("This channel is used for fcm message notifications.");
        channel.setVibrationPattern(vibrationPattern);
        notificationManager.createNotificationChannel(channel);


        // Create an Intent for the activity you want to start.
        Intent resultIntent = new Intent(this, OrderTrackActivity.class);
        resultIntent.putExtra("orderToTrackOrderID", trackOrderID);

        // Create the TaskStackBuilder and add the intent, which inflates the back
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);

        // Get the PendingIntent containing the entire back stack.
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "FCM_CHANNEL")
                .setSmallIcon(R.drawable.ic_launcher_order_app_icon_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(resultPendingIntent)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(vibrationPattern)
                .setAutoCancel(true);

        Notification notification = builder.build();
        notificationManager.notify(0, notification);

    }


    private String getClientId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            return user.getUid();
        } else {
            return null;
        }
    }


    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        if (getClientId() != null) {
            DbHandler.updateFCMTokenToDB(token, getClientId());
        }
        Log.i(LOG_TAG, "Refreshed token: " + token);
    }

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
    }

    @Override
    public void onMessageSent(@NonNull String msgId) {
        super.onMessageSent(msgId);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (!remoteMessage.getData().isEmpty()) {

//            FCMNotificationStorageHelper.saveNotificationPayload(getApplicationContext(),
//                    remoteMessage.getData().get("user_id"), remoteMessage.getData().get("shop_id"));
//            EventBus.getDefault().post(new OrderReceiveService(remoteMessage.getData().get("user_id"),
//            remoteMessage.getData().get("shop_id"), remoteMessage.getData().get("shop_loc"),
//            remoteMessage.getData().get("order_time")));
            Log.i(LOG_TAG, "Data Payload: " + remoteMessage.getData());
            trackOrderID = remoteMessage.getData().get("order_id");
        }

        if (remoteMessage.getNotification() != null) {
            // Handle notification payload
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            Log.d(LOG_TAG, remoteMessage.getData().toString());
            Log.i(LOG_TAG, "Notification Title: " + title);
            Log.i(LOG_TAG, "Notification Body: " + body);

            // Display notification if app is in the foreground
            showForegroundFCMNotification(title, body);
        }
    }


}
