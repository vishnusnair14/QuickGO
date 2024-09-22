package com.vishnu.quickgoorder.service;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.ui.track.OrderTrackActivity;

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

        // Create an Intent for the activity you want to start.
        Intent resultIntent = new Intent(this, OrderTrackActivity.class);

        // Create the TaskStackBuilder and add the intent, which inflates the back
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);

        // Get the PendingIntent containing the entire back stack.
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "FCM_CHANNEL")
                .setSmallIcon(R.drawable.ic_launcher_order_app_icon_foreground)
                .setContentTitle("New Order Received")
                .setContentIntent(resultPendingIntent)
                .setContentText(fcmNotificationText)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.notify(1234, builder.build());
    }
}

