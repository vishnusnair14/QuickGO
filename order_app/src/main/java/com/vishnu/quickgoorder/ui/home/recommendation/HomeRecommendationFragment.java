package com.vishnu.quickgoorder.ui.home.recommendation;


import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.core.view.GestureDetectorCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.databinding.FragmentHomeRecommendationBinding;
import com.vishnu.quickgoorder.eventbus.EBDeliveryAddressData;
import com.vishnu.quickgoorder.eventbus.EBSyncEmptyShopData;
import com.vishnu.quickgoorder.miscellaneous.PreferenceKeys;
import com.vishnu.quickgoorder.miscellaneous.Utils;
import com.vishnu.quickgoorder.server.sapi.APIService;
import com.vishnu.quickgoorder.server.sapi.ApiServiceGenerator;
import com.vishnu.quickgoorder.service.LocationService;
import com.vishnu.quickgoorder.ui.home.recommendation.address.SavedAddressAdapter;
import com.vishnu.quickgoorder.ui.home.recommendation.address.SavedAddressModel;
import com.vishnu.quickgoorder.ui.home.recommendation.orders.AllOrdersAdapter;
import com.vishnu.quickgoorder.ui.home.recommendation.orders.AllOrdersModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeRecommendationFragment extends Fragment {
    private final String LOG_TAG = "HomeRecommendationFragment";
    FirebaseAuth mAuth;
    TextView recommendedForYouTV;
    TextView serverStatusFeedbackTV;
    ProgressBar shopsRecycleViewPB;
    SharedPreferences preferences;
    SharedPreferences settingsPreferences;
    Button enableLocationBtn;
    Button setDeliveryToCurrentLocBtn;
    ViewGroup root;
    TextView noAddrFndBnr;
    private List<SavedAddressModel> savedAddressList;
    private double latitude;
    private double longitude;
    private GestureDetectorCompat gestureDetector;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##########");
    private SavedAddressAdapter savedAddressAdapter;
    private AllOrdersAdapter allOrdersAdapter;
    CollectionReference placedOrderDataRef;
    FloatingActionButton trackOrderFab;
    FirebaseUser user;
    TextView locationTV;
    List<AllOrdersModel> placedOrdersList;
    private com.vishnu.quickgoorder.databinding.FragmentHomeRecommendationBinding binding;
    private BottomSheetDialog setDeliveryAddrBtmView;
    private BottomSheetDialog selectOrderToTrackBtmView;
    private static final int INITIAL_RETRY_DELAY_MS = 2000;
    private static final int MAX_RETRY_COUNT = 5;
    private ImageView addressRefreshBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        placedOrderDataRef = db.collection("Users")
                .document(user.getUid()).collection("placedOrderData");

        preferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        settingsPreferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentHomeRecommendationBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        gestureDetector = new GestureDetectorCompat(requireContext(), new BottomSheetGestureListener());

        root.setOnTouchListener(this::onTouch);

        serverStatusFeedbackTV = binding.serverStatusFeedbackTextView;
        recommendedForYouTV = binding.recommendedForYouTextView;
        shopsRecycleViewPB = binding.serverStatusProgressBar;
        trackOrderFab = binding.trackOrder2FloatingActionButton;
        locationTV = binding.locationViewTextView;
//        nearbyShopCardView = binding.recommendedShopsCardView;
        TabLayout tabLayout = binding.orderModeTabLayout;
//        recordVoiceOrderCardView = binding.recordVoiceOrderCardView;

        Objects.requireNonNull(tabLayout.getTabAt(settingsPreferences.getInt("orderModeSelectedTabIndex", 0))).select();

        binding.selectedAddresViewCardView.setOnClickListener(v -> showSetDeliveryAddressBtmView());

        binding.selectedAddressTypeViewTextView.setText(preferences.getString(
                PreferenceKeys.HOME_RECOMMENDATION_SELECTED_ADDRESS_TYPE, "Select an address"));
        binding.selectedFullAddressViewTextView.setText(preferences.getString(
                PreferenceKeys.HOME_RECOMMENDATION_SELECTED_ADDRESS_FULL_ADDRESS, ""));


        // Add a real-time listener for all-orders button
        placedOrderDataRef.addSnapshotListener((value, e) -> {
            if (e != null) {
                Log.e(LOG_TAG, "Error fetching orders: " + e.getMessage());
                return;
            }

            if (value != null && !value.isEmpty()) {
                trackOrderFab.setVisibility(View.VISIBLE);
                trackOrderFab.setOnClickListener(v ->
                        showOrderToTrackBtmView());
            } else {
                trackOrderFab.setVisibility(View.GONE);
                Log.d(LOG_TAG, "No orders in placedOrder bucket");
            }
        });

        if (preferences.getString(PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_AUDIO_REF_ID, "0").equals("0")) {
            preferences.edit().putString(PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_AUDIO_REF_ID, Utils.generateAudioRefID()).apply();
            Toast.makeText(requireContext(), preferences.getString(
                    PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_AUDIO_REF_ID, "0"), Toast.LENGTH_SHORT).show();
        }

        if (preferences.getString(PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_ORDER_ID, "0").equals("0")) {
            preferences.edit().putString(PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_ORDER_ID, Utils.generateOrderID()).apply();
        } else {
            Log.d(LOG_TAG, PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_ORDER_ID + ": already exists");
        }


        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Handle tab selection
                int position = tab.getPosition();
                // Access the data or perform actions based on selected tab
                if (position == 0) {
                    settingsPreferences.edit().putInt("defaultHomeView", 0).apply();
                    settingsPreferences.edit().putInt("orderModeSelectedTabIndex", 0).apply();
                    Toast.makeText(requireContext(), "Now you have switched to, order by voice feature.", Toast.LENGTH_SHORT).show();
                } else if (position == 1) {
                    settingsPreferences.edit().putInt("defaultHomeView", 1).apply();
                    settingsPreferences.edit().putInt("orderModeSelectedTabIndex", 1).apply();
                    Toast.makeText(requireContext(), "Now you can order from recommended shops.", Toast.LENGTH_SHORT).show();
                }
//                } else if (position == 2) {
//                    settingsPreferences.edit().putInt("defaultHomeView", 2).apply();
//                    settingsPreferences.edit().putInt("orderModeSelectedTabIndex", 2).apply();
//                    Toast.makeText(requireContext(), "Now you have enabled, store preference feature.", Toast.LENGTH_SHORT).show();
//                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        return root;
    }


    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocationService.ACTION_LOCATION_BROADCAST.equals(intent.getAction())) {
                latitude = intent.getDoubleExtra(LocationService.EXTRA_LATITUDE, 0.00);
                longitude = intent.getDoubleExtra(LocationService.EXTRA_LONGITUDE, 0.00);
                if (locationTV.getVisibility() == View.GONE) {
                    locationTV.setVisibility(View.VISIBLE);
                }
                locationTV.setText(MessageFormat.format("{0}°N\n{1}°E",
                        decimalFormat.format(latitude), decimalFormat.format(longitude)));
            }
        }
    };


    private boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private class BottomSheetGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 50;

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            assert e1 != null;
            float deltaY = e2.getY() - e1.getY();
            if (Math.abs(deltaY) > SWIPE_THRESHOLD) {
                showSetDeliveryAddressBtmView();
                return true;
            }
            return false;
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


    private void showOrderToTrackBtmView() {
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


    private void getSavedAddressData(String userId, ProgressBar progressBar) {
        JsonArray savedData = loadAddressDataFromFile();

        if (!savedData.isEmpty()) {
            updateRecyclerView(savedData, progressBar);
        } else {
            fetchAddressDataFromServer(userId, progressBar);
        }
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
                            Log.d(LOG_TAG, "Address data retrieved successfully");
                        }
                        addressRefreshBtn.clearAnimation();
                    } else {
                        Log.e(LOG_TAG, "Invalid response format: Missing 'address_data' field");
                        addressRefreshBtn.clearAnimation();
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to fetch address data: Invalid response format", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    String errorMessage = "Failed to fetch address data";
                    errorMessage += ": " + response.message();
                    Log.e(LOG_TAG, errorMessage);
                    if (isAdded()) {
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                    addressRefreshBtn.clearAnimation();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to fetch address data", Toast.LENGTH_SHORT).show();
                }
                addressRefreshBtn.clearAnimation();
                Log.e(LOG_TAG, "Failed to fetch address data", t);
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void showSetDeliveryAddressBtmView() {
        View setDefAddrView = LayoutInflater.from(requireContext()).inflate(
                R.layout.bottomview_set_default_delivery_address, null, false);

        setDeliveryAddrBtmView = new BottomSheetDialog(requireContext());
        setDeliveryAddrBtmView.setContentView(setDefAddrView);
        Objects.requireNonNull(setDeliveryAddrBtmView.getWindow()).setGravity(Gravity.TOP);

        RecyclerView recyclerView = setDefAddrView.findViewById(R.id.setDefaultDeliveryAddress_recycleView);
        enableLocationBtn = setDefAddrView.findViewById(R.id.enableDeviceLocation_button);
        setDeliveryToCurrentLocBtn = setDefAddrView.findViewById(R.id.setDeliveryToCurrentLoc_button);
        CardView enableLocView = setDefAddrView.findViewById(R.id.enableLocation_cardView);
        CardView setDelToCrntLoc = setDefAddrView.findViewById(R.id.setDeliveryToCurrentLoc_cardView);
//        ConstraintLayout csl = setDefAddrView.findViewById(R.id.linearLayout2);
        noAddrFndBnr = setDefAddrView.findViewById(R.id.savedAddressRecycleViewStatusTV_textView);
        ProgressBar progressBar = setDefAddrView.findViewById(R.id.selectAddressForDelivery_progressBar);
        TextView addNewDeliveryAddressTV = setDefAddrView.findViewById(R.id.setDefaultDeliveryAddressAddNewAddress_textView);
        addressRefreshBtn = setDefAddrView.findViewById(R.id.addressRefresh_imageView);

        Animation rotateAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate);

        if (isLocationNotEnabled(requireContext())) {
            enableLocView.setVisibility(View.VISIBLE);
            setDelToCrntLoc.setVisibility(View.GONE);

        } else {
            enableLocView.setVisibility(View.GONE);
            setDelToCrntLoc.setVisibility(View.VISIBLE);
        }

        savedAddressList = new ArrayList<>();
        savedAddressAdapter = new SavedAddressAdapter(binding, preferences, settingsPreferences,
                savedAddressList, setDeliveryAddrBtmView, this);
        recyclerView.setAdapter(savedAddressAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        enableLocationBtn.setOnClickListener(v -> {
            setDeliveryAddrBtmView.hide();
            showLocationSettings(requireContext());
        });

        setDeliveryToCurrentLocBtn.setOnClickListener(v -> {
            EventBus.getDefault().post(new EBSyncEmptyShopData("{'recommended_shop_data':[]}", 1274));

            //TODO: update auto detect pincode
            getRecommendedShopsData(binding, latitude, longitude, "None", "None", "None");
            preferences.edit().putBoolean(PreferenceKeys.IS_SET_TO_CURRENT_LOCATION, true).apply();
            preferences.edit().putString("currentLocLat", String.valueOf(latitude)).apply();
            preferences.edit().putString("currentLocLon", String.valueOf(longitude)).apply();
        });


        getSavedAddressData(user.getUid(), progressBar);

        addNewDeliveryAddressTV.setOnClickListener(v -> {
            setDeliveryAddrBtmView.dismiss();
            new Handler().postDelayed(() -> {
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_nav_recommendation_to_nav_addAddress);
            }, 350);
        });

        addressRefreshBtn.setOnClickListener(v -> {
            addressRefreshBtn.startAnimation(rotateAnimation);
            savedAddressList.clear();
            savedAddressAdapter.notifyDataSetChanged();
            fetchAddressDataFromServer(user.getUid(), progressBar);
        });

        if (setDeliveryAddrBtmView != null && !setDeliveryAddrBtmView.isShowing()) {
            setDeliveryAddrBtmView.show();
        }
    }


    private void getRecommendedShopsData(FragmentHomeRecommendationBinding binding,
                                         double la, double lo, String user_state,
                                         String user_district, String user_pincode) {

        shopsRecycleViewPB.setVisibility(View.VISIBLE);
        serverStatusFeedbackTV.setVisibility(View.VISIBLE);

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call3499 = apiService.getShopRecommendations(la, lo, user_state, user_district, user_pincode);

        Log.i(LOG_TAG, "Initiating shop recommendation request...");
        attemptFetch(call3499, binding, la, lo, user_state, user_district, user_pincode, 0, INITIAL_RETRY_DELAY_MS);
    }

    private void attemptFetch(Call<JsonObject> call, FragmentHomeRecommendationBinding binding,
                              double la, double lo, String user_state, String user_district, String user_pincode,
                              int attempt, int delayMs) {

        Log.i(LOG_TAG, "Attempt #" + (attempt + 1) + " to fetch recommended shops.");
        updateServerStatusFeedback(attempt);

        call.clone().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call34, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        Log.i(LOG_TAG, "Shop recommendation request successful on attempt #" + (attempt + 1));
                        handleSuccessfulResponse(response.body(), binding);
                    }
                } else {
                    Log.w(LOG_TAG, "Shop recommendation request failed (HTTP " + response.code() + "). Retrying...");
                    retryWithBackoff(call, binding, la, lo, user_state, user_district, user_pincode, attempt + 1, delayMs);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(LOG_TAG, "Request to fetch recommended shops failed due to network error: " + t.getMessage());
                retryWithBackoff(call, binding, la, lo, user_state, user_district, user_pincode, attempt + 1, delayMs);
            }
        });
    }

    private void retryWithBackoff(Call<JsonObject> call, FragmentHomeRecommendationBinding binding,
                                  double la, double lo, String user_state, String user_district, String user_pincode,
                                  int attempt, int delayMs) {

        if (attempt < MAX_RETRY_COUNT) {
            Log.i(LOG_TAG, "Retrying in " + delayMs + "ms (attempt #" + (attempt + 1) + ")");
            new Handler().postDelayed(() -> {
                int newDelayMs = delayMs * 2; // Exponential backoff
                attemptFetch(call, binding, la, lo, user_state, user_district, user_pincode, attempt, newDelayMs);
            }, delayMs);
        } else {
            Log.e(LOG_TAG, "Maximum retry attempts reached. Server might be offline.");
            handleMaxRetryExceeded();
        }
    }

    private void updateServerStatusFeedback(int attempt) {
        Log.i(LOG_TAG, "Updating server status feedback. Attempt #" + (attempt + 1));
        if (attempt == 0) {
            serverStatusFeedbackTV.setTextColor(getResources().getColor(R.color.serverStatusFeedbackConnectingColor, null));
            serverStatusFeedbackTV.setText(R.string.finding_perfects_shops);
        } else if (attempt < MAX_RETRY_COUNT) {
            serverStatusFeedbackTV.setTextColor(getResources().getColor(R.color.serverStatusFeedbackOfflineColor, null));
            serverStatusFeedbackTV.setText(R.string.retrying_connection);
        }
    }

    private void handleSuccessfulResponse(JsonObject shopData, FragmentHomeRecommendationBinding binding) {
        Log.i(LOG_TAG, "Handling successful shop recommendation response.");
        shopsRecycleViewPB.setVisibility(View.GONE);
        serverStatusFeedbackTV.setVisibility(View.GONE);

        try {
            if (!isRecommendedShopDataEmpty(new JSONObject(shopData.toString()))) {
                Log.i(LOG_TAG, "Shop data received and not empty. Updating UI and storing data.");
                writeShopDataToFile(requireContext(), String.valueOf(shopData));
                syncRecommendedShopDataRecycleView(binding, String.valueOf(shopData));
                preferences.edit().putString(PreferenceKeys.HOME_RECOMMENDATION_CURRENT_SHOP_PINCODE, shopData.get("shop_pincode").getAsString()).apply();
            } else {
                Log.w(LOG_TAG, "No nearby shops found.");
                handleNoShopsFound();
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error parsing shop data: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void handleMaxRetryExceeded() {
        Log.e(LOG_TAG, "Max retry attempts exceeded. Updating UI to indicate server is offline.");
        shopsRecycleViewPB.setVisibility(View.GONE);
        serverStatusFeedbackTV.setTextColor(getResources().getColor(R.color.serverStatusFeedbackTryAgainColor, null));
        serverStatusFeedbackTV.setText(R.string.server_offline1);
    }

    private void handleNoShopsFound() {
        Log.i(LOG_TAG, "No shops found within the specified criteria. Updating UI.");
        deleteShopDataFile(requireContext());
        shopsRecycleViewPB.setVisibility(View.GONE);
        serverStatusFeedbackTV.setTextColor(getResources().getColor(R.color.serverStatusFeedbackNoShopsFoundColor, null));
        serverStatusFeedbackTV.setText(R.string.no_nearby_shops_found);

        if (setDeliveryAddrBtmView != null) {
            setDeliveryAddrBtmView.hide();
            setDeliveryAddrBtmView.dismiss();
            showSetDeliveryAddressBtmView();
        }
    }


    public static void deleteShopDataFile(@NonNull Context context) {
        String filename = "recommended_shops.json";
        if (context.deleteFile(filename)) {
            Log.d("LOG_TAG", "File deleted successfully.");
        } else {
            Log.d("LOG_TAG", "File does not exist or could not be deleted.");
        }
    }


    public static void writeShopDataToFile(@NonNull Context context, String jsonData) {
        String filename = "recommended_shops.json";
        try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
            fos.write(jsonData.getBytes());
        } catch (IOException e) {
            Log.e("LOG_TAG", e.toString());
        }
    }

    public String readShopDataFromFile(Context context) {
        String filename = "recommended_shops.json";
        try (FileInputStream fis = context.openFileInput(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.toString());
            return null;
        }
    }

    private boolean isRecommendedShopDataEmpty(JSONObject jsonObject) {
        JSONArray recommendedShopData = jsonObject.optJSONArray("recommended_shop_data");
        return recommendedShopData == null || recommendedShopData.length() == 0;
    }


    @SuppressLint("NotifyDataSetChanged")
    public void syncRecommendedShopDataRecycleView(@NonNull FragmentHomeRecommendationBinding
                                                           binding, String jsonData) {
        // Parse JSON string to ShopDataWrapper object
        ShopDataWrapper shopDataWrapper = new Gson().fromJson(jsonData, ShopDataWrapper.class);

        // Get the list of recommended shop data from the wrapper
        List<HomeRecommendationModel> shopList = shopDataWrapper != null ? shopDataWrapper.getRecommendedShopData() : null;


        // Initialize RecyclerView
        RecyclerView recyclerView = binding.shopInfoFragmentRecycleView;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Create and set adapter
        if (shopList != null) {
            recommendedForYouTV.setVisibility(View.VISIBLE);
            if (setDeliveryAddrBtmView != null) {
                setDeliveryAddrBtmView.hide();
            }
            HomeRecommendationAdapter homeRecommendationAdapter = new HomeRecommendationAdapter(shopList, requireContext(), this);
            recyclerView.setAdapter(homeRecommendationAdapter);
        }
    }

    private static boolean isLocationNotEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Check if either GPS or network provider is enabled ...
        return locationManager == null ||
                (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                        !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private void showLocationSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
    }


    @Override
    public void onResume() {
        super.onResume();


    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(LocationService.ACTION_LOCATION_BROADCAST);
        requireContext().registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        EventBus.getDefault().register(this);

        String jsonData = readShopDataFromFile(requireContext());
        if (jsonData != null && jsonData.isEmpty()) {
            // Data not found, pop up to select address for recommendation
            Toast.makeText(requireContext(), "Select an address to get recommendations", Toast.LENGTH_SHORT).show();
            showSetDeliveryAddressBtmView();
        } else {
            syncRecommendedShopDataRecycleView(binding, jsonData);
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (setDeliveryAddrBtmView != null) {
            setDeliveryAddrBtmView.hide();
            setDeliveryAddrBtmView.dismiss();
        }
        if (selectOrderToTrackBtmView != null) {
            selectOrderToTrackBtmView.hide();
            selectOrderToTrackBtmView.dismiss();
        }

        requireContext().unregisterReceiver(locationReceiver);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (setDeliveryAddrBtmView != null) {
            setDeliveryAddrBtmView.hide();
            setDeliveryAddrBtmView.dismiss();
        }
        if (selectOrderToTrackBtmView != null) {
            selectOrderToTrackBtmView.hide();
            selectOrderToTrackBtmView.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (setDeliveryAddrBtmView != null) {
            setDeliveryAddrBtmView.hide();
            setDeliveryAddrBtmView.dismiss();
        }

        if (selectOrderToTrackBtmView != null) {
            selectOrderToTrackBtmView.hide();
            selectOrderToTrackBtmView.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (setDeliveryAddrBtmView != null) {
            setDeliveryAddrBtmView.hide();
            setDeliveryAddrBtmView.dismiss();
        }

        if (selectOrderToTrackBtmView != null) {
            selectOrderToTrackBtmView.hide();
            selectOrderToTrackBtmView.dismiss();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EBSyncEmptyShopData ev) {
        syncRecommendedShopDataRecycleView(binding, ev.jsonData);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EBDeliveryAddressData ad) {
        // Handle the event here
        preferences.edit().putString("selectedDeliveryAddressKey", ad.addr_ph_key).apply();
        preferences.edit().putString("selectedDeliveryAddressLat", Double.toString(ad.addr_lat)).apply();
        preferences.edit().putString("selectedDeliveryAddressLon", Double.toString(ad.addr_lon)).apply();

        getRecommendedShopsData(binding, ad.addr_lat, ad.addr_lon, ad.addr_state, ad.addr_district, ad.pincode);
        if (setDeliveryAddrBtmView != null && setDeliveryAddrBtmView.isShowing()) {
            setDeliveryAddrBtmView.hide();
        }
    }
}