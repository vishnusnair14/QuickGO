package com.vishnu.quickgoorder.server.sse;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.vishnu.quickgoorder.ui.settings.PreferencesManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

public class SSEClient {
    private static final String LOG_TAG = "SSEClient";
    private EventSource eventSource;
    private final OkHttpClient client;
    private final String userID;
    private final String orderID;
    private final Activity activity;
    private final String baseUrl;

    private int retryCount = 0;
    private SSEConnectionListener connectionListener;

    public SSEClient(Activity activity, String userID, String orderID) {
        this.activity = activity;
        this.userID = userID;
        this.orderID = orderID;

        baseUrl = PreferencesManager.getBaseUrl(activity.getBaseContext()).substring(8);

        client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public void setConnectionListener(SSEConnectionListener listener) {
        this.connectionListener = listener;
    }

    public void start(SSEListener listener) {
        if (eventSource != null) {
            stop();
        }

        Request request = new Request.Builder()
                .url("https://" + baseUrl + "/stream/" + userID + "/" + orderID)
                .build();

        EventSourceListener eventSourceListener = new EventSourceListener() {
            @Override
            public void onOpen(@NonNull EventSource eventSource, @NonNull Response response) {
                Log.d(LOG_TAG, "SSE connection opened to: " + userID);
                retryCount = 0;
                if (connectionListener != null) {
                    connectionListener.onConnected();
                }
            }

            @Override
            public void onClosed(@NonNull EventSource eventSource) {
                Log.d(LOG_TAG, "SSE connection closed");
                stop();
                if (connectionListener != null) {
                    connectionListener.onDisconnected();
                }
            }

            @Override
            public void onFailure(@NonNull EventSource eventSource, Throwable t, Response response) {
                Log.e(LOG_TAG, "Error in SSE connection to: " + userID, t);
                if (connectionListener != null) {
                    connectionListener.onDisconnected();
                }
                reconnect(listener);
            }

            @Override
            public void onEvent(@NonNull EventSource eventSource, String id, String eventType, @NonNull String data) {
                if (listener != null) {
                    new Handler(Looper.getMainLooper()).post(() -> listener.onMessageReceived(data));
                }
            }
        };

        eventSource = EventSources.createFactory(client).newEventSource(request, eventSourceListener);
    }

    public void stop() {
        if (eventSource != null) {
            eventSource.cancel();
            eventSource = null;
            Log.d(LOG_TAG, "SSE connection to " + userID + " stopped");
        }
    }

    private void reconnect(SSEListener listener) {
        int maxRetries = 5;
        long initialRetryDelayMs = 5000;
        if (retryCount < maxRetries) {
            retryCount++;
            long delay = initialRetryDelayMs * (long) Math.pow(2, retryCount - 1); // Exponential backoff
            Log.d(LOG_TAG, "Retrying SSE connection in " + delay + "ms (Retry attempt: " + retryCount + ")");

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (activity != null && !activity.isDestroyed()) {
                    Log.d(LOG_TAG, "Attempting to reconnect...");
                    start(listener);
                } else {
                    Log.d(LOG_TAG, "Unable to reconnect SSE, activity may be null or destroyed!");
                }
            }, delay);
        } else {
            Log.e(LOG_TAG, "Max retry attempts reached. Could not establish SSE connection.");
        }
    }

    public interface SSEListener {
        void onMessageReceived(String message);
    }

    public interface SSEConnectionListener {
        void onConnected();

        void onDisconnected();
    }
}
