package com.vishnu.quickgoorder.cloud;

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.ui.home.recommendation.items.ProductModel;

import java.util.HashMap;
import java.util.Map;

public class DbHandler {
    private final FirebaseFirestore db;
    private static FirebaseUser user;
    private final static String LOG_TAG = "DbHandler";

    public DbHandler() {

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
    }

    /**
     * update items to user manual cart collection (on use add-to-cart btn) [OUTGOING ONLY]
     */
    public void addItemToManualCartDB(View view, ProductModel item, Vibrator vibrator, String shopID, TextView btn) {

        DocumentReference cartDataCollection = db.collection("Users")
                .document(user.getUid()).collection("userCartData")
                .document(shopID).collection("manualCartProductData").document(item.getItem_name());

        Map<String, Object> fieldName_mapType = getStringObjectMap(item);

        cartDataCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot documentSnapshot = task.getResult();
                if (documentSnapshot.exists()) {
                    cartDataCollection.update(fieldName_mapType).addOnSuccessListener(var -> {
                                btn.setText(R.string.ADDED);
                                btn.setTextSize(12);
                                btn.setTextColor(ContextCompat.getColor(view.getContext(), R.color.itemAdded));
                                btn.setBackgroundColor(ContextCompat.getColor(view.getContext(), R.color.itemAddedBck));
                                btn.setEnabled(false);

                                vibrate(vibrator);
                                Log.i(LOG_TAG, "Item added to cart");
                            }
                    ).addOnFailureListener(e ->
                            Toast.makeText(view.getContext(), "fail", Toast.LENGTH_SHORT).show());
                } else {
                    cartDataCollection.set(fieldName_mapType).addOnSuccessListener(var -> {
                        btn.setText(R.string.ADDED);
                        btn.setTextSize(12);
                        btn.setTextColor(ContextCompat.getColor(view.getContext(), R.color.itemAdded));
                        btn.setBackgroundColor(ContextCompat.getColor(view.getContext(), R.color.itemAddedBck));
                        btn.setEnabled(false);

                        vibrate(vibrator);
                        Log.i(LOG_TAG, "Item added to cart");
                    }).addOnFailureListener(e ->
                            Toast.makeText(view.getContext(), "fail", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    @NonNull
    private static Map<String, Object> getStringObjectMap(@NonNull ProductModel item) {
        Map<String, Object> subFieldName_mapType = new HashMap<>();
        subFieldName_mapType.put("item_name", item.getItem_name());
        subFieldName_mapType.put("item_image_url", item.getItem_image_url());
        subFieldName_mapType.put("item_qty", 1);
        subFieldName_mapType.put("item_price", item.getItem_price());
        subFieldName_mapType.put("item_price_unit", item.getItem_price_unit());

        return subFieldName_mapType;
    }


    public static void updateFCMTokenToDB(String updatedToken, String clientID) {
        DocumentReference FCMTokenBucketRef = FirebaseFirestore.getInstance().document("FCMTokenMapping/OrderAppClient");

        Map<String, Object> subAttributes = new HashMap<>();
        subAttributes.put("client_id", clientID);
        subAttributes.put("fcm_token", updatedToken);
        subAttributes.put("token_creation_date", FieldValue.serverTimestamp());

        Map<String, Object> keyMain = new HashMap<>();
        keyMain.put(clientID, subAttributes);

        FCMTokenBucketRef.get().addOnCompleteListener(task23 -> {
            if (task23.isSuccessful()) {
                DocumentSnapshot documentSnapshot = task23.getResult();
                if (documentSnapshot.exists()) {
                    FCMTokenBucketRef.update(keyMain).addOnSuccessListener(var -> {
                                Log.d(LOG_TAG, "FCM Token updated to db: SUCCESS");
                            }
                    ).addOnFailureListener(e ->
                            Log.d(LOG_TAG, "FCM Token updated to db: FAILED!"));
                } else {
                    FCMTokenBucketRef.set(keyMain).addOnSuccessListener(var -> {
                                Log.d(LOG_TAG, "FCM Token added to db: SUCCESS");
                            }
                    ).addOnFailureListener(e -> {
                        Log.d(LOG_TAG, "FCM Token added to db: FAILED!");
                    });
                }
            }
        });
    }


    private void vibrate(Vibrator vibrator) {
        vibrator.vibrate(VibrationEffect.createOneShot(50, 2));
    }
}