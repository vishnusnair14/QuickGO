package com.vishnu.quickgodelivery.ui.all_orders.legacy;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;
import com.vishnu.quickgodelivery.R;
import com.vishnu.quickgodelivery.server.APIService;
import com.vishnu.quickgodelivery.server.ApiServiceGenerator;
import com.vishnu.quickgodelivery.ui.all_orders.AllOrdersFragment;
import com.vishnu.quickgodelivery.ui.all_orders.OBSOrderModel;
import com.vishnu.quickgodelivery.ui.order.OrderDetailsMainActivity;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OBSOrderAdapter extends RecyclerView.Adapter<OBSOrderAdapter.ViewHolder> {
    private List<OBSOrderModel> OBSOrderModels;
    private final Context context;
    private final AllOrdersFragment allOrdersFragment;
    private final FirebaseUser user;
    private final SharedPreferences preferences;
    private BottomSheetDialog acceptOrderBtmView;
    private BottomSheetDialog declineOrKeepItOrderBtmView;
    private BottomSheetDialog nextDeliveryOrderBtmView;
    private final ViewGroup root;
    private final Intent callIntent;

    public OBSOrderAdapter(AllOrdersFragment allOrdersFragment, ViewGroup root,
                           Context context, SharedPreferences preferences,
                           List<OBSOrderModel> OBSOrderModels) {
        this.allOrdersFragment = allOrdersFragment;
        this.root = root;
        this.context = context;
        this.OBSOrderModels = OBSOrderModels;
        this.preferences = preferences;
        user = FirebaseAuth.getInstance().getCurrentUser();
        callIntent = new Intent(Intent.ACTION_CALL);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.srv_obs_order_list,
                parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        OBSOrderModel orderData = OBSOrderModels.get(position);

        holder.orderNoTV.setText(MessageFormat.format("ORDER #{0}", position + 1));

        // Null check for orderID
        String orderID = orderData.getOrder_id();
        if (orderID != null && orderID.length() >= 28) {
            holder.orderIDTV.setText(orderID.substring(6, 28));
        } else {
            holder.orderIDTV.setText(R.string.invalid_order_id);
        }

        holder.orderType.setText(orderData.getOrder_type());
        // Null check for shopName
        String shopName = orderData.getShop_name();
        if (shopName != null) {
            holder.shopNameTV.setText(shopName.toUpperCase());
        } else {
            holder.shopNameTV.setText(R.string.unknown_shop);
        }

        holder.TIDTV.setText(orderData.getOrder_time());

        if ("Order Saved".equals(orderData.getOrder_saved_status())) {
            holder.orderSavedIV.setVisibility(View.VISIBLE);
        } else {
            holder.orderSavedIV.setVisibility(View.GONE);
        }

        if (preferences.getString("currentDeliveryOrderID", "0").equals(orderID)) {
            holder.selectedViewTV.setVisibility(View.VISIBLE);
            holder.orderListCardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(),
                    R.color.selectedOrderCardView));
            holder.orderSavedIV.setVisibility(View.GONE);
        } else {
            holder.selectedViewTV.setVisibility(View.GONE);
            holder.orderListCardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(),
                    R.color.normalOrderCardView));
        }

        holder.itemView.setOnClickListener(v -> {
            if (preferences.getBoolean("isOnDuty", false)) {
                String cdoID = preferences.getString("currentDeliveryOrderID", "0");

                if (cdoID.equals(orderID) || cdoID.equals("0")) {
                    if (!cdoID.equals(orderID)) {
                        showAcceptOrderBtmView(orderData.getUser_id(), orderID,
                                orderData.getShop_id(), orderData.getOrder_by_voice_doc_id(), orderData.getOrder_by_voice_audio_ref_id(),
                                shopName, orderData.getShop_phno(), orderData.getDelivery_full_address(),
                                orderData.getPickup_destination_distance(),
                                orderData.getOrder_delivery_destination_distance(),
                                orderData);
                    } else {
                        Intent intent = getIntent(orderData);
                        context.startActivity(intent);
                    }
                } else if ("Order Saved".equals(orderData.getOrder_saved_status())) {
                    showNextDeliveryOrderBtmView();
                } else {
                    showDeclineOrKeepItOrderBtmView(orderData.getUser_id(), orderID, orderData.getShop_id(),
                            orderData.getOrder_by_voice_doc_id(), orderData.getOrder_by_voice_audio_ref_id(), shopName);
                }
            } else {
                Toast.makeText(context, "Turn on duty toggle", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @NonNull
    private Intent getIntent(OBSOrderModel listModel) {
        Intent intent = new Intent(context, OrderDetailsMainActivity.class);
        intent.putExtra("user_id", listModel.getUser_id());
        intent.putExtra("order_id", listModel.getOrder_id());
        intent.putExtra("shop_id", listModel.getShop_id());
        intent.putExtra("order_by_voice_type", listModel.getOrder_type());
        intent.putExtra("order_by_voice_doc_id", listModel.getOrder_by_voice_doc_id());
        intent.putExtra("order_by_voice_audio_ref_id", listModel.getOrder_by_voice_audio_ref_id());
        intent.putExtra("shop_name", listModel.getShop_name());
        return intent;
    }

    @Override
    public int getItemCount() {
        return OBSOrderModels.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView TIDTV, orderNoTV;
        TextView shopNameTV;
        CardView orderListCardView;
        ImageView orderSavedIV;
        TextView orderIDTV;
        TextView selectedViewTV;
        TextView orderType;

        public ViewHolder(View itemView) {
            super(itemView);
            orderNoTV = itemView.findViewById(R.id.srvObsOrderNo_textView);
            orderIDTV = itemView.findViewById(R.id.srvObsOrderOid_textView);
            shopNameTV = itemView.findViewById(R.id.srvObsOrderShopName_textView);
            orderType = itemView.findViewById(R.id.srvObsOrderType_textView);
            TIDTV = itemView.findViewById(R.id.srvObsOrderTid_textView);
            orderListCardView = itemView.findViewById(R.id.srvObsOrderInnerCard);
            orderSavedIV = itemView.findViewById(R.id.srvObsOrderKeepIt_imageView);
            selectedViewTV = itemView.findViewById(R.id.srvObsOrderCurrentSelected_textView);
        }
    }

    private void showNextDeliveryOrderBtmView() {
        View orderView = LayoutInflater.from(context).inflate(
                R.layout.bottomview_next_delivery_dialog, root, false);

        nextDeliveryOrderBtmView = new BottomSheetDialog(context);
        nextDeliveryOrderBtmView.setContentView(orderView);
        Objects.requireNonNull(nextDeliveryOrderBtmView.getWindow()).setGravity(Gravity.TOP);

        nextDeliveryOrderBtmView.show();
    }

    private void showDeclineOrKeepItOrderBtmView(String userId, String orderID, String shopID, String orderByVoiceDocID, String orderByVoiceAudioRefID, String shopName) {
        View orderView = LayoutInflater.from(context).inflate(
                R.layout.bottomview_decline_order_confirm, root, false);

        declineOrKeepItOrderBtmView = new BottomSheetDialog(context);
        declineOrKeepItOrderBtmView.setContentView(orderView);
        Objects.requireNonNull(declineOrKeepItOrderBtmView.getWindow()).setGravity(Gravity.TOP);

        TextView declineOrderTV = orderView.findViewById(R.id.btmViewCallConfirmCancelBtn_textView);
        TextView keepItBtn = orderView.findViewById(R.id.btmViewCallConfirmProceedBtn_textView);

        declineOrderTV.setOnClickListener(v -> {
            orderDeclineRequest(user.getUid(), userId, orderID);
            if (declineOrKeepItOrderBtmView != null && declineOrKeepItOrderBtmView.isShowing()) {
                declineOrKeepItOrderBtmView.hide();
                declineOrKeepItOrderBtmView.dismiss();
            }
        });

        keepItBtn.setOnClickListener(v -> {
            if (declineOrKeepItOrderBtmView != null && declineOrKeepItOrderBtmView.isShowing()) {
                declineOrKeepItOrderBtmView.hide();
                declineOrKeepItOrderBtmView.dismiss();
            }
            orderKeepItRequest(user.getUid(), userId, orderID, shopID, orderByVoiceAudioRefID, shopName);
        });

        declineOrKeepItOrderBtmView.show();
    }

    private void showAcceptOrderBtmView(String userId, String orderID, String shopID,
                                        String orderByVoiceDocID, String orderByVoiceAudioRefID,
                                        String shopName, String shopPhone, String deliveryAddress,
                                        double pdd, double oddd, OBSOrderModel listModel) {
        View orderView = LayoutInflater.from(context).inflate(
                R.layout.bottomview_accept_order_confirm, root, false);

        acceptOrderBtmView = new BottomSheetDialog(context);
        acceptOrderBtmView.setContentView(orderView);
        Objects.requireNonNull(acceptOrderBtmView.getWindow()).setGravity(Gravity.TOP);

        ImageView shopPhnoTV = orderView.findViewById(R.id.acceptOrderBtmViewCallToShopBtn_imageView);

        TextView acceptOrderTV = orderView.findViewById(R.id.acceptOrderView_textView);
        TextView declineOrderTV = orderView.findViewById(R.id.declineOrderView_textView);
        TextView shopNameTV = orderView.findViewById(R.id.acceptOrderBtmViewShopname_textView);
        TextView shopPhoneTV = orderView.findViewById(R.id.acceptOrderBtmViewShopPhno_textView);
        TextView shopPlaceTV = orderView.findViewById(R.id.acceptOrderBtmViewShopPlaceAndKm_textView);
        TextView deliveryAddressTV = orderView.findViewById(R.id.acceptOrderBtmViewDeliveryAddress_textView);
        TextView pddTV = orderView.findViewById(R.id.acceptOrderBtmViewPDD_textView);
        TextView odddTV = orderView.findViewById(R.id.acceptOrderBtmViewODDD_textView);
        TextView btmViewStatusTV = orderView.findViewById(R.id.btmViewOrderAcceptStatusView_textView);
        ProgressBar btmViewStatusPB = orderView.findViewById(R.id.btmViewOrderAcceptStatusPB_progressBar);
        btmViewStatusPB.setVisibility(View.GONE);

        if (oddd < 1) {
            odddTV.setText(MessageFormat.format("DELIVERY DISTANCE: {0} mtr", oddd * 1000));
        } else {
            odddTV.setText(MessageFormat.format("DELIVERY DISTANCE: {0} km", oddd));
        }

        if (pdd < 1) {
            pddTV.setText(MessageFormat.format("PICKUP DISTANCE: {0} mtr", pdd * 1000));
        } else {
            pddTV.setText(MessageFormat.format("PICKUP DISTANCE: {0} km", pdd));
        }

        shopNameTV.setText(MessageFormat.format("{0}", shopName.toUpperCase(Locale.ROOT)));
        shopPlaceTV.setText(shopID);
        shopPhoneTV.setText(MessageFormat.format("+91 {0}", shopPhone));

        deliveryAddressTV.setText(MessageFormat.format("DELIVERY ADDRESS: {0}", deliveryAddress));

        acceptOrderTV.setOnClickListener(v -> {
            acceptOrderTV.setEnabled(false);
            acceptOrderTV.setVisibility(View.GONE);

            declineOrderTV.setEnabled(false);
            declineOrderTV.setVisibility(View.GONE);

            btmViewStatusPB.setVisibility(View.VISIBLE);
            btmViewStatusTV.setText(R.string.please_wait);

            sendOrderAcceptedRequest(acceptOrderTV, declineOrderTV, btmViewStatusTV, btmViewStatusPB,
                    user.getUid(), userId, orderID, shopID, orderByVoiceDocID, orderByVoiceAudioRefID,
                    shopName, listModel);
        });

        shopPhnoTV.setOnClickListener(v -> {
            if (shopPhone.trim().length() > 1) {
                callIntent.setData(Uri.parse("tel:" + shopPhone.trim()));
                context.startActivity(callIntent);
            } else {
                Toast.makeText(context, "No Phone Number Found", Toast.LENGTH_SHORT).show();
            }
        });

        acceptOrderBtmView.show();
    }

    private void orderDeclineRequest(String dpIdd, String userId, String orderID) {
        ProgressBar progressBar = root.findViewById(R.id.btmViewOrderAcceptStatusPB_progressBar);
        progressBar.setVisibility(View.VISIBLE);

        APIService apiService = ApiServiceGenerator.getApiService(context);
        Call<JsonObject> call = apiService.declineDeliveryOrder(dpIdd, userId, orderID);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject jsonObject1 = response.body();
                    String msg = jsonObject1.get("msg").getAsString();

                    progressBar.setVisibility(View.INVISIBLE);

                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                } else {
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }


    private void orderKeepItRequest(String dBoyID, String userId, String orderID, String shopID, String voiceOrderID, String shopName) {
        ProgressBar progressBar = root.findViewById(R.id.btmViewOrderAcceptStatusPB_progressBar);
        progressBar.setVisibility(View.VISIBLE);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("dBoyID", dBoyID);
        jsonObject.addProperty("orderID", orderID);
        jsonObject.addProperty("userID", userId);
        jsonObject.addProperty("shopID", shopID);
        jsonObject.addProperty("voiceOrderID", voiceOrderID);
        jsonObject.addProperty("shopName", shopName);

        APIService apiService = ApiServiceGenerator.getApiService(context);
        Call<JsonObject> call = apiService.keepItDeliveryOrder(dBoyID, userId, orderID);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject jsonObject1 = response.body();
                    String msg = jsonObject1.get("msg").getAsString();

                    progressBar.setVisibility(View.INVISIBLE);

                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                } else {
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void sendOrderAcceptedRequest(TextView acceptOrderTV, TextView declineOrderTV, TextView btmViewStatusTV,
                                          ProgressBar btmViewStatusPB, String dpID, String userID, String orderID,
                                          String shopID, String orderByVoiceDocID, String orderByVoiceAudioRefID, String shopName, OBSOrderModel listModel) {

        APIService apiService = ApiServiceGenerator.getApiService(context);
        Call<JsonObject> call3422 = apiService.setCurrentDeliveryOrder(dpID, userID, orderID);

        call3422.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call34, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null && !response.body().isEmpty()) {
                        if (response.body().get("response_status").getAsBoolean()) {
                            btmViewStatusPB.setVisibility(View.GONE);
                            btmViewStatusTV.setText("");

                            if (acceptOrderBtmView != null) {
                                acceptOrderBtmView.hide();
                                acceptOrderBtmView.dismiss();
                            }

                            preferences.edit().putString("currentDeliveryOrderID", orderID).apply();

                            // START ORDER DETAILS MAIN ACTIVITY:
                            Intent intent = getIntent(listModel);
                            context.startActivity(intent);


                        } else if (!response.body().get("response_status").getAsBoolean()) {
                            btmViewStatusTV.setText(R.string.retry_again);

                            acceptOrderTV.setEnabled(true);
                            acceptOrderTV.setVisibility(View.VISIBLE);

                            declineOrderTV.setEnabled(true);
                            declineOrderTV.setVisibility(View.VISIBLE);

                            btmViewStatusPB.setVisibility(View.GONE);
                            Toast.makeText(context, response.body().get("message").getAsString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                btmViewStatusTV.setText(R.string.retry_again);

                btmViewStatusPB.setVisibility(View.GONE);
                acceptOrderTV.setEnabled(true);
                acceptOrderTV.setVisibility(View.VISIBLE);

                declineOrderTV.setEnabled(true);
                declineOrderTV.setVisibility(View.VISIBLE);
            }
        });
    }

}
