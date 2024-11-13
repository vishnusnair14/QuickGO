package com.vishnu.quickgoorder.ui.home.recommendation.orders;

import com.google.firebase.firestore.GeoPoint;

public class AllOrdersModel {
    private String shop_name;
    private String order_id;
    private String order_time;
    private String order_status_label;
    private String order_status_fg_color;
    private String delivery_full_address;
    private GeoPoint delivery_address_loc;
    private String order_status_bg_color;

    public String getShop_name() {
        return shop_name;
    }

    public void setShop_name(String shop_name) {
        this.shop_name = shop_name;
    }

    public String getOrder_id() {
        return order_id;
    }

    public void setOrder_id(String order_id) {
        this.order_id = order_id;
    }

    public String getOrder_time() {
        return order_time;
    }

    public String getDelivery_full_address() {
        return delivery_full_address;
    }


    public GeoPoint getDelivery_loc_coordinates() {
        return delivery_address_loc;
    }

    public void setOrder_time(String order_time) {
        this.order_time = order_time;
    }

    public String getOrder_status_label() {
        return order_status_label;
    }

    public void setOrder_status_label(String order_status_label) {
        this.order_status_label = order_status_label;
    }

    public String getOrder_status_fg_color() {
        return order_status_fg_color;
    }

    public void setOrder_status_fg_color(String order_status_fg_color) {
        this.order_status_fg_color = order_status_fg_color;
    }

    public String getOrder_status_bg_color() {
        return order_status_bg_color;
    }

    public void setOrder_status_bg_color(String order_status_bg_color) {
        this.order_status_bg_color = order_status_bg_color;
    }
}
