package com.vishnu.quickgodelivery.ui.all_orders;


public class OBSOrderModel implements OrderModel {
    private String order_type;
    private String order_by_voice_doc_id;
    private String order_by_voice_audio_ref_id;
    private String delivery_full_address;
    private double order_delivery_destination_distance;
    private String order_id;
    private String order_saved_status;
    private String order_time;
    private long order_time_millis;
    private double pickup_destination_distance;
    private String shop_id;
    private String shop_name;
    private String user_phno;
    private String shop_phno;
    private String user_id;

    @Override
    public String getOrder_by_voice_audio_ref_id() {
        return order_by_voice_audio_ref_id;
    }

    @Override
    public String getOrder_by_voice_doc_id() {
        return order_by_voice_doc_id;
    }

    @Override
    public String getOrder_type() {
        return order_type;
    }

    @Override
    public String getUser_phno() {
        return user_phno;
    }

    @Override
    public String getDelivery_full_address() {
        return delivery_full_address;
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


    public double getPickup_destination_distance() {
        return pickup_destination_distance;
    }


    public String getShop_id() {
        return shop_id;
    }


    public String getShop_name() {
        return shop_name;
    }


    public String getShop_phno() {
        return shop_phno;
    }

    @Override
    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }


    public void setOrder_type(String order_type) {
        this.order_type = order_type;
    }

    public void setOrder_by_voice_doc_id(String order_by_voice_doc_id) {
        this.order_by_voice_doc_id = order_by_voice_doc_id;
    }

    public void setOrder_by_voice_audio_ref_id(String order_by_voice_audio_ref_id) {
        this.order_by_voice_audio_ref_id = order_by_voice_audio_ref_id;
    }

    public void setDelivery_full_address(String delivery_full_address) {
        this.delivery_full_address = delivery_full_address;
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

    public void setPickup_destination_distance(double pickup_destination_distance) {
        this.pickup_destination_distance = pickup_destination_distance;
    }

    public void setShop_id(String shop_id) {
        this.shop_id = shop_id;
    }

    public void setShop_name(String shop_name) {
        this.shop_name = shop_name;
    }

    public void setShop_phno(String shop_phno) {
        this.shop_phno = shop_phno;
    }
}
