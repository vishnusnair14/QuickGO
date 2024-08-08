package com.vishnu.quickgoorder.server.sse;

public class SSEModel {
    private String order_status_label;
    private int order_status_no;
    private String order_id;
    private String order_time;
    private String time;
    private String order_status_label_fg_color;
    private String order_status_label_bg_color;
    private boolean is_partner_assigned;

    public String getOrder_status_label() {
        return order_status_label;
    }

    public void setOrder_status_label(String order_status_label) {
        this.order_status_label = order_status_label;
    }

    public int getOrder_status_no() {
        return order_status_no;
    }

    public void setOrder_status_no(int order_status_no) {
        this.order_status_no = order_status_no;
    }

    public boolean getIs_partner_assigned() {
        return is_partner_assigned;
    }

    public void setIs_partner_assigned(boolean is_partner_assigned) {
        this.is_partner_assigned = is_partner_assigned;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
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

    public void setOrder_time(String order_time) {
        this.order_time = order_time;
    }

    public String getOrder_status_label_fg_color() {
        return order_status_label_fg_color;
    }

    public void setOrder_status_label_fg_color(String order_status_label_fg_color) {
        this.order_status_label_fg_color = order_status_label_fg_color;
    }

    public String getOrder_status_label_bg_color() {
        return order_status_label_bg_color;
    }

    public void setOrder_status_label_bg_color(String order_status_label_bg_color) {
        this.order_status_label_bg_color = order_status_label_bg_color;
    }
}
