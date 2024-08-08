package com.vishnu.quickgoorder.ui.settings.storepreference.store;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.databinding.FragmentStorePreferenceSetStoreBinding;
import com.vishnu.quickgoorder.server.sapi.APIService;
import com.vishnu.quickgoorder.server.sapi.ApiServiceGenerator;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SetStorePrefFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private final String LOG_TAG = "StorePrefSetStoreFragment";
    private double mParamLat;
    private double mParamLon;
    private String mParamCity;
    private String mParamPincode;
    private String mParamPhone;
    private Button sendPrefBtn;

    private static List<StoreData> nearbyShops;
    private StoreDataAdapter storeDataAdapter;
    private static FragmentStorePreferenceSetStoreBinding binding;
    private FirebaseUser user;
    private ProgressBar progressBar;
    private TextView statusTV;

    public SetStorePrefFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParamLat = getArguments().getDouble("param_lat");
            mParamLon = getArguments().getDouble("param_lon");
            mParamCity = getArguments().getString("param_city");
            mParamPincode = getArguments().getString("param_pincode");
            mParamPhone = getArguments().getString("param_phone");
        }
        user = FirebaseAuth.getInstance().getCurrentUser();

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentStorePreferenceSetStoreBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        GridView gridView = binding.gridViewStores;
        sendPrefBtn = binding.btnSendPreferences;
        progressBar = binding.progressBarLoading;
        statusTV = binding.statusStorePrefTextView;
        binding.accoTextView.setVisibility(View.GONE);

        nearbyShops = new ArrayList<>();
        storeDataAdapter = new StoreDataAdapter(requireContext(), nearbyShops);
        gridView.setAdapter(storeDataAdapter);

        fetchNearbyShops();

        sendPrefBtn.setOnClickListener(v -> {
            sendPrefBtn.setEnabled(false);
            sendPrefBtn.setText(R.string.please_wait);

            savePreferences();
        });

        return root;
    }


    private void savePreferences() {
        List<StoreData> selectedShops = storeDataAdapter.getSelectedStores();

        // Construct the request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("user_id", user.getUid());
        requestBody.addProperty("address_phno", mParamPhone);

        // Convert selected shops to JSON array
        JsonArray shopsArray = new JsonArray();
        Gson gson = new Gson();
        for (StoreData shop : selectedShops) {
            JsonElement shopElement = gson.toJsonTree(shop);
            shopsArray.add(shopElement);
        }

        requestBody.add("shop_preferences", shopsArray);

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call = apiService.saveSelectedStores(requestBody);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    // Handle the response from the server
                    JsonObject responseBody = response.body();
                    if (isAdded() && responseBody != null) {
                        Toast.makeText(requireContext(), responseBody.get("message").getAsString(), Toast.LENGTH_SHORT).show();
                        sendPrefBtn.setText(R.string.set_preference);
                        sendPrefBtn.setEnabled(true);
                    }
                } else {
                    // Handle the error response
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Failed to save preferences. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                    sendPrefBtn.setText(R.string.set_preference);
                    sendPrefBtn.setEnabled(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "An error occurred: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
                sendPrefBtn.setText(R.string.set_preference);
                sendPrefBtn.setEnabled(true);
            }
        });
    }


    public static void checkForDuplicatePreferences() {
        boolean hasDuplicate = false;
        Set<Integer> uniquePreferences = new HashSet<>();
        for (StoreData store : nearbyShops) {
            if (!uniquePreferences.add(store.getShop_preference())) {
                hasDuplicate = true;
                break;
            }
        }
        binding.btnSendPreferences.setEnabled(!hasDuplicate);
    }

    private void fetchNearbyShops() {
        progressBar.setVisibility(View.VISIBLE);

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call = apiService.getShopRecommendations(mParamLat, mParamLon, mParamCity, mParamPincode);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressBar.setVisibility(View.GONE);
                statusTV.setVisibility(View.GONE);
                binding.accoTextView.setVisibility(View.VISIBLE);
                binding.accoTextView.setText(MessageFormat.format(
                        "The shops mentioned below are within a 5 km radius of the address associated with the phone no {0}", mParamPhone));

                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseData = response.body();

                    if (responseData.has("recommended_shop_data")) {
                        JsonArray recommendedShopsArray = responseData.getAsJsonArray("recommended_shop_data");

                        if (!recommendedShopsArray.isEmpty()) {
                            nearbyShops.clear();
                            for (JsonElement shopElement : recommendedShopsArray) {
                                Log.d(LOG_TAG, "Shop data: " + shopElement);
                                StoreData shop = new Gson().fromJson(shopElement, StoreData.class);
                                nearbyShops.add(shop);
                                if (nearbyShops.size() >= 4) {
                                    break; // Only add the first 4 shops
                                }
                            }
                            storeDataAdapter.notifyDataSetChanged();
                            checkForDuplicatePreferences();

                        } else {
                            Log.e(LOG_TAG, "No shops found in 'recommended_shop_data'");
                            binding.accoTextView.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                            statusTV.setVisibility(View.VISIBLE);
                            statusTV.setText(R.string.no_nearby_shops_found_for_this_address);
                        }
                    } else {
                        Log.e(LOG_TAG, "Invalid response format: Missing 'recommended_shop_data' field");
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Failed to fetch nearby shops, invalid response format", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    String errorMessage = "Failed to fetch nearby shops";
                    binding.accoTextView.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    statusTV.setVisibility(View.VISIBLE);
                    statusTV.setText(R.string.failed_to_fetch_nearby_shops_try_again);
                    errorMessage += ": " + response.message();
                    Log.e(LOG_TAG, errorMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                binding.accoTextView.setVisibility(View.GONE);
                statusTV.setVisibility(View.VISIBLE);
                statusTV.setText(R.string.failed_to_fetch_nearby_shops_try_again);
                Log.e(LOG_TAG, "Failed to fetch nearby shops", t);
            }
        });
    }

}
