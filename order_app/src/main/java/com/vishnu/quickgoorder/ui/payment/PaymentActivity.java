package com.vishnu.quickgoorder.ui.payment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;
import com.razorpay.Checkout;
import com.razorpay.PaymentData;
import com.razorpay.PaymentResultWithDataListener;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.callbacks.PaymentOrderID;
import com.vishnu.quickgoorder.callbacks.PaymentVerification;
import com.vishnu.quickgoorder.crypto.DESCore;
import com.vishnu.quickgoorder.miscellaneous.PreferenceKeys;
import com.vishnu.quickgoorder.miscellaneous.Utils;
import com.vishnu.quickgoorder.server.sapi.APIService;
import com.vishnu.quickgoorder.server.sapi.ApiServiceGenerator;
import com.vishnu.quickgoorder.ui.track.OrderTrackActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentActivity extends AppCompatActivity implements PaymentResultWithDataListener {
    private final String LOG_TAG = "PaymentActivity";
    Checkout checkout;
    TextView dateViewTV;
    TextView grandTotalTV;
    TextView orderIDTV;
    SharedPreferences preferences;
    FirebaseUser user;
    private Date date;
    String shopID;
    String shopDistrict;
    private float PAY_AMOUNT = 5;
    SimpleDateFormat dateFormat;
    private String userEmail;
    private String orderByVoiceDocID;
    private String orderByVoiceType;
    private String orderByVoiceAudioRefID;
    BottomSheetDialog paymentOrderBtmView;
    BottomSheetDialog paymentSignatureBtmView;
    private String orderID;
    private boolean fromHomeOrderByVoiceFragment = false;
    private boolean fromHomeRecommendationFragment = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userEmail = user.getEmail();
        }

        date = new Date();
        dateFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mma", Locale.ENGLISH);

        checkout = new Checkout();
        Checkout.preload(getApplicationContext());

        checkout.setKeyID("rzp_test_NCpSk028UHZ9ug");

        checkout.setImage(R.drawable.baseline_payment_24);

        Button payButton = findViewById(R.id.pay_button);
        orderIDTV = findViewById(R.id.orderIDView_textView);
        grandTotalTV = findViewById(R.id.grandTotal_textView);
        dateViewTV = findViewById(R.id.orderDateView_textView);

        Intent intent = getIntent();

        // Check if the Intent has extra data
        if (intent != null && intent.hasExtra("from")) {
            if (Objects.equals(intent.getStringExtra("from"), "fromHomeOrderByVoiceFragment")) {
                fromHomeOrderByVoiceFragment = true;
                if (intent.hasExtra("order_by_voice_doc_id") && intent.hasExtra("order_by_voice_audio_ref_id")) {
                    orderID = intent.getStringExtra("order_id");
                    orderByVoiceDocID = intent.getStringExtra("order_by_voice_doc_id");
                    orderByVoiceAudioRefID = intent.getStringExtra("order_by_voice_audio_ref_id");
                    orderByVoiceType = intent.getStringExtra("order_by_voice_type");
                }
            } else if (Objects.equals(intent.getStringExtra("from"), "fromHomeRecommendationFragment")) {
                fromHomeRecommendationFragment = true;
                if (intent.hasExtra("shop_id") && intent.hasExtra("shop_district")) {
                    shopID = intent.getStringExtra("shop_id");
                    shopDistrict = intent.getStringExtra("shop_district");
                    orderID = intent.getStringExtra("order_id");
                    orderByVoiceDocID = intent.getStringExtra("order_by_voice_doc_id");
                    orderByVoiceAudioRefID = intent.getStringExtra("order_by_voice_audio_ref_id");
                    orderByVoiceType = intent.getStringExtra("order_by_voice_type");
                }
            }
        } else {
            Toast.makeText(this, "Unable to order, try again later!", Toast.LENGTH_SHORT).show();
            return;
        }

//        PAY_AMOUNT = preferences.getFloat("final_amount_payable", 0);

        if (!orderID.isEmpty()) {
            orderIDTV.setText(MessageFormat.format("Order ID: {0}", orderID));
        }
        dateViewTV.setText(MessageFormat.format("Order Date: {0}", getCurrentDate()));

        grandTotalTV.setText(MessageFormat.format("₹ {0}/-", String.format(
                Locale.ENGLISH, "%.2f", PAY_AMOUNT)));

        payButton.setOnClickListener(view -> showPaymentOrderCreationBtmView());
    }

    private void startPayment(String order_id) {
        final Activity activity = this;
        try {
            JSONObject options = getJsonObject(order_id);

            checkout.open(activity, options);


        } catch (Exception e) {
            Log.e("TAG", "Error in starting Razorpay Checkout", e);
        }
    }

    private void sendPaymentOrderCreationRequest(float amount, PaymentOrderID paymentOrderID) {
        APIService apiService = ApiServiceGenerator.getApiService(this);
        Call<JsonObject> call3347 = apiService.createPaymentOrder(amount);

        call3347.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject resp = response.body();
                    if (resp != null && !resp.isEmpty()) {
                        if (resp.has("id")) {
                            String rz_order_id = resp.get("id").getAsString();
                            paymentOrderID.onReceived(rz_order_id);
                            new Handler().postDelayed(() -> startPayment(rz_order_id), 500);
                        } else {
                            Toast.makeText(PaymentActivity.this, "Unable to create payment order at the moment, try again", Toast.LENGTH_LONG).show();
                            paymentOrderBtmView.hide();
                            paymentOrderBtmView.dismiss();
                        }
                    } else {
                        Toast.makeText(PaymentActivity.this, "Server busy (404)", Toast.LENGTH_LONG).show();
                        paymentOrderBtmView.hide();
                        paymentOrderBtmView.dismiss();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (paymentOrderBtmView != null) {
                    paymentOrderBtmView.hide();
                    paymentOrderBtmView.dismiss();
                }
                Toast.makeText(PaymentActivity.this, "Failed, try again", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPaymentOrderCreationBtmView() {
        View rzOrderProcess = LayoutInflater.from(this).inflate(
                R.layout.bottomview_creating_order_processing, null);

        /* Create a BottomSheetDialog with TOP gravity */
        paymentOrderBtmView = new BottomSheetDialog(this);
        paymentOrderBtmView.setContentView(rzOrderProcess);
        paymentOrderBtmView.setCancelable(false);
        paymentOrderBtmView.setCanceledOnTouchOutside(false);

        Objects.requireNonNull(paymentOrderBtmView.getWindow()).setGravity(Gravity.TOP);

        paymentOrderBtmView.show();

        sendPaymentOrderCreationRequest(PAY_AMOUNT, id -> {
            if (!id.isEmpty()) {
                if (paymentOrderBtmView != null) {
                    paymentOrderBtmView.hide();
                    paymentOrderBtmView.dismiss();
                }
                Toast.makeText(this, "Order initiated", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendPaymentVerificationRequest(PaymentData paymentData, PaymentVerification
            callback) {

        APIService apiService = ApiServiceGenerator.getApiService(this);
        Call<JsonObject> call3127 = apiService.verifyPaymentSignature(paymentData.getOrderId(), paymentData.getPaymentId(), paymentData.getSignature());

        call3127.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject resp = response.body();

                    if (resp != null && !resp.isEmpty()) {
                        if (Objects.equals(resp.get("is_verified").getAsString().trim(), "VERIFIED")) {
                            callback.onPaymentVerified(true);
//                            Toast.makeText(PaymentActivity.this, resp.get("is_verified").getAsString(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(PaymentActivity.this, "Unable to verify payment!", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (paymentSignatureBtmView != null) {
                    paymentSignatureBtmView.dismiss();
                }
                callback.onPaymentVerified(false);
            }
        });
    }

    private void showPaymentSignatureCheckBtmView(PaymentData paymentData) {
        View rzPaymentSignatureView = LayoutInflater.from(this).inflate(
                R.layout.bottomview_payment_signature_checking, null);

        TextView bannerTV = rzPaymentSignatureView.findViewById(R.id.signatureVerificationBanner_textView);
        TextView statusTV = rzPaymentSignatureView.findViewById(R.id.signatureVerificationStatus_textView);
        ProgressBar pb = rzPaymentSignatureView.findViewById(R.id.signatureVerificationPB_progressBar);

        /* Create a BottomSheetDialog with TOP gravity */
        paymentSignatureBtmView = new BottomSheetDialog(this);
        paymentSignatureBtmView.setContentView(rzPaymentSignatureView);
        paymentOrderBtmView.setCancelable(false);
        paymentSignatureBtmView.setCanceledOnTouchOutside(false);

        Objects.requireNonNull(paymentSignatureBtmView.getWindow()).setGravity(Gravity.TOP);

        paymentSignatureBtmView.show();

        sendPaymentVerificationRequest(paymentData, isVerified -> {
            if (isVerified) {
                bannerTV.setVisibility(View.GONE);
                pb.setVisibility(View.GONE);
                statusTV.setText(R.string.payment_received);

                new Handler().postDelayed(() -> {
                    pb.setVisibility(View.VISIBLE);
                }, 750);

                new Handler().postDelayed(() -> {
                    try {
                        if (fromHomeOrderByVoiceFragment) {
                            statusTV.setText(R.string.placing_order_obv);
                            placeOrderOBV(this, paymentSignatureBtmView, statusTV);
                        } else if (fromHomeRecommendationFragment) {
                            statusTV.setText(R.string.placing_order_obs);
                            placeOrderOBS(this, paymentSignatureBtmView, statusTV);
                        } else {
                            Log.d(LOG_TAG, "unable to place order, invalid source");
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }, 1400);
            } else {
                statusTV.setText(R.string.payment_source_not_authenticated);
                bannerTV.setVisibility(View.GONE);
                pb.setVisibility(View.GONE);
            }
        });
    }


    @NonNull
    private JSONObject getJsonObject(String rz_order_id) throws JSONException {
        JSONObject options = new JSONObject();
        JSONObject retryObj = new JSONObject();

        options.put("name", Objects.requireNonNull(user.getEmail()).substring(0, user.getEmail().indexOf('@')));
        options.put("description", "Reference No. #" + user.getUid().substring(0, 12));
        options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.jpg");
        options.put("order_id", rz_order_id);
        options.put("theme.color", "#3399cc");
        options.put("currency", "INR");
        options.put("prefill.email", userEmail);
        options.put("prefill.contact", "9588784552");

        retryObj.put("enabled", true);
        retryObj.put("max_count", 4);
        options.put("retry", retryObj);
        return options;
    }

    private JsonObject getOBVOrderData() throws Exception {
        JsonObject jsonData = new JsonObject();
        jsonData.addProperty("order_id", orderID);
        jsonData.addProperty("user_id", DESCore.encrypt(user.getUid().trim()));
        jsonData.addProperty("user_email", DESCore.encrypt(userEmail).trim());
        jsonData.addProperty("user_phno", DESCore.encrypt(preferences.getString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_KEY, "0").trim()));
        jsonData.addProperty("order_by_voice_doc_id", orderByVoiceDocID);
        jsonData.addProperty("order_by_voice_audio_ref_id", orderByVoiceAudioRefID);
//        jsonData.addProperty("curr_lat", currLat);
//        jsonData.addProperty("curr_lon", currLon);

        return jsonData;
    }

    private void placeOrderOBV(Context context, BottomSheetDialog btmDlgSht, TextView statusTV) throws
            Exception {

        JsonObject jsonData = getOBVOrderData();
        RequestBody data = RequestBody.create(jsonData.toString(), MediaType.parse("application/json"));

        APIService apiService = ApiServiceGenerator.getApiService(this);
        Call<JsonObject> call3710 = apiService.placeOrderOBV(data);

        call3710.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject resp = response.body();
                    if (resp != null && resp.has("dp_id")) {
                        statusTV.setText(R.string.order_placed_successfully);

                        preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_FRAGMENT_ORDER_ID, "0").apply();
                        preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_FRAGMENT_AUDIO_REF_ID, "0").apply();
                        Utils.deleteVoiceOrderFile(context, orderByVoiceDocID);

                        Toast.makeText(PaymentActivity.this, "Order placed successfully\n" +
                                resp.get("dp_id").getAsString(), Toast.LENGTH_SHORT).show();

                        new Handler().postDelayed(() -> {
                            btmDlgSht.hide();
                            btmDlgSht.dismiss();
                            Intent intent = new Intent(PaymentActivity.this, OrderTrackActivity.class);
                            if (orderID != null && !orderID.isEmpty()) {
                                intent.putExtra("orderToTrackOrderID", orderID);
                            }
                            startActivity(intent);
                            finish();
                        }, 1200);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (paymentSignatureBtmView != null) {
                    paymentSignatureBtmView.dismiss();
                }
                Toast.makeText(PaymentActivity.this, "onFailure", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void placeOrderOBS(Context context, BottomSheetDialog btmDlgSht, TextView statusTV) throws
            Exception {

        APIService apiService = ApiServiceGenerator.getApiService(this);

        Call<JsonObject> call3710 = apiService.placeOrderOBS(orderID,
                DESCore.encrypt(user.getUid().trim()), DESCore.encrypt(userEmail).trim(),
                DESCore.encrypt(preferences.getString(PreferenceKeys.HOME_RECOMMENDATION_SELECTED_ADDRESS_KEY, "0").trim()),
                orderByVoiceType, orderByVoiceDocID, orderByVoiceAudioRefID, shopID,
                shopDistrict, preferences.getString(PreferenceKeys.HOME_RECOMMENDATION_CURRENT_SHOP_PINCODE, "0"),
                "None", "None");

        call3710.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject resp = response.body();
                    if (resp != null && resp.has("dp_id")) {
                        statusTV.setText(R.string.order_placed_successfully);

                        preferences.edit().putString(PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_ORDER_ID, "0").apply();
                        preferences.edit().putString(PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_AUDIO_REF_ID, "0").apply();
                        Utils.deleteVoiceOrderFile(context, shopID);

                        Toast.makeText(PaymentActivity.this, "Order placed successfully\n" +
                                resp.get("dp_id").getAsString(), Toast.LENGTH_SHORT).show();

                        new Handler().postDelayed(() -> {
                            btmDlgSht.hide();
                            btmDlgSht.dismiss();
                            Intent intent = new Intent(PaymentActivity.this, OrderTrackActivity.class);
                            if (orderID != null && !orderID.isEmpty()) {
                                intent.putExtra("orderToTrackOrderID", orderID);
                            }
                            startActivity(intent);
                            finish();
                        }, 1200);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (paymentSignatureBtmView != null) {
                    paymentSignatureBtmView.dismiss();
                }
                Toast.makeText(PaymentActivity.this, "onFailure", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPaymentSuccess(String s, PaymentData paymentData) {
        showPaymentSignatureCheckBtmView(paymentData);
    }

    @Override
    public void onPaymentError(int i, String s, PaymentData paymentData) {
        Toast.makeText(this, "Payment Failed!", Toast.LENGTH_LONG).show();

    }

    private String getCurrentDate() {
        return dateFormat.format(date);
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (paymentOrderBtmView != null) {
            paymentOrderBtmView.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (paymentOrderBtmView != null) {
            paymentOrderBtmView.dismiss();
        }
    }
}