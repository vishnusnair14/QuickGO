package com.vishnu.quickgoorder.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.vishnu.quickgoorder.R;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class FCMNotification extends Service {
    String fcmNotificationText;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            fcmNotificationText = intent.getStringExtra("FCM_NOTIFICATION_TEXT");
            showFCMNotification();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showFCMNotification() {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "FCM_CHANNEL")
                .setSmallIcon(R.drawable.baseline_cloud_24)
                .setContentTitle("New Order Received")
                .setContentText(fcmNotificationText)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.notify(1234, builder.build());
    }
}

