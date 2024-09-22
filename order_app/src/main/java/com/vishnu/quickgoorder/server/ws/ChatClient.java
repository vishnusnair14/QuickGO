package com.vishnu.quickgoorder.server.ws;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseUser;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.ui.settings.PreferencesManager;
import com.vishnu.quickgoorder.ui.track.OrderTrackActivity;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class ChatClient extends WebSocketListener {

    private static final String LOG_TAG = "ChatWebSocketClient";
    private final OrderTrackActivity activity;
    public WebSocket webSocket;
    private final FirebaseUser user;
    private final TextView chatStatusTV;
    private final TextView chatBtmStatusTV;
    private final ProgressBar chatViewPB;
    private OkHttpClient client;
    private Request request;
    private final EditText msgET;
    private final Button chatSendBtn;
    private int retryCount = 0;
    private final int maxRetries = 5;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isActivityDestroyed = false;
    private RecyclerView chatRecycleView;

    public ChatClient(OrderTrackActivity activity, FirebaseUser user, RecyclerView chatRecycleView,
                      String chatId, TextView chatStatusTV, ProgressBar chatViewPB,
                      TextView chatBtmStatusTV, EditText msgET, Button chatSendBtn) {
        this.activity = activity;
        this.user = user;
        this.chatRecycleView = chatRecycleView;
        this.chatStatusTV = chatStatusTV;
        this.chatBtmStatusTV = chatBtmStatusTV;
        this.chatViewPB = chatViewPB;
        this.msgET = msgET;
        this.chatSendBtn = chatSendBtn;
        client = new OkHttpClient();

        chatSendBtn.setEnabled(false);
        msgET.setEnabled(false);

        String baseUrl = PreferencesManager.getBaseUrl(activity.getBaseContext()).substring(8);

        request = new Request.Builder().url("ws://" + baseUrl + "/ws/chat/" + chatId + "/" +
                user.getUid() + "/order/").build();

        connect();
    }

    private void connect() {
        if (!isActivityDestroyed && retryCount < maxRetries) {
            chatStatusTV.setTextColor(activity.getColor(R.color.wsc_disconnected));
            chatViewPB.setVisibility(View.VISIBLE);
            chatBtmStatusTV.setTextColor(activity.getColor(R.color.wsc_disconnected));
            chatStatusTV.setText(R.string.retrying_connection);
            chatRecycleView.setVisibility(View.INVISIBLE);

            webSocket = client.newWebSocket(request, this);
        } else {
            if (isActivityDestroyed) {
                Log.d(LOG_TAG, "Activity is destroyed. Not attempting to reconnect.");
            } else {
                Log.e(LOG_TAG, "Max retry attempts reached. Could not establish chat connection.");
                Toast.makeText(activity, "Max retry attempts reached. Could not establish chat connection.", Toast.LENGTH_LONG).show();
                chatStatusTV.setTextColor(activity.getColor(R.color.status_tv_default));
                chatViewPB.setVisibility(View.GONE);
                chatStatusTV.setText(R.string.max_retry_attempts);
            }
        }
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull okhttp3.Response response) {
        retryCount = 0;

        mainHandler.post(() -> {
            chatBtmStatusTV.setTextColor(activity.getColor(R.color.wsc_connected));
            chatStatusTV.setTextColor(activity.getColor(R.color.wsc_connected));
            chatBtmStatusTV.setText(R.string._connected);
            chatStatusTV.setText(R.string.you_are_ready_to_chat);
            chatViewPB.setVisibility(View.GONE);
            chatRecycleView.setVisibility(View.VISIBLE);
        });

        Log.d(LOG_TAG, "WebSocket Connection opened");
    }

    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        Log.d(LOG_TAG, "Received message: " + text);
        try {
            JSONObject json = new JSONObject(text);
            String receivedMessage = json.getString("message");
            String receivedUserId = json.getString("user_id");
            String receivedUserName = json.getString("user_name");
            String receivedMessageTime = json.getString("message_time");

            if (!receivedUserId.equals(user.getUid())) {

                activity.runOnUiThread(() ->
                        activity.addMessage(receivedUserName, receivedMessage, receivedMessageTime));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "JSON decode error: " + e.getMessage());
        }
    }

    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        Log.d(LOG_TAG, "WebSocket Connection closing: " + reason);
        webSocket.close(code, null);
        mainHandler.post(() -> chatStatusTV.setText(R.string._disconnected));
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, okhttp3.Response response) {
        mainHandler.post(() -> {
            chatBtmStatusTV.setTextColor(activity.getColor(R.color.wsc_disconnected));
            chatBtmStatusTV.setText(R.string._disconnected);
            chatSendBtn.setEnabled(false);
            msgET.setEnabled(false);
        });

        Log.e(LOG_TAG, "WebSocket error: " + t.getMessage());
        webSocket.cancel();
        retryCount++;
        long delay = (long) Math.pow(2, retryCount) * 1000; // Exponential backoff
        Log.d(LOG_TAG, "Retrying websocket connection in " + delay + "ms...");

        if (!isActivityDestroyed) {
            mainHandler.postDelayed(this::connect, delay);
        }
    }

    public void sendMessage(String message, String messageTime) {
        if (webSocket != null) {
            try {
                String username = user.getDisplayName();
                if (username != null) {
                    if (!username.isEmpty()) {
                        username = "Unknown";
                    }
                } else {
                    username = "Unknown";
                }

                JSONObject json = new JSONObject();
                json.put("user_id", user.getUid());
                json.put("user_name", username);
                json.put("message", message);
                json.put("client_type", "order");
                json.put("message_time", messageTime);

                webSocket.send(json.toString());
                mainHandler.post(() -> activity.addSentMessage(message, messageTime));
            } catch (JSONException e) {
                Log.e(LOG_TAG, "JSON encode error: " + e.getMessage());
            }
        }
    }

    public void onActivityDestroyed() {
        isActivityDestroyed = true;
        if (webSocket != null) {
            webSocket.cancel();
        }
        Log.d(LOG_TAG, "Activity destroyed. WebSocket connection closed.");
    }
}
