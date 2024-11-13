package com.vishnu.quickgoorder.ui.home.recommendation;

import com.google.gson.annotations.SerializedName;

public class HomeRecommendationModel {
    @SerializedName("distance_km")
    private double distanceKm;

    @SerializedName("shop_id")
    private String shopId;

    @SerializedName("shop_image_url")
    private String shopImageUrl;

    @SerializedName("shop_lat")
    private double shopLat;

    @SerializedName("shop_lon")
    private double shopLon;

    @SerializedName("shop_name")
    private String shopName;

    @SerializedName("shop_street")
    private String shopStreet;

    @SerializedName("shop_district")
    private String shopDistrict;

    @SerializedName("shop_state")
    private String shopState;



    public String getShopDistrict() {
        return shopDistrict;
    }

    public void setShopDistrict(String shopDistrict) {
        this.shopDistrict = shopDistrict;
    }


    public String getShopStreet() {
        return shopStreet;
    }

    public void setShopStreet(String shopStreet) {
        this.shopStreet = shopStreet;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getShopId() {
        return shopId;
    }

    public String getShopState() {
        return shopState;
    }

    public void setShopState(String shopState) {
        this.shopState = shopState;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
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

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

}
