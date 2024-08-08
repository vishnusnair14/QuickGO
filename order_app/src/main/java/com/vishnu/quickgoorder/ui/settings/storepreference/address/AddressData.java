package com.vishnu.quickgoorder.ui.settings.storepreference.address;

import com.google.gson.annotations.SerializedName;
import com.vishnu.quickgoorder.ui.settings.storepreference.store.StoreData;

import java.util.List;

public class AddressData {
    @SerializedName("name")
    private String name;
    @SerializedName("full_address")
    private String fullAddress;

    @SerializedName("address_lat")
    private double addressLat;

    @SerializedName("address_type")
    private String addressType;

    @SerializedName("address_lon")
    private double addressLon;

    @SerializedName("city")
    private String city;

    @SerializedName("phone_no")
    private String phoneNo;

    @SerializedName("pincode")
    private String pincode;

    private List<StoreData> nearbyShops;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Getters and Setters
    public String getFullAddress() {
        return fullAddress;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public void setFullAddress(String fullAddress) {
        this.fullAddress = fullAddress;
    }

    public double getAddressLat() {
        return addressLat;
    }

    public void setAddressLat(double addressLat) {
        this.addressLat = addressLat;
    }

    public double getAddressLon() {
        return addressLon;
    }

    public void setAddressLon(double addressLon) {
        this.addressLon = addressLon;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public List<StoreData> getNearbyShops() {
        return nearbyShops;
    }

    public void setNearbyShops(List<StoreData> nearbyShops) {
        this.nearbyShops = nearbyShops;
    }
}
