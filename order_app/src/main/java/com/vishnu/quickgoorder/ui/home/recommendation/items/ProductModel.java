package com.vishnu.quickgoorder.ui.home.recommendation.items;

public class ProductModel {
    private String item_image_url, item_name, item_qty, item_price_unit, item_name_reference;
    private int item_price;
    private String item_id;


    public ProductModel() {
    }

    public ProductModel(String item_id, String item_image_url, String item_name
            , int item_price, String item_price_unit,
                        String item_name_reference) {
        this.item_id = item_id;
        this.item_image_url = item_image_url;
        this.item_name = item_name;
        this.item_price = item_price;
        this.item_price_unit = item_price_unit;
        this.item_name_reference = item_name_reference;
    }


    public String getItem_price_unit() {
        return item_price_unit;
    }


    public String getItem_id() {
        return item_id;
    }


    public int getItem_price() {
        return item_price;
    }


    public String getItem_image_url() {
        return item_image_url;
    }


    public String getItem_name() {
        return item_name;
    }


}
