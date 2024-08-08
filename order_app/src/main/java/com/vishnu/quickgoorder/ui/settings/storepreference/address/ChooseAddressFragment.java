package com.vishnu.quickgoorder.ui.settings.storepreference.address;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vishnu.quickgoorder.databinding.FragmentStorePreferenceChooseAddressBinding;
import com.vishnu.quickgoorder.server.sapi.APIService;
import com.vishnu.quickgoorder.server.sapi.ApiServiceGenerator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChooseAddressFragment extends Fragment {

    private final String LOG_TAG = "StorePreferenceFragment";
    private FragmentStorePreferenceChooseAddressBinding binding;
    private AddressDataAdapter addressAdapter;
    private List<AddressData> addressList;
    ProgressBar progressBar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentStorePreferenceChooseAddressBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        RecyclerView recyclerViewAddresses = binding.addressListStorePrefRecycleView;
        progressBar = binding.progressBarLoading;

        addressList = new ArrayList<>();

        // Handle address item click to fetch nearby shops
        if (user != null) {
            addressAdapter = new AddressDataAdapter(addressList, requireContext(), user.getUid());

            // Set the adapter to the RecyclerView
            recyclerViewAddresses.setAdapter(addressAdapter);
            recyclerViewAddresses.setLayoutManager(new LinearLayoutManager(getContext()));

            getSavedAddressData(user.getUid(), progressBar);
        }
        return root;
    }

    private void getSavedAddressData(String userId, ProgressBar progressBar) {
        JsonArray savedData = loadAddressDataFromFile();

        if (!savedData.isEmpty()) {
            updateRecyclerView(savedData, progressBar);
        } else {
            fetchAddressDataFromServer(userId, progressBar);
        }
    }

    private void fetchAddressDataFromServer(String userId, ProgressBar progressBar) {
        progressBar.setVisibility(View.VISIBLE);

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call0455 = apiService.getSavedAddresses(userId);

        call0455.enqueue(new Callback<>() {
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

                        Log.d(LOG_TAG, addressData.toString());

                        if (isAdded()) {
                            // Toast.makeText(getContext(), "Address data retrieved successfully", Toast.LENGTH_SHORT).show();
                            Log.d(LOG_TAG, "Address data retrieved successfully");
                        }
                    } else {
                        Log.e(LOG_TAG, "Invalid response format: Missing 'address_data' field");
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

    @SuppressLint("NotifyDataSetChanged")
    private void updateRecyclerView(JsonArray addressData, ProgressBar progressBar) {
        progressBar.setVisibility(View.VISIBLE);
        addressList.clear();
        for (JsonElement element : addressData) {
            AddressData book = new Gson().fromJson(element, AddressData.class);
            addressList.add(book);
        }
        addressAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
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


}
