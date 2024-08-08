package com.vishnu.quickgodelivery.ui.all_orders;


public class OBVOrderModel implements OrderModel {

    private String delivery_full_address;
    private String order_by_voice_audio_ref_id;
    private String order_by_voice_doc_id;
    private double order_delivery_destination_distance;
    private String order_id;
    private String order_saved_status;
    private String order_time;
    private long order_time_millis;
    private String order_type;
    private double pickup_destination_distance;
    private String user_id;


    @Override
    public String getDelivery_full_address() {
        return delivery_full_address;
    }

    @Override
    public String getOrder_by_voice_audio_ref_id() {
        return order_by_voice_audio_ref_id;
    }


    public double getOrder_delivery_destination_distance() {
        return order_delivery_destination_distance;
    }

    @Override
    public String getOrder_id() {
        return order_id;
    }


    public String getOrder_saved_status() {
        return order_saved_status;
    }

    public String getOrder_time() {
        return order_time;
    }

    @Override
    public String getOrder_type() {
        return order_type;
    }

    @Override
    public String getOrder_by_voice_doc_id() {
        return order_by_voice_doc_id;
    }


    public double getPickup_destination_distance() {
        return pickup_destination_distance;
    }

    @Override
    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public void setDelivery_full_address(String delivery_full_address) {
        this.delivery_full_address = delivery_full_address;
    }

    public void setOrder_by_voice_audio_ref_id(String order_by_voice_audio_ref_id) {
        this.order_by_voice_audio_ref_id = order_by_voice_audio_ref_id;
    }

    public void setOrder_by_voice_doc_id(String order_by_voice_doc_id) {
        this.order_by_voice_doc_id = order_by_voice_doc_id;
    }

    public void setOrder_delivery_destination_distance(double order_delivery_destination_distance) {
        this.order_delivery_destination_distance = order_delivery_destination_distance;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }

    public void setOrder_saved_status(String order_saved_status) {
        this.order_saved_status = order_saved_status;
    }

    public void setOrder_time(String order_time) {
        this.order_time = order_time;
    }

    public void setOrder_time_millis(long order_time_millis) {
        this.order_time_millis = order_time_millis;
    }

    public void setOrder_type(String order_type) {
        this.order_type = order_type;
    }

    public void setPickup_destination_distance(double pickup_destination_distance) {
        this.pickup_destination_distance = pickup_destination_distance;
    }
}
