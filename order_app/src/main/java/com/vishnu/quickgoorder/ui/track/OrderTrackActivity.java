package com.vishnu.quickgoorder.ui.track;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.vishnu.quickgoorder.MainActivity;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.miscellaneous.Utils;
import com.vishnu.quickgoorder.server.sse.SSEClient;
import com.vishnu.quickgoorder.server.sse.SSEModel;
import com.vishnu.quickgoorder.server.ws.ChatAdapter;
import com.vishnu.quickgoorder.server.ws.ChatClient;
import com.vishnu.quickgoorder.server.ws.ChatModel;
import com.vishnu.quickgoorder.server.ws.DeliveryPartnerStatusListener;
import com.vishnu.quickgoorder.ui.track.orderstatus.OrderStatusAdapter;

import java.text.MessageFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class OrderTrackActivity extends AppCompatActivity implements DeliveryPartnerStatusListener {
    private final String LOG_TAG = "OrderTrackActivity";
    private FirebaseFirestore db;
    private FirebaseUser user;
    TextView orderIDTV;
    TextView orderTimeTV;
    TextView updatedTimeTV;
    Button chatSendBtn;
    EditText messageET;
    TextView dropDownIV;
    private CardView orderDetailsCardView;
    private LinearLayout orderStatusUpdateLayout;
    FloatingActionButton chatFab;
    private boolean isAnimationShown = false;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatModel> chatMessageList;
    BottomSheetDialog setDeliveryAddrBtmView;
    private ChatClient chatClient;
    private RecyclerView orderStatusRecyclerView;
    private TextView chatViewStatusTV;
    private TextView sseViewTV;
    private OrderStatusAdapter orderStatusAdapter;
    private SSEClient sseClient;
    private List<Map<String, String>> statusList = new ArrayList<>();
    TextView orderStatusTV;
    private String orderToTrackOrderID;
    ImageView[] statusIconViews;
    ProgressBar[] statusProgressBarViews;
    private int orderStatusNo = 0;
    private boolean isOrderStatusCardViewExpanded = false;
    private boolean isDeliveryPartnerAssigned = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_track);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        MapBottomSheetFragment mapFragment = new MapBottomSheetFragment();

        orderIDTV = findViewById(R.id.orderTrackOrderIDView_textView);
        orderTimeTV = findViewById(R.id.orderTrackOrderTimeView_textView);
        updatedTimeTV = findViewById(R.id.orderTrackOrderUpdatedTimeView_textView);
        chatFab = findViewById(R.id.chat_floatingActionButton);
        sseViewTV = findViewById(R.id.sseView_textView);
        orderDetailsCardView = findViewById(R.id.trackOrderDetails_cardView);
        orderStatusUpdateLayout = findViewById(R.id.orderStatusUpdate_linearLayout);
        orderStatusTV = findViewById(R.id.trackOrderStatusTV_textView);
        orderStatusRecyclerView = findViewById(R.id.orderStatusUpdate_recycleView);
        dropDownIV = findViewById(R.id.dropDownView_textView);
        ConstraintLayout mainLayout = findViewById(R.id.orderTrackMain_Layout_constraintLayout);
        FloatingActionButton gotoMap = findViewById(R.id.gotoMap_floatingActionButton);

        statusIconViews = new ImageView[]{
                findViewById(R.id.imageView11),
                findViewById(R.id.imageView12),
                findViewById(R.id.imageView16),
                findViewById(R.id.imageView15),
                findViewById(R.id.imageView13),
                findViewById(R.id.imageView14)
        };

        statusProgressBarViews = new ProgressBar[]{
                findViewById(R.id.orderStatus2_progressBar),
                findViewById(R.id.orderStatus3_progressBar),
                findViewById(R.id.orderStatus4_progressBar),
                findViewById(R.id.orderStatus5_progressBar),
                findViewById(R.id.orderStatus6_progressBar)
        };


        int collapsedHeight = getResources().getDimensionPixelSize(R.dimen.cardview_collapsed_height);
        // Set the CardView height to the collapsed height initially
        ViewGroup.LayoutParams params = orderStatusUpdateLayout.getLayoutParams();
        params.height = collapsedHeight;
        orderStatusUpdateLayout.setLayoutParams(params);

        ViewGroup rootViewGroup = findViewById(android.R.id.content);
        Intent mainActivityIntent = new Intent(this, MainActivity.class);

        Intent intent = getIntent();
        orderToTrackOrderID = intent.getStringExtra("orderToTrackOrderID");
        preferences.edit().putInt("orderStatus", 0).apply();

        // Initialize the adapter with an empty list
        statusList = new ArrayList<>();
        orderStatusAdapter = new OrderStatusAdapter(statusList, orderToTrackOrderID, this);

        orderStatusRecyclerView.setLayoutManager(new LinearLayoutManager(this));
//        orderStatusRecyclerView.setItemAnimator(new ItemAnimatorOrderStatusUpdates());
        orderStatusRecyclerView.setAdapter(orderStatusAdapter);

        // Load initial data (this should be done with your actual data fetching logic)
        Map<String, Map<String, String>> initialData = getInitialOrderStatusData();
        updateOrderStatus(initialData);

        chatFab.setVisibility(View.INVISIBLE);
        chatFab.setEnabled(false);

        if (orderToTrackOrderID != null && !orderToTrackOrderID.isEmpty()) {
            fetchData(orderToTrackOrderID);
            initSSE(orderToTrackOrderID);
            initChatBtmView(rootViewGroup);
        } else {
            Toast.makeText(this, "Invalid order-id", Toast.LENGTH_SHORT).show();
        }

        chatFab.setOnClickListener(v -> {
            if (setDeliveryAddrBtmView != null && !setDeliveryAddrBtmView.isShowing()) {
                chatFab.setEnabled(true);
                setDeliveryAddrBtmView.show();
            } else {
                chatFab.setEnabled(false);
            }
        });

        gotoMap.setOnClickListener(v -> mapFragment.show(getSupportFragmentManager(), mapFragment.getTag()));

        mainLayout.setOnClickListener(v -> {
            if (isOrderStatusCardViewExpanded) {
                toggleCardView();
            }
        });

        dropDownIV.setOnClickListener(v -> toggleCardView());

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Handle the back button event
                mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainActivityIntent);
                finish();
            }
        };

        Snackbar.make(findViewById(android.R.id.content), "Persistent Snackbar", Snackbar.LENGTH_INDEFINITE)
                .setAction("DISMISS", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Dismiss action
                    }
                })
                .show();



        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void animateOrderStatus(int step, boolean isDeliveryPartnerAssigned) {

        if (step > 2) {
            statusIconViews[1].setImageResource(R.drawable.account_circle_24px_diabled);
        }

        // Reset all progress bars and image views
        for (ProgressBar progressBar : statusProgressBarViews) {
            progressBar.setProgress(0);
        }

        // Start the animation sequence
        animateStep(0, step, statusIconViews, statusProgressBarViews, isDeliveryPartnerAssigned);
    }

    private void animateStep(final int currentStep, final int maxStep,
                             final ImageView[] imageViews,
                             final ProgressBar[] progressBars, boolean isDeliveryPartnerAssigned) {
        if (currentStep >= maxStep) {
            return;
        }

        // Fade in the image view
        if (isDeliveryPartnerAssigned) {
            animateImageViewResourceChange(imageViews[currentStep], getNewResourceForStep1(currentStep));
        } else {
            animateImageViewResourceChange(imageViews[currentStep], getNewResourceForStep0(currentStep));
        }

        // If there is a progress bar to animate
        if (currentStep < maxStep - 1 && currentStep < progressBars.length) {
            // Add a delay to start animating the progress bar after the image has faded in
            new Handler().postDelayed(() -> {
                animateProgressBar(progressBars[currentStep]);

                // After the progress bar has animated, move to the next step
                new Handler().postDelayed(() -> animateStep(currentStep + 1, maxStep, imageViews, progressBars, isDeliveryPartnerAssigned), 1500); // Adjust this delay to match the progress bar animation duration

            }, 750);
        }
    }

    private void animateImageViewResourceChange(final ImageView imageView, final int newResource) {
        // Fade out the current image
        imageView.animate()
                .alpha(0f)
                .setDuration(500) // Adjust the duration as needed
                .withEndAction(() -> {
                    // Change the image resource once fade out is complete
                    imageView.setImageResource(newResource);

                    // Fade in the new image
                    imageView.animate()
                            .alpha(1f)
                            .setDuration(500) // Adjust the duration as needed
                            .start();
                })
                .start();
    }

    // Method to animate a ProgressBar
    private void animateProgressBar(ProgressBar progressBar) {
        ObjectAnimator progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100);
        progressAnimator.setDuration(1500); // Duration of progress animation
        progressAnimator.setInterpolator(new DecelerateInterpolator());
        progressAnimator.start();

    }


    // Method to get the new image resource for the given step
    private int getNewResourceForStep0(int step) {
        return switch (step) {
            case 0 -> R.drawable.order_approve_24px;
            case 1 -> R.drawable.account_circle_off_24px;
            case 2 -> R.drawable.check_circle_24px;
            case 3 -> R.drawable.orders_24px;
            case 4 -> R.drawable.route_24px;
            case 5 -> R.drawable.door_front_24px;
            default -> R.drawable.baseline_broken_image_24;
        };
    }

    // Method to get the new image resource for the given step
    private int getNewResourceForStep1(int step) {
        return switch (step) {
            case 0 -> R.drawable.order_approve_24px;
            case 1 -> R.drawable.account_circle_24px;
            case 2 -> R.drawable.check_circle_24px;
            case 3 -> R.drawable.orders_24px;
            case 4 -> R.drawable.route_24px;
            case 5 -> R.drawable.door_front_24px;
            default -> R.drawable.baseline_broken_image_24;
        };
    }


    public void updateOrderStatus(Map<String, Map<String, String>> newOrderStatusData) {
        // Convert new status data to a sorted list
        List<Map<String, String>> newStatusList = new ArrayList<>(newOrderStatusData.entrySet())
                .stream()
                .sorted(Comparator.comparingInt(entry -> Integer.parseInt(entry.getKey())))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());

        // Create a set to keep track of existing status keys
        Set<String> existingKeys = statusList.stream()
                .map(status -> status.get("key"))
                .collect(Collectors.toSet());

        // Filter out already existing statuses
        List<Map<String, String>> filteredNewStatusList = newStatusList.stream()
                .filter(status -> !existingKeys.contains(status.get("key")))
                .collect(Collectors.toList());

        // Proceed only if there are new items
        if (!filteredNewStatusList.isEmpty()) {
            // Create a list to hold both existing and new statuses
            List<Map<String, String>> combinedStatusList = new ArrayList<>(statusList);
            combinedStatusList.addAll(filteredNewStatusList);

            // Update the adapter with the new combined list
            if (orderStatusAdapter != null) {
                int previousSize = statusList.size();
                statusList.addAll(filteredNewStatusList);

                orderStatusAdapter.notifyItemRangeInserted(previousSize, filteredNewStatusList.size());

                orderStatusAdapter.notifyItemInserted(orderStatusAdapter.getItemCount() - 1);
                orderStatusRecyclerView.scrollToPosition(combinedStatusList.size() - 1);

            }

            orderDetailsCardView.setVisibility(View.VISIBLE);

        } else {
            Log.d(LOG_TAG, "No new unique status data to update.");
        }
    }

    private void toggleCardView() {
        ValueAnimator animator;

        if (isOrderStatusCardViewExpanded) {
            animator = ValueAnimator.ofInt(orderStatusUpdateLayout.getHeight(), getResources().getDimensionPixelSize(R.dimen.cardview_collapsed_height));
        } else {
            orderStatusUpdateLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            animator = ValueAnimator.ofInt(orderStatusUpdateLayout.getHeight(), orderStatusUpdateLayout.getMeasuredHeight());
        }
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(300);

        animator.addUpdateListener(animation -> {
            orderStatusUpdateLayout.getLayoutParams().height = (Integer) animation.getAnimatedValue();
            orderStatusUpdateLayout.requestLayout();

            if (isOrderStatusCardViewExpanded) {
                dropDownIV.setText(R.string.hide_detailed_update);
                dropDownIV.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null,
                        ContextCompat.getDrawable(OrderTrackActivity.this, R.drawable.baseline_arrow_drop_up_24), null);
            } else {
                dropDownIV.setText(R.string.view_detailed_update);
                dropDownIV.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null,
                        ContextCompat.getDrawable(OrderTrackActivity.this, R.drawable.baseline_arrow_drop_down_24), null);
            }
        });

        animator.start();
        isOrderStatusCardViewExpanded = !isOrderStatusCardViewExpanded;
    }

    public Map<String, Map<String, String>> getInitialOrderStatusData() {
        Map<String, Map<String, String>> initialData = new LinkedHashMap<>();

        initialData.put("1", new HashMap<>() {{
            put("key", "1");
            put("title", "Order placed");
            put("sub_title", "You have successfully placed your order.");
        }});

        return initialData;
    }


    private void initSSE(String orderID) {
        sseClient = new SSEClient(this, user.getUid(), orderID);

        sseClient.setConnectionListener(new SSEClient.SSEConnectionListener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> Toast.makeText(OrderTrackActivity.this, "SSE connected", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> Toast.makeText(OrderTrackActivity.this, "SSE disconnected", Toast.LENGTH_SHORT).show());
            }
        });

        sseClient.start(message -> {
            // Handle incoming SSE messages
            runOnUiThread(() -> {
                // Update UI or perform necessary actions
                SSEModel data = new Gson().fromJson(message, SSEModel.class);
                boolean isPartnerAssigned = data.getIs_partner_assigned();

                onPartnerStatusChanged(isPartnerAssigned);

                if ("None".equals(data.getDp_name())) {
                    orderStatusTV.setText(R.string.delivery_partner_not_assigned);
                    orderStatusTV.setTextColor(getColor(R.color.partner_not_assigned));
                } else {
                    if (data.getDp_name() != null) {
                        orderStatusTV.setText(MessageFormat.format("{0} is your delivery partner",
                                data.getDp_name().toUpperCase()));
                        orderStatusTV.setTextColor(getColor(R.color.partner_assigned));
                    }
                }

                if (data.getOrder_id() != null) {
                    orderIDTV.setText(MessageFormat.format(getText(R.string.order_ID) + ": {0}", data.getOrder_id().substring(6)));
                }
                if (data.getOrder_time() != null) {
                    orderTimeTV.setText(data.getOrder_time());
                }
                if (data.getOrder_status_label() != null) {
                    sseViewTV.setText(data.getOrder_status_label());
                }
                if (data.getTime() != null) {
                    updatedTimeTV.setText(data.getTime());
                }

                if (!isAnimationShown) {
                    animateOrderStatus(data.getOrder_status_no(), isPartnerAssigned);
                    orderStatusNo = data.getOrder_status_no();
                    isAnimationShown = true;
                }

                if (orderStatusNo != data.getOrder_status_no()) {
                    animateOrderStatus(data.getOrder_status_no(), isPartnerAssigned);
                    orderStatusNo = data.getOrder_status_no();
                    isAnimationShown = true;
                }

                // Update order status in RecyclerView
                if (data.getOrder_status_data() != null) {
                    updateOrderStatus(data.getOrder_status_data());
                } else {
                    Log.e(LOG_TAG, "Order status data is null");
                }

                Log.d(LOG_TAG, message);

//                if (!isOrderStatusCardViewExpanded) {
//                    toggleCardView();
//                }

            });
        });
    }


    private void wobbleAnimation(EditText editText) {
        // Define the wobble animation
        ObjectAnimator wobbleAnimator = ObjectAnimator.ofFloat(editText, "translationX", 0f, 5f, -5f, 10f, -10f, 5f, -5f, 0f);
        wobbleAnimator.setDuration(500);
        wobbleAnimator.setInterpolator(new CycleInterpolator(1));
        wobbleAnimator.start();
    }


    @SuppressLint("NotifyDataSetChanged")
    private void initChatBtmView(ViewGroup root) {
        chatFab.setVisibility(View.VISIBLE);
        chatFab.setEnabled(true);

        View chatView = LayoutInflater.from(this).inflate(
                R.layout.bottomview_chat_with_partner, root, false);

        setDeliveryAddrBtmView = new BottomSheetDialog(this);
        setDeliveryAddrBtmView.setContentView(chatView);
        Objects.requireNonNull(setDeliveryAddrBtmView.getWindow()).setGravity(Gravity.TOP);

        messageET = chatView.findViewById(R.id.chaViewtMsgET_editTextText);
        chatSendBtn = chatView.findViewById(R.id.chatSendButton_button);
        chatRecyclerView = chatView.findViewById(R.id.chatMessages_recyclerView);
        TextView chatViewBtmStatusTV = chatView.findViewById(R.id.chatViewBtmStatusTV_textView);
        chatViewStatusTV = chatView.findViewById(R.id.chatViewStatusTV_textView);
        TextView clearAllChatBtn = chatView.findViewById(R.id.chatClearAllBtn_textView);
        ProgressBar chatViewPB = chatView.findViewById(R.id.chatView_progressBar);

        chatSendBtn.setEnabled(false);
        messageET.setEnabled(false);

        chatClient = new ChatClient(this, user, chatRecyclerView, orderToTrackOrderID,
                chatViewStatusTV, chatViewPB, chatViewBtmStatusTV,
                messageET, chatSendBtn);

        chatMessageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessageList);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        chatSendBtn.setOnClickListener(v -> {
            String message = messageET.getText().toString();
            if (!TextUtils.isEmpty(message)) {
                sendMessage(message);
                messageET.setText("");
                chatViewStatusTV.setText("");
            } else {
                wobbleAnimation(messageET);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Utils.vibrate(this, 0, VibrationEffect.DEFAULT_AMPLITUDE);
                }
            }
        });

        clearAllChatBtn.setOnClickListener(v -> {
            chatMessageList.clear();
            chatAdapter.notifyDataSetChanged();
            chatViewStatusTV.setText(R.string.ready_to_chat);
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
    public void addMessage(String senderName, String message, String messageTime) {
        chatMessageList.add(new ChatModel(senderName, message, messageTime, false));
        chatAdapter.notifyItemInserted(chatMessageList.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessageList.size() - 1);
    }

    public void addSentMessage(String message, String messageTime) {
        chatMessageList.add(new ChatModel("Me", message, messageTime, true));
        chatAdapter.notifyItemInserted(chatMessageList.size() - 1);
        chatRecyclerView.scrollToPosition(chatMessageList.size() - 1);
    }

    private void fetchData(String orderID) {
        DocumentReference docRef = db.collection("Users")
                .document(user.getUid())
                .collection("placedOrderData").document(orderID)
                .collection("orderData").document("info");

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // Get the data map from the snapshot
                    Map<String, Object> data = document.getData();
                    if (data != null && !data.isEmpty()) {

                        if (data.get("order_id") != null && data.get("order_time") != null) {
                            orderIDTV.setText(MessageFormat.format(getText(R.string.order_ID) + ": {0}",
                                    Objects.requireNonNull(data.get("order_id")).toString().substring(6)));
                            orderTimeTV.setText((String) data.get("order_time"));
                        }
                    }
                } else {
                    Log.d(LOG_TAG, "Document does not exist.");
                }
            } else {
                Log.d(LOG_TAG, "Error fetching document: " + task.getException());
            }
        });
    }

    private void resetAnimations() {
        // Array of default image resources for each ImageView
        int[] defaultImages = new int[]{
                R.drawable.order_approve_24px_disabled,
                R.drawable.account_circle_off_24px_disabled,
                R.drawable.check_circle_24px_disabled,
                R.drawable.orders_24px_disabled,
                R.drawable.route_24px_disabed,
                R.drawable.door_front_24px_disabled,
        };

        // Reset all ProgressBars to 0 progress
        for (ProgressBar progressBar : statusProgressBarViews) {
            progressBar.setProgress(0);
        }

        try {
            // Reset each ImageView with its corresponding default image resource
            for (int i = 0; i < statusIconViews.length; i++) {
                statusIconViews[i].setAlpha(1f);
                statusIconViews[i].setImageResource(defaultImages[i]);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        resetAnimations();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (orderStatusNo != 0) {
            animateOrderStatus(orderStatusNo, isDeliveryPartnerAssigned);
            isAnimationShown = true;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (sseClient != null) {
            sseClient.stop();
        }

        if (chatClient != null) {
            chatClient.onActivityDestroyed();

        }

        if (setDeliveryAddrBtmView != null) {
            setDeliveryAddrBtmView.hide();
            setDeliveryAddrBtmView.dismiss();
        }

    }

    @Override
    public void onPartnerStatusChanged(boolean isAssigned) {
        runOnUiThread(() -> {
            if (isAssigned) {
                chatSendBtn.setEnabled(true);
                messageET.setEnabled(true);
                isDeliveryPartnerAssigned = true;

                if (chatMessageList.isEmpty()) {
                    chatViewStatusTV.setText(R.string.ready_to_chat);
                    chatViewStatusTV.setTextColor(getColor(R.color.wsc_connected));
                } else {
                    chatViewStatusTV.setText("");

                }
            } else {
                chatSendBtn.setEnabled(false);
                messageET.setEnabled(false);
                chatViewStatusTV.setText(R.string.partner_not_assigned);
                chatViewStatusTV.setTextColor(getColor(R.color.wsc_partner_not_avail));
            }
        });
    }

}