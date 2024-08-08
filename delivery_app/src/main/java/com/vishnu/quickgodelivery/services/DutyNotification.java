package com.vishnu.quickgodelivery.services;

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

import com.vishnu.quickgodelivery.R;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class DutyNotification extends Service {
    String notificationText;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            notificationText = intent.getStringExtra("DUTY_NOTIFICATION_TEXT");
            showDutyNotification();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showDutyNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "CHANNEL_ID")
                .setSmallIcon(R.drawable.baseline_delivery_dining_24)
                .setContentTitle("Delivery Duty Status")
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.notify(1237, builder.build());
    }
}

