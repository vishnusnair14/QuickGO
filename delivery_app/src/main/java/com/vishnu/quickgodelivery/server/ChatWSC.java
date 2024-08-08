package com.vishnu.quickgodelivery.server;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.vishnu.quickgodelivery.R;
import com.vishnu.quickgodelivery.ui.order.info.obs.OBSOrderInformationFragment;
import com.vishnu.quickgodelivery.ui.order.info.obv.OBVOrderInformationFragment;
import com.vishnu.quickgodelivery.ui.settings.PreferencesManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatWSC extends WebSocketListener {

    private static final String TAG = "ChatWebSocketClient";
    private final Activity activity;
    private final OBSOrderInformationFragment obsOrderInformationFragment;
    private final OBVOrderInformationFragment obvOrderInformationFragment;
    public WebSocket webSocket;
    private final FirebaseUser user;
    private final TextView chatStatusTV;
    private final TextView chatBtmStatusTV;
    private final EditText msgET;
    private final ProgressBar chatViewPB;
    private final Button chatSendBtn;
    private final OkHttpClient client;
    private final Request request;
    private int retryCount = 0;
    private final int maxRetries = 5;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ChatWSC(Activity activity, OBSOrderInformationFragment obsOrderInformationFragment,
                   FirebaseUser user, String chatId, TextView chatStatusTV, ProgressBar chatViewPB,
                   TextView chatBtmStatusTV, EditText msgET, Button chatSendBtn) {
        this.activity = activity;
        this.obsOrderInformationFragment = obsOrderInformationFragment;
        this.obvOrderInformationFragment = null;
        this.user = user;
        this.chatStatusTV = chatStatusTV;
        this.chatBtmStatusTV = chatBtmStatusTV;
        this.chatViewPB = chatViewPB;
        this.msgET = msgET;
        this.chatSendBtn = chatSendBtn;
        client = new OkHttpClient();

        String baseUrl = PreferencesManager.getBaseUrl(activity.getBaseContext()).substring(8);

        request = new Request.Builder().url("ws://" + baseUrl + "/ws/chat/" + chatId + "/" +
                user.getUid() + "/delivery/").build();

        connect();
    }

    public ChatWSC(Activity activity, OBVOrderInformationFragment obvOrderInformationFragment,
                   FirebaseUser user, String chatId, TextView chatStatusTV, ProgressBar chatViewPB,
                   TextView chatBtmStatusTV, EditText msgET, Button chatSendBtn) {
        this.activity = activity;
        this.obvOrderInformationFragment = obvOrderInformationFragment;
        this.obsOrderInformationFragment = null;
        this.user = user;
        this.chatStatusTV = chatStatusTV;
        this.chatBtmStatusTV = chatBtmStatusTV;
        this.chatViewPB = chatViewPB;
        this.msgET = msgET;
        this.chatSendBtn = chatSendBtn;
        client = new OkHttpClient();

        String baseUrl = PreferencesManager.getBaseUrl(activity.getBaseContext()).substring(8);

        request = new Request.Builder().url("ws://" + baseUrl + "/ws/chat/" + chatId + "/" +
                user.getUid() + "/delivery/").build();

        connect();
    }


    private void connect() {
        if (retryCount < maxRetries) {
            chatStatusTV.setTextColor(activity.getColor(R.color.wsc_disconnected));
            chatViewPB.setVisibility(View.VISIBLE);
            chatBtmStatusTV.setTextColor(activity.getColor(R.color.wsc_disconnected));
            chatStatusTV.setText(R.string.retrying_connection);

            webSocket = client.newWebSocket(request, this);
        } else {
            Log.e(TAG, "Max retry attempts reached. Could not establish chat connection.");
            Toast.makeText(activity, "Max retry attempts reached. Could not establish chat connection.", Toast.LENGTH_LONG).show();
            chatStatusTV.setTextColor(activity.getColor(R.color.status_tv_default));
            chatViewPB.setVisibility(View.GONE);
            chatStatusTV.setText(R.string.max_retry_attempts);
        }
    }


    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull okhttp3.Response response) {
        Log.d(TAG, "WebSocket Connection opened");
        retryCount = 0;
        mainHandler.post(() -> {
            chatBtmStatusTV.setTextColor(activity.getColor(R.color.wsc_connected));
            chatBtmStatusTV.setText(R.string.connected);

            chatStatusTV.setText("");
            chatViewPB.setVisibility(View.GONE);

            chatSendBtn.setEnabled(true);
            msgET.setEnabled(true);
        });
    }


    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        Log.d(TAG, "Received message: " + text);
        try {
            JSONObject json = new JSONObject(text);
            String message = json.getString("message");
            String receiverUserID = json.getString("user_id");
            String receivedUserName = json.getString("user_name");
            String receivedMessageTime = json.getString("message_time");

            if (!receiverUserID.equals(user.getUid())) {
                activity.runOnUiThread(() -> {
                    if (obsOrderInformationFragment != null) {
                        obsOrderInformationFragment.addMessage(receivedUserName, message, receivedMessageTime);
                    }
                    if (obvOrderInformationFragment != null) {
                        obvOrderInformationFragment.addMessage(receivedUserName, message, receivedMessageTime);
                    }
                });
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON decode error: " + e.getMessage());
        }
    }


    @Override
    public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        Log.d(TAG, "WebSocket Connection closing: " + reason);
        webSocket.close(code, null);
        mainHandler.post(() -> chatStatusTV.setText(R.string.disconnected));
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, okhttp3.Response response) {
        mainHandler.post(() -> {
            chatBtmStatusTV.setTextColor(activity.getColor(R.color.wsc_disconnected));
            chatBtmStatusTV.setText(R.string.disconnected);

            chatSendBtn.setEnabled(false);
            msgET.setEnabled(false);

        });

        Log.e(TAG, "WebSocket error: " + t.getMessage());
        webSocket.cancel();
        retryCount++;
        long delay = (long) Math.pow(2, retryCount) * 1000; // Exponential backoff
        Log.d(TAG, "Retrying connection in " + delay + "ms...");

        mainHandler.postDelayed(this::connect, delay);
    }


    public void sendMessage(String message, String messageTime) {
        if (webSocket != null) {
            try {
                JSONObject json = new JSONObject();
                json.put("user_id", user.getUid());
                json.put("user_name", user.getDisplayName());
                json.put("message", message);
                json.put("client_type", "delivery");
                json.put("message_time", messageTime);

                webSocket.send(json.toString());
                activity.runOnUiThread(() -> {
                    if (obsOrderInformationFragment != null) {
                        obsOrderInformationFragment.addSentMessage(message, messageTime);
                    }
                    if (obvOrderInformationFragment != null) {
                        obvOrderInformationFragment.addSentMessage(message, messageTime);
                    }
                });
            } catch (JSONException e) {
                Log.e(TAG, "JSON encode error: " + e.getMessage());
            }
        }
    }
}
