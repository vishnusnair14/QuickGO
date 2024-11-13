package com.vishnu.quickgoorder.server.models;

public class VoiceOrderRequest {
    private String user_id;
    private String order_id;
    private String voice_order_ref_id;
    private String audio_key;
    private String audio_storage_url;
    private String audio_title;

    // Constructor, getters, and setters

    public VoiceOrderRequest(String user_id, String order_id, String voice_order_ref_id, String audio_key, String audio_storage_url, String audio_title) {
        this.user_id = user_id;
        this.order_id = order_id;
        this.voice_order_ref_id = voice_order_ref_id;
        this.audio_key = audio_key;
        this.audio_storage_url = audio_storage_url;
        this.audio_title = audio_title;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getOrder_id() {
        return order_id;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }

    public String getVoice_order_ref_id() {
        return voice_order_ref_id;
    }

    public void setVoice_order_ref_id(String voice_order_ref_id) {
        this.voice_order_ref_id = voice_order_ref_id;
    }

    public String getAudio_key() {
        return audio_key;
    }

    public void setAudio_key(String audio_key) {
        this.audio_key = audio_key;
    }

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
}