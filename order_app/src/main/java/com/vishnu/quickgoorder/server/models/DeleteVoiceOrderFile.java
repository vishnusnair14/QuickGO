package com.vishnu.quickgoorder.server.models;


public class DeleteVoiceOrderFile {

    private final String from;
    private String user_id;
    private String order_by_voice_doc_id;
    private String order_by_voice_audio_ref_id;
    private String audio_key;
    private boolean delete_all_files;
    private String shop_id;

    public DeleteVoiceOrderFile(String from, String user_id,
                                String order_by_voice_doc_id,
                                String order_by_voice_audio_ref_id,
                                String shop_id,
                                String audio_key, boolean delete_all_files) {
        this.from = from;
        this.user_id = user_id;
        this.order_by_voice_doc_id = order_by_voice_doc_id;
        this.order_by_voice_audio_ref_id = order_by_voice_audio_ref_id;
        this.shop_id = shop_id;

        this.audio_key = audio_key;
        this.delete_all_files = delete_all_files;

    }
}
