package com.vishnu.quickgoorder.ui.settings.address.manage_address;

import com.google.gson.annotations.SerializedName;
import com.vishnu.quickgoorder.ui.settings.storepreference.store.StoreData;

import java.util.List;

public class SavedAddressModel {
    @SerializedName("name")
    private String name;

    @SerializedName("full_address")
    private String fullAddress;

    @SerializedName("address_lat")
    private double addressLat;

    @SerializedName("address_lon")
    private double addressLon;

    @SerializedName("district")
    private String district;

    @SerializedName("pincode")
    private String pincode;

    @SerializedName("phone_no")
    private  String phoneNo;

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

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
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

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
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
