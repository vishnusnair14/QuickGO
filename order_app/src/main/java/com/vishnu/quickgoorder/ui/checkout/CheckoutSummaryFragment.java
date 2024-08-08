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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.JsonObject;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.callbacks.StorePref;
import com.vishnu.quickgoorder.crypto.DESCore;
import com.vishnu.quickgoorder.databinding.FragmentCheckoutSummaryBinding;
import com.vishnu.quickgoorder.miscellaneous.PreferenceKeys;
import com.vishnu.quickgoorder.miscellaneous.SharedDataView;
import com.vishnu.quickgoorder.miscellaneous.Utils;
import com.vishnu.quickgoorder.server.sapi.APIService;
import com.vishnu.quickgoorder.server.sapi.ApiServiceGenerator;
import com.vishnu.quickgoorder.ui.payment.PaymentActivity;

import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CheckoutSummaryFragment extends Fragment {
    private final String LOG_TAG = "CheckoutSummaryFragment";
    private FirebaseFirestore db;
    private CartCheckoutVA checkoutAdapter;
    private CheckBox setDefaultAsDeliveryCheckBox;
    private TextView proceedToPaymentBtn;
    private TextView grandTotalTV;
    private static SharedDataView sharedDataView;
    private SharedPreferences preferences;
    private Intent paymentIntent;
    Bundle bundle;
    private FirebaseUser user;
    private int hasData = -2;
    BottomSheetDialog savedStorePrefBtmView;
    private BottomSheetDialog storePrefCheckFailedBtmView;
    String shopID;
    String shopDistrict;


    public CheckoutSummaryFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        user = FirebaseAuth.getInstance().getCurrentUser();

        sharedDataView = new ViewModelProvider(requireActivity()).get(SharedDataView.class);
        preferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        bundle = new Bundle();

        if (getArguments() != null) {
            shopID = getArguments().getString("shop_id");
            shopDistrict = getArguments().getString("shop_district");

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
        db = FirebaseFirestore.getInstance();

        setDefaultAsDeliveryCheckBox = binding.setDefaultAsDeliveryCheckBox;
        proceedToPaymentBtn = binding.proceedFromCheckoutTextView;

        paymentIntent = new Intent(requireActivity(), PaymentActivity.class);

        setDefaultAsDeliveryCheckBox.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                proceedToPaymentBtn.setText(R.string.proceed_to_payment);
            } else {
                proceedToPaymentBtn.setText(R.string.select_a_delivery_address);
            }
        });


        proceedToPaymentBtn.setOnClickListener(view -> {
            if (setDefaultAsDeliveryCheckBox.isChecked()) {
                if (this.hasData == -1) {
                    proceedToPaymentBtn.setText("PROCEED ANYWAY");
                    showStorePrefCheckFailedProceedAnywayBtmView();
                } else if (this.hasData == 0) {
                    proceedToPaymentBtn.setText("SET PREFERENCE");
                    showSetStorePreferenceBtmView();
                } else if (this.hasData == 1) {
                    Utils.vibrate(requireContext(), 50, 2);
                    paymentIntent.putExtras(bundle);
                    startActivity(paymentIntent);
                    Utils.vibrate(requireContext(), 50, 2);
                } else {
                    tryCheckForStorePrefData(null);
                }

            } else if (!setDefaultAsDeliveryCheckBox.isChecked()) {
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_checkoutSummaryFragment_to_savedAddressFragment);
            }
        });

        return root;
    }

    private void checkAndSaveStorePrefDataState() {
        try {
            checkIsPrefSavedForAddress(preferences.getString(
                            PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_KEY, "0"),
                    hasData -> {
                        proceedToPaymentBtn.setText("PROCEED TO PAYMENT");
                        proceedToPaymentBtn.setEnabled(true);
                        if (hasData == 1) {
                            this.hasData = 1;
                        } else if (hasData == 0) {
                            this.hasData = 0;
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
                            proceedToPaymentBtn.setText("PROCEED TO PAYMENT");
                            Utils.vibrate(requireContext(), 50, 2);
                            paymentIntent.putExtras(bundle);
                            startActivity(paymentIntent);
                            Utils.vibrate(requireContext(), 50, 2);
                        } else if (hasData == 0) {
                            this.hasData = 0;
                            proceedToPaymentBtn.setText("SET PREFERENCE");
                            showSetStorePreferenceBtmView();
                        } else if (hasData == -1) {
                            this.hasData = -1;
                            proceedToPaymentBtn.setText("PROCEED ANYWAY");
                            showStorePrefCheckFailedProceedAnywayBtmView();
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void showStorePrefCheckFailedProceedAnywayBtmView() {
        if (storePrefCheckFailedBtmView != null && storePrefCheckFailedBtmView.isShowing()) {
            // Hide and dismiss the existing dialog if it's showing
            storePrefCheckFailedBtmView.hide();
            storePrefCheckFailedBtmView.dismiss();
            return;
        }

        // Create the dialog if it does not exist or if it has been dismissed
        View storePrefCheckFailedView = LayoutInflater.from(requireContext()).inflate(
                R.layout.bottomview_failed_to_check_store_pref_data, null, false);

        storePrefCheckFailedBtmView = new BottomSheetDialog(requireContext());
        storePrefCheckFailedBtmView.setContentView(storePrefCheckFailedView);
        Objects.requireNonNull(storePrefCheckFailedBtmView.getWindow()).setGravity(Gravity.TOP);

        Button retryBtn = storePrefCheckFailedView.findViewById(R.id.btmviewFailedToCheckStorePrefDataRetryAgainBtn_button);
        Button setPrefBtn = storePrefCheckFailedView.findViewById(R.id.btmviewFailedToCheckStorePrefDataSetPrefBtn_button);
        Button cancelBtn = storePrefCheckFailedView.findViewById(R.id.btmviewFailedToCheckStorePrefDataCancelBtn_button);
        Button proceedAnywayBtn = storePrefCheckFailedView.findViewById(R.id.btmviewFailedToCheckStorePrefProceedAnywaysBtn_button);

        retryBtn.setOnClickListener(v -> {
            tryCheckForStorePrefData(storePrefCheckFailedBtmView);
        });

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

            cancelBtn.setOnClickListener(v -> {
                savedStorePrefBtmView.dismiss();
            });

            actionBtn.setOnClickListener(v -> {
                savedStorePrefBtmView.dismiss();

                new Handler().postDelayed(() -> {
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_checkoutSummaryFragment_to_nav_store_pref_choose_address);
                }, 500);
            });
        }

        // Show the dialog only if it is not already showing
        if (savedStorePrefBtmView != null && !savedStorePrefBtmView.isShowing()) {
            savedStorePrefBtmView.show();
        }
    }


    private void checkIsPrefSavedForAddress(String phno, StorePref storePref) throws Exception {
        proceedToPaymentBtn.setText("PLEASE WAIT...");
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
                            }

                        } else {
                            storePref.isDataFound(0);
//                                Toast.makeText(context, responseBody.get("message").getAsString(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        storePref.isDataFound(-1);
                        Toast.makeText(requireContext(), responseBody.get("message").getAsString(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    storePref.isDataFound(-1);
                    Log.e(LOG_TAG, "Failed to fetch store preference data" + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(LOG_TAG, "Failed to fetch store preference data", t);
                storePref.isDataFound(-1);
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
        checkAndSaveStorePrefDataState();
    }
}