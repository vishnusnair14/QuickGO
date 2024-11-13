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
import android.widget.ProgressBar;
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
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.cloud.DbHandler;
import com.vishnu.quickgoorder.databinding.FragmentItemsBinding;
import com.vishnu.quickgoorder.miscellaneous.PreferenceKeys;
import com.vishnu.quickgoorder.miscellaneous.SoundManager;
import com.vishnu.quickgoorder.miscellaneous.Utils;
import com.vishnu.quickgoorder.server.sapi.APIService;
import com.vishnu.quickgoorder.server.sapi.ApiServiceGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class ProductFragment extends Fragment {
    private static final String LOG_TAG = "HomeFragment";
    FirebaseFirestore db;
    FloatingActionButton recordBtn;
    FloatingActionButton showRecFab;
    private static MediaRecorder mediaRecorder;
    private DbHandler dbHandler;
    private boolean isButtonHeld = false;
    TextView recordingStatusTV;
    TextView pressAndRecMainTV;
    private ProductAdapter productAdapter;
    File AppAudioDir;
    private SharedPreferences preferences;
    TextView shopBannerTV;
    private final Handler handler = new Handler();
    List<ProductModel> itemList = new ArrayList<>();
    private static Vibrator vibrator;
    TextView gotoCartBtn;
    static String audioFileName = "_voice.mp3";
    private String shopID;
    TabLayout tabLayout;
    private Chronometer chronometer;
    ProgressBar progressBar;
    private FirebaseUser user;
    String shopState, shopDistrict;
    private GridView gridViewItems;


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

    public ProductFragment() {
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

        showRecFab = binding.homeItemsFragmentGotoCartButtonFab;
        shopBannerTV = binding.shopNameBannerViewTextView;
        gotoCartBtn = binding.homeItemsFragmentGotoCartButton;
        gridViewItems = binding.itemsGridView;
        progressBar = binding.progressBar3;
        tabLayout = binding.tabLayout2;

        productAdapter = new ProductAdapter(requireContext(), dbHandler, vibrator, itemList, shopID);
        gridViewItems.setAdapter(productAdapter);

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
            shopState = getArguments().getString("shop_state");
            shopDistrict = getArguments().getString("shop_district");

            bundle.putBoolean("fromHomeRecommendationFragment", true);
            bundle.putString("shop_id", shopID);
            bundle.putString("shop_state", shopState);
            bundle.putString("shop_district", shopDistrict);
        }

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

        // Set up TabLayout listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                progressBar.setVisibility(View.VISIBLE);
                switch (tab.getPosition()) {
                    case 0:
                        fetchItems("fruits");
                        break;
                    case 1:
                        fetchItems("vegetables");
                        break;
                    case 2:
                        fetchItems("others");
                        break;
                    default:
                        itemList.clear();
                        productAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Optional: Handle unselected state if needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Optional: Handle reselected state if needed
            }
        });

        fetchItems("fruits");

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

    @SuppressLint("NotifyDataSetChanged")
    private void updateGridView(JsonArray itemData, ProgressBar progressBar) {
//        itemList.clear();
//        productAdapter.notifyDataSetChanged();
        Gson gson = new Gson();

        for (JsonElement element : itemData) {
            // Since each element has a nested object, get the first entry in the map
            JsonObject mainObject = element.getAsJsonObject();
            if (!mainObject.entrySet().isEmpty()) {
                JsonElement nestedElement = mainObject.entrySet().iterator().next().getValue();
                ProductModel product = gson.fromJson(nestedElement, ProductModel.class);
                itemList.add(product);
            }
        }

        productAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
    }


    private void fetchItems(String itemType) {
        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call = apiService.getItems(itemType, shopID, shopState, shopDistrict);

        itemList.clear();
        productAdapter.notifyDataSetChanged();

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonArray itemData = response.body().getAsJsonArray("item_data");
                    Log.d("itemData", itemData.toString());
                    updateGridView(itemData, progressBar);
                } else {
                    Toast.makeText(requireContext(), "Failed to load items", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}

