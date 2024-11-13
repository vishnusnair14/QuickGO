package com.vishnu.quickgodelivery.cloud;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class DbHandler {
    //    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String LOG_TAG = "DatabaseHandler";

    public static void updateFCMTokenToDB(String updatedToken, String clientID) {
        try {
            DocumentReference FCMTokenBucketRef = FirebaseFirestore.getInstance().document("FCMTokenMapping/DeliveryAppClient");

            Map<String, Object> subAttributes = new HashMap<>();
            subAttributes.put("delivery_client_id", clientID);
            subAttributes.put("fcm_token", updatedToken);
            subAttributes.put("token_creation_date", FieldValue.serverTimestamp());

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


    private static String generateTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyyy_HHmmss"));
    }
}
