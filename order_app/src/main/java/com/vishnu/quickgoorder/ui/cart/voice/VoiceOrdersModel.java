package com.vishnu.quickgoorder.ui.cart.voice;

public class VoiceOrdersModel {
    private String audio_storage_url;
    private String audio_title;
    private long audio_download_id;
    private boolean is_downloaded;

    public String getAudio_storage_url() {
        return audio_storage_url;
    }

    public void setAudio_storage_url(String audio_storage_url) {
        this.audio_storage_url = audio_storage_url;
    }

    public String getAudio_title() {
        return audio_title;
    }

    public void setAudio_title(String audio_title) {
        this.audio_title = audio_title;
    }

    public long getAudio_download_id() {
        return audio_download_id;
    }

    public void setAudio_download_id(long audio_download_id) {
        this.audio_download_id = audio_download_id;
    }

    public boolean isIs_downloaded() {
        return is_downloaded;
    }

    public void setIs_downloaded(boolean is_downloaded) {
        this.is_downloaded = is_downloaded;
    }
}
