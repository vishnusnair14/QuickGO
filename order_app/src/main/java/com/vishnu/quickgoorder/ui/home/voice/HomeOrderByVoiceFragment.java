package com.vishnu.quickgoorder.ui.home.voice;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.databinding.FragmentHomeOrderByVoiceBinding;
import com.vishnu.quickgoorder.miscellaneous.PreferenceKeys;
import com.vishnu.quickgoorder.miscellaneous.SoundManager;
import com.vishnu.quickgoorder.miscellaneous.Utils;
import com.vishnu.quickgoorder.server.sapi.APIService;
import com.vishnu.quickgoorder.server.sapi.ApiServiceGenerator;
import com.vishnu.quickgoorder.ui.home.recommendation.orders.AllOrdersAdapter;
import com.vishnu.quickgoorder.ui.home.recommendation.orders.AllOrdersModel;
import com.vishnu.quickgoorder.ui.home.voice.address.SavedAddressAdapter;
import com.vishnu.quickgoorder.ui.home.voice.address.SavedAddressModel;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class HomeOrderByVoiceFragment extends Fragment {

    private static final String LOG_TAG = "HomeOrderByVoiceFragment";
    private FragmentHomeOrderByVoiceBinding binding;
    private static MediaRecorder mediaRecorder;
    private boolean isButtonHeld = false;
    TextView recordingStatusTV, pressHoldTV, recordVoiceTimerTV;
    private boolean isRunning = false;
    private long startTime = 0;
    File AppAudioDir;
    private final Handler handler = new Handler();
    TextView pressAndRecMainTV;
    private FirebaseUser user;
    private FirebaseFirestore db;
    static String audioFileName = "_voice.mp3";
    private SharedPreferences preferences;
    private BottomSheetDialog setDeliveryAddrBtmView;
    Button enableLocationBtn;
    CollectionReference placedOrderDataRef;
    Button setDeliveryToCurrentLocBtn;
    TextView savedAddressStatusTV;
    FloatingActionButton trackOrderFab;
    List<AllOrdersModel> placedOrdersList;
    ImageButton recordBtn;
    private List<SavedAddressModel> savedAddressList;
    private SavedAddressAdapter savedAddressAdapter;
    private Chronometer chronometer;
    private BottomSheetDialog addressNotSelectedView;
    private Runnable checkConditionRunnable;
    private BottomSheetDialog selectOrderToTrackBtmView;
    private AllOrdersAdapter allOrdersAdapter;

    public HomeOrderByVoiceFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        placedOrderDataRef = db.collection("Users")
                .document(user.getUid()).collection("placedOrderData");

        preferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SoundManager.initialize(requireContext());

        File externalFilesDir = requireContext().getExternalFilesDir(Context.AUDIO_SERVICE);

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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentHomeOrderByVoiceBinding.inflate(inflater, container, false);
        ViewGroup root = binding.getRoot();

        recordBtn = binding.recordVoiceImageButton;
        pressAndRecMainTV = binding.tapAndRecordMainTextView;
        recordingStatusTV = binding.recordingStatusTextView;
        chronometer = binding.chronometer29;
        trackOrderFab = binding.trackOrder1FloatingActionButton;
        ImageView recIcon = binding.micIconImageView;

        Animation blinkAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.blink);
        recIcon.setVisibility(View.INVISIBLE);

        Typeface typeface = ResourcesCompat.getFont(requireContext(), R.font.archivo_black);
        chronometer.setTypeface(typeface);

        binding.selectedAddressTypeViewTextView.setText(preferences.getString(
                PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_TYPE, "Select an address"));
        binding.selectedFullAddressViewTextView.setText(preferences.getString(
                PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_FULL_ADDRESS, "Tap on a delivery address to make it as default"));

        // Define the Runnable
        checkConditionRunnable = () -> {
            if (preferences.getString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_KEY, "None").equals("None")) {
                showAddressNotSelectedBtmView(root);
            }
        };

        // Add a real-time listener for all-orders button
        placedOrderDataRef.addSnapshotListener((value, e) -> {
            if (e != null) {
                Log.e(LOG_TAG, "Error fetching orders: " + e.getMessage());
                return;
            }

            if (value != null && !value.isEmpty()) {
                trackOrderFab.setVisibility(View.VISIBLE);
                trackOrderFab.setOnClickListener(v ->
                        showOrderToTrackBtmView(root));
            } else {
                trackOrderFab.setVisibility(View.GONE);
                Log.d(LOG_TAG, "No orders in placedOrder bucket");
            }
        });

        recordBtn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    isButtonHeld = true;
                    recIcon.setVisibility(View.VISIBLE);
                    recIcon.setAnimation(blinkAnimation);

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
                        performOnButtonRelease();

                    }

                    return true;
                }
            }
            return false;
        });

        if (preferences.getString(PreferenceKeys.HOME_ORDER_BY_VOICE_FRAGMENT_AUDIO_REF_ID, "0").equals("0")) {
            preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_FRAGMENT_AUDIO_REF_ID, Utils.generateAudioRefID()).apply();
        } else {
            Log.d(LOG_TAG, PreferenceKeys.HOME_ORDER_BY_VOICE_FRAGMENT_AUDIO_REF_ID + ": already exists");
        }

        if (preferences.getString(PreferenceKeys.HOME_ORDER_BY_VOICE_FRAGMENT_ORDER_ID, "0").equals("0")) {
            preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_FRAGMENT_ORDER_ID, Utils.generateOrderID()).apply();
        } else {
            Log.d(LOG_TAG, PreferenceKeys.HOME_ORDER_BY_VOICE_FRAGMENT_ORDER_ID + ": already exists");
        }

        binding.homeOrderByVoiceGotoCartButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putBoolean("fromHomeOrderByVoiceFragment", true);
            bundle.putString("shop_id", "None");
            bundle.putString("shop_district", "None");
            NavHostFragment.findNavController(this).navigate(R.id.action_nav_orderbyvoice_to_nav_mcart, bundle);
            Utils.vibrate(requireContext(), 50, 2);
        });

        binding.selectedAddresViewCardView.setOnClickListener(v -> showSetDeliveryAddressBtmView(root));

        return root;
    }

    private void showOrderToTrackBtmView(ViewGroup root) {
        View orderView = LayoutInflater.from(requireContext()).inflate(
                R.layout.bottomview_select_order_to_track, root, false);

        selectOrderToTrackBtmView = new BottomSheetDialog(requireContext());
        selectOrderToTrackBtmView.setContentView(orderView);
        Objects.requireNonNull(selectOrderToTrackBtmView.getWindow()).setGravity(Gravity.TOP);

        RecyclerView allOrdersRecycleView = orderView.findViewById(R.id.selectOrderToTrack_recycleView);
        ProgressBar progressBar = orderView.findViewById(R.id.allPlacedOrder_progressBar);
        ViewGroup.LayoutParams params = allOrdersRecycleView.getLayoutParams();

        if (selectOrderToTrackBtmView != null && !selectOrderToTrackBtmView.isShowing()) {
            selectOrderToTrackBtmView.show();
            progressBar.setVisibility(View.VISIBLE);
            syncAllOrdersRecycleView(allOrdersRecycleView, progressBar);
        }
    }

    private JsonArray loadAddressDataFromFile() {
        try {
            File file = new File(requireContext().getFilesDir(), "address_data.json");
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                JsonElement jsonElement = JsonParser.parseReader(reader);
                reader.close();
                if (jsonElement.isJsonArray()) {
                    return jsonElement.getAsJsonArray();
                }
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error loading address data from file", e);
        }
        return new JsonArray();
    }

    private void getSavedAddressData(String userId, ProgressBar progressBar) {
        JsonArray savedData = loadAddressDataFromFile();

        if (!savedData.isEmpty()) {
            updateRecyclerView(savedData, progressBar);
        } else {
            fetchAddressDataFromServer(userId, progressBar);
        }
    }

    private void updatePlacedOrdersList(AllOrdersModel updatedData) {
        String orderId = updatedData.getOrder_id();
        boolean found = false;

        for (int i = 0; i < placedOrdersList.size(); i++) {
            if (placedOrdersList.get(i).getOrder_id().equals(orderId)) {
                // Update the existing item
                placedOrdersList.set(i, updatedData);
                allOrdersAdapter.notifyItemChanged(i);
                found = true;
                break;
            }
        }

        if (!found) {
            // Add new item if it doesn't exist
            placedOrdersList.add(updatedData);
            allOrdersAdapter.notifyItemInserted(placedOrdersList.size() - 1);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void syncAllOrdersRecycleView(RecyclerView allOrdersRecycleView, ProgressBar progressBar) {

        placedOrdersList = new ArrayList<>();
        allOrdersAdapter = new AllOrdersAdapter(requireActivity(), preferences, placedOrdersList);

        allOrdersRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));
        allOrdersRecycleView.setAdapter(allOrdersAdapter);

        placedOrderDataRef.addSnapshotListener((value, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                return;
            }

            assert value != null;
            Log.d(TAG, "VALUES: " + value.getDocuments());

            for (QueryDocumentSnapshot doc : value) {
                if (doc.exists()) {
                    Log.d(LOG_TAG, "All orders data: " + doc.getData());

                    String orderType = doc.getString("order_type");
                    DocumentReference orderDataRef;

                    orderDataRef = doc.getDocumentReference("order_data_payload_reference");

                    if (orderDataRef != null) {
                        // Add a snapshot listener to the orderDataRef
                        orderDataRef.addSnapshotListener((additionalDataDoc, innerException) -> {
                            if (innerException != null) {
                                Log.e(LOG_TAG, "Error listening to order data changes", innerException);
                                return;
                            }

                            if (additionalDataDoc != null && additionalDataDoc.exists()) {
                                Log.d(LOG_TAG, "Order data updated: " + additionalDataDoc.getData());

                                AllOrdersModel updatedData = additionalDataDoc.toObject(AllOrdersModel.class);
                                if (updatedData != null) {
                                    updatePlacedOrdersList(updatedData);
                                }
                            } else {
                                Log.w(LOG_TAG, "No order data found for order: " + doc.getId());
                            }

                            progressBar.setVisibility(View.GONE);
                        });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Log.e(LOG_TAG, "orderDataRef is null");
                    }
                }
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateRecyclerView(JsonArray addressData, ProgressBar progressBar) {
        savedAddressList.clear();
        for (JsonElement element : addressData) {
            SavedAddressModel book = new Gson().fromJson(element, SavedAddressModel.class);
            savedAddressList.add(book);
        }
        savedAddressAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
    }

    private void fetchAddressDataFromServer(String userId, ProgressBar progressBar) {
        progressBar.setVisibility(View.VISIBLE);

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call = apiService.getSavedAddresses(userId);

        call.enqueue(new Callback<>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();

                    if (responseBody.has("address_data")) {
                        JsonArray addressData = responseBody.getAsJsonArray("address_data");

                        saveAddressDataToFile(addressData);
                        updateRecyclerView(addressData, progressBar);

                        if (isAdded()) {
//                            Toast.makeText(getContext(), "Address data retrieved successfully", Toast.LENGTH_SHORT).show();
                            savedAddressStatusTV.setVisibility(View.GONE);
                            Log.d(LOG_TAG, "Address data retrieved successfully");
                        }
                    } else {
                        Log.e(LOG_TAG, "Invalid response format: Missing 'address_data' field");
                        savedAddressStatusTV.setVisibility(View.VISIBLE);
                        savedAddressStatusTV.setText("\nUnable to fetch address data, invalid format");
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to fetch address data: Invalid response format", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    String errorMessage = "Failed to fetch address data";
                    errorMessage += ": " + response.message();
                    Log.e(LOG_TAG, errorMessage);
                    savedAddressStatusTV.setVisibility(View.VISIBLE);
                    savedAddressStatusTV.setText("\nFailed to fetch address data");
                    if (isAdded()) {
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to fetch address data", Toast.LENGTH_SHORT).show();
                }
                Log.e(LOG_TAG, "Failed to fetch address data", t);
            }
        });
    }

    private void saveAddressDataToFile(JsonArray addressData) {
        try {
            File file = new File(requireContext().getFilesDir(), "address_data.json");
            FileWriter writer = new FileWriter(file);
            writer.write(addressData.toString());
            writer.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error saving address data to file", e);
        }
    }

    private void showAddressNotSelectedBtmView(View root) {
        if (addressNotSelectedView == null) {
            View savedStorePrefView = LayoutInflater.from(requireContext()).inflate(
                    R.layout.bottomview_delivery_address_not_selected, null, false);

            addressNotSelectedView = new BottomSheetDialog(requireContext());
            addressNotSelectedView.setContentView(savedStorePrefView);
            addressNotSelectedView.setDismissWithAnimation(true);
            Objects.requireNonNull(addressNotSelectedView.getWindow()).setGravity(Gravity.TOP);

            addressNotSelectedView.setOnDismissListener(dialog ->
                    handler.postDelayed(checkConditionRunnable, 5000));

            Button selectAddressBtn = savedStorePrefView.findViewById(R.id.btmviewDeliveryAddressNotSelectedSelectAdressBtn_button);
//            Button cancelBtn = savedStorePrefView.findViewById(R.id.btmviewStorePrefDataNotSavedCancelBtn_button);

//            cancelBtn.setOnClickListener(v -> {
//                addressNotSelectedView.hide();
//                addressNotSelectedView.dismiss();
//            });

            selectAddressBtn.setOnClickListener(v -> {
                addressNotSelectedView.dismiss();
                showSetDeliveryAddressBtmView(root);
            });
        }

        if (!addressNotSelectedView.isShowing()) {
            if (setDeliveryAddrBtmView == null) {
                if (!isButtonHeld) {
                    addressNotSelectedView.show();
                }
            } else if (!setDeliveryAddrBtmView.isShowing()) {
                if (!isButtonHeld) {
                    addressNotSelectedView.show();
                }
            } else {
                Toast.makeText(requireContext(), "Select any address.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void showSetDeliveryAddressBtmView(View root) {
        View setDefAddrView = LayoutInflater.from(requireContext()).inflate(
                R.layout.bottomview_set_default_delivery_address, (ViewGroup) root, false);

        setDeliveryAddrBtmView = new BottomSheetDialog(requireContext());
        setDeliveryAddrBtmView.setContentView(setDefAddrView);
        Objects.requireNonNull(setDeliveryAddrBtmView.getWindow()).setGravity(Gravity.TOP);

        RecyclerView recyclerView = setDefAddrView.findViewById(R.id.setDefaultDeliveryAddress_recycleView);
        enableLocationBtn = setDefAddrView.findViewById(R.id.enableDeviceLocation_button);
        setDeliveryToCurrentLocBtn = setDefAddrView.findViewById(R.id.setDeliveryToCurrentLoc_button);
        CardView enableLocView = setDefAddrView.findViewById(R.id.enableLocation_cardView);
        CardView setDelToCrntLoc = setDefAddrView.findViewById(R.id.setDeliveryToCurrentLoc_cardView);
//        ConstraintLayout csl = setDefAddrView.findViewById(R.id.linearLayout2);
        savedAddressStatusTV = setDefAddrView.findViewById(R.id.savedAddressRecycleViewStatusTV_textView);
        ProgressBar progressBar = setDefAddrView.findViewById(R.id.selectAddressForDelivery_progressBar);
        TextView addNewDeliveryAddressTV = setDefAddrView.findViewById(R.id.setDefaultDeliveryAddressAddNewAddress_textView);

        if (Utils.isLocationNotEnabled(requireContext())) {
            enableLocView.setVisibility(View.VISIBLE);
            setDelToCrntLoc.setVisibility(View.GONE);

        } else {
            enableLocView.setVisibility(View.GONE);
            setDelToCrntLoc.setVisibility(View.VISIBLE);
        }

        savedAddressList = new ArrayList<>();
        savedAddressAdapter = new SavedAddressAdapter(binding, preferences,
                savedAddressList, setDeliveryAddrBtmView);
        recyclerView.setAdapter(savedAddressAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        enableLocationBtn.setOnClickListener(v -> {
            setDeliveryAddrBtmView.hide();
            Utils.showLocationSettings(requireContext());
        });

        setDeliveryToCurrentLocBtn.setOnClickListener(v -> {
            preferences.edit().putBoolean(PreferenceKeys.IS_SET_TO_CURRENT_LOCATION, true).apply();
            //TODO put latLng
            preferences.edit().putString("currentLocLat", String.valueOf("12.24")).apply();
            preferences.edit().putString("currentLocLon", String.valueOf("75.1212")).apply();
        });

        addNewDeliveryAddressTV.setOnClickListener(v -> {
            setDeliveryAddrBtmView.dismiss();
            new Handler().postDelayed(() -> NavHostFragment.findNavController(this)
                    .navigate(R.id.action_nav_orderbyvoice_to_nav_addAddress), 350);

        });

        getSavedAddressData(user.getUid(), progressBar);

        if (setDeliveryAddrBtmView != null && !setDeliveryAddrBtmView.isShowing()) {
            setDeliveryAddrBtmView.show();
        }
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
                SoundManager.playOnButtonRelease();
            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
                Toast.makeText(context, "error!", Toast.LENGTH_SHORT).show();
            }
        }
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


    private void performOnButtonHold() {
        startChronometer();
        pressAndRecMainTV.setText(R.string.recording);
        pressAndRecMainTV.setTextColor(requireActivity().getColor(R.color.recording));
        recordingStatusTV.setText(R.string.recording);
        startRecording(AppAudioDir);
        Log.d(TAG, "testButtonUI: onButtonHold");
    }

    private void addVoiceOrderToDB(String key, Context context, String downloadUrl,
                                   String voiceDocID, String voiceOrderID) {

        DocumentReference orderByVoiceDataRef = db.collection("Users")
                .document(user.getUid()).collection("voiceOrdersData")
                .document("obv").collection(voiceDocID)
                .document(voiceOrderID).collection("voiceData").document(key);


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
                                Utils.deleteVoiceOrderCacheFile(context, voiceDocID, null);
                                Log.d(LOG_TAG, "Audio url updated to db: success");
                            }
                    ).addOnFailureListener(e ->
                            Toast.makeText(context, "url server update failed!", Toast.LENGTH_SHORT).show());
                    Log.d(LOG_TAG, "Audio url update to db: failed!");
                } else {
                    orderByVoiceDataRef.set(voiceOrderFields).addOnSuccessListener(var -> {
                                Utils.deleteVoiceOrderCacheFile(context, voiceDocID, null);
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
        String orderID = preferences.getString(PreferenceKeys.HOME_ORDER_BY_VOICE_FRAGMENT_ORDER_ID, "0");
        String audioRefID = preferences.getString(PreferenceKeys.HOME_ORDER_BY_VOICE_FRAGMENT_AUDIO_REF_ID, "0");

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

    private void performOnButtonRelease() {
        stopRecording(requireContext());
        recordBtn.setEnabled(false);
        recordingStatusTV.setText(R.string.uploading_to_db);

        // uploads order-voice audio file to storage
        uploadAudioToStorageRec(requireContext(), String.valueOf(AppAudioDir));
    }

    private final Runnable onButtonHoldRunnable = () -> {
        if (isButtonHeld) {
            SoundManager.playOnButtonHold();
            performOnButtonHold();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        handler.post(checkConditionRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(checkConditionRunnable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        SoundManager.release();
    }
}