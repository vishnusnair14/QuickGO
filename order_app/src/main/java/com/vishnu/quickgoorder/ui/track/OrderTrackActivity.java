package com.vishnu.quickgoorder.ui.track;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.vishnu.quickgoorder.server.ws.ChatModel;
import com.vishnu.quickgoorder.server.ws.ChatClient;
import com.vishnu.quickgoorder.server.ws.DeliveryPartnerStatusListener;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OrderTrackActivity extends AppCompatActivity implements DeliveryPartnerStatusListener {
    private final String LOG_TAG = "OrderTrackActivity";
    private FirebaseFirestore db;
    private FirebaseUser user;
    private SharedPreferences preferences;
    TextView orderIDTV;
    TextView orderTimeTV;
    TextView updatedTimeTV;
    Button chatSendBtn;
    EditText messageET;
    FloatingActionButton chatFab;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatModel> chatMessageList;
    BottomSheetDialog setDeliveryAddrBtmView;
    private ChatClient chatClient;
    private TextView chatViewBtmStatusTV;
    private TextView chatViewStatusTV;
    private TextView chatClearAllBtnTV;
    private TextView sseViewTV;
    private TextView updateStatusTV;
    private SSEClient sseClient;
    private TextView step1, step2, step3, step4, step5, step6;
    private View progressLine;
    private View verticalLine;
    private ProgressBar orderUpdatePB;
    private ProgressBar chatViewPB;
    private int currentStatusNo = 1;
    private boolean isOrderStatusAlreadyShownOnce = false;
    private boolean flag = true;
    private final int[] stepHeights = new int[6];
    TextView[] statusTVArray;
    private String orderToTrackOrderID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_track);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        orderIDTV = findViewById(R.id.orderTrackOrderIDView_textView);
        orderTimeTV = findViewById(R.id.orderTrackOrderTimeView_textView);
        updatedTimeTV = findViewById(R.id.orderTrackOrderUpdatedTimeView_textView);
        chatFab = findViewById(R.id.chat_floatingActionButton);
        sseViewTV = findViewById(R.id.sseView_textView);
        updateStatusTV = findViewById(R.id.fetchingUpdates_textView);
        orderUpdatePB = findViewById(R.id.fetchingUpdates_progressBar);
        FloatingActionButton gotoMap = findViewById(R.id.gotoMap_floatingActionButton);

        chatFab.setVisibility(View.INVISIBLE);
        chatFab.setEnabled(false);

        step1 = findViewById(R.id.step1);
        step2 = findViewById(R.id.step2);
        step3 = findViewById(R.id.step3);
        step4 = findViewById(R.id.step4);
        step5 = findViewById(R.id.step5);
        step6 = findViewById(R.id.step6);
        verticalLine = findViewById(R.id.vertical_line);
        progressLine = findViewById(R.id.progress_line);

        statusTVArray = new TextView[]{step1, step2, step3, step4, step5, step6};

        step1.post(() -> stepHeights[0] = step1.getHeight());
        step2.post(() -> stepHeights[1] = step2.getHeight());
        step3.post(() -> stepHeights[2] = step3.getHeight());
        step4.post(() -> stepHeights[3] = step4.getHeight());
        step5.post(() -> stepHeights[4] = step5.getHeight());
        step6.post(() -> stepHeights[5] = step6.getHeight());

        ViewGroup rootViewGroup = findViewById(android.R.id.content);
        Intent mainActivityIntent = new Intent(this, MainActivity.class);

        Intent intent = getIntent();
        orderToTrackOrderID = intent.getStringExtra("orderToTrackOrderID");
        preferences.edit().putInt("orderStatus", 0).apply();

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

        gotoMap.setOnClickListener(v -> {

        });

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Handle the back button event
                mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(mainActivityIntent);
                finish();
            }
        };

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void syncOrderStatusUI(int _no) {
        if (isOrderStatusAlreadyShownOnce) {
            if (currentStatusNo != _no) {
                if (currentStatusNo > _no) {
                    for (int i = _no; i <= 5; i++) {
                        statusTVArray[i].setVisibility(View.GONE);
                    }
                }
                animateStep(_no);
                currentStatusNo = _no;
            } else {
                if (flag) {
                    for (int i = 1; i <= _no; i++) {
                        animateStep(i);
                        currentStatusNo = i;
                    }
                    flag = false;
                }
            }
        } else {
            isOrderStatusAlreadyShownOnce = true;
            flag = false;
            for (int i = 1; i <= _no; i++) {
                animateStep(i);
                currentStatusNo = i;
            }
        }
    }

    private void initSSE(String orderID) {
        sseClient = new SSEClient(this, user.getUid(), orderID);

        sseClient.setConnectionListener(new SSEClient.SSEConnectionListener() {
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    Toast.makeText(OrderTrackActivity.this, "SSE connected", Toast.LENGTH_SHORT).show();

                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    Toast.makeText(OrderTrackActivity.this, "SSE disconnected", Toast.LENGTH_SHORT).show();

                });
            }
        });

        sseClient.start(message -> {
            // Handle incoming SSE messages
            runOnUiThread(() -> {
                updateStatusTV.setVisibility(View.GONE);
                orderUpdatePB.setVisibility(View.GONE);
                verticalLine.setVisibility(View.VISIBLE);

                // Update UI or perform necessary actions
                SSEModel data = new Gson().fromJson(message, SSEModel.class);
                boolean isPartnerAssigned = data.getIs_partner_assigned();

                onPartnerStatusChanged(isPartnerAssigned);

                orderIDTV.setText(data.getOrder_id());
                orderTimeTV.setText(data.getOrder_time());
                sseViewTV.setText(data.getOrder_status_label());
                updatedTimeTV.setText(data.getTime());

                if (isPartnerAssigned) {
                    step2.setText(R.string.delivery_partner_assigned);
                } else {
                    step2.setText(R.string.delivery_partner_not_assigned);
                }
                syncOrderStatusUI(data.getOrder_status_no());
                Log.d(LOG_TAG, message);
            });
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void initChatBtmView(ViewGroup root) {
        chatFab.setVisibility(View.VISIBLE);
        chatFab.setEnabled(true);

        View chatView = LayoutInflater.from(this).inflate(
                R.layout.bottomview_chat_with_partner, root, false);

        setDeliveryAddrBtmView = new BottomSheetDialog(this);
        setDeliveryAddrBtmView.setContentView(chatView);
        setDeliveryAddrBtmView.setCanceledOnTouchOutside(false);
        Objects.requireNonNull(setDeliveryAddrBtmView.getWindow()).setGravity(Gravity.TOP);

        messageET = chatView.findViewById(R.id.chaViewtMsgET_editTextText);
        chatSendBtn = chatView.findViewById(R.id.chatSendButton_button);
        chatRecyclerView = chatView.findViewById(R.id.chatMessages_recyclerView);
        chatViewBtmStatusTV = chatView.findViewById(R.id.chatViewBtmStatusTV_textView);
        chatViewStatusTV = chatView.findViewById(R.id.chatViewStatusTV_textView);
        chatClearAllBtnTV = chatView.findViewById(R.id.chatClearAllBtn_textView);
        chatViewPB = chatView.findViewById(R.id.chatView_progressBar);

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
                Utils.vibrate(this, 0, VibrationEffect.DEFAULT_AMPLITUDE);
            }
        });
        chatClearAllBtnTV.setOnClickListener(v -> {
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
                .collection("orderData").document("obsData");

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // Get the data map from the snapshot
                    Map<String, Object> data = document.getData();
                    if (data != null && !data.isEmpty()) {

                        if (data.get("order_id") != null && data.get("order_time") != null) {

                            orderIDTV.setText(Objects.requireNonNull(data.get("order_id"))
                                    .toString().substring(6));
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

    private void animateStep(int step) {
        TextView stepView = null;
        int stepHeight = 0;
        int delay = 1200 * (step - 1);

        stepHeight = switch (step) {
            case 1 -> {
                stepView = step1;
                yield stepHeights[0];
            }
            case 2 -> {
                stepView = step2;
                yield stepHeights[1];
            }
            case 3 -> {
                stepView = step3;
                yield stepHeights[2];
            }
            case 4 -> {
                stepView = step4;
                yield stepHeights[3];
            }
            case 5 -> {
                stepView = step5;
                yield stepHeights[4];
            }
            case 6 -> {
                stepView = step6;
                yield stepHeights[5];
            }
            default -> stepHeight;
        };

        if (stepView != null) {
            final int finalStepHeight = stepHeight;
            final TextView finalStepView = stepView;

            stepView.postDelayed(() -> {
                finalStepView.setVisibility(View.VISIBLE);
                Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
                finalStepView.startAnimation(slideIn);

                int previousStepsHeight = 0;
                for (int i = 0; i < step; i++) {
                    previousStepsHeight += stepHeights[i];
                }

                progressLine.animate()
                        .scaleY((float) (previousStepsHeight + finalStepHeight) / verticalLine.getHeight())
                        .setDuration(500)
                        .start();
            }, delay);
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

        isOrderStatusAlreadyShownOnce = false;
        step1.setVisibility(View.GONE);
        step2.setVisibility(View.GONE);
        step3.setVisibility(View.GONE);
        step4.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
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

        isOrderStatusAlreadyShownOnce = false;
    }

    @Override
    public void onPartnerStatusChanged(boolean isAssigned) {
        runOnUiThread(() -> {
            if (isAssigned) {
                chatSendBtn.setEnabled(true);
                messageET.setEnabled(true);
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


//    @Override
//    public void onBackPressed() {
//        // Navigate to Main Activity (home page)
//        super.onBackPressed();
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//        finish(); // Optional: close the current activity to remove it from the stack
//    }

}