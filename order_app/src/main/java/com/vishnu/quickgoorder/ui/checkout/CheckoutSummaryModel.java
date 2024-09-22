package com.vishnu.quickgoorder.ui.checkout;

public class CheckoutSummaryModel {
    private String item_name;
    private int item_price, item_qty, item_final_price, grand_total = 0;

    public int getItem_final_price() {
        return item_final_price;
    }


    public CheckoutSummaryModel(String item_name, int item_price, int item_qty, int item_final_price, int grand_total) {
        this.item_name = item_name;
        this.item_price = item_price;
        this.item_qty = item_qty;
        this.item_final_price = item_final_price;
        this.grand_total += grand_total;
    }

    public int getGrand_total() {
        return grand_total;
    }


    public String getItem_name() {
        return item_name;
    }


    public int getItem_price() {
        return item_price;
    }


    public int getItem_qty() {
        return item_qty;
    }
}
