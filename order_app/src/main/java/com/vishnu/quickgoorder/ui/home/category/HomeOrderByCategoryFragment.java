package com.vishnu.quickgoorder.ui.home.category;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.vishnu.quickgoorder.databinding.FragmentHomeOrderByCategoryBinding;
import com.vishnu.quickgoorder.eventbus.EBSyncEmptyShopData;
import com.vishnu.quickgoorder.miscellaneous.PreferenceKeys;
import com.vishnu.quickgoorder.miscellaneous.Utils;
import com.vishnu.quickgoorder.server.sapi.APIService;
import com.vishnu.quickgoorder.server.sapi.ApiServiceGenerator;
import com.vishnu.quickgoorder.service.LocationService;
import com.vishnu.quickgoorder.ui.home.category.address.SavedAddressAdapter;
import com.vishnu.quickgoorder.ui.home.category.address.SavedAddressModel;
import com.vishnu.quickgoorder.ui.home.recommendation.orders.AllOrdersAdapter;
import com.vishnu.quickgoorder.ui.home.recommendation.orders.AllOrdersModel;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeOrderByCategoryFragment extends Fragment {
    private final String LOG_TAG = "HomeOrderByCategoryFragment";
    private FragmentHomeOrderByCategoryBinding binding;
    SharedPreferences settingsPreferences;
    SharedPreferences preferences;
    private BottomSheetDialog setDeliveryAddrBtmView;
    Button enableLocationBtn;
    FirebaseAuth mAuth;
    private BottomSheetDialog selectOrderToTrackBtmView;
    private double latitude;
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##########");
    private double longitude;
    private AllOrdersAdapter allOrdersAdapter;
    FirebaseUser user;
    Button setDeliveryToCurrentLocBtn;
    CollectionReference placedOrderDataRef;
    TextView noAddrFndBnr;
    private ImageView addressRefreshBtn;
    private List<SavedAddressModel> savedAddressList;
    private SavedAddressAdapter savedAddressAdapter;
    TextView locationTV;
    List<AllOrdersModel> placedOrdersList;
    FloatingActionButton trackOrderFab;

    public HomeOrderByCategoryFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        preferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        settingsPreferences = requireActivity().getSharedPreferences("settings", Context.MODE_PRIVATE);

        placedOrderDataRef = db.collection("Users")
                .document(user.getUid()).collection("placedOrderData");

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentHomeOrderByCategoryBinding.inflate(inflater, container, false);

        ViewGroup root = binding.getRoot();

        TabLayout tabLayout = binding.orderModeTabLayout;
        trackOrderFab = binding.trackOrder1FloatingActionButton;
        locationTV = binding.locationViewTextView;
        Objects.requireNonNull(tabLayout.getTabAt(settingsPreferences.getInt("orderModeSelectedTabIndex", 0))).select();

        binding.selectedAddresViewCardView.setOnClickListener(v -> showSetDeliveryAddressBtmView());

        binding.selectedAddressTypeViewTextView.setText(preferences.getString(
                PreferenceKeys.HOME_RECOMMENDATION_SELECTED_ADDRESS_TYPE, "Select an address"));
        binding.selectedFullAddressViewTextView.setText(preferences.getString(
                PreferenceKeys.HOME_RECOMMENDATION_SELECTED_ADDRESS_FULL_ADDRESS, ""));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Handle tab selection
                int position = tab.getPosition();
                // Access the data or perform actions based on selected tab
                if (position == 0) {
                    settingsPreferences.edit().putInt("defaultHomeView", 0).apply();
                    settingsPreferences.edit().putInt("orderModeSelectedTabIndex", 0).apply();
                    Toast.makeText(requireContext(), "Now you have selected order by category.", Toast.LENGTH_SHORT).show();
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

    private void updatePlacedOrdersList(@NonNull AllOrdersModel updatedData) {
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
    private void syncAllOrdersRecycleView(@NonNull RecyclerView allOrdersRecycleView, ProgressBar progressBar) {

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

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
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
            showLocationSettings(requireContext());
        });

        setDeliveryToCurrentLocBtn.setOnClickListener(v -> {
            EventBus.getDefault().post(new EBSyncEmptyShopData("{'recommended_shop_data':[]}", 1274));

            //TODO: update auto detect pincode
//            getRecommendedShopsData(binding, latitude, longitude, "None", "None", "None");
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

    private void saveAddressDataToFile(@NonNull JsonArray addressData) {
        try {
            File file = new File(requireContext().getFilesDir(), "address_data.json");
            FileWriter writer = new FileWriter(file);
            writer.write(addressData.toString());
            writer.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error saving address data to file", e);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateRecyclerView(@NonNull JsonArray addressData, ProgressBar progressBar) {
        savedAddressList.clear();
        for (JsonElement element : addressData) {
            com.vishnu.quickgoorder.ui.home.category.address.SavedAddressModel book = new Gson().fromJson(element, com.vishnu.quickgoorder.ui.home.category.address.SavedAddressModel.class);
            savedAddressList.add(book);
        }
        savedAddressAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
    }


    private void fetchAddressDataFromServer(String userId, @NonNull ProgressBar progressBar) {
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

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(LocationService.ACTION_LOCATION_BROADCAST);
        requireContext().registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (setDeliveryAddrBtmView != null) {
            setDeliveryAddrBtmView.hide();
            setDeliveryAddrBtmView.dismiss();
        }
//        if (selectOrderToTrackBtmView != null) {
//            selectOrderToTrackBtmView.hide();
//            selectOrderToTrackBtmView.dismiss();
//        }

        requireContext().unregisterReceiver(locationReceiver);
    }

    private void showLocationSettings(@NonNull Context context) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
    }
}