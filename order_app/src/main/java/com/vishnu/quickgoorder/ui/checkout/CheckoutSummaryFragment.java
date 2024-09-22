package com.vishnu.quickgoorder.ui.checkout;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.callbacks.StorePref;
import com.vishnu.quickgoorder.crypto.DESCore;
import com.vishnu.quickgoorder.databinding.FragmentCheckoutSummaryBinding;
import com.vishnu.quickgoorder.miscellaneous.PreferenceKeys;
import com.vishnu.quickgoorder.miscellaneous.Utils;
import com.vishnu.quickgoorder.server.sapi.APIService;
import com.vishnu.quickgoorder.server.sapi.ApiServiceGenerator;
import com.vishnu.quickgoorder.ui.payment.PaymentActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutSummaryFragment extends Fragment {
    private final String LOG_TAG = "CheckoutSummaryFragment";
    private CheckoutSummaryAdapter checkoutAdapter;
    private CheckBox setDefaultAsDeliveryCheckBox;
    private TextView proceedToPaymentBtn, storePrefView;
    private TextView grandTotalTV;
    private SharedPreferences preferences;
    private Intent paymentIntent;
    Bundle bundle;
    private FirebaseUser user;
    ProgressBar storePrefDataViewPB;
    private int hasData = -2;
    BottomSheetDialog savedStorePrefBtmView;
    private BottomSheetDialog storePrefCheckFailedBtmView;
    String shopID;
    String shopDistrict;
    TextView storePrefBanner;
    String from;
    TextView changeStorePref;

    public CheckoutSummaryFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = FirebaseAuth.getInstance().getCurrentUser();

        preferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        bundle = new Bundle();

        if (getArguments() != null) {
            shopID = getArguments().getString("shop_id");
            shopDistrict = getArguments().getString("shop_district");
            from = getArguments().getString("from");

            bundle.putString("from", getArguments().getString("from"));
            bundle.putString("shop_id", getArguments().getString("shop_id"));
            bundle.putString("shop_district", getArguments().getString("shop_district"));
            bundle.putString("order_id", getArguments().getString("order_id"));
            bundle.putString("order_by_voice_type", getArguments().getString("order_by_voice_type"));
            bundle.putString("order_by_voice_doc_id", getArguments().getString("order_by_voice_doc_id"));
            bundle.putString("order_by_voice_audio_ref_id", getArguments().getString("order_by_voice_audio_ref_id"));
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        com.vishnu.quickgoorder.databinding.FragmentCheckoutSummaryBinding binding = FragmentCheckoutSummaryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setDefaultAsDeliveryCheckBox = binding.setDefaultAsDeliveryCheckBox;
        proceedToPaymentBtn = binding.proceedFromCheckoutTextView;
        changeStorePref = binding.chnageStorePrefCheckoutSummaryButton;
        TextView defaultAddrViewTV = binding.checkoutSummaryDefaultAddressViewTextView;
        TextView defaultAddrPhoneViewTV = binding.checkoutSummaryDefaultAddressPhonenoViewTextView;
        storePrefView = binding.shopPreferenceViewTextView;
        storePrefDataViewPB = binding.storePrefDataViewPBProgressBar;
        storePrefBanner = binding.textView49;

        paymentIntent = new Intent(requireActivity(), PaymentActivity.class);

        changeStorePref.setOnClickListener(v -> NavHostFragment.findNavController(this)
                .navigate(R.id.action_checkoutSummaryFragment_to_nav_store_pref_choose_address));

        setDefaultAsDeliveryCheckBox.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                proceedToPaymentBtn.setText(R.string.proceed_to_payment);
            } else {
                proceedToPaymentBtn.setText(R.string.select_a_delivery_address);
            }
        });


        proceedToPaymentBtn.setOnClickListener(view -> {
            if (setDefaultAsDeliveryCheckBox.isChecked()) {
                if (from.equals("fromHomeOrderByVoiceFragment")) {
                    if (this.hasData == -1) {
                        proceedToPaymentBtn.setText(R.string.proceed_anyway);
                        showStorePrefCheckFailedProceedAnywayBtmView();
                    } else if (this.hasData == 0) {
                        proceedToPaymentBtn.setText(R.string.set_preference);
                        showSetStorePreferenceBtmView();
                    } else if (this.hasData == 1) {
                        paymentIntent.putExtras(bundle);
                        startActivity(paymentIntent);
                        Utils.vibrate(requireContext(), 50, 2);
                    } else {
                        tryCheckForStorePrefData(null);
                    }
                } else {
                    paymentIntent.putExtras(bundle);
                    startActivity(paymentIntent);
                    Utils.vibrate(requireContext(), 50, 2);
                }
            } else if (!setDefaultAsDeliveryCheckBox.isChecked()) {
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_checkoutSummaryFragment_to_savedAddressFragment);
            }
        });

        if (from.equals("fromHomeOrderByVoiceFragment")) {
            defaultAddrViewTV.setText(preferences.getString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_FULL_ADDRESS, ""));
            defaultAddrPhoneViewTV.setText(preferences.getString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_KEY, ""));
        } else if (from.equals("fromHomeRecommendationFragment")) {
            defaultAddrViewTV.setText(preferences.getString(PreferenceKeys.HOME_RECOMMENDATION_SELECTED_ADDRESS_STREET_ADDRESS, ""));
            defaultAddrPhoneViewTV.setText(preferences.getString(PreferenceKeys.HOME_RECOMMENDATION_SELECTED_ADDRESS_KEY, ""));
        } else {
            defaultAddrViewTV.setText("");
        }

        return root;
    }

    private void checkAndSaveStorePrefDataState() {
        try {
            checkIsPrefSavedForAddress(preferences.getString(
                            PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_KEY, "0"),
                    hasData -> {
                        proceedToPaymentBtn.setText(R.string.proceed_to_payment);
                        proceedToPaymentBtn.setEnabled(true);
                        if (hasData == 1) {
                            this.hasData = 1;
                        } else if (hasData == 0) {
                            this.hasData = 0;
                            changeStorePref.setText(R.string.set_store_preference);
                        } else if (hasData == -1) {
                            this.hasData = -1;
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void tryCheckForStorePrefData(BottomSheetDialog bottomSheetDialog) {
        try {
            checkIsPrefSavedForAddress(preferences.getString(
                            PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_KEY, "0"),
                    hasData -> {
                        proceedToPaymentBtn.setEnabled(true);

                        if (bottomSheetDialog != null) {
                            bottomSheetDialog.dismiss();
                        }

                        if (hasData == 1) {
                            this.hasData = 1;
                            proceedToPaymentBtn.setText(R.string.proceed_to_payment);
                            Utils.vibrate(requireContext(), 50, 2);
                            paymentIntent.putExtras(bundle);
                            startActivity(paymentIntent);
                            Utils.vibrate(requireContext(), 50, 2);
                        } else if (hasData == 0) {
                            this.hasData = 0;
                            changeStorePref.setText(R.string.set_store_preference);
                            proceedToPaymentBtn.setText(R.string.set_preference);
                            showSetStorePreferenceBtmView();
                        } else if (hasData == -1) {
                            this.hasData = -1;
                            proceedToPaymentBtn.setText(R.string.proceed_anyway);
                            showStorePrefCheckFailedProceedAnywayBtmView();
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void showStorePrefCheckFailedProceedAnywayBtmView() {
        if (storePrefCheckFailedBtmView != null && storePrefCheckFailedBtmView.isShowing()) {
            storePrefCheckFailedBtmView.hide();
            storePrefCheckFailedBtmView.dismiss();
            return;
        }

        View storePrefCheckFailedView = LayoutInflater.from(requireContext()).inflate(
                R.layout.bottomview_failed_to_check_store_pref_data, null, false);

        storePrefCheckFailedBtmView = new BottomSheetDialog(requireContext());
        storePrefCheckFailedBtmView.setContentView(storePrefCheckFailedView);
        Objects.requireNonNull(storePrefCheckFailedBtmView.getWindow()).setGravity(Gravity.TOP);

        Button retryBtn = storePrefCheckFailedView.findViewById(R.id.btmviewFailedToCheckStorePrefDataRetryAgainBtn_button);
        Button setPrefBtn = storePrefCheckFailedView.findViewById(R.id.btmviewFailedToCheckStorePrefDataSetPrefBtn_button);
        Button cancelBtn = storePrefCheckFailedView.findViewById(R.id.btmviewFailedToCheckStorePrefDataCancelBtn_button);
        Button proceedAnywayBtn = storePrefCheckFailedView.findViewById(R.id.btmviewFailedToCheckStorePrefProceedAnywaysBtn_button);

        retryBtn.setOnClickListener(v -> tryCheckForStorePrefData(storePrefCheckFailedBtmView));

        setPrefBtn.setOnClickListener(v -> {
            if (storePrefCheckFailedBtmView != null) {
                storePrefCheckFailedBtmView.dismiss();
            }

            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_checkoutSummaryFragment_to_nav_store_pref_choose_address);
        });

        cancelBtn.setOnClickListener(v -> {
            if (storePrefCheckFailedBtmView != null) {
                storePrefCheckFailedBtmView.dismiss();
            }
        });

        proceedAnywayBtn.setOnClickListener(v -> {
            if (storePrefCheckFailedBtmView != null) {
                storePrefCheckFailedBtmView.dismiss();
            }
            paymentIntent.putExtras(bundle);
            startActivity(paymentIntent);
            Utils.vibrate(requireContext(), 50, 2);
        });

        storePrefCheckFailedBtmView.show();
    }

    private void showSetStorePreferenceBtmView() {
        if (savedStorePrefBtmView == null) {
            // Dialog is not created yet, create it
            View savedStorePrefView = LayoutInflater.from(requireContext()).inflate(
                    R.layout.bottomview_store_pref_data_not_saved, null, false);

            savedStorePrefBtmView = new BottomSheetDialog(requireContext());
            savedStorePrefBtmView.setContentView(savedStorePrefView);
            Objects.requireNonNull(savedStorePrefBtmView.getWindow()).setGravity(Gravity.TOP);

            Button actionBtn = savedStorePrefView.findViewById(R.id.btmviewStorePrefDataNotSavedGoToSettingsBtn_button);
            Button cancelBtn = savedStorePrefView.findViewById(R.id.btmviewStorePrefDataNotSavedCancelBtn_button);

            cancelBtn.setOnClickListener(v -> savedStorePrefBtmView.dismiss());

            actionBtn.setOnClickListener(v -> {
                savedStorePrefBtmView.dismiss();

                new Handler().postDelayed(() -> NavHostFragment.findNavController(this)
                        .navigate(R.id.action_checkoutSummaryFragment_to_nav_store_pref_choose_address), 500);
            });
        }

        // Show the dialog only if it is not already showing
        if (savedStorePrefBtmView != null && !savedStorePrefBtmView.isShowing()) {
            savedStorePrefBtmView.show();
        }
    }

    private StringBuilder extractShopNames(String jsonData) {
        StringBuilder sData = new StringBuilder();
        try {
            // Parse the JSON data
            JSONObject data = new JSONObject(jsonData);
            List<Shop> shopList = new ArrayList<>();

            // Iterate over the keys in the JSON object
            Iterator<String> keys = data.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject shopDetails = data.getJSONObject(key);

                // Extract the shop_name and shop_preference
                String shopName = shopDetails.getString("shop_name").substring(0, 1).toUpperCase() +
                        shopDetails.getString("shop_name").substring(1).toLowerCase();
                String pref = shopDetails.getString("shop_preference");

                // Add to list as a Shop object
                shopList.add(new Shop(shopName, pref));
            }

            // Sort the list based on shop preference
            shopList.sort(Comparator.comparing(Shop::getPreference));

            // Append sorted data to StringBuilder
            for (Shop shop : shopList) {
                sData.append("• ").append(shop.getName()).append(" ").append(shop.getPreference()).append("\n");
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.toString());
        }
        return sData;
    }

    private void checkIsPrefSavedForAddress(String phno, StorePref storePref) throws Exception {
        proceedToPaymentBtn.setText(R.string.please_wait);
        proceedToPaymentBtn.setEnabled(false);

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call0455 = apiService.fetchStorePrefData(user.getUid(), DESCore.encrypt(phno));

        call0455.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();

                    boolean hasDataSaved;
                    if (responseBody.has("has_data")) {
                        hasDataSaved = responseBody.get("has_data").getAsBoolean();

                        if (hasDataSaved) {
                            storePref.isDataFound(1);

                            if (responseBody.has("data")) {
                                JsonObject storePefData = responseBody.get("data").getAsJsonObject();
                                String sData = extractShopNames(String.valueOf(storePefData)) + "";
                                storePrefDataViewPB.setVisibility(View.GONE);
                                storePrefView.setVisibility(View.VISIBLE);
                                storePrefBanner.setVisibility(View.VISIBLE);
                                storePrefView.setText(sData);
                            }

                        } else {
                            storePref.isDataFound(0);
                            storePrefBanner.setVisibility(View.GONE);
                            storePrefDataViewPB.setVisibility(View.GONE);
//                                Toast.makeText(context, responseBody.get("message").getAsString(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        storePref.isDataFound(-1);
                        storePrefBanner.setVisibility(View.GONE);
                        storePrefDataViewPB.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), responseBody.get("message").getAsString(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    storePref.isDataFound(-1);
                    storePrefBanner.setVisibility(View.GONE);
                    storePrefDataViewPB.setVisibility(View.GONE);
                    Log.e(LOG_TAG, "Failed to fetch store preference data" + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(LOG_TAG, "Failed to fetch store preference data", t);
                storePref.isDataFound(-1);
                storePrefBanner.setVisibility(View.GONE);
                storePrefDataViewPB.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (from.equals("fromHomeOrderByVoiceFragment")) {
            checkAndSaveStorePrefDataState();
        }
    }
}