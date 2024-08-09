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
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.cloud.DbHandler;
import com.vishnu.quickgoorder.databinding.FragmentItemsBinding;
import com.vishnu.quickgoorder.miscellaneous.PreferenceKeys;
import com.vishnu.quickgoorder.miscellaneous.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    TextView recordingStatusTV, recordVoiceTimerTV;
    TextView pressAndRecMainTV;
    private boolean isRunning = false;
    File AppAudioDir;
    private SharedPreferences preferences;
    TextView shopBannerTV;
    private final Handler handler = new Handler();
    List<ItemModel> itemList = new ArrayList<>();
    private long startTime = 0;
    private static Vibrator vibrator;
    TextView gotoCartBtn;
    private String shopID;
    private String shopDistrict;
    private Chronometer chronometer;

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

    @SuppressLint({"ClickableViewAccessibility", "NotifyDataSetChanged"})
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        com.vishnu.quickgoorder.databinding.FragmentItemsBinding binding = FragmentItemsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();
        preferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);

        showRecFab = binding.showRecViewFloatingActionButton;
        shopBannerTV = binding.shopNameBannerViewTextView;
        gotoCartBtn = binding.homeItemsFragmentGotoCartButton;

        Bundle bundle = new Bundle();
        dbHandler = new DbHandler();

        File externalFilesDir = requireContext().getExternalFilesDir(null);
        AppAudioDir = new File(externalFilesDir, "Voice-order-Recordings");

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
            shopBannerTV.setText(getArguments().getString("shop_name"));
            shopID = getArguments().getString("shop_id");
            shopDistrict = getArguments().getString("shop_district");

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
        recordVoiceOrderBtmView.setCanceledOnTouchOutside(false);
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

                    onButtonHoldRunnable.run();
                    recordVoiceOrderBtmView.setCancelable(false);
                    return true;
                }
                case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isButtonHeld = false;
                    handler.removeCallbacks(onButtonHoldRunnable);
                    stopChronometer();
                    recordVoiceOrderBtmView.setCancelable(true);
                    recordingStatusTV.setText("");
                    pressAndRecMainTV.setText(R.string.send_your_voice_orders);
                    pressAndRecMainTV.setTextColor(requireActivity().getColor(R.color.default_textview));
                    recIcon.setVisibility(View.INVISIBLE);
                    recIcon.setAnimation(null);

                    if (!isButtonHeld) {
                        performOnButtonRelease();
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

    private void performOnButtonRelease() {
        stopRecording(requireContext());
        recordBtn.setEnabled(false);

        recordingStatusTV.setText(R.string.uploading_to_db);

        // uploads order-voice audio file to storage
        dbHandler.uploadAudioToStorageRec(requireContext(), String.valueOf(AppAudioDir), shopID,
                preferences.getString(
                        PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_AUDIO_REF_ID, "0"), recordBtn, recordingStatusTV);
    }

    private static void startRecording(File AppAudioDir) {
        if (!AppAudioDir.exists()) {
            if (AppAudioDir.mkdirs()) {
                Log.d(ContentValues.TAG, "AppAudioDir: dir created successful");
            } else {
                Log.d(ContentValues.TAG, "AppAudioDir: unable to create dir!");
            }
        } else {
            Log.d(ContentValues.TAG, "AppAudioDir: already exists!");
        }

        String audioFileName = "voiceOrderAudio.mp3";
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
}

