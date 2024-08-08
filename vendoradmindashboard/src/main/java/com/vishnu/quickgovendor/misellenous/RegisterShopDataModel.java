package com.vishnu.quickgovendor.misellenous;

import com.google.firebase.firestore.GeoPoint;
import com.google.gson.annotations.SerializedName;

public class RegisterShopDataModel {

    @SerializedName("shop_name")
    private String shopName;

    @SerializedName("shop_reg_email")
    private String shopRegEmail;

    @SerializedName("shop_reg_password")
    private String shopRegPassword;

    @SerializedName("shop_image_url")
    private String shopImageUrl;

    @SerializedName("shop_loc_coords")
    private GeoPoint shopLocCords;

    @SerializedName("shop_city")
    private String shopCity;

    @SerializedName("shop_pincode")
    private String shopPincode;

    public void setShopRegPassword(String shopRegPassword) {
        this.shopRegPassword = shopRegPassword;
    }

    @SerializedName("shop_street")
    private String shopStreet;

    public RegisterShopDataModel(String shopName, String shopRegEmail, String shopRegPassword, String shopImageUrl, GeoPoint shopLocCords, String shopCity, String shopPincode, String shopStreet) {
        this.shopName = shopName;
        this.shopRegEmail = shopRegEmail;
        this.shopRegPassword = shopRegPassword;
        this.shopImageUrl = shopImageUrl;
        this.shopLocCords = shopLocCords;
        this.shopCity = shopCity;
        this.shopPincode = shopPincode;
        this.shopStreet = shopStreet;
    }

    public String getShopCity() {
        return shopCity;
    }

    public void setShopCity(String shopCity) {
        this.shopCity = shopCity;
    }

    public String getShopRegEmail() {
        return shopRegEmail;
    }

    public void setShopRegEmail(String shopRegEmail) {
        this.shopRegEmail = shopRegEmail;
    }

    public String getShopStreet() {
        return shopStreet;
    }

    public void setShopStreet(String shopStreet) {
        this.shopStreet = shopStreet;
    }

    public String getShopImageUrl() {
        return shopImageUrl;
    }

    public void setShopImageUrl(String shopImageUrl) {
        this.shopImageUrl = shopImageUrl;
    }

    public GeoPoint getShopLocCords() {
        return shopLocCords;
    }

    public void setShopLocCords(GeoPoint shopLocCords) {
        this.shopLocCords = shopLocCords;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getShopPincode() {
        return shopPincode;
    }

    public void setShopPincode(String shopPincode) {
        this.shopPincode = shopPincode;
    }

    public String getShopRegPassword() {
        return shopRegPassword;
    }
}

