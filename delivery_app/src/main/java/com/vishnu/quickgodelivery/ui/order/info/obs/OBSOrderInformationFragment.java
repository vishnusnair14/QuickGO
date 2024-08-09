package com.vishnu.quickgodelivery.ui.order.info.obs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.JsonObject;
import com.vishnu.quickgodelivery.R;
import com.vishnu.quickgodelivery.callbacks.ChatID;
import com.vishnu.quickgodelivery.databinding.FragmentOrderInformationObsBinding;
import com.vishnu.quickgodelivery.miscellaneous.Utils;
import com.vishnu.quickgodelivery.server.APIService;
import com.vishnu.quickgodelivery.server.ApiServiceGenerator;
import com.vishnu.quickgodelivery.server.ChatWSC;
import com.vishnu.quickgodelivery.server.MessageAdapter;
import com.vishnu.quickgodelivery.server.MessageModel;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class OBSOrderInformationFragment extends Fragment {
    private final String LOG_TAG = "OrderInformationFragment";
    FirebaseFirestore db;
    FirebaseUser user;
    TextView callUserIV;
    TextView callShopIV;
    Button showDeliveryLocOnMapBtn;
    Button showShopLocOnMapBtn;
    private String orderKey;
    private String shopID;
    Intent callIntent;
    String receiverPhno;
    private String shopPhno;
    private String userID;
    private String userPhno;
    SharedPreferences preferences;
    MessageAdapter messageAdapter;
    FragmentOrderInformationObsBinding binding;
    DecimalFormat decimalFormat = new DecimalFormat("#.#######");
    private View root;
    Button chatSendBtn;
    EditText messageET;
    TextView chatBtmStatusTV;
    TextView chatStatusTV;
    ProgressBar chatStatusPB;
    Button chatFab;
    private RecyclerView recyclerView;
    private List<MessageModel> messageList;
    BottomSheetDialog chatBtmDialogView;
    BottomSheetDialog callConfirmBtmDialogView;
    private ChatWSC chatWSC;
    BottomSheetDialog allDeliveryOptionsBtmView;
    private String orderType;
    private String orderByVoiceAudioRefID;
    private String orderByVoiceDocID;

    public OBSOrderInformationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            userID = arguments.getString("user_id");
            userPhno = arguments.getString("user_phno");
            orderKey = arguments.getString("order_id");
            shopID = arguments.getString("shop_id");
            orderType = arguments.getString("order_by_voice_type");
            orderByVoiceDocID = arguments.getString("order_by_voice_doc_id");
            orderByVoiceAudioRefID = arguments.getString("order_by_voice_audio_ref_id");
        }

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        preferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        callIntent = new Intent(Intent.ACTION_CALL);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentOrderInformationObsBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        callUserIV = binding.callOrderReceiverClientTextView;
        callShopIV = binding.callShopClientTextView;
        showDeliveryLocOnMapBtn = binding.showDeliveryDestnOnMapButton;
        showShopLocOnMapBtn = binding.showShopDestnOnMapButton;
        chatFab = binding.chatFloatingActionButton;

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        messageAdapter = new MessageAdapter(messageList);

        showLoadingUI();

        initChatBtmView((ViewGroup) root);

        fetchData(orderKey, chatId -> {
            if (chatId != null) {
                // TODO: migrate to base activity
                chatWSC = new ChatWSC(requireActivity(), this, user, chatId, chatStatusTV,
                        chatStatusPB, chatBtmStatusTV, messageET, chatSendBtn);
            } else {
                Toast.makeText(requireContext(), "No channel for chat found!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        callUserIV.setEnabled(false);
        callShopIV.setEnabled(false);
        showDeliveryLocOnMapBtn.setEnabled(false);

        callUserIV.setOnClickListener(v -> showConfirmCallBtmView(root, receiverPhno));

        callShopIV.setOnClickListener(v -> showConfirmCallBtmView(root, shopPhno));

        chatFab.setOnClickListener(v -> {
            if (getActivity() == null || getActivity().isFinishing()) {
                Log.d(LOG_TAG, " Avoid showing dialog if the activity is not in a valid state");
                return;
            }

            if (chatBtmDialogView != null && !chatBtmDialogView.isShowing()) {
                chatBtmDialogView.show();
            } else {
                initChatBtmView((ViewGroup) root);
                if (chatBtmDialogView != null && !chatBtmDialogView.isShowing()) {
                    chatBtmDialogView.show();
                } else {
                    Toast.makeText(requireContext(), "Unable to load chat at the moment!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.allDeliveryOptionsFloatingActionButton.setOnClickListener(v -> showAllDeliveryOptionsBtmView());

        return root;
    }

    private void initChatBtmView(ViewGroup root) {
        View chatView = LayoutInflater.from(requireContext()).inflate(
                R.layout.bottomview_chat_with_partner, root, false);

        chatBtmDialogView = new BottomSheetDialog(requireContext());
        chatBtmDialogView.setContentView(chatView);
        chatBtmDialogView.setCanceledOnTouchOutside(false);
        Objects.requireNonNull(chatBtmDialogView.getWindow()).setGravity(Gravity.TOP);

        messageET = chatView.findViewById(R.id.chatField_editTextText);
        chatSendBtn = chatView.findViewById(R.id.chatSendButton_button);
        recyclerView = chatView.findViewById(R.id.chatMessages_recyclerView);
        chatBtmStatusTV = chatView.findViewById(R.id.chatViewBtmStatusTV_textView);
        chatStatusTV = chatView.findViewById(R.id.chatViewStatusTV_textView);
        chatStatusPB = chatView.findViewById(R.id.chatView_progressBar);

        recyclerView.setItemAnimator(new DefaultItemAnimator());

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(messageAdapter);

        chatSendBtn.setOnClickListener(v -> {
            String message = messageET.getText().toString();
            if (!TextUtils.isEmpty(message)) {
                sendMessage(message);
                messageET.setText("");
            }
        });
    }

    private void showConfirmCallBtmView(View root, String phno) {
        View chatView = LayoutInflater.from(requireContext()).inflate(
                R.layout.bottomview_call_confirmation, (ViewGroup) root, false);

        callConfirmBtmDialogView = new BottomSheetDialog(requireContext());
        callConfirmBtmDialogView.setContentView(chatView);
        callConfirmBtmDialogView.setCanceledOnTouchOutside(false);
        Objects.requireNonNull(callConfirmBtmDialogView.getWindow()).setGravity(Gravity.TOP);


        TextView proceedBtn = chatView.findViewById(R.id.btmViewCallConfirmProceedBtn_textView);
        TextView cancelBtn = chatView.findViewById(R.id.btmViewCallConfirmCancelBtn_textView);
        TextView subTitle = chatView.findViewById(R.id.btmViewSubTitleCallConfirm_textView);
        ProgressBar progressBar = chatView.findViewById(R.id.btmViewCallConfirm_progressBar);
        progressBar.setVisibility(View.GONE);

        subTitle.setText(MessageFormat.format("Are you sure you want to call +91{0}?" +
                "\nThis action will use your phone''s call functionality, " +
                "also applies standard carrier charges.", phno));

        proceedBtn.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> {
                if (phno.length() == 10) {
                    callIntent.setData(Uri.parse("tel: +91" + phno));
                    startActivity(callIntent);
                    progressBar.setVisibility(View.GONE);
                    callConfirmBtmDialogView.hide();
                    callConfirmBtmDialogView.dismiss();
                }
            }, 1000);

        });

        cancelBtn.setOnClickListener(v -> {
            callConfirmBtmDialogView.hide();
            callConfirmBtmDialogView.dismiss();
        });

        callConfirmBtmDialogView.show();
    }

    // Method to send a message via WebSocket
    public void sendMessage(String message) {
        if (chatWSC != null) {
            String messageTime = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")).toUpperCase();
            chatWSC.sendMessage(message, messageTime);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addMessage(String messageId, String message, String messageTime) {
        if (messageList != null) {
            messageList.add(new MessageModel(messageId, message, messageTime, false));
            messageAdapter.notifyItemInserted(messageList.size() - 1);
            if (recyclerView != null) {
                recyclerView.scrollToPosition(messageList.size() - 1);
            }
        }
    }

    public void addSentMessage(String message, String messageTime) {
        messageList.add(new MessageModel("Me", message, messageTime, true));
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        if (recyclerView != null) {
            recyclerView.scrollToPosition(messageList.size() - 1);
        }
    }

    private void openGoogleMaps(String destLat, String destLng) {
        Uri gmmIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" +
                destLat + "," + destLng + "&travelmode=driving");

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        }
    }

    private void showAllUI() {
        binding.loadingPBProgressBar.setVisibility(View.GONE);
        binding.textView9.setVisibility(View.VISIBLE);
        binding.callOrderReceiverClientTextView.setVisibility(View.VISIBLE);
        binding.callShopClientTextView.setVisibility(View.VISIBLE);
        binding.textView7.setVisibility(View.VISIBLE);
        binding.separator.setVisibility(View.VISIBLE);
        binding.cardView1.setVisibility(View.VISIBLE);
        binding.showDeliveryDestnOnMapButton.setVisibility(View.VISIBLE);
        binding.cardView3.setVisibility(View.VISIBLE);
        binding.showShopDestnOnMapButton.setVisibility(View.VISIBLE);
    }

    private void showLoadingUI() {
        binding.loadingPBProgressBar.setVisibility(View.VISIBLE);
        binding.callOrderReceiverClientTextView.setVisibility(View.GONE);
        binding.callShopClientTextView.setVisibility(View.GONE);
        binding.textView9.setVisibility(View.GONE);
        binding.textView7.setVisibility(View.GONE);
        binding.separator.setVisibility(View.GONE);
        binding.cardView1.setVisibility(View.GONE);
        binding.showDeliveryDestnOnMapButton.setVisibility(View.GONE);
        binding.cardView3.setVisibility(View.GONE);
        binding.showShopDestnOnMapButton.setVisibility(View.GONE);
    }

    private void showAllDeliveryOptionsBtmView() {

        View orderView = LayoutInflater.from(requireContext()).inflate(
                R.layout.bottomview_all_delivery_actions, (ViewGroup) root, false);

        allDeliveryOptionsBtmView = new BottomSheetDialog(requireContext());
        allDeliveryOptionsBtmView.setContentView(orderView);
        allDeliveryOptionsBtmView.setCanceledOnTouchOutside(false);
        Objects.requireNonNull(allDeliveryOptionsBtmView.getWindow()).setGravity(Gravity.TOP);

        Button orderPickupBtn = orderView.findViewById(R.id.orderPickup_button);
        Button orderDeliveredBtn = orderView.findViewById(R.id.orderDelivered_button);
        TextView allActionStatusTV = orderView.findViewById(R.id.allDeliveryActionsStatusView_textView);
        ProgressBar allActionsStatusPB = orderView.findViewById(R.id.allDeliveryActionsStatusPB_progressBar);

        orderPickupBtn.setOnClickListener(v -> {
            orderPickupBtn.setEnabled(false);
            allActionStatusTV.setText(R.string.please_wait);
            allActionsStatusPB.setVisibility(View.VISIBLE);

            if (Utils.isNetworkConnected(requireContext())) {
                sendOrderPickedUpRequest(user.getUid(), userID, orderKey, orderPickupBtn,
                        allDeliveryOptionsBtmView, allActionStatusTV, allActionsStatusPB);
            } else {
                Toast.makeText(requireContext(), "No internet connection.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        orderDeliveredBtn.setOnClickListener(v -> {
            allActionStatusTV.setText(R.string.please_wait);
            allActionsStatusPB.setVisibility(View.VISIBLE);
            sendOrderDeliveredRequest(preferences, user.getUid(), userID, orderByVoiceDocID,
                    orderByVoiceAudioRefID, orderKey, allDeliveryOptionsBtmView,
                    allActionStatusTV, allActionsStatusPB);
        });

        allDeliveryOptionsBtmView.show();
    }

    private void parseData(JsonObject data, ChatID chatID) {

        // Parsing logic specific to 'obs' response
        JsonObject deliveryLocJson = data.has("delivery_address_loc") ?
                data.getAsJsonObject("delivery_address_loc") : null;
        JsonObject shopLocJson = data.has("shop_loc") ?
                data.getAsJsonObject("shop_loc") : null;

        GeoPoint deliveryLoc;
        if (deliveryLocJson != null) {
            double deliveryLat = deliveryLocJson.has("latitude") ?
                    deliveryLocJson.get("latitude").getAsDouble() : 0;
            double deliveryLng = deliveryLocJson.has("longitude") ?
                    deliveryLocJson.get("longitude").getAsDouble() : 0;
            deliveryLoc = new GeoPoint(deliveryLat, deliveryLng);
        } else {
            deliveryLoc = null;
        }

        GeoPoint shopLoc;
        if (shopLocJson != null) {
            double shopLat = shopLocJson.has("latitude") ?
                    shopLocJson.get("latitude").getAsDouble() : 0;
            double shopLng = shopLocJson.has("longitude") ?
                    shopLocJson.get("longitude").getAsDouble() : 0;
            shopLoc = new GeoPoint(shopLat, shopLng);
        } else {
            shopLoc = null;
        }

        receiverPhno = data.has("user_phno") ?
                data.get("user_phno").getAsString() : null;
        shopPhno = data.has("shop_phno") ?
                data.get("shop_phno").getAsString() : null;
        String orderId = data.has("order_id") ?
                data.get("order_id").getAsString() : null;

        chatID.setChatId(orderId);

        if (receiverPhno != null && receiverPhno.length() == 10) {
            callUserIV.setEnabled(true);
        } else {
            callUserIV.setEnabled(false);
            Toast.makeText(requireContext(), "Invalid user phno!", Toast.LENGTH_LONG).show();
        }

        if (shopPhno != null && shopPhno.length() == 10) {
            callShopIV.setEnabled(true);
        } else {
            callShopIV.setEnabled(false);
            Toast.makeText(requireContext(), "Invalid shop phno!", Toast.LENGTH_LONG).show();
        }

        if (shopLoc != null) {
            showShopLocOnMapBtn.setText(R.string.show_on_map);
            showShopLocOnMapBtn.setEnabled(true);
            showShopLocOnMapBtn.setOnClickListener(v ->
                    openGoogleMaps(decimalFormat.format(shopLoc.getLatitude()),
                            decimalFormat.format(shopLoc.getLongitude())));
        }

        if (deliveryLoc != null) {
            showDeliveryLocOnMapBtn.setText(R.string.show_on_map);
            showDeliveryLocOnMapBtn.setEnabled(true);
            showDeliveryLocOnMapBtn.setOnClickListener(v ->
                    openGoogleMaps(decimalFormat.format(deliveryLoc.getLatitude()),
                            decimalFormat.format(deliveryLoc.getLongitude())));
        }

        binding.orderInfoShopNameViewTextView.setText(MessageFormat.format(
                "{0}\n{1}, {2}\n\nOrder ID: {3}\nOrder time: {4}\nShop distance: {5} km",
                data.get("shop_name").getAsString().toUpperCase(),
                data.get("shop_street").getAsString(), data.get("shop_district").getAsString(),
                data.get("order_id").getAsString().substring(6),
                data.get("order_time").getAsString(), data.get("pickup_destination_distance").getAsBigDecimal()));

        binding.orderInfoReceiverFullAddressViewTextView.setText(
                MessageFormat.format("{0}",
                        data.get("delivery_full_address").getAsString()));
        showAllUI();
    }

    private void fetchData(String key, ChatID chatID) {

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call = apiService.fetchOrderData(orderType, userID, user.getUid(), userPhno, key);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();
                    String status = responseBody.has("status") ?
                            responseBody.get("status").getAsString() : "error";

                    if ("success".equals(status)) {
                        JsonObject data = responseBody.has("data") ?
                                responseBody.get("data").getAsJsonObject() : null;

                        String responseType = responseBody.has("order_type") ?
                                responseBody.get("order_type").getAsString() : "";

                        if (data != null) {
                            Log.d(LOG_TAG, data + "");

                            if ("obs".equals(responseType)) {
                                parseData(data, chatID);
                            } else {
                                Log.d(LOG_TAG, "Unknown order type: " + responseType);
                            }
                        } else {
                            Log.d(LOG_TAG, "Data field is null.");
                        }
                    } else {
                        Log.d(LOG_TAG, "Error status or message: " +
                                responseBody.get("message").getAsString());
                    }
                } else {
                    Log.d(LOG_TAG, "Response is not successful or body is null.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.d(LOG_TAG, "Error fetching document: " + t.getMessage());
                Toast.makeText(requireContext(), "Network error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendOrderPickedUpRequest(String dpID, String userID, String orderID,
                                          Button orderPickupBtn,
                                          BottomSheetDialog allDeliveryOptionsBtmView,
                                          TextView allActionStatusTV, ProgressBar allActionsStatusPB) {

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call3480 = apiService.orderPickedUp(dpID, userID, orderID);

        call3480.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call34, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null && !response.body().isEmpty()) {
                        if (response.body().get("response_status").getAsBoolean()) {
                            allActionStatusTV.setText("");
                            allActionsStatusPB.setVisibility(View.GONE);
                            orderPickupBtn.setEnabled(false);
                            Toast.makeText(requireContext(), "Informed client success",
                                    Toast.LENGTH_SHORT).show();
                            allDeliveryOptionsBtmView.hide();
                            allDeliveryOptionsBtmView.dismiss();
                        } else {
                            orderPickupBtn.setEnabled(true);
                            allActionStatusTV.setText("");
                            allActionsStatusPB.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), response.body().get("message").getAsString(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {

            }
        });
    }

    private void sendOrderDeliveredRequest(SharedPreferences preferences, String dpID, String
            userID, String orderByVoiceDocID, String orderByVoiceAudioRefID,
                                           String orderID, BottomSheetDialog allDeliveryOptionsBtmView,
                                           TextView allActionStatusTV, ProgressBar allActionsStatusPB) {

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call3480 = apiService.orderDelivered(dpID, userID, orderByVoiceDocID,
                orderByVoiceAudioRefID, orderID);

        call3480.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call34, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null && !response.body().isEmpty()) {
                        if (response.body().get("response_status").getAsBoolean()) {
                            preferences.edit().putString("currentDeliveryOrderID", "0").apply();
                            Toast.makeText(requireContext(), response.body().get("message").getAsString(), Toast.LENGTH_SHORT).show();
                            allDeliveryOptionsBtmView.hide();
                            allDeliveryOptionsBtmView.dismiss();
                            allActionStatusTV.setText("");
                            allActionsStatusPB.setVisibility(View.GONE);
                            requireActivity().onBackPressed();
                        } else {
                            if (response.body().has("message")) {
                                Toast.makeText(requireContext(), response.body().get("message").getAsString(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Close WebSocket connection when activity is destroyed
        if (chatWSC != null) {
            chatWSC.webSocket.close(1000, "client disconnected");
            chatBtmDialogView.dismiss();
            chatBtmDialogView = null;
        }

        if (allDeliveryOptionsBtmView != null) {
            allDeliveryOptionsBtmView.hide();
            allDeliveryOptionsBtmView.dismiss();
        }
    }

}