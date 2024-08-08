package com.vishnu.quickgovendor.vmodels;

public class EditProductVM {
    private String imageUrl;
    private String itemDescp;
    private String itemNameRef;

    public String getItemNameRef() {
        return itemNameRef;
    }

    public void setItemNameRef(String itemNameRef) {
        this.itemNameRef = itemNameRef;
    }

    public int getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(int itemPrice) {
        this.itemPrice = itemPrice;
    }

    private int itemPrice;

    public EditProductVM(String imageUrl, String itemDescp, String itemNameRef, int itemPrice) {
        this.imageUrl = imageUrl;
        this.itemDescp = itemDescp;
        this.itemNameRef = itemNameRef;
        this.itemPrice = itemPrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getItemDescp() {
        return itemDescp;
    }

    public void setItemDescp(String itemDescp) {
        this.itemDescp = itemDescp;
    }
}
