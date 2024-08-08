package com.vishnu.quickgoorder.ui.home.recommendation.items;

public class ItemModel {
    private String item_image_url, item_name, item_qty, item_price_unit, item_name_reference;
    private int item_price;
    private String item_id;

    public String getItem_name_reference() {
        return item_name_reference;
    }

    public void setItem_name_reference(String item_name_reference) {
        this.item_name_reference = item_name_reference;
    }

    public ItemModel() {
    }

    public ItemModel(String item_id, String item_image_url, String item_name, String item_qty, int item_price, String item_price_unit, String item_name_reference) {
        this.item_id = item_id;
        this.item_image_url = item_image_url;
        this.item_name = item_name;
        this.item_qty = item_qty;
        this.item_price = item_price;
        this.item_price_unit = item_price_unit;
        this.item_name_reference = item_name_reference;
    }


    public String getItem_price_unit() {
        return item_price_unit;
    }

    public void setItem_price_unit(String item_price_unit) {
        this.item_price_unit = item_price_unit;
    }

    public String getItem_id() {
        return item_id;
    }

    public void setItem_id(String item_id) {
        this.item_id = item_id;
    }

    public int getItem_price() {
        return item_price;
    }

    public void setItem_price(int item_price) {
        this.item_price = item_price;
    }

    public String getItem_image_url() {
        return item_image_url;
    }

    public void setItem_image_url(String item_image_url) {
        this.item_image_url = item_image_url;
    }

    public String getItem_name() {
        return item_name;
    }

    public void setItem_name(String item_name) {
        this.item_name = item_name;
    }

    public String getItem_qty() {
        return item_qty;
    }

    public void setItem_qty(String item_qty) {
        this.item_qty = item_qty;
    }

}
