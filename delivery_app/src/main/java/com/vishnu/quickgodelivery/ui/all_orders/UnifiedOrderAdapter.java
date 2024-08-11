package com.vishnu.quickgodelivery.ui.all_orders;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.vishnu.quickgodelivery.ui.order.OrderDetailsMainActivity;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UnifiedOrderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_OBS = 0;
    private static final int VIEW_TYPE_OBV = 1;
    public Context context;
    static SharedPreferences preferences;
    private final FirebaseUser user;
    private final Intent callIntent;
    private final ViewGroup root;
    private BottomSheetDialog acceptOrderBtmView;
    private BottomSheetDialog declineOrKeepItOrderBtmView;
    private BottomSheetDialog nextDeliveryOrderBtmView;


    private List<AllOrdersModel> orderList;

    public UnifiedOrderAdapter(ViewGroup root, Context context, SharedPreferences preferences, List<AllOrdersModel> orderList) {
        this.context = context;
        this.preferences = preferences;
        this.orderList = orderList;
        this.root = root;
        callIntent = new Intent(Intent.ACTION_CALL);
        user = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public int getItemViewType(int position) {
        AllOrdersModel order = orderList.get(position);
        return "obs".equals(order.getOrderType()) ? VIEW_TYPE_OBS : VIEW_TYPE_OBV;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_OBS) {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.srv_obs_order_list, parent, false);
            return new OBSViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.srv_obv_order_list, parent, false);
            return new OBVViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AllOrdersModel order = orderList.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_OBS) {
            ((OBSViewHolder) holder).bind(this, order.getObsOrderData(), position);
        } else {
            ((OBVViewHolder) holder).bind(this, order.getObvOrderData(), position);
        }
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OBSViewHolder extends RecyclerView.ViewHolder {
        // Initialize views specific to OBSOrderModel
        TextView TIDTV, orderNoTV;
        TextView shopNameTV;
        CardView orderListCardView;
        ImageView orderSavedIV;
        TextView orderIDTV;
        TextView selectedViewTV;
        TextView orderType;

        public OBSViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views
            orderNoTV = itemView.findViewById(R.id.srvObsOrderNo_textView);
            orderIDTV = itemView.findViewById(R.id.srvObsOrderOid_textView);
            shopNameTV = itemView.findViewById(R.id.srvObsOrderShopName_textView);
            orderType = itemView.findViewById(R.id.srvObsOrderType_textView);
            TIDTV = itemView.findViewById(R.id.srvObsOrderTid_textView);
            orderListCardView = itemView.findViewById(R.id.srvObsOrderInnerCard);
            orderSavedIV = itemView.findViewById(R.id.srvObsOrderKeepIt_imageView);
            selectedViewTV = itemView.findViewById(R.id.srvObsOrderCurrentSelected_textView);
        }

        public void bind(UnifiedOrderAdapter adapter, OBSOrderModel obsOrder, int position) {
            // Bind data to views
            orderNoTV.setText(MessageFormat.format("ORDER #{0}", position + 1));

            // Null check for orderID
            String orderID = obsOrder.getOrder_id();
            if (orderID != null && orderID.length() >= 29) {
                orderIDTV.setText(orderID.substring(6, 29));
            } else {
                orderIDTV.setText(R.string.invalid_order_id);
            }

            orderType.setText(obsOrder.getOrder_type());
            // Null check for shopName
            String shopName = obsOrder.getShop_name();
            if (shopName != null) {
                shopNameTV.setText(shopName.toUpperCase());
            } else {
                shopNameTV.setText(R.string.unknown_shop);
            }

            TIDTV.setText(obsOrder.getOrder_time());

            if ("Order Saved".equals(obsOrder.getOrder_saved_status())) {
                orderSavedIV.setVisibility(View.VISIBLE);
            } else {
                orderSavedIV.setVisibility(View.GONE);
            }

            if (preferences.getString("currentDeliveryOrderID", "0").equals(orderID)) {
                selectedViewTV.setVisibility(View.VISIBLE);
                orderListCardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                        R.color.selectedOrderCardView));
                orderSavedIV.setVisibility(View.GONE);
            } else {
                selectedViewTV.setVisibility(View.GONE);
                orderListCardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                        R.color.normalOrderCardView));
            }

            itemView.setOnClickListener(v -> {
                if (preferences.getBoolean("isOnDuty", false)) {
                    String cdoID = preferences.getString("currentDeliveryOrderID", "0");

                    if (cdoID.equals(orderID) || cdoID.equals("0")) {
                        if (!cdoID.equals(orderID)) {
                            adapter.showAcceptOrderBtmView(adapter, obsOrder.getUser_id(), orderID,
                                    obsOrder.getShop_id(), obsOrder.getOrder_by_voice_doc_id(),
                                    obsOrder.getOrder_by_voice_audio_ref_id(),
                                    shopName, obsOrder.getShop_phno(),
                                    obsOrder.getDelivery_full_address(),
                                    obsOrder.getPickup_destination_distance(),
                                    obsOrder.getOrder_delivery_destination_distance(),
                                    obsOrder);
                        } else {
                            Intent intent = getIntent(adapter, obsOrder);
                            adapter.context.startActivity(intent);
                        }
                    } else if ("Order Saved".equals(obsOrder.getOrder_saved_status())) {
                        adapter.showNextDeliveryOrderBtmView();
                    } else {
                        adapter.showDeclineOrKeepItOrderBtmView(obsOrder.getUser_id(), orderID,
                                obsOrder.getOrder_by_voice_doc_id(),
                                obsOrder.getOrder_by_voice_audio_ref_id(), shopName);
                    }
                } else {
                    Toast.makeText(adapter.context, "Turn on duty toggle", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    static class OBVViewHolder extends RecyclerView.ViewHolder {
        // Initialize views specific to OBVOrderModel
        private final TextView TIDTV;
        private final TextView orderNoTV;
        private TextView shopNameTV;
        private final CardView orderListCardView;
        private final ImageView orderSavedIV;
        private final TextView orderIDTV;
        private final TextView selectedViewTV;
        private final TextView orderType;

        public OBVViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views
            orderNoTV = itemView.findViewById(R.id.srvObvOrderNo_textView);
            orderIDTV = itemView.findViewById(R.id.srvObvOrderOid_textView);
            shopNameTV = itemView.findViewById(R.id.srvObvOrderShopName_textView);
            shopNameTV = itemView.findViewById(R.id.srvObvOrderShopName_textView);
            orderType = itemView.findViewById(R.id.srvObvOrdersOrderType_textView);
            TIDTV = itemView.findViewById(R.id.srvObvOrderTid_textView);
            orderListCardView = itemView.findViewById(R.id.obvOrderItem_cardView);
            orderSavedIV = itemView.findViewById(R.id.srvObvOrderKeepItView_imageView);
            selectedViewTV = itemView.findViewById(R.id.srvObvCurrentSelectedOrderView_textView);
        }

        public void bind(UnifiedOrderAdapter adapter, OBVOrderModel obvOrder, int position) {
            // Bind data to views

            orderNoTV.setText(MessageFormat.format("ORDER #{0}", position + 1));

            String orderID = obvOrder.getOrder_id();
            if (orderID != null && orderID.length() >= 29) {
                orderIDTV.setText(orderID.substring(6, 29));
            } else {
                orderIDTV.setText(R.string.invalid_order_id);
            }

            orderType.setText(obvOrder.getOrder_type());
            TIDTV.setText(obvOrder.getOrder_time());
            shopNameTV.setText("Purchase by store preference");

            if ("Order Saved".equals(obvOrder.getOrder_saved_status())) {
                orderSavedIV.setVisibility(View.VISIBLE);
            } else {
                orderSavedIV.setVisibility(View.GONE);
            }

            if (preferences.getString("currentDeliveryOrderID", "0").equals(orderID)) {
                selectedViewTV.setVisibility(View.VISIBLE);
                orderListCardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                        R.color.selectedOrderCardView));
                orderSavedIV.setVisibility(View.GONE);
            } else {
                selectedViewTV.setVisibility(View.GONE);
                orderListCardView.setCardBackgroundColor(ContextCompat.getColor(itemView.getContext(),
                        R.color.normalOrderCardView));
            }

            itemView.setOnClickListener(v -> {
                if (preferences.getBoolean("isOnDuty", false)) {
                    String cdoID = preferences.getString("currentDeliveryOrderID", "0");
//                    Toast.makeText(adapter.context, cdoID, Toast.LENGTH_SHORT).show();

                    if (cdoID.equals(orderID) || cdoID.equals("0")) {
                        if (!cdoID.equals(orderID)) {
                            adapter.showAcceptOrderBtmView(adapter, obvOrder.getUser_id(), orderID,
                                    "", obvOrder.getOrder_by_voice_doc_id(),
                                    obvOrder.getOrder_by_voice_audio_ref_id(),
                                    "Purchase by store preference", "0",
                                    obvOrder.getDelivery_full_address(),
                                    obvOrder.getPickup_destination_distance(),
                                    obvOrder.getOrder_delivery_destination_distance(),
                                    obvOrder);
                        } else {
                            Intent intent = getIntent(adapter, obvOrder);
                            adapter.context.startActivity(intent);
                        }
                    } else if ("Order Saved".equals(obvOrder.getOrder_saved_status())) {
                        adapter.showNextDeliveryOrderBtmView();
                    } else {
                        adapter.showDeclineOrKeepItOrderBtmView(obvOrder.getUser_id(), orderID,
                                obvOrder.getOrder_by_voice_doc_id(),
                                obvOrder.getOrder_by_voice_audio_ref_id(), "Purchase by store preference");
                    }
                } else {
                    Toast.makeText(adapter.context, "Turn on duty toggle", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    @androidx.annotation.NonNull
    private static Intent getIntent(UnifiedOrderAdapter adapter, OrderModel listModel) {
        Intent intent = new Intent(adapter.context, OrderDetailsMainActivity.class);
        intent.putExtra("user_id", listModel.getUser_id());
        intent.putExtra("order_id", listModel.getOrder_id());
        intent.putExtra("user_phno", listModel.getUser_phno());
        intent.putExtra("order_by_voice_type", listModel.getOrder_type());
        intent.putExtra("order_by_voice_doc_id", listModel.getOrder_by_voice_doc_id());
        intent.putExtra("order_by_voice_audio_ref_id", listModel.getOrder_by_voice_audio_ref_id());
        return intent;
    }


    private void showAcceptOrderBtmView(UnifiedOrderAdapter adapter, String userId, String orderID, String shopID,
                                        String orderByVoiceDocID, String orderByVoiceAudioRefID,
                                        String shopName, String shopPhone, String deliveryAddress,
                                        double pdd, double oddd, OrderModel listModel) {
        View orderView = LayoutInflater.from(context).inflate(
                R.layout.bottomview_accept_order_confirm, root, false);

        acceptOrderBtmView = new BottomSheetDialog(context);
        acceptOrderBtmView.setContentView(orderView);
        Objects.requireNonNull(acceptOrderBtmView.getWindow()).setGravity(Gravity.TOP);

        ImageView shopPhnoCallTV = orderView.findViewById(R.id.acceptOrderBtmViewCallToShopBtn_imageView);

        TextView acceptOrderTV = orderView.findViewById(R.id.acceptOrderView_textView);
        TextView declineOrderTV = orderView.findViewById(R.id.declineOrderView_textView);
        TextView shopNameTV = orderView.findViewById(R.id.acceptOrderBtmViewShopname_textView);
        TextView shopPhoneNoTV = orderView.findViewById(R.id.acceptOrderBtmViewShopPhno_textView);
        TextView shopPlaceKmTV = orderView.findViewById(R.id.acceptOrderBtmViewShopPlaceAndKm_textView);
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
            pddTV.setText(MessageFormat.format("PICKUP DISTANCE: (total travel) {0} mtr", pdd * 1000));
        } else {
            pddTV.setText(MessageFormat.format("PICKUP DISTANCE: (total travel) {0} km", pdd));
        }

        shopNameTV.setText(MessageFormat.format("{0}", shopName.toUpperCase(Locale.ROOT)));
        shopPlaceKmTV.setText(shopID);

        if (shopPhone.trim().length() == 10) {
            shopPhoneNoTV.setText(MessageFormat.format("+91 {0}", shopPhone));
            shopPhoneNoTV.setVisibility(View.VISIBLE);
            shopPhnoCallTV.setVisibility(View.VISIBLE);

            shopPhnoCallTV.setOnClickListener(v -> {
                if (shopPhone.trim().length() == 10) {
                    callIntent.setData(Uri.parse("tel:" + shopPhone.trim()));
                    context.startActivity(callIntent);
                } else {
                    Toast.makeText(context, "Invalid Phone Number", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            shopPhoneNoTV.setVisibility(View.INVISIBLE);
            shopPhnoCallTV.setVisibility(View.INVISIBLE);
        }

        deliveryAddressTV.setText(MessageFormat.format("DELIVERY ADDRESS: {0}", deliveryAddress));

        acceptOrderTV.setOnClickListener(v -> {
            acceptOrderTV.setEnabled(false);
            acceptOrderTV.setVisibility(View.GONE);

            declineOrderTV.setEnabled(false);
            declineOrderTV.setVisibility(View.GONE);

            btmViewStatusPB.setVisibility(View.VISIBLE);
            btmViewStatusTV.setText(R.string.please_wait);

            sendOrderAcceptedRequest(adapter, acceptOrderTV, declineOrderTV, btmViewStatusTV, btmViewStatusPB,
                    user.getUid(), userId, orderID, shopID, orderByVoiceDocID, orderByVoiceAudioRefID,
                    shopName, listModel);
        });

        acceptOrderBtmView.show();
    }

    private void showDeclineOrKeepItOrderBtmView(String userId, String orderID,
                                                 String orderByVoiceDocID,
                                                 String orderByVoiceAudioRefID, String shopName) {
        View orderView = LayoutInflater.from(context).inflate(
                R.layout.bottomview_decline_order_confirm, root, false);

        declineOrKeepItOrderBtmView = new BottomSheetDialog(context);
        declineOrKeepItOrderBtmView.setContentView(orderView);
        Objects.requireNonNull(declineOrKeepItOrderBtmView.getWindow()).setGravity(Gravity.TOP);

        TextView declineOrderTV = orderView.findViewById(R.id.btmViewCallConfirmCancelBtn_textView);
        TextView keepItBtn = orderView.findViewById(R.id.btmViewCallConfirmProceedBtn_textView);
        ProgressBar progressBar = orderView.findViewById(R.id.btmViewCallConfirm_progressBar);

        declineOrderTV.setOnClickListener(v -> {
            if (declineOrKeepItOrderBtmView != null && declineOrKeepItOrderBtmView.isShowing()) {
                declineOrKeepItOrderBtmView.hide();
                declineOrKeepItOrderBtmView.dismiss();
            }
            orderDeclineRequest(user.getUid(), userId, orderID, progressBar);
        });

        keepItBtn.setOnClickListener(v -> {
            if (declineOrKeepItOrderBtmView != null && declineOrKeepItOrderBtmView.isShowing()) {
                declineOrKeepItOrderBtmView.hide();
                declineOrKeepItOrderBtmView.dismiss();
            }
            orderKeepItRequest(user.getUid(), userId, orderID, orderByVoiceAudioRefID,
                    shopName, progressBar);
        });

        declineOrKeepItOrderBtmView.show();
    }

    private void orderDeclineRequest(String dpIdd, String userId, String orderID, ProgressBar progressBar) {
        progressBar.setVisibility(View.VISIBLE);

        APIService apiService = ApiServiceGenerator.getApiService(context);
        Call<JsonObject> call = apiService.declineDeliveryOrder(dpIdd, userId, orderID);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@androidx.annotation.NonNull Call<JsonObject> call, @androidx.annotation.NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject jsonObject1 = response.body();
                    String msg = jsonObject1.get("message").getAsString();

                    progressBar.setVisibility(View.INVISIBLE);

                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                } else {
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onFailure(@androidx.annotation.NonNull Call<JsonObject> call, @androidx.annotation.NonNull Throwable t) {
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void orderKeepItRequest(String dBoyID, String userId, String orderID,
                                    String voiceOrderID, String shopName, ProgressBar progressBar) {
        progressBar.setVisibility(View.VISIBLE);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("dBoyID", dBoyID);
        jsonObject.addProperty("orderID", orderID);
        jsonObject.addProperty("userID", userId);
//        jsonObject.addProperty("shopID", shopID);
        jsonObject.addProperty("voiceOrderID", voiceOrderID);
//        jsonObject.addProperty("shopName", shopName);

        APIService apiService = ApiServiceGenerator.getApiService(context);
        Call<JsonObject> call = apiService.keepItDeliveryOrder(dBoyID, userId, orderID);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@androidx.annotation.NonNull Call<JsonObject> call, @androidx.annotation.NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject jsonObject1 = response.body();
                    String msg = jsonObject1.get("message").getAsString();

                    progressBar.setVisibility(View.INVISIBLE);

                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                } else {
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onFailure(@androidx.annotation.NonNull Call<JsonObject> call, @androidx.annotation.NonNull Throwable t) {
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void sendOrderAcceptedRequest(UnifiedOrderAdapter adapter, TextView acceptOrderTV, TextView declineOrderTV, TextView btmViewStatusTV,
                                          ProgressBar btmViewStatusPB, String dpID, String userID, String orderID,
                                          String shopID, String orderByVoiceDocID, String orderByVoiceAudioRefID, String shopName, OrderModel listModel) {

        APIService apiService = ApiServiceGenerator.getApiService(context);
        Call<JsonObject> call3422 = apiService.setCurrentDeliveryOrder(dpID, userID, orderID);

        call3422.enqueue(new Callback<>() {
            @Override
            public void onResponse(@androidx.annotation.NonNull Call<JsonObject> call34, @androidx.annotation.NonNull Response<JsonObject> response) {
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
                            Intent intent = getIntent(adapter, listModel);
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
            public void onFailure(@androidx.annotation.NonNull Call<JsonObject> call, @androidx.annotation.NonNull Throwable t) {
                btmViewStatusTV.setText(R.string.retry_again);

                btmViewStatusPB.setVisibility(View.GONE);
                acceptOrderTV.setEnabled(true);
                acceptOrderTV.setVisibility(View.VISIBLE);

                declineOrderTV.setEnabled(true);
                declineOrderTV.setVisibility(View.VISIBLE);
            }
        });
    }

    private void showNextDeliveryOrderBtmView() {
        View orderView = LayoutInflater.from(context).inflate(
                R.layout.bottomview_next_delivery_dialog, root, false);

        nextDeliveryOrderBtmView = new BottomSheetDialog(context);
        nextDeliveryOrderBtmView.setContentView(orderView);
        Objects.requireNonNull(nextDeliveryOrderBtmView.getWindow()).setGravity(Gravity.TOP);

        nextDeliveryOrderBtmView.show();
    }

}
