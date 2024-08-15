package com.vishnu.quickgoorder.ui.settings.storepreference.store;

import com.google.gson.annotations.SerializedName;

public class StoreData {

    @SerializedName("shop_id")
    private String shopId;

    @SerializedName("shop_preference")
    private int shop_preference;

    @SerializedName("shop_name")
    private String shopName;

    @SerializedName("shop_place")
    private String shopPlace;

    @SerializedName("shop_image_url")
    private String shopImageUrl;

    @SerializedName("shop_lat")
    private double shopLat;

    @SerializedName("shop_lon")
    private double shopLon;

    @SerializedName("shop_city")
    private String shopCity;

    @SerializedName("shop_district")
    private String shopDistrict;

    @SerializedName("distance_km")
    private double distanceKm;

    public int getShop_preference() {
        return shop_preference;
    }

    public void setShop_preference(int shop_preference) {
        this.shop_preference = shop_preference;
    }

    public StoreData(String shopName) {
        this.shopName = shopName;
        this.shop_preference = 1; // Default preference
    }

    // Getters and Setters
    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getShopPlace() {
        return shopPlace;
    }

    public void setShopPlace(String shopPlace) {
        this.shopPlace = shopPlace;
    }

    public String getShopImageUrl() {
        return shopImageUrl;
    }

    public void setShopImageUrl(String shopImageUrl) {
        this.shopImageUrl = shopImageUrl;
    }

    public double getShopLat() {
        return shopLat;
    }

    public void setShopLat(double shopLat) {
        this.shopLat = shopLat;
    }

    public double getShopLon() {
        return shopLon;
    }

    public void setShopLon(double shopLon) {
        this.shopLon = shopLon;
    }

    public String getShopCity() {
        return shopCity;
    }

    public void setShopCity(String shopCity) {
        this.shopCity = shopCity;
    }

    public String getShopDistrict() {
        return shopDistrict;
    }

    public void setShopDistrict(String shopDistrict) {
        this.shopDistrict = shopDistrict;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }
}
