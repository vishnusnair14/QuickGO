package com.vishnu.quickgoorder.ui.home.recommendation.items;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Chronometer;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.cloud.DbHandler;
import com.vishnu.quickgoorder.databinding.FragmentItemsBinding;
import com.vishnu.quickgoorder.miscellaneous.PreferenceKeys;
import com.vishnu.quickgoorder.miscellaneous.SoundManager;
import com.vishnu.quickgoorder.miscellaneous.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class ItemFragment extends Fragment {
    private static final String LOG_TAG = "HomeFragment";
    FirebaseFirestore db;
    FloatingActionButton recordBtn;
    FloatingActionButton showRecFab;
    private static MediaRecorder mediaRecorder;
    private DbHandler dbHandler;
    private boolean isButtonHeld = false;
    TextView recordingStatusTV;
    TextView pressAndRecMainTV;
    File AppAudioDir;
    private SharedPreferences preferences;
    TextView shopBannerTV;
    private final Handler handler = new Handler();
    List<ItemModel> itemList = new ArrayList<>();
    private static Vibrator vibrator;
    TextView gotoCartBtn;
    static String audioFileName = "_voice.mp3";
    private String shopID;
    private Chronometer chronometer;
    private FirebaseUser user;

    String[] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    public ItemFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        preferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);

        dbHandler = new DbHandler();

        File externalFilesDir = requireContext().getExternalFilesDir(Context.AUDIO_SERVICE);
        SoundManager.initialize(requireContext());

        // Check if the directory exists; if not, create it
        if (externalFilesDir != null && !externalFilesDir.exists()) {
            boolean isDirCreated = externalFilesDir.mkdirs();
            if (!isDirCreated) {
                // Handle the error - directory creation failed
                Log.e("DirectoryError", "Failed to create the directory: " + externalFilesDir.getAbsolutePath());
            }
        }

        AppAudioDir = new File(externalFilesDir, "orderByVoice");

    }

    @SuppressLint({"ClickableViewAccessibility", "NotifyDataSetChanged"})
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        com.vishnu.quickgoorder.databinding.FragmentItemsBinding binding = FragmentItemsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        Bundle bundle = new Bundle();

        showRecFab = binding.showRecViewFloatingActionButton;
        shopBannerTV = binding.shopNameBannerViewTextView;
        gotoCartBtn = binding.homeItemsFragmentGotoCartButton;

        // OnCreate permission request
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), permissionsToRequest.toArray(new String[0]), 1);
        }

        itemList.clear();

        if (getArguments() != null) {
            shopBannerTV.setText(requireArguments().getString("shop_name".toUpperCase()));
            shopID = getArguments().getString("shop_id");
            String shopDistrict = getArguments().getString("shop_district");

            bundle.putBoolean("fromHomeRecommendationFragment", true);
            bundle.putString("shop_id", shopID);
            bundle.putString("shop_district", shopDistrict);
        }

        syncItemGridView(binding, shopID);

        gotoCartBtn.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_nav_mcart, bundle);
            Utils.vibrate(requireContext(), 50, VibrationEffect.EFFECT_TICK);
        });

        showRecFab.setOnLongClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_home_to_nav_mcart, bundle);
            Utils.vibrate(requireContext(), 0, VibrationEffect.EFFECT_TICK);
            return true;
        });


        showRecFab.setOnClickListener(v -> showRecordBtmView(root));

        return root;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showRecordBtmView(View fragmentView) {
        View orderByVoiceView = LayoutInflater.from(requireContext()).inflate(
                R.layout.bottomview_recommendation_order_by_voice, (ViewGroup) fragmentView, false);

        // Create a BottomSheetDialog with TOP gravity
        BottomSheetDialog recordVoiceOrderBtmView = new BottomSheetDialog(requireContext());
        recordVoiceOrderBtmView.setContentView(orderByVoiceView);
        recordVoiceOrderBtmView.setCanceledOnTouchOutside(true);
        Objects.requireNonNull(recordVoiceOrderBtmView.getWindow()).setGravity(Gravity.TOP);

        recordBtn = orderByVoiceView.findViewById(R.id.recordVoice_imageButton);
        recordingStatusTV = orderByVoiceView.findViewById(R.id.recordingStatus_textView);
        pressAndRecMainTV = orderByVoiceView.findViewById(R.id.tapAndRecordMain_textView);
        chronometer = orderByVoiceView.findViewById(R.id.chronometer2);
        ImageView recIcon = orderByVoiceView.findViewById(R.id.micIcon_imageView);

        Animation blinkAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.blink);
        recIcon.setVisibility(View.INVISIBLE);

        Typeface typeface = ResourcesCompat.getFont(requireContext(), R.font.archivo_black);
        chronometer.setTypeface(typeface);

        recordBtn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    isButtonHeld = true;
                    recIcon.setVisibility(View.VISIBLE);
                    recIcon.setAnimation(blinkAnimation);
                    recordVoiceOrderBtmView.setCanceledOnTouchOutside(false);
                    recordVoiceOrderBtmView.setCancelable(false);
                    onButtonHoldRunnable.run();
                    return true;
                }
                case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isButtonHeld = false;
                    handler.removeCallbacks(onButtonHoldRunnable);
                    stopChronometer();
                    recordingStatusTV.setText("");
                    pressAndRecMainTV.setText(R.string.send_your_voice_orders);
                    pressAndRecMainTV.setTextColor(requireActivity().getColor(R.color.default_textview));
                    recIcon.setVisibility(View.INVISIBLE);
                    recIcon.setAnimation(null);

                    if (!isButtonHeld) {
                        performOnButtonRelease(recordVoiceOrderBtmView);
                    }
                    return true;
                }
            }
            return false;
        });

        recordVoiceOrderBtmView.show();
    }

    private void startChronometer() {
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.setVisibility(View.VISIBLE);
        chronometer.start();
    }

    private void stopChronometer() {
        chronometer.stop();
        chronometer.setBase(SystemClock.elapsedRealtime());
    }

    /**
     * sync home-product recycle view with 'ShopIDMap/ShopItemInfo' db document [INCOMING ONLY]
     *
     * @fragment_home.xml
     */
    private void syncItemGridView(@NonNull FragmentItemsBinding binding, String SHOP_ID) {
        GridView gridView = binding.homeFragmentGridView;
//        gridView.setNumColumns(3);

        itemList.clear();

        /* Retrieve data from Firestore */
        db.collection("ShopData").document("itemData").collection("allAvailableItems")
                .document(SHOP_ID).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Map<String, Object> itemData = document.getData();
                            if (itemData != null) {
                                for (String field : itemData.keySet()) {
                                    Map<String, Object> dataMap1 = (Map<String, Object>) itemData.get(field);
                                    if (dataMap1 != null) {
                                        // Extract item data and add to itemList
                                        String itemID = (String) dataMap1.get("item_id");
                                        String itemName = (String) dataMap1.get("item_name");
                                        String itemImageUrl = (String) dataMap1.get("item_image_url");
                                        String itemQty = (String) dataMap1.get("item_qty");
                                        Long itemPrice = (Long) dataMap1.get("item_price");
                                        String itemPriceUnit = (String) dataMap1.get("item_price_unit");
                                        String itemNameReference = (String) dataMap1.get("item_name_reference");
                                        int price = 0;
                                        if (itemPrice != null) {
                                            price = Math.toIntExact(itemPrice);
                                        }
                                        ItemModel item = new ItemModel(itemID, itemImageUrl, itemName, itemQty, price, itemPriceUnit, itemNameReference);
                                        itemList.add(item);
                                    }
                                }
                                // Update the GridView adapter
                                ItemAdapter adapter = new ItemAdapter(requireContext(), dbHandler, vibrator, itemList, SHOP_ID);
                                gridView.setAdapter(adapter);

                                binding.homeFragmentLoadingProgressBar.setVisibility(View.GONE);
                                binding.homeFragLoadingViewTextView.setVisibility(View.GONE);
                            }
                        }
                    } else {
                        Log.d("Firestore", "Error fetching document: ", task.getException());
                    }
                });
    }

    private final Runnable onButtonHoldRunnable = () -> {
        if (isButtonHeld) {
            performOnButtonHold();
        }
    };


    private void performOnButtonHold() {
        startChronometer();
        pressAndRecMainTV.setText(R.string.recording);
        pressAndRecMainTV.setTextColor(requireActivity().getColor(R.color.recording));
        recordingStatusTV.setText(R.string.recording);
        startRecording(AppAudioDir);
        Log.d(TAG, "testButtonUI: onButtonHold");
    }

    private void performOnButtonRelease(@NonNull BottomSheetDialog recordVoiceOrderBtmView) {
        stopRecording(requireContext());

        recordBtn.setEnabled(false);
        recordVoiceOrderBtmView.setCancelable(true);

        recordingStatusTV.setText(R.string.uploading_to_db);

        uploadAudioToStorageRec(requireContext(), String.valueOf(AppAudioDir));
    }

    private static void startRecording(@NonNull File AppAudioDir) {
        if (!AppAudioDir.exists()) {
            if (AppAudioDir.mkdirs()) {
                Log.d(ContentValues.TAG, "AppAudioDir: dir created successful");
            } else {
                Log.d(ContentValues.TAG, "AppAudioDir: unable to create dir!");
            }
        } else {
            Log.d(ContentValues.TAG, "AppAudioDir: already exists!");
        }

        mediaRecorder = new MediaRecorder();
        File audioFile = new File(AppAudioDir, audioFileName);

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(audioFile);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioEncodingBitRate(128000);
        mediaRecorder.setAudioSamplingRate(44100);
        Log.d(ContentValues.TAG, "OutputFilePath: " + audioFile.getAbsolutePath());

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    private static void stopRecording(Context context) {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
                Toast.makeText(context, "error!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void addVoiceOrderToDB(String key, Context context, String downloadUrl,
                                   String voiceDocID, String voiceOrderID) {

        DocumentReference orderByVoiceDataRef = db.collection("Users")
                .document(user.getUid()).collection("voiceOrdersData")
                .document("obs").collection(voiceDocID)
                .document(voiceOrderID).collection(shopID).document(key);


        Map<String, Object> voiceOrderFields = new HashMap<>();
        voiceOrderFields.put("audio_key", key);
        voiceOrderFields.put("audio_storage_url", downloadUrl);
        voiceOrderFields.put("audio_title", Utils.generateTimestamp());

        orderByVoiceDataRef.get().addOnCompleteListener(task2 -> {
            if (task2.isSuccessful()) {
                DocumentSnapshot documentSnapshot = task2.getResult();
                if (documentSnapshot.exists()) {
                    orderByVoiceDataRef.update(voiceOrderFields).addOnSuccessListener(var -> {
//                                Toast.makeText(context, "Item added to cart.", Toast.LENGTH_SHORT).show();
                                Utils.deleteVoiceOrderCacheFile(context, voiceDocID, shopID);
                                Log.d(LOG_TAG, "Audio url updated to db: success");
                            }
                    ).addOnFailureListener(e ->
                            Toast.makeText(context, "url server update failed!", Toast.LENGTH_SHORT).show());
                    Log.d(LOG_TAG, "Audio url update to db: failed!");
                } else {
                    orderByVoiceDataRef.set(voiceOrderFields).addOnSuccessListener(var -> {
                                Utils.deleteVoiceOrderCacheFile(context, voiceDocID, shopID);
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

    private void uploadAudioToStorageRec(Context context, String audioFileDir) {
        String key = Utils.generateRandomKey();

        String orderID = preferences.getString(PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_ORDER_ID, "0");
        String audioRefID = preferences.getString(PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_AUDIO_REF_ID, "0");

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference("orderData/" + orderID + "/orderByVoiceData/" + audioRefID + "/" + key);

        File audioFile = new File(audioFileDir, "/" + audioFileName);

        StorageReference audioStorageRef = storageRef.child("audio_file_" + key + audioFileName);

        UploadTask uploadTask = audioStorageRef.putFile(Uri.fromFile(audioFile));

        uploadTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                audioStorageRef.getDownloadUrl().addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Uri downloadUri = task1.getResult();
                        String downloadURL = downloadUri.toString();

                        // update the audio url to db
                        addVoiceOrderToDB(key, context, downloadURL, orderID, audioRefID);
                        recordBtn.setEnabled(true);
                        recordingStatusTV.setText("");
                        Toast.makeText(context, "Upload success", Toast.LENGTH_SHORT).show();
                    } else {
                        // Handle getting download URL failure
                        recordBtn.setEnabled(true);
                        Toast.makeText(context, "download URL failure occurred!", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                recordBtn.setEnabled(true);
                Toast.makeText(context, "server upload failed!", Toast.LENGTH_SHORT).show();

            }
        });
    }
}

