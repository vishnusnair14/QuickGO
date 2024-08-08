package com.vishnu.quickgodelivery.ui.order.info.obv;

public class StorePrefDataModel {
    private String shopDistrict;
    private String shopStreet;
    private String shopId;
    private String shopEmail;
    private String shopPhone;
    private String shopImageUrl;
    private String shopPincode;
    private double shopLat;
    private double shopLon;
    private String shopCity;
    private String shopName;
    private String shopAddress;
    private int shopPreference;
    private double distanceKm;
    private double displacement;

    // Constructor
    public StorePrefDataModel(String shopDistrict, String shopStreet, String shopId, String shopEmail, String shopPhone,
                              String shopImageUrl, String shopPincode, double shopLat, double shopLon, String shopCity,
                              String shopName, String shopAddress, int shopPreference, double distanceKm, double displacement) {
        this.shopDistrict = shopDistrict;
        this.shopStreet = shopStreet;
        this.shopId = shopId;
        this.shopEmail = shopEmail;
        this.shopPhone = shopPhone;
        this.shopImageUrl = shopImageUrl;
        this.shopPincode = shopPincode;
        this.shopLat = shopLat;
        this.shopLon = shopLon;
        this.shopCity = shopCity;
        this.shopName = shopName;
        this.shopAddress = shopAddress;
        this.shopPreference = shopPreference;
        this.distanceKm = distanceKm;
        this.displacement = displacement;
    }

    // Getters and setters for all fields

    public String getShopDistrict() {
        return shopDistrict;
    }

    public String getShopStreet() {
        return shopStreet;
    }

    public String getShopId() {
        return shopId;
    }

    public String getShopEmail() {
        return shopEmail;
    }

    public String getShopPhone() {
        return shopPhone;
    }

    public String getShopImageUrl() {
        return shopImageUrl;
    }

    public String getShopPincode() {
        return shopPincode;
    }

    public double getShopLat() {
        return shopLat;
    }

    public double getShopLon() {
        return shopLon;
    }

    public String getShopCity() {
        return shopCity;
    }

    public String getShopName() {
        return shopName;
    }

    public String getShopAddress() {
        return shopAddress;
    }

    public int getShopPreference() {
        return shopPreference;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public double getDisplacement() {
        return displacement;
    }
}
