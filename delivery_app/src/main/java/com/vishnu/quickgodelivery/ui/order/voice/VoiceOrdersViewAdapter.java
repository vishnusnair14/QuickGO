package com.vishnu.quickgodelivery.ui.order.voice;

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

import com.vishnu.quickgodelivery.R;

import java.io.File;
import java.io.IOException;
import java.util.List;

// AudioAdapter.java
public class VoiceOrdersViewAdapter extends RecyclerView.Adapter<VoiceOrdersViewAdapter.ViewHolder> {
    private final List<VoiceOrdersModel> voiceOrdersViewModel;
    private final Context context;
    private long CURRENT_PLAYING_REF;
    private long mDownloadId;
    private int CURRENT_PLAY_POS = -1;
    private boolean isPaused = false;
    public static MediaPlayer mediaPlayer = new MediaPlayer();

    public VoiceOrdersViewAdapter(Context context, List<VoiceOrdersModel> voiceOrdersViewModel) {
        this.context = context;
        this.voiceOrdersViewModel = voiceOrdersViewModel;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.srv_voiceorders_list, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VoiceOrdersModel audioModel = voiceOrdersViewModel.get(position);

        holder.audioTitleTV.setText(String.format("%s", audioModel.getAudio_title()));

        setAction(holder.audioActionButton, audioModel.getAudio_title());

        holder.audioActionButton.setOnClickListener(v -> {
            downloadAndPlayAudio(audioModel.getAudio_storage_url(), audioModel.getAudio_title(), holder.audioFeedbackTV, holder.audioActionButton);

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
        TextView audioTitleTV, audioFeedbackTV, audioTimerTV;
        ImageButton audioActionButton;
        ConstraintLayout constItemLayout;

        public ViewHolder(View itemView) {
            super(itemView);
            audioTitleTV = itemView.findViewById(R.id.audioTitle_textView);
            audioActionButton = itemView.findViewById(R.id.orderAudioAction_imageButton);
            audioFeedbackTV = itemView.findViewById(R.id.audioFeedback_textView);
            constItemLayout = itemView.findViewById(R.id.item_layout);
            audioTimerTV = itemView.findViewById(R.id.audioTimer_textView);
        }
    }


    private void downloadAudio(String audioUrl, String fileName, TextView fdbk, ImageButton actionBtn) {
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(audioUrl);

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName + ".mp3");
        request.setTitle("Downloading Audio");
        request.setDescription("Order voice" + fileName);

        fdbk.setText(R.string.downloading);
        Toast.makeText(context, "Downloading audio...", Toast.LENGTH_SHORT).show();

        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                fileName + ".mp3");

        mDownloadId = downloadManager.enqueue(request);
        DownloadReceiver onComplete = new DownloadReceiver(context, fdbk, mDownloadId, actionBtn);
        context.registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);

        playMusic(file.getAbsolutePath(), fdbk, actionBtn);
    }

    public void downloadAndPlayAudio(String audioUrl, String fileName, TextView fdbkTV, ImageButton actionBtn) {
        // Check if file exists
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
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
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
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
        private TextView mFeedback;
        private ImageButton actionBtn;
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
                    mFeedback.setText(R.string.download_completed);
                    actionBtn.setImageResource(R.drawable.baseline_play_arrow_24);
                    actionBtn.setBackgroundColor(ContextCompat.getColor(context, R.color.ctbBtnPlayBg));
//                    Toast.makeText(mContext, "Download completed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}