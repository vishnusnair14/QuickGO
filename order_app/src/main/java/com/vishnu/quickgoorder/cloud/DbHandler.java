package com.vishnu.quickgoorder.cloud;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.miscellaneous.Utils;
import com.vishnu.quickgoorder.ui.home.recommendation.items.ItemModel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public void addItemToManualCartDB(View view, ItemModel item, Vibrator vibrator, String shopID, TextView btn) {

        DocumentReference cartDataCollection = db.collection("Users")
                .document(user.getUid()).collection("userCartData")
                .document(shopID).collection("manualCartProductData").document(item.getItem_name());  // TODO update item_id

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
    private static Map<String, Object> getStringObjectMap(@NonNull ItemModel item) {
        Map<String, Object> subFieldName_mapType = new HashMap<>();
        subFieldName_mapType.put("item_name", item.getItem_name());
        subFieldName_mapType.put("item_image_url", item.getItem_image_url());
        subFieldName_mapType.put("item_qty", 1);
        subFieldName_mapType.put("item_price", item.getItem_price());
        subFieldName_mapType.put("item_price_unit", item.getItem_price_unit());

        return subFieldName_mapType;
    }

    /**
     * clears user manual cart-data from db [request.OUTGOING]
     */
    @SuppressLint("NotifyDataSetChanged")
    public void deleteManualCartDBData(View view, String SHOP_ID) {
        CollectionReference orderAppCollRef = db.collection("IntellicartOrderApp");
        DocumentReference userAppdataCollectionRef = orderAppCollRef.document("userAppData");
        DocumentReference cartDataCollection = userAppdataCollectionRef.collection(user.getUid()).document("userCartData")
                .collection(SHOP_ID).document("manualCartProductData");

        cartDataCollection.get().addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get all field names
                        List<String> fieldNames = new ArrayList<>(Objects.requireNonNull(documentSnapshot.getData()).keySet());

                        // Iterate through the field names and delete each field
                        for (String fieldName : fieldNames) {
                            // Create a map to delete the field
                            Map<String, Object> updateMap = new HashMap<>();
                            updateMap.put(fieldName, FieldValue.delete());

                            // Update the document to delete the specified field
                            cartDataCollection.update(updateMap)
                                    .addOnSuccessListener(aVoid -> {

                                    })
                                    .addOnFailureListener(e -> Toast.makeText(view.getContext(), "Error connecting to DB!", Toast.LENGTH_SHORT).show());
                        }
                        Toast.makeText(view.getContext(), "Cleared m-cart successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(view.getContext(), "Document doesn't exist!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(view.getContext(), "Query failure occurred!", Toast.LENGTH_SHORT).show());
    }


    public void uploadAudioToStorageRec(Context context, String audioFileDir, String docID,
                                        String voiceOrderRefID, FloatingActionButton recBtn,
                                        TextView recordingStatusTV) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference("voiceOrderData/" + docID + "/" + voiceOrderRefID);

        File audioFile = new File(audioFileDir, "/voiceOrderAudio.mp3");

        StorageReference audioStorageRef = storageRef.child(Utils.generateRandomKey());

        UploadTask uploadTask = audioStorageRef.putFile(Uri.fromFile(audioFile));

        uploadTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                audioStorageRef.getDownloadUrl().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Uri downloadUri = task1.getResult();
                        String downloadURL = downloadUri.toString();

                        // update the audio url to db
                        addVoiceOrderToDB(context, downloadURL, docID, voiceOrderRefID);
                        recBtn.setEnabled(true);
                        recordingStatusTV.setText("");
                        Toast.makeText(context, "Upload success", Toast.LENGTH_SHORT).show();
                    } else {
                        // Handle getting download URL failure
                        Toast.makeText(context, "download URL failure occurred!", Toast.LENGTH_SHORT).show();
                        recBtn.setEnabled(true);
                    }
                });
            } else {
                Toast.makeText(context, "server upload failed!", Toast.LENGTH_SHORT).show();
                recBtn.setEnabled(true);

            }
        });
    }

    /**
     * update recent order-voice storage url to "OrderByVoiceAudioData" db collection
     */

    private void addVoiceOrderToDB(Context context, String downloadUrl, String docID, String voiceOrderRefID) {
        String key = Utils.generateRandomKey();
        DocumentReference orderByVoiceDataRef = db.collection("Users")
                .document(user.getUid()).collection("voiceOrdersData").document("obs")
                .collection(docID).document(voiceOrderRefID)
                .collection("voiceData").document(key);


        Map<String, Object> voiceOrderFields = new HashMap<>();
        voiceOrderFields.put("audio_key", key);
        voiceOrderFields.put("audio_storage_url", downloadUrl);
        voiceOrderFields.put("audio_title", Utils.generateTimestamp());

//         Create a map to hold the entire document data
//        Map<String, Object> voiceOrderData = new HashMap<>();
//        voiceOrderData.put(key, voiceOrderFields);

        orderByVoiceDataRef.get().addOnCompleteListener(task2 -> {
            if (task2.isSuccessful()) {
                DocumentSnapshot documentSnapshot = task2.getResult();
                if (documentSnapshot.exists()) {
                    orderByVoiceDataRef.update(voiceOrderFields).addOnSuccessListener(var -> {
//                                Toast.makeText(context, "Item added to cart.", Toast.LENGTH_SHORT).show();
                                Utils.deleteVoiceOrderFile(context, docID);
                                Log.d(LOG_TAG, "Audio url updated to db: success");
                            }
                    ).addOnFailureListener(e ->
                            Toast.makeText(context, "url server update failed!", Toast.LENGTH_SHORT).show());
                    Log.d(LOG_TAG, "Audio url update to db: failed!");
                } else {
                    orderByVoiceDataRef.set(voiceOrderFields).addOnSuccessListener(var -> {
                                Utils.deleteVoiceOrderFile(context, docID);
//                                Toast.makeText(context, "Item added to cart.", Toast.LENGTH_SHORT).show();
                                Log.d(LOG_TAG, "Audio url uploaded to db: success");
                            }
                    ).addOnFailureListener(e -> {
                        Toast.makeText(context, "url server upload failed!", Toast.LENGTH_SHORT).show();
                        Log.d(LOG_TAG, "Audio url uploaded to db: failed");
                    });
                }
            }
        });
    }

    /**
     * add or update new address to "userAddress" db collection
     */
//    public void addNewAddressToDB(Context context, Map<String, Object> addressInfo, String phno) {
//        CollectionReference IntellicartOrderAppRef = db.collection("IntellicartOrderApp");
//        DocumentReference orderByVoiceDataRef = IntellicartOrderAppRef.document("userAppData").collection(userId)
//                .document("userAddress");
//        Log.i(LOG_TAG, phno);
//        // Create a map to hold the entire document data
//        Map<String, Object> addressMap = new HashMap<>();
//        addressMap.put(phno, addressInfo);
//
//        orderByVoiceDataRef.get().addOnCompleteListener(task2 -> {
//            if (task2.isSuccessful()) {
//                DocumentSnapshot documentSnapshot = task2.getResult();
//                if (documentSnapshot.exists()) {
//                    orderByVoiceDataRef.update(addressMap).addOnSuccessListener(var -> {
//                                Toast.makeText(context, "New address has been updated to this account", Toast.LENGTH_SHORT).show();
//                                Log.d(LOG_TAG, "New address updated to db: success");
//                            }
//                    ).addOnFailureListener(e ->
//                            Toast.makeText(context, "Unable to update new address!", Toast.LENGTH_SHORT).show());
//                    Log.d(LOG_TAG, "New address updated to db: failed!");
//                } else {
//                    orderByVoiceDataRef.set(addressMap).addOnSuccessListener(var -> {
//                                Toast.makeText(context, "New address has been added to this account", Toast.LENGTH_SHORT).show();
//                                Log.d(LOG_TAG, "New address added to db: success");
//                            }
//                    ).addOnFailureListener(e -> {
//                                Toast.makeText(context, "Unable to add new address!", Toast.LENGTH_SHORT).show();
//                                Log.d(LOG_TAG, "New address added to db: failed");
//                            }
//                    );
//                }
//            }
//        });
//    }
    public static void updateFCMTokenToDB(String updatedToken, String clientID) {
        DocumentReference FCMTokenBucketRef = FirebaseFirestore.getInstance().document("FCMTokenMapping/OrderAppClient");

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