package com.vishnu.quickgoorder.ui.cart.voice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
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
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.databinding.FragmentVoiceOrderCartBinding;
import com.vishnu.quickgoorder.miscellaneous.PreferenceKeys;
import com.vishnu.quickgoorder.miscellaneous.Utils;
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

public class VoiceCartFragment extends Fragment {
    private final String LOG_TAG = "CartFragment";
    private static FirebaseUser user;
    private List<VoiceOrdersModel> voiceOrderItemList;
    private FloatingActionButton clearVoiceCartBtn, refreshVoiceCartBtn;
    RecyclerView voiceOrderRecycleView;
    TextView checkoutCartButton;
    TextView statusTV;
    private VoiceOrdersAdapter voiceOrdersAdapter;
    private ProgressBar progressBar;
    private SharedPreferences preferences;
    private Bundle bundle;
    private String shopID;
    private boolean fromHomeRecommendationFragment = false;
    private boolean fromHomeOrderByVoiceFragment = false;
    private String orderByVoiceAudioRefID;
    private String orderByVoiceDocID;


    public VoiceCartFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        user = FirebaseAuth.getInstance().getCurrentUser();
        bundle = new Bundle();

        if (getArguments() != null) {
            if (getArguments().containsKey("fromHomeRecommendationFragment")
                    && getArguments().getBoolean("fromHomeRecommendationFragment")) {
                fromHomeRecommendationFragment = true;
                shopID = getArguments().getString("shop_id");

                bundle.putString("from", "fromHomeRecommendationFragment");
                bundle.putString("shop_id", getArguments().getString("shop_id"));
                bundle.putString("shop_district", getArguments().getString("shop_district"));
            }

            if (getArguments().containsKey("fromHomeOrderByVoiceFragment")
                    && getArguments().getBoolean("fromHomeOrderByVoiceFragment")) {
                fromHomeOrderByVoiceFragment = true;

                bundle.putString("from", "fromHomeOrderByVoiceFragment");
                bundle.putString("shop_id", "None");
                bundle.putString("shop_district", "None");

                orderByVoiceDocID = preferences.getString(PreferenceKeys.HOME_ORDER_BY_VOICE_FRAGMENT_ORDER_ID, "0");
                orderByVoiceAudioRefID = preferences.getString(PreferenceKeys.HOME_ORDER_BY_VOICE_FRAGMENT_AUDIO_REF_ID, "0");
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        com.vishnu.quickgoorder.databinding.FragmentVoiceOrderCartBinding binding = FragmentVoiceOrderCartBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        refreshVoiceCartBtn = binding.voiceOrderRefreshFloatingActionButton;
        clearVoiceCartBtn = binding.deleteAllVoiceOrdersFloatingActionButton;
        checkoutCartButton = binding.checkoutTextView;
        statusTV = binding.voiceOrdersStatusViewTextView;
        progressBar = binding.voiceOrderCartRecycleViewProgressBar;
        voiceOrderRecycleView = binding.voiceOrdersRecycleView;

        progressBar.setVisibility(View.GONE);
//        checkoutCartButton.setEnabled(false);

        voiceOrderItemList = new ArrayList<>();
        voiceOrdersAdapter = new VoiceOrdersAdapter(requireContext(), voiceOrderItemList);
        voiceOrderRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));
        voiceOrderRecycleView.setAdapter(voiceOrdersAdapter);

        if (fromHomeRecommendationFragment) {
            // from RECOMMENDATION-SHOP-FRAGMENT
            refreshVoiceCartBtn.setEnabled(true);
            clearVoiceCartBtn.setEnabled(true);
//            checkoutCartButton.setEnabled(true);

            if (preferences.getString(PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_ORDER_ID, "0").equals("0")) {
                preferences.edit().putString(PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_ORDER_ID, Utils.generateOrderID()).apply();
                String orderIDForHomeRecommendation = preferences.getString("", "0");
                bundle.putString("order_id", orderIDForHomeRecommendation);
            } else {
                bundle.putString("order_id", preferences.getString(PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_ORDER_ID, "0"));
            }

            bundle.putString("order_by_voice_type", "obs");
            bundle.putString("order_by_voice_doc_id", shopID);
            bundle.putString("order_by_voice_audio_ref_id", preferences.getString(
                    PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_AUDIO_REF_ID, "0"));

            getRecShopVoiceOrderData(user.getUid(), "obs", shopID, preferences.getString(
                    PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_AUDIO_REF_ID, "0"), progressBar);

            refreshVoiceCartBtn.setOnClickListener(view -> {
                Utils.deleteVoiceOrderFile(requireContext(), shopID);
                voiceOrdersAdapter.clear();
                sendRecShopVoiceDataNetworkRequest(user.getUid(), "obs", shopID,
                        preferences.getString(PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_AUDIO_REF_ID, "0"), progressBar);
                voiceOrdersAdapter.notifyDataSetChanged();
            });
        } else if (fromHomeOrderByVoiceFragment) {
            // from ORDER-BY-VOICE-FRAGMENT
            refreshVoiceCartBtn.setEnabled(true);
            clearVoiceCartBtn.setEnabled(true);
//            checkoutCartButton.setEnabled(true);

            bundle.putString("order_by_voice_type", "obv");
            bundle.putString("order_id", orderByVoiceDocID);
            bundle.putString("order_by_voice_doc_id", orderByVoiceDocID);
            bundle.putString("order_by_voice_audio_ref_id", orderByVoiceAudioRefID);

            getRecShopVoiceOrderData(user.getUid(), "obv", orderByVoiceDocID,
                    orderByVoiceAudioRefID, progressBar);

            refreshVoiceCartBtn.setOnClickListener(view -> {
                voiceOrdersAdapter.clear();
                Utils.deleteVoiceOrderFile(requireContext(), orderByVoiceDocID);
                sendRecShopVoiceDataNetworkRequest(user.getUid(), "obv",
                        orderByVoiceDocID, orderByVoiceAudioRefID, progressBar);
                voiceOrdersAdapter.notifyDataSetChanged();
            });
        } else {
            statusTV.setText(R.string.unable_to_load_voice_order_files);
            refreshVoiceCartBtn.setEnabled(false);
            clearVoiceCartBtn.setEnabled(false);
            checkoutCartButton.setEnabled(false);
        }

        clearVoiceCartBtn.setOnClickListener(view -> {

        });

        checkoutCartButton.setOnClickListener(v -> {
            Utils.vibrate(requireContext(), 50, 2);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_nav_mcart_to_checkoutSummaryFragment, bundle);
        });
        return root;
    }

//    private void checkAndSaveStorePrefDataState() {
//        try {
//            checkIsPrefSavedForAddress(preferences.getString(
//                            PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_KEY, "0"),
//                    hasData -> {
//                        checkoutCartButton.setText("CHECKOUT");
//                        checkoutCartButton.setEnabled(true);
//                        if (hasData == 1) {
//                            this.hasData = 1;
//                        } else if (hasData == 0) {
//                            this.hasData = 0;
//                        } else if (hasData == -1) {
//                            this.hasData = -1;
//                        }
//                    });
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

//    private void tryCheckForStorePrefData(BottomSheetDialog bottomSheetDialog) {
//        try {
//            checkIsPrefSavedForAddress(preferences.getString(
//                            PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_KEY, "0"),
//                    hasData -> {
//                        checkoutCartButton.setEnabled(true);
//
//                        if (bottomSheetDialog != null) {
//                            bottomSheetDialog.dismiss();
//                        }
//
//                        if (hasData == 1) {
//                            this.hasData = 1;
//                            checkoutCartButton.setText("CHECKOUT");
//                            Utils.vibrate(requireContext(), 50, 2);
//                            NavHostFragment.findNavController(this)
//                                    .navigate(R.id.action_nav_mcart_to_checkoutSummaryFragment, bundle);
//                        } else if (hasData == 0) {
//                            this.hasData = 0;
////                                checkoutCartButton.setTextColor(requireActivity().getColor(R.color.checkoutBtnFg1));
////                                checkoutCartButton.setBackgroundColor(requireActivity().getColor(R.color.checkoutBtnBg1));
//                            checkoutCartButton.setText("SET PREFERENCE");
//                            showSetStorePreferenceBtmView();
//                        } else if (hasData == -1) {
//                            this.hasData = -1;
//                            checkoutCartButton.setText("PROCEED ANYWAY");
//                            showStorePrefCheckFailedProceedAnywayBtmView();
//                        }
//                    });
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

//    private void showStorePrefCheckFailedProceedAnywayBtmView() {
//        if (storePrefCheckFailedBtmView != null) {
//            storePrefCheckFailedBtmView.hide();
//            storePrefCheckFailedBtmView.dismiss();
//        }
//
//        View storePrefCheckFailedView = LayoutInflater.from(requireContext()).inflate(
//                R.layout.bottomview_failed_to_check_store_pref_data, null, false);
//
//        storePrefCheckFailedBtmView = new BottomSheetDialog(requireContext());
//        storePrefCheckFailedBtmView.setContentView(storePrefCheckFailedView);
//        Objects.requireNonNull(storePrefCheckFailedBtmView.getWindow()).setGravity(Gravity.TOP);
//
//        Button retryBtn = storePrefCheckFailedView.findViewById(R.id.btmviewFailedToCheckStorePrefDataRetryAgainBtn_button);
//        Button setPrefBtn = storePrefCheckFailedView.findViewById(R.id.btmviewFailedToCheckStorePrefDataSetPrefBtn_button);
//        Button cancelBtn = storePrefCheckFailedView.findViewById(R.id.btmviewFailedToCheckStorePrefDataCancelBtn_button);
//        Button proceedAnywayBtn = storePrefCheckFailedView.findViewById(R.id.btmviewFailedToCheckStorePrefProceedAnywaysBtn_button);
//
//        retryBtn.setOnClickListener(v -> {
//            tryCheckForStorePrefData(storePrefCheckFailedBtmView);
//        });
//
//        setPrefBtn.setOnClickListener(v -> {
//            if (storePrefCheckFailedBtmView != null) {
//                storePrefCheckFailedBtmView.hide();
//                storePrefCheckFailedBtmView.dismiss();
//            }
//
//            NavHostFragment.findNavController(this)
//                    .navigate(R.id.action_nav_mcart_to_nav_store_pref_choose_address);
//        });
//
//        cancelBtn.setOnClickListener(v -> {
//            if (storePrefCheckFailedBtmView != null) {
//                storePrefCheckFailedBtmView.hide();
//                storePrefCheckFailedBtmView.dismiss();
//            }
//        });
//
//        proceedAnywayBtn.setOnClickListener(v -> {
//            if (storePrefCheckFailedBtmView != null) {
//                storePrefCheckFailedBtmView.hide();
//                storePrefCheckFailedBtmView.dismiss();
//            }
//
//            NavHostFragment.findNavController(this)
//                    .navigate(R.id.action_nav_mcart_to_checkoutSummaryFragment, bundle);
//        });
//
//        storePrefCheckFailedBtmView.show();
//    }

//    private void showStorePrefCheckFailedProceedAnywayBtmView() {
//        if (storePrefCheckFailedBtmView != null && storePrefCheckFailedBtmView.isShowing()) {
//            // Hide and dismiss the existing dialog if it's showing
//            storePrefCheckFailedBtmView.hide();
//            storePrefCheckFailedBtmView.dismiss();
//            return;
//        }
//
//        // Create the dialog if it does not exist or if it has been dismissed
//        View storePrefCheckFailedView = LayoutInflater.from(requireContext()).inflate(
//                R.layout.bottomview_failed_to_check_store_pref_data, null, false);
//
//        storePrefCheckFailedBtmView = new BottomSheetDialog(requireContext());
//        storePrefCheckFailedBtmView.setContentView(storePrefCheckFailedView);
//        Objects.requireNonNull(storePrefCheckFailedBtmView.getWindow()).setGravity(Gravity.TOP);
//
//        Button retryBtn = storePrefCheckFailedView.findViewById(R.id.btmviewFailedToCheckStorePrefDataRetryAgainBtn_button);
//        Button setPrefBtn = storePrefCheckFailedView.findViewById(R.id.btmviewFailedToCheckStorePrefDataSetPrefBtn_button);
//        Button cancelBtn = storePrefCheckFailedView.findViewById(R.id.btmviewFailedToCheckStorePrefDataCancelBtn_button);
//        Button proceedAnywayBtn = storePrefCheckFailedView.findViewById(R.id.btmviewFailedToCheckStorePrefProceedAnywaysBtn_button);
//
//        retryBtn.setOnClickListener(v -> {
//            tryCheckForStorePrefData(storePrefCheckFailedBtmView);
//        });
//
//        setPrefBtn.setOnClickListener(v -> {
//            if (storePrefCheckFailedBtmView != null) {
//                storePrefCheckFailedBtmView.dismiss();
//            }
//
//            NavHostFragment.findNavController(this)
//                    .navigate(R.id.action_nav_mcart_to_nav_store_pref_choose_address);
//        });
//
//        cancelBtn.setOnClickListener(v -> {
//            if (storePrefCheckFailedBtmView != null) {
//                storePrefCheckFailedBtmView.dismiss();
//            }
//        });
//
//        proceedAnywayBtn.setOnClickListener(v -> {
//            if (storePrefCheckFailedBtmView != null) {
//                storePrefCheckFailedBtmView.dismiss();
//            }
//
//            NavHostFragment.findNavController(this)
//                    .navigate(R.id.action_nav_mcart_to_checkoutSummaryFragment, bundle);
//        });
//
//        storePrefCheckFailedBtmView.show();
//    }


//    private void showSetStorePreferenceBtmView() {
//        if (savedStorePrefBtmView == null) {
//            // Dialog is not created yet, create it
//            View savedStorePrefView = LayoutInflater.from(requireContext()).inflate(
//                    R.layout.bottomview_store_pref_data_not_saved, null, false);
//
//            savedStorePrefBtmView = new BottomSheetDialog(requireContext());
//            savedStorePrefBtmView.setContentView(savedStorePrefView);
//            Objects.requireNonNull(savedStorePrefBtmView.getWindow()).setGravity(Gravity.TOP);
//
//            Button actionBtn = savedStorePrefView.findViewById(R.id.btmviewStorePrefDataNotSavedGoToSettingsBtn_button);
//            Button cancelBtn = savedStorePrefView.findViewById(R.id.btmviewStorePrefDataNotSavedCancelBtn_button);
//
//            cancelBtn.setOnClickListener(v -> {
//                savedStorePrefBtmView.dismiss();
//            });
//
//            actionBtn.setOnClickListener(v -> {
//                savedStorePrefBtmView.dismiss();
//
//                new Handler().postDelayed(() -> {
//                    NavHostFragment.findNavController(this)
//                            .navigate(R.id.action_nav_mcart_to_nav_store_pref_choose_address);
//                }, 500);
//            });
//        }
//
//        // Show the dialog only if it is not already showing
//        if (savedStorePrefBtmView != null && !savedStorePrefBtmView.isShowing()) {
//            savedStorePrefBtmView.show();
//        }
//    }


//    private void checkIsPrefSavedForAddress(String phno, StorePref storePref) throws Exception {
//        checkoutCartButton.setText("PLEASE WAIT...");
//        checkoutCartButton.setEnabled(false);
//
//        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
//        Call<JsonObject> call0455 = apiService.fetchStorePrefData(user.getUid(), DESCore.encrypt(phno));
//
//        call0455.enqueue(new Callback<>() {
//            @Override
//            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    JsonObject responseBody = response.body();
//
//                    boolean hasDataSaved;
//                    if (responseBody.has("has_data")) {
//                        hasDataSaved = responseBody.get("has_data").getAsBoolean();
//
//                        if (hasDataSaved) {
//                            storePref.isDataFound(1);
//
//                            if (responseBody.has("data")) {
//                                JsonObject storePefData = responseBody.get("data").getAsJsonObject();
//                            }
//
//                        } else {
//                            storePref.isDataFound(0);
////                                Toast.makeText(context, responseBody.get("message").getAsString(), Toast.LENGTH_SHORT).show();
//                        }
//                    } else {
//                        storePref.isDataFound(-1);
//                        Toast.makeText(requireContext(), responseBody.get("message").getAsString(), Toast.LENGTH_SHORT).show();
//                    }
//                } else {
//                    storePref.isDataFound(-1);
//                    Log.e(LOG_TAG, "Failed to fetch store preference data" + response.message());
//                }
//            }
//
//            @Override
//            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
//                Log.e(LOG_TAG, "Failed to fetch store preference data", t);
//                storePref.isDataFound(-1);
//            }
//        });
//    }


    @SuppressLint("NotifyDataSetChanged")
    private void updateRecyclerView(@NonNull JsonArray addressData, @NonNull ProgressBar progressBar) {
        statusTV.setText("");
        progressBar.setVisibility(View.VISIBLE);
        voiceOrderItemList.clear();
        for (JsonElement element : addressData) {
            VoiceOrdersModel data = new Gson().fromJson(element, VoiceOrdersModel.class);
            voiceOrderItemList.add(data);
        }
        voiceOrdersAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
    }


    private void getRecShopVoiceOrderData(String userID, String orderByVoiceType, String orderByVoiceDocID,
                                          String orderByVoiceAudioRefID, ProgressBar progressBar) {
        try {
            File folder = new File(requireContext().getFilesDir(), "voice_orders");
            File file = new File(folder, "voice_orders_data_" + orderByVoiceDocID + ".json");

            if (file.exists()) {
                processVoiceOrderFile(file, progressBar);
            } else {
                if (orderByVoiceType.equals("obs")) {
                    sendRecShopVoiceDataNetworkRequest(userID, orderByVoiceType,
                            orderByVoiceDocID, orderByVoiceAudioRefID, progressBar);
                } else if (orderByVoiceType.equals("obv")) {
                    sendRecShopVoiceDataNetworkRequest(userID, orderByVoiceType,
                            orderByVoiceDocID, orderByVoiceAudioRefID, progressBar);
                }
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error loading address data from file", e);
        }
    }


    private void processVoiceOrderFile(File file, ProgressBar progressBar) throws IOException {
        FileReader reader = new FileReader(file);
        JsonElement jsonElement = JsonParser.parseReader(reader);
        reader.close();

        if (jsonElement.isJsonArray() && !jsonElement.getAsJsonArray().isEmpty()) {
            updateRecyclerView(jsonElement.getAsJsonArray(), progressBar);
        } else {
            statusTV.setText(R.string.no_voice_orders_recorded_yet);
            progressBar.setVisibility(View.GONE);
        }
    }


    private void saveVoiceOrderDataToFile(String docID, JsonArray addressData) {
        try {
            File folder = new File(requireContext().getFilesDir(), "voice_orders");

            if (!folder.exists()) {
                boolean isCreated = folder.mkdirs();
                if (isCreated) {
                    Log.d(LOG_TAG, "Folder created successfully");
                } else {
                    Log.e(LOG_TAG, "Failed to create folder");
                }
            }

            // Define the file inside the folder
            File file = new File(folder, "voice_orders_data_" + docID + ".json");

            // Write data to the file
            FileWriter writer = new FileWriter(file);
            writer.write(addressData.toString());
            writer.close();

            Log.d(LOG_TAG, "Data saved to file: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error saving address data to file", e);
        }
    }


    private void sendRecShopVoiceDataNetworkRequest(String userID, String orderByVoiceType, String orderByVoiceDocID,
                                                    String orderByVoiceAudioRefID,
                                                    @NonNull ProgressBar progressBar) {
        statusTV.setText("");
        progressBar.setVisibility(View.VISIBLE);

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call1209 = apiService.getRecShopVoiceOrderData(userID, orderByVoiceType,
                orderByVoiceDocID, orderByVoiceAudioRefID);

        call1209.enqueue(new Callback<>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();

                    if (responseBody.has("voice_orders_data")) {
                        JsonArray voiceOrdersData = responseBody.getAsJsonArray(
                                "voice_orders_data");

                        if (voiceOrdersData.isEmpty()) {
                            if (isAdded()) {
                                saveVoiceOrderDataToFile(orderByVoiceDocID, voiceOrdersData);
                                statusTV.setText(R.string.no_voice_orders_recorded_yet);
                                Log.d(LOG_TAG, "Voice order data is empty");
                            }
                        } else {
                            saveVoiceOrderDataToFile(orderByVoiceDocID, voiceOrdersData);
                            voiceOrderItemList.clear();
                            for (JsonElement element : voiceOrdersData) {
                                VoiceOrdersModel data = new Gson().fromJson(element,
                                        VoiceOrdersModel.class);
                                voiceOrderItemList.add(data);
                            }

//                            Utils.vibrate(requireContext(), 0, VibrationEffect.DEFAULT_AMPLITUDE);
                            voiceOrdersAdapter.notifyDataSetChanged();

                            if (isAdded()) {
//                                Toast.makeText(getContext(), "Voice order data retrieved successfully", Toast.LENGTH_SHORT).show();
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
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }
}
