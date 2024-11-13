package com.vishnu.quickgodelivery.ui.order.voice;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vishnu.quickgodelivery.R;
import com.vishnu.quickgodelivery.databinding.FragmentVoiceOrdersBinding;
import com.vishnu.quickgodelivery.server.sapi.APIService;
import com.vishnu.quickgodelivery.server.sapi.ApiServiceGenerator;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VoiceOrdersFragment extends Fragment {
    private FragmentVoiceOrdersBinding binding;
    RecyclerView voiceOrderRecycleView;
    private TextView statusTV;
    //    private String shopName;
    private String userID;
    private String shopID;
    private String orderByVoiceAudioRefID;
    private String orderByVoiceType;
    private String orderByVoiceDocID;
    private List<VoiceOrdersModel> voiceOrderItemList;
    private final String LOG_TAG = "VoiceOrdersFragment";
    ProgressBar progressBar;
    private VoiceOrdersViewAdapter voiceOrdersViewAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Retrieve arguments from bundle
        Bundle arguments = getArguments();
        if (arguments != null) {
            userID = arguments.getString("user_id");
            orderByVoiceType = arguments.getString("order_by_voice_type");
            shopID = arguments.getString("shop_id");
            orderByVoiceDocID = arguments.getString("order_by_voice_doc_id");
            orderByVoiceAudioRefID = arguments.getString("order_by_voice_audio_ref_id");
        } else {
            Toast.makeText(requireContext(), "Error retrieving arguments", Toast.LENGTH_SHORT).show();
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentVoiceOrdersBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        voiceOrderRecycleView = binding.voiceOrdersRecycleView;
        statusTV = binding.statusViewVoiceOrderTextView;
        progressBar = binding.voiceOrdersRecycleViewProgressBar;

        voiceOrderItemList = new ArrayList<>();
        voiceOrdersViewAdapter = new VoiceOrdersViewAdapter(requireContext(), voiceOrderItemList);
        voiceOrderRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));
        voiceOrderRecycleView.setAdapter(voiceOrdersViewAdapter);

//        Toast.makeText(requireContext(), orderByVoiceDocID, Toast.LENGTH_SHORT).show();
        getVoiceOrderData();

        return root;
    }


    private void getVoiceOrderData() {
        progressBar.setVisibility(View.VISIBLE);

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());

        Call<JsonObject> call8400 = apiService.getVoiceOrderData(userID, orderByVoiceType,
                orderByVoiceDocID, orderByVoiceAudioRefID,
                orderByVoiceType.equals("obv") ? "0" : shopID);

        call8400.enqueue(new Callback<>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();

                    if (responseBody.has("voice_orders_data")) {
                        JsonArray voiceOrdersData = responseBody.getAsJsonArray("voice_orders_data");

                        if (voiceOrdersData.isEmpty()) {
                            if (isAdded()) {
                                statusTV.setText(R.string.no_voice_orders_for_this_order);
                                Log.d(LOG_TAG, "Voice order data is empty");
                            }
                        } else {
                            voiceOrderItemList.clear();
                            for (JsonElement element : voiceOrdersData) {
                                VoiceOrdersModel data = new Gson().fromJson(element, VoiceOrdersModel.class);
                                voiceOrderItemList.add(data);
                            }

                            voiceOrdersViewAdapter.notifyDataSetChanged();

                            if (isAdded()) {
                                Toast.makeText(getContext(), "Voice order data retrieved successfully", Toast.LENGTH_SHORT).show();
                                Log.d(LOG_TAG, "Voice order data retrieved successfully");
                            }
                        }
                    } else {
                        statusTV.setText(R.string.an_unexpected_error_occurred);
                        Log.e(LOG_TAG, "Invalid response format: Missing 'voice_orders_data' field");
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to fetch voice order data: Invalid response format", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    String errorMessage = "Failed to fetch voice order data";
                    statusTV.setText(R.string.failed_to_fetch_voice_order_data);
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
                    Toast.makeText(getContext(), "Failed to fetch voice order data", Toast.LENGTH_SHORT).show();
                }
                Log.e(LOG_TAG, "Failed to fetch voice order data", t);
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}