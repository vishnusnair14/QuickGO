package com.vishnu.quickgovendor.services;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.telephony.SmsManager;

import androidx.annotation.RequiresApi;

public class SMS {

    // Constants for action strings
    private static final String SMS_SENT_ACTION = "SMS_SENT";
    private static final String SMS_DELIVERED_ACTION = "SMS_DELIVERED";

    // Use PendingIntent to check the result of the SMS sending
    private final PendingIntent sentPendingIntent;
    // Use PendingIntent to check the result of the SMS delivery
    private final PendingIntent deliveredPendingIntent;

    // Callback for sent status
    public interface OnMessageSentListener {
        void onMessageSent();
        void onMessageFailed();
    }

    // Callback for delivered status
    public interface OnMessageDeliveredListener {
        void onMessageDelivered();
        void onMessageNotDelivered();
    }

    // Broadcast receiver for sent status
    private final BroadcastReceiver sentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (onMessageSentListener != null) {
                if (getResultCode() == Activity.RESULT_OK) {
                    onMessageSentListener.onMessageSent();
                } else {
                    onMessageSentListener.onMessageFailed();
                }
            }
        }
    };

    // Broadcast receiver for delivered status
    private final BroadcastReceiver deliveredReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (onMessageDeliveredListener != null) {
                if (getResultCode() == Activity.RESULT_OK) {
                    onMessageDeliveredListener.onMessageDelivered();
                } else {
                    onMessageDeliveredListener.onMessageNotDelivered();
                }
            }
        }
    };

    private final OnMessageSentListener onMessageSentListener;
    private final OnMessageDeliveredListener onMessageDeliveredListener;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public SMS(Context context, OnMessageSentListener sentListener, OnMessageDeliveredListener deliveredListener) {
        this.onMessageSentListener = sentListener;
        this.onMessageDeliveredListener = deliveredListener;

        // Initialize PendingIntent for sent status
        sentPendingIntent = PendingIntent.getBroadcast(
                context, 0, new Intent(SMS_SENT_ACTION), PendingIntent.FLAG_IMMUTABLE
        );

        // Initialize PendingIntent for delivered status
        deliveredPendingIntent = PendingIntent.getBroadcast(
                context, 0, new Intent(SMS_DELIVERED_ACTION), PendingIntent.FLAG_IMMUTABLE
        );

        // Register the BroadcastReceiver for sent status
        context.registerReceiver(sentReceiver, new IntentFilter(SMS_SENT_ACTION), Context.RECEIVER_NOT_EXPORTED);

        // Register the BroadcastReceiver for delivered status
        context.registerReceiver(deliveredReceiver, new IntentFilter(SMS_DELIVERED_ACTION), Context.RECEIVER_NOT_EXPORTED);
    }

    public void sendSMS(String phoneNumber, String message) {
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(phoneNumber, null, message, sentPendingIntent, deliveredPendingIntent);
    }

    public void unregisterReceivers(Context context) {
        // Unregister the receivers when they are no longer needed
        context.unregisterReceiver(sentReceiver);
        context.unregisterReceiver(deliveredReceiver);
    }
}
