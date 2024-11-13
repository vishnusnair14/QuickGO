package com.vishnu.quickgoorder.ui.cart.voice;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.miscellaneous.SoundManager;
import com.vishnu.quickgoorder.miscellaneous.Utils;
import com.vishnu.quickgoorder.server.models.DeleteVoiceOrderFile;
import com.vishnu.quickgoorder.server.sapi.APIService;
import com.vishnu.quickgoorder.server.sapi.ApiServiceGenerator;

import java.io.File;
import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// AudioAdapter.java
public class VoiceOrdersAdapter extends RecyclerView.Adapter<VoiceOrdersAdapter.ViewHolder> {
    private final String LOG_TAG = "voiceOrderAdapter";
    private final List<VoiceOrdersModel> voiceOrdersViewModel;
    private final Context context;
    private final FirebaseUser user;
    private long mDownloadId;
    private final String orderByVoiceDocID;
    private final String orderByVoiceAudioRefID;
    private final TextView statusTV;
    private boolean isPaused = false;
    private final String from;
    private String shopID;
    public static MediaPlayer mediaPlayer = new MediaPlayer();

    public VoiceOrdersAdapter(FirebaseUser user, Context context, String from,
                              List<VoiceOrdersModel> voiceOrdersViewModel,
                              String orderByVoiceDocID, String orderByVoiceAudioRefID,
                              String shopID, TextView statusTV) {
        this.user = user;
        this.context = context;
        this.from = from;
        this.voiceOrdersViewModel = voiceOrdersViewModel;
        this.orderByVoiceDocID = orderByVoiceDocID;
        this.orderByVoiceAudioRefID = orderByVoiceAudioRefID;
        this.shopID = shopID;
        this.statusTV = statusTV;
        SoundManager.initialize(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.srv_voice_order_cart,
                parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VoiceOrdersModel audioModel = voiceOrdersViewModel.get(position);

        holder.audioIDTV.setText(audioModel.getAudio_key());
        holder.audioTitleTV.setText(String.format("%s", audioModel.getAudio_title()));

        setAction(holder.audioActionButton, audioModel.getAudio_title());

        holder.audioActionButton.setOnClickListener(v -> {
            downloadAndPlayAudio(audioModel.getAudio_storage_url(), audioModel.getAudio_title(),
                    holder.audioFeedbackTV, holder.audioActionButton);
        });

        holder.audioDeleteButton.setOnClickListener(v -> {
            sendDeleteVoiceOrderFileRequest(audioModel.getAudio_key(), audioModel.getAudio_title(), false, null, shopID);
        });
    }

    @Override
    public int getItemCount() {
        return voiceOrdersViewModel.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void clear() {
        if (voiceOrdersViewModel != null) {
            voiceOrdersViewModel.clear();
            notifyDataSetChanged();
        }
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView audioTitleTV, audioFeedbackTV, audioTimerTV, audioIDTV;
        FloatingActionButton audioActionButton;
        FloatingActionButton audioDeleteButton;
        ConstraintLayout constItemLayout;

        public ViewHolder(View itemView) {
            super(itemView);
            audioTitleTV = itemView.findViewById(R.id.voiceOrderTitle_textView);
            audioActionButton = itemView.findViewById(R.id.voiceOrderActionBtn_imageButton);
            audioFeedbackTV = itemView.findViewById(R.id.audioFeedback_textView);
            constItemLayout = itemView.findViewById(R.id.item_layout);
            audioTimerTV = itemView.findViewById(R.id.audioTimer_textView);
            audioIDTV = itemView.findViewById(R.id.voiceOrderID_textView);
            audioDeleteButton = itemView.findViewById(R.id.voiceOrderDeleteBtn_imageButton);
        }
    }


    public void sendDeleteVoiceOrderFileRequest(String audio_key, String audioTitle,
                                                boolean doClearCart, BottomSheetDialog bottomSheetDialog, String shopID) {
        DeleteVoiceOrderFile deleteVoiceOrderFileModel = new DeleteVoiceOrderFile(this.from, user.getUid(),
                orderByVoiceDocID, orderByVoiceAudioRefID, shopID, audio_key, doClearCart);

        APIService apiService = ApiServiceGenerator.getApiService(context);
        Call<JsonObject> call = apiService.deleteVoiceOrderFromCart(deleteVoiceOrderFileModel);

        call.enqueue(new Callback<>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject responseBody = response.body();

                    if (responseBody.has("is_success") && responseBody.get("is_success").getAsBoolean()) {

                        if (bottomSheetDialog != null) {
                            bottomSheetDialog.hide();
                            bottomSheetDialog.dismiss();
                        }
                        SoundManager.playOnDelete();

                        if (from.equals("obs")) {
                            Utils.deleteVoiceOrderCacheFile(context, orderByVoiceDocID, shopID);
                        } else {
                            Utils.deleteVoiceOrderCacheFile(context, orderByVoiceDocID, null);
                        }

                        if (doClearCart) {
                            Utils.deleteAllDownloadedVoiceOrderCartFiles(context);
                            voiceOrdersViewModel.clear();
                            notifyDataSetChanged();
                        } else {
                            Utils.deleteDownloadedVoiceOrderCartFile(audioTitle + ".mp3", context);
                        }

                        // Find and remove the deleted item from the list
                        for (int i = 0; i < voiceOrdersViewModel.size(); i++) {
                            if (voiceOrdersViewModel.get(i).getAudio_key().equals(audio_key)) {
                                voiceOrdersViewModel.remove(i);
                                notifyItemRemoved(i);
                                break;
                            }
                        }

                        if (voiceOrdersViewModel.isEmpty()) {
                            statusTV.setText(R.string.no_voice_orders_recorded_yet);
                        }

                        Toast.makeText(context, responseBody.get("message").getAsString(), Toast.LENGTH_SHORT).show();
                        Log.d(LOG_TAG, "Audio file deleted successfully");
                    } else {
                        Toast.makeText(context, responseBody.get("message").getAsString(), Toast.LENGTH_SHORT).show();
                        Log.e(LOG_TAG, "Unable to delete audio file, at the moment");
                    }
                } else {
                    String errorMessage = "Failed to delete voice data";
                    errorMessage += ": " + response.message();
                    Log.e(LOG_TAG, errorMessage);
                    Log.e(LOG_TAG, errorMessage);
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
                }
            }


            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Toast.makeText(context, "Failed to delete voice data", Toast.LENGTH_SHORT).show();
                Log.e(LOG_TAG, "Failed to delete voice data", t);
            }
        });
    }


    private void downloadAudio(String audioUrl, String fileName, TextView fdbk, ImageButton actionBtn) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(audioUrl);

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS + "/orderByVoice", fileName + ".mp3");
        request.setTitle("Please wait...");
        request.setDescription("Order voice" + fileName);

        fdbk.setText(R.string.Please_wait);
        Toast.makeText(context, R.string.Please_wait, Toast.LENGTH_SHORT).show();

        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS + "/orderByVoice"),
                fileName + ".mp3");

        mDownloadId = downloadManager.enqueue(request);
        DownloadReceiver onComplete = new DownloadReceiver(context, fdbk, mDownloadId, actionBtn);
        context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);

        playMusic(file.getAbsolutePath(), fdbk, actionBtn);
    }

    private void downloadAndPlayAudio(String audioUrl, String fileName, TextView fdbkTV, ImageButton actionBtn) {
        // Check if file exists
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS + "/orderByVoice"),
                fileName + ".mp3");
//        Toast.makeText(mContext, file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        if (file.exists()) {
            playMusic(file.getAbsolutePath(), fdbkTV, actionBtn);
        } else {
            // Download logic
            downloadAudio(audioUrl, fileName, fdbkTV, actionBtn);
        }
    }

    private void setAction(ImageButton btn, String fn) {
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS + "/orderByVoice"),
                fn + ".mp3");
        if (file.exists()) {
            btn.setImageResource(R.drawable.baseline_play_arrow_24);
            btn.setBackgroundColor(ContextCompat.getColor(context, R.color.ctbBtnPlayBg));
        } else {
            btn.setImageResource(R.drawable.baseline_file_download_24);
            btn.setBackgroundColor(ContextCompat.getColor(context, R.color.ctbBtnDownloadBg));
        }
    }

    // plays the MP3 from a given file path
    private void playMusic(String filePath, TextView fdbk, ImageButton actionBtn) {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);

            mediaPlayer.setOnCompletionListener(mp -> {
                fdbk.setText("");
                actionBtn.setImageResource(R.drawable.baseline_play_arrow_24);
                mediaPlayer.release();
            });
            fdbk.setText(R.string.playing);
            actionBtn.setImageResource(R.drawable.baseline_pause_24);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e("TAG", e.toString());
        }
    }

    public class DownloadReceiver extends BroadcastReceiver {
        private Context mContext;
        private final TextView mFeedback;
        private final ImageButton actionBtn;
        private long downloadID;

        public DownloadReceiver(Context context, TextView feedback, long downloadID, ImageButton actionBtn) {
            this.mContext = context;
            this.mFeedback = feedback;
            this.downloadID = downloadID;
            this.actionBtn = actionBtn;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId != -1) {
                // Check if the completed download matches the one you initiated
                if (downloadId == mDownloadId) {
                    // Download completed, perform your actions here
                    mFeedback.setText(R.string.complete);
                    actionBtn.setImageResource(R.drawable.baseline_play_arrow_24);
                    actionBtn.setBackgroundColor(ContextCompat.getColor(context, R.color.ctbBtnPlayBg));
//                    Toast.makeText(mContext, "Download completed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}