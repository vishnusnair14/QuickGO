package com.vishnu.quickgoorder.service;

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
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.cloud.DbHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class FCM extends FirebaseMessagingService {
    private final String LOG_TAG = "FCM";

    public FCM() {
    }

    private void showFCMNotification(String title, String body) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel("FCM_CHANNEL", "Order accepted",
                NotificationManager.IMPORTANCE_DEFAULT);

        notificationManager.createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "FCM_CHANNEL")
                .setSmallIcon(R.drawable.ic_launcher_order_app_icon_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

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

    private String generateTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss"));
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


}
