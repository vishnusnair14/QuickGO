package com.vishnu.quickgodelivery.ui.all_orders;

public interface OrderModel {
    String getUser_id();
    String getOrder_id();
    String getOrder_type();
    String getOrder_by_voice_doc_id();

    String getDelivery_full_address();

    String getOrder_by_voice_audio_ref_id();
}
