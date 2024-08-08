package com.vishnu.quickgodelivery.ui.order.voice;

public class VoiceOrdersViewModel {
    private String audioUrl;
    private String audioTitle;
    private long audioDownloadID;
    private boolean isDownloaded;

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean downloaded) {
        isDownloaded = downloaded;
    }

    public long getAudioDownloadID() {
        return audioDownloadID;
    }

    public void setAudioDownloadID(long audioDownloadID) {
        this.audioDownloadID = audioDownloadID;
    }

    public VoiceOrdersViewModel() {

    }

    public String getAudioTitle() {
        return audioTitle;
    }

    public void setAudioTitle(String audioTitle) {
        this.audioTitle = audioTitle;
    }


    public VoiceOrdersViewModel(String audioUrl, String audioTitle, boolean isDownloaded) {
        this.audioUrl = audioUrl;
        this.audioTitle = audioTitle;
        this.isDownloaded = isDownloaded;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }
}
