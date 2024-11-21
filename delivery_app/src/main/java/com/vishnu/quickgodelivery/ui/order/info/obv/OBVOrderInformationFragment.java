package com.vishnu.quickgodelivery.ui.order.info.obv;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.vishnu.quickgodelivery.R;
import com.vishnu.quickgodelivery.callbacks.ChatID;
import com.vishnu.quickgodelivery.databinding.FragmentObvOrderInformationBinding;
import com.vishnu.quickgodelivery.miscellaneous.Utils;
import com.vishnu.quickgodelivery.server.sapi.APIService;
import com.vishnu.quickgodelivery.server.sapi.ApiServiceGenerator;
import com.vishnu.quickgodelivery.server.ws.ChatClient;
import com.vishnu.quickgodelivery.server.ws.ChatAdapter;
import com.vishnu.quickgodelivery.server.ws.ChatModel;

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


public class OBVOrderInformationFragment extends Fragment {

    private final String LOG_TAG = "OrderInformationFragment";
    FirebaseFirestore db;
    FirebaseUser user;
    TextView callUserIV;
    Button showDeliveryLocOnMapBtn;
    private String orderKey;
    private String userPhno;
    Intent callIntent;
    String receiverPhno;
    String shopID;
    String shopName;
    String shopPhno;
    private String userID;
    SharedPreferences preferences;
    ChatAdapter chatAdapter;
    DecimalFormat decimalFormat = new DecimalFormat("#.#######");
    private View root;
    Button chatSendBtn;
    EditText messageET;
    TextView chatBtmStatusTV;
    TextView chatStatusTV;
    ProgressBar chatStatusPB;
    Button chatFab;
    private RecyclerView recyclerView;
    private List<ChatModel> chatMessageList;
    BottomSheetDialog chatBtmDialogView;
    BottomSheetDialog callConfirmBtmDialogView;
    private ChatClient chatClient;
    BottomSheetDialog allDeliveryOptionsBtmView;
    private String orderType;
    private String orderByVoiceAudioRefID;
    private String orderByVoiceDocID;
    private GridView gridView;
    private SharedPreferences.OnSharedPreferenceChangeListener deliveryPreferenceChangeListener;
    private TextView gridViewStatusTV;
    private TextView statusTV;
    TextView allActionStatusTV;
    ProgressBar allActionsStatusPB;
    private FragmentObvOrderInformationBinding binding;

    public OBVOrderInformationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = getArguments();
        if (arguments != null) {
            userID = arguments.getString("user_id");
            orderKey = arguments.getString("order_id");
            userPhno = arguments.getString("user_phno");
            orderType = arguments.getString("order_by_voice_type");
            shopID = arguments.getString("shop_id");
            shopName = arguments.getString("shop_name");
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
        binding = FragmentObvOrderInformationBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        callUserIV = binding.obvOrderInfoCallOrderReceiverClientTextView;
        showDeliveryLocOnMapBtn = binding.obvOrderInfoShowDeliveryDestnOnMapButton;
        chatFab = binding.obvChatFloatingActionButton;
        gridView = binding.storePrefShopDataGridView;
        gridViewStatusTV = binding.storePrefShopDetailsGridViewStatusTextView;
        statusTV = binding.orderInfoObvStatusTVTextView;

        statusTV.setOnClickListener(v -> {
            fetchData(orderKey, chatId -> {
                if (chatId != null) {
                    // TODO: migrate to base activity
                    chatClient = new ChatClient(requireActivity(), this, user, chatId, chatStatusTV,
                            chatStatusPB, chatBtmStatusTV, messageET, chatSendBtn);
                } else {
                    Toast.makeText(requireContext(), "No channel for chat found!",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });

        statusTV.setVisibility(View.GONE);

        chatMessageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessageList);

        showLoadingUI();

        initChatBtmView((ViewGroup) root);

        fetchData(orderKey, chatId -> {
            if (chatId != null) {
                // TODO: migrate to base activity
                chatClient = new ChatClient(requireActivity(), this, user, chatId, chatStatusTV,
                        chatStatusPB, chatBtmStatusTV, messageET, chatSendBtn);
            } else {
                Toast.makeText(requireContext(), "No channel for chat found!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        callUserIV.setEnabled(false);
        showDeliveryLocOnMapBtn.setEnabled(false);

        callUserIV.setOnClickListener(v -> showConfirmCallBtmView(root, receiverPhno));

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
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        binding.obvAllDeliveryOptionsFloatingActionButton.setOnClickListener(v -> showAllDeliveryOptionsBtmView());
        binding.obvAllDeliveryOptionsFloatingActionButton.setOnLongClickListener(v -> {
            sendOrderDeliveredRequest(preferences, user.getUid(), userID, orderByVoiceDocID, orderByVoiceAudioRefID, orderKey, allDeliveryOptionsBtmView, allActionStatusTV, allActionsStatusPB);
            return false;
        });
        return root;
    }

    @SuppressLint("NotifyDataSetChanged")
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
        TextView clearAllChatBtn = chatView.findViewById(R.id.clearAllChat_textView);

        recyclerView.setItemAnimator(new DefaultItemAnimator());

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(chatAdapter);

        chatSendBtn.setOnClickListener(v -> {
            String message = messageET.getText().toString();
            if (!TextUtils.isEmpty(message)) {
                sendMessage(message);
                messageET.setText("");
            }
        });

        clearAllChatBtn.setOnClickListener(v -> {
            chatMessageList.clear();
            chatAdapter.notifyDataSetChanged();
            chatStatusTV.setTextColor(requireContext().getColor(R.color.wsc_connected));
            chatStatusTV.setText(R.string.ready_to_chat);
        });
    }

    // Method to send a message via WebSocket
    public void sendMessage(String message) {
        if (chatClient != null) {
            String messageTime = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a")).toUpperCase();
            chatClient.sendMessage(message, messageTime);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addMessage(String messageId, String message, String messageTime) {
        if (chatMessageList != null) {
            chatStatusTV.setText("");
            chatMessageList.add(new ChatModel(messageId, message, messageTime, false));
            chatAdapter.notifyItemInserted(chatMessageList.size() - 1);
            if (recyclerView != null) {
                recyclerView.scrollToPosition(chatMessageList.size() - 1);
            }
        }
    }

    public void addSentMessage(String message, String messageTime) {
        chatStatusTV.setText("");
        chatMessageList.add(new ChatModel("Me", message, messageTime, true));
        chatAdapter.notifyItemInserted(chatMessageList.size() - 1);
        if (recyclerView != null) {
            recyclerView.scrollToPosition(chatMessageList.size() - 1);
        }
    }

    private void openGoogleMaps(String destLat, String destLng) {
        Uri gmmIntentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" +
                destLat + "," + destLng + "&travelmode=driving");

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

        if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            this.startActivity(mapIntent);
        } else {
            Toast.makeText(requireContext(), "Unable to start maps", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateButtonStates(Button reachedShopBtn, Button orderPickupBtn, Button orderEnrouteBtn, Button orderDeliveredBtn, CardView orderEnrouteCardView) {
        boolean isReachedShop = preferences.getBoolean("isReachedShop", false);
        boolean isOrderPickedUp = preferences.getBoolean("isOrderPickedUp", false);
        boolean isOrderEnroute = preferences.getBoolean("isOrderEnroute", false);
        boolean isOrderDelivered = preferences.getBoolean("isOrderDelivered", false);

        // Update button states based on preferences
        reachedShopBtn.setEnabled(!isReachedShop);
        orderPickupBtn.setEnabled(isReachedShop && !isOrderPickedUp);
        orderEnrouteBtn.setEnabled(isOrderPickedUp && !isOrderEnroute);
        orderDeliveredBtn.setEnabled(isOrderEnroute && !isOrderDelivered);

//        if (isOrderDelivered) {
//            reachedShopBtn.setEnabled(true);
//            orderPickupBtn.setEnabled(true);
//            orderEnrouteBtn.setEnabled(true);
//            orderDeliveredBtn.setEnabled(true);
//        }

        // Show the amount input only when order pickup is done
        if (preferences.getBoolean("isOrderPickedUp", false) && !preferences.getBoolean("isOrderEnroute", false)) {
            orderEnrouteCardView.setVisibility(View.VISIBLE);
        } else {
            orderEnrouteCardView.setVisibility(View.GONE);
        }
    }

    private void showAllDeliveryOptionsBtmView() {

        View orderView = LayoutInflater.from(requireContext()).inflate(
                R.layout.bottomview_all_delivery_actions, (ViewGroup) root, false);

        allDeliveryOptionsBtmView = new BottomSheetDialog(requireContext());
        allDeliveryOptionsBtmView.setContentView(orderView);
        allDeliveryOptionsBtmView.setCanceledOnTouchOutside(false);
        Objects.requireNonNull(allDeliveryOptionsBtmView.getWindow()).setGravity(Gravity.TOP);

        Button reachedShopBtn = orderView.findViewById(R.id.reachedShop_button);
        Button orderPickupBtn = orderView.findViewById(R.id.orderPickup_button);
        Button orderDeliveredBtn = orderView.findViewById(R.id.orderDelivered_button);
        allActionStatusTV = orderView.findViewById(R.id.allDeliveryActionsStatusView_textView);
        allActionsStatusPB = orderView.findViewById(R.id.allDeliveryActionsStatusPB_progressBar);
        Button orderEnrouteBtn = orderView.findViewById(R.id.orderEnroute_button);
        CardView orderEnrouteCardView = orderView.findViewById(R.id.orderEnroute_cardView);
        EditText amountEditText = orderView.findViewById(R.id.totalAmount_editTextNumberDecimal);

        // Initial button states based on preferences
        updateButtonStates(reachedShopBtn, orderPickupBtn, orderEnrouteBtn, orderDeliveredBtn, orderEnrouteCardView);

        // Set up the SharedPreference change listener
        deliveryPreferenceChangeListener = (sharedPreferences, key) -> {
            assert key != null;
            if (key.equals("isReachedShop") || key.equals("isOrderPickedUp") || key.equals("isOrderEnroute") || key.equals("isOrderDelivered")) {
                updateButtonStates(reachedShopBtn, orderPickupBtn, orderEnrouteBtn, orderDeliveredBtn, orderEnrouteCardView);
            }
        };

        preferences.registerOnSharedPreferenceChangeListener(deliveryPreferenceChangeListener);

        // Check if amount is entered before enabling Order Enroute button
        amountEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                orderEnrouteBtn.setEnabled(!s.toString().trim().isEmpty());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        // Button click listeners
        reachedShopBtn.setOnClickListener(v -> {
            if (Utils.isNetworkConnected(requireContext())) {
                reachedShopBtn.setEnabled(false);
                allActionStatusTV.setText(R.string.please_wait);
                allActionsStatusPB.setVisibility(View.VISIBLE);
                preferences.edit().putBoolean("isReachedShop", true).apply();
                sendReachedShopRequest(user.getUid(), userID, orderKey, reachedShopBtn,
                        allDeliveryOptionsBtmView, allActionStatusTV, allActionsStatusPB);
            } else {
                Toast.makeText(requireContext(), "No internet connection.", Toast.LENGTH_SHORT).show();
            }
        });

        orderPickupBtn.setOnClickListener(v -> {
            if (Utils.isNetworkConnected(requireContext())) {
                orderPickupBtn.setEnabled(false);
                allActionStatusTV.setText(R.string.please_wait);
                allActionsStatusPB.setVisibility(View.VISIBLE);
                sendOrderPickedUpRequest(user.getUid(), userID, orderKey, orderPickupBtn,
                        allDeliveryOptionsBtmView, allActionStatusTV, allActionsStatusPB);
            } else {
                Toast.makeText(requireContext(), "No internet connection.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        orderEnrouteBtn.setOnClickListener(v -> {
            if (Utils.isNetworkConnected(requireContext())) {
                if (amountEditText.length() == 0) {
                    Toast.makeText(requireContext(), "Enter the total amount", Toast.LENGTH_SHORT).show();
                } else {
                    orderEnrouteBtn.setEnabled(false);
                    allActionStatusTV.setText(R.string.please_wait);
                    allActionsStatusPB.setVisibility(View.VISIBLE);
                    preferences.edit().putBoolean("isOrderEnroute", true).apply();
                    preferences.edit().putBoolean("isOrderDelivered", false).apply();

                    sendOrderEnrouteRequest(user.getUid(), userID, orderKey, orderEnrouteBtn,
                            allDeliveryOptionsBtmView, allActionStatusTV, allActionsStatusPB);
                }
            } else {
                Toast.makeText(requireContext(), "No internet connection.", Toast.LENGTH_SHORT).show();
            }
        });

        orderDeliveredBtn.setOnClickListener(v -> {
            if (Utils.isNetworkConnected(requireContext())) {
                allActionStatusTV.setText(R.string.please_wait);
                allActionsStatusPB.setVisibility(View.VISIBLE);
                sendOrderDeliveredRequest(preferences, user.getUid(), userID, orderByVoiceDocID,
                        orderByVoiceAudioRefID, orderKey, allDeliveryOptionsBtmView,
                        allActionStatusTV, allActionsStatusPB);
            } else {
                Toast.makeText(requireContext(), "No internet connection.",
                        Toast.LENGTH_SHORT).show();
            }
        });


        allDeliveryOptionsBtmView.show();
    }


    private void fetchData(String key, ChatID chatID) {
        statusTV.setVisibility(View.GONE);
        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call = apiService.fetchOrderData(orderType, userID, user.getUid(), shopID, shopName, userPhno, key);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    handleResponse(response.body(), chatID);
                } else {
                    binding.obvOrderInfoLoadingPBProgressBar.setVisibility(View.GONE);
                    logError("Response is not successful or body is null.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                logError("Error fetching document: " + t.getMessage());
                statusTV.setVisibility(View.VISIBLE);
                statusTV.setText(R.string.unable_to_fetch_data_at_the_moment);
                binding.obvOrderInfoLoadingPBProgressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Network error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleResponse(@NonNull JsonObject responseBody, ChatID chatID) {
        String status = responseBody.has("status") ? responseBody.get("status").getAsString() : "error";

        if ("success".equals(status)) {
            JsonObject data = responseBody.has("data") ? responseBody.get("data").getAsJsonObject() : null;
            String responseType = responseBody.has("order_type") ? responseBody.get("order_type").getAsString() : "";

            if (data != null) {
                if ("obv".equals(responseType)) {
                    parseData(data, chatID);
                } else {
                    logError("Unknown order type: " + responseType);
                }
            } else {
                logError("Data field is null.");
            }
        } else {
            logError("Error status or message: " + responseBody.get("message").getAsString());
        }
    }

    private void logError(String message) {
        Log.d(LOG_TAG, message);
    }


    private void parseData(@NonNull JsonObject data, @NonNull ChatID chatID) {
        JsonObject deliveryLocJson = data.has("delivery_address_loc") ? data.getAsJsonObject("delivery_address_loc") : null;

        setupGridView(data);

        GeoPoint deliveryLoc = getDeliveryLocation(deliveryLocJson);

        String orderId = data.has("order_id") ? data.get("order_id").getAsString() : null;
        chatID.setChatId(orderId);

        receiverPhno = data.has("user_phno") ? data.get("user_phno").getAsString() : null;
        shopPhno = data.has("shop_phno") ? data.get("shop_phno").getAsString() : null;

        handleUserPhone(receiverPhno);
        setupDeliveryLocationButton(deliveryLoc);

        String fullAddress = data.has("delivery_full_address") ? data.get("delivery_full_address").getAsString() : "";
        binding.obvobvOrderInfoReceiverFullAddressViewTextView.setText(fullAddress);

        showAllUI();
    }

    private void setupGridView(JsonObject data) {
        if (gridView.getAdapter() != null) {
            gridView.setAdapter(null);
        }

        if (data.has("store_pref_data")) {
            JsonArray shopData = data.get("store_pref_data").getAsJsonArray();

            StorePrefAdapter adapter = new StorePrefAdapter(requireContext(), getShops(shopData));
            gridView.setAdapter(adapter);

            gridViewStatusTV.setVisibility(View.GONE);
        } else {
            gridViewStatusTV.setVisibility(View.VISIBLE);
            gridViewStatusTV.setText(R.string.no_store_preference_details_found);
        }
    }

    private GeoPoint getDeliveryLocation(JsonObject deliveryLocJson) {
        if (deliveryLocJson != null) {
            double deliveryLat = deliveryLocJson.has("latitude") ? deliveryLocJson.get("latitude").getAsDouble() : 0;
            double deliveryLng = deliveryLocJson.has("longitude") ? deliveryLocJson.get("longitude").getAsDouble() : 0;
            return new GeoPoint(deliveryLat, deliveryLng);
        } else {
            return null;
        }
    }

    private void handleUserPhone(String phone) {
        if (phone != null && phone.length() == 10) {
            callUserIV.setEnabled(true);
        } else {
            callUserIV.setEnabled(false);
            Toast.makeText(requireContext(), "Invalid user phno!", Toast.LENGTH_LONG).show();
        }
    }

    private void setupDeliveryLocationButton(GeoPoint deliveryLoc) {
        if (deliveryLoc != null) {
            showDeliveryLocOnMapBtn.setEnabled(true);

            showDeliveryLocOnMapBtn.setOnClickListener(v -> {
                Toast.makeText(requireContext(), deliveryLoc.getLatitude() + "", Toast.LENGTH_SHORT).show();
                openGoogleMaps(decimalFormat.format(deliveryLoc.getLatitude()),
                        decimalFormat.format(deliveryLoc.getLongitude()));
            });
        }
    }


    private @NonNull List<StorePrefDataModel> getShops(@NonNull JsonArray shopData) {
        List<StorePrefDataModel> storePrefDataModelList = new ArrayList<>();

        for (JsonElement element : shopData) {
            JsonObject shopObject = element.getAsJsonObject();

            StorePrefDataModel storePrefDataModel = new StorePrefDataModel(
                    shopObject.get("shop_district").getAsString(),
                    shopObject.get("shop_street").getAsString(),
                    shopObject.get("shop_id").getAsString(),
                    shopObject.get("shop_email").getAsString(),
                    shopObject.get("shop_phone").getAsString(),
                    shopObject.get("shop_image_url").getAsString(),
                    shopObject.get("shop_pincode").getAsString(),
                    shopObject.get("shop_lat").getAsDouble(),
                    shopObject.get("shop_lon").getAsDouble(),
                    shopObject.get("shop_state").getAsString(),
                    shopObject.get("shop_name").getAsString(),
                    shopObject.get("shop_address").getAsString(),
                    1, shopObject.get("distance_km").getAsDouble(),
                    shopObject.get("displacement").getAsDouble()
            );

            storePrefDataModelList.add(storePrefDataModel);
//            Toast.makeText(requireContext(), shopObject.get("shop_id").getAsString(), Toast.LENGTH_SHORT).show();
        }
        return storePrefDataModelList;
    }

    private void sendReachedShopRequest(String dpID, String userID, String orderID,
                                        Button orderPickupBtn,
                                        BottomSheetDialog allDeliveryOptionsBtmView,
                                        TextView allActionStatusTV, ProgressBar allActionsStatusPB) {

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call3480 = apiService.reachedShop(dpID, userID, orderID);

        call3480.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call34, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null && !response.body().isEmpty()) {
                        if (response.body().get("response_status").getAsBoolean()) {
                            allActionStatusTV.setText("");
                            allActionsStatusPB.setVisibility(View.GONE);
                            orderPickupBtn.setEnabled(false);
                            Toast.makeText(requireContext(), response.body().get("message").getAsString(), Toast.LENGTH_SHORT).show();
                            preferences.edit().putBoolean("isReachedShop", true).apply();
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

    private void sendOrderEnrouteRequest(String dpID, String userID, String orderID,
                                         Button orderPickupBtn,
                                         BottomSheetDialog allDeliveryOptionsBtmView,
                                         TextView allActionStatusTV, ProgressBar allActionsStatusPB) {

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call3480 = apiService.orderEnroute(dpID, userID, orderID);

        call3480.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call34, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null && !response.body().isEmpty()) {
                        if (response.body().get("response_status").getAsBoolean()) {
                            allActionStatusTV.setText("");
                            allActionsStatusPB.setVisibility(View.GONE);
                            orderPickupBtn.setEnabled(false);
                            Toast.makeText(requireContext(), response.body().get("message").getAsString(), Toast.LENGTH_SHORT).show();
                            preferences.edit().putBoolean("isOrderEnroute", true).apply();
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
                            Toast.makeText(requireContext(), response.body().get("message").getAsString(), Toast.LENGTH_SHORT).show();
                            preferences.edit().putBoolean("isOrderPickedUp", true).apply();
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
                            preferences.edit().putBoolean("isReachedShop", false).apply();
                            preferences.edit().putBoolean("isOrderPickedUp", false).apply();
                            preferences.edit().putBoolean("isOrderEnroute", false).apply();
                            preferences.edit().putBoolean("isOrderDelivered", true).apply();
                            if (allDeliveryOptionsBtmView != null) {
                                allDeliveryOptionsBtmView.hide();
                                allDeliveryOptionsBtmView.dismiss();
                                allActionStatusTV.setText("");
                                allActionsStatusPB.setVisibility(View.GONE);
                            }
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

    private void showAllUI() {
        binding.obvOrderInfoLoadingPBProgressBar.setVisibility(View.GONE);
        binding.textView9.setVisibility(View.VISIBLE);
        binding.obvOrderInfoCallOrderReceiverClientTextView.setVisibility(View.VISIBLE);
        binding.textView7.setVisibility(View.VISIBLE);
        binding.obvobvOrderInfoReceiverFullAddressViewTextView.setVisibility(View.VISIBLE);
        showDeliveryLocOnMapBtn.setVisibility(View.VISIBLE);
        binding.obvChatFloatingActionButton.setVisibility(View.VISIBLE);
        binding.obvAllDeliveryOptionsFloatingActionButton.setVisibility(View.VISIBLE);
    }

    private void showLoadingUI() {
        binding.obvOrderInfoLoadingPBProgressBar.setVisibility(View.VISIBLE);
        binding.obvOrderInfoCallOrderReceiverClientTextView.setVisibility(View.GONE);
        binding.textView9.setVisibility(View.GONE);
        binding.textView7.setVisibility(View.GONE);
        binding.obvobvOrderInfoReceiverFullAddressViewTextView.setVisibility(View.GONE);
        showDeliveryLocOnMapBtn.setVisibility(View.GONE);
        binding.obvChatFloatingActionButton.setVisibility(View.GONE);
        binding.obvAllDeliveryOptionsFloatingActionButton.setVisibility(View.GONE);
    }


    private void showConfirmCallBtmView(View root, String phno) {
        View chatView = LayoutInflater.from(requireContext()).inflate(
                R.layout.bottomview_call_confirmation, (ViewGroup) root, false);

        callConfirmBtmDialogView = new BottomSheetDialog(requireContext());
        callConfirmBtmDialogView.setContentView(chatView);
        Objects.requireNonNull(callConfirmBtmDialogView.getWindow()).setGravity(Gravity.TOP);

        TextView proceedBtn = chatView.findViewById(R.id.btmViewCallConfirmProceedBtn_textView);
        TextView cancelBtn = chatView.findViewById(R.id.btmViewCallConfirmCancelBtn_textView);
        TextView subTitle = chatView.findViewById(R.id.btmViewSubTitleCallConfirm_textView);
        ProgressBar progressBar = chatView.findViewById(R.id.btmViewCallConfirm_progressBar);
        progressBar.setVisibility(View.GONE);

        subTitle.setText(MessageFormat.format("Are you sure you want to call +91 {0}?" +
                "This action will use your phone''s call functionality, " +
                "applies standard carrier charges.", phno));

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

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Close WebSocket connection when activity is destroyed
        if (chatClient != null) {
            chatClient.webSocket.close(1000, "client disconnected");
            chatBtmDialogView.dismiss();
            chatBtmDialogView = null;
        }

        if (allDeliveryOptionsBtmView != null) {
            allDeliveryOptionsBtmView.hide();
            allDeliveryOptionsBtmView.dismiss();
        }
    }

}