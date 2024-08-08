package com.vishnu.quickgoorder.miscellaneous;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedDataView extends ViewModel {
    private final MutableLiveData<Float> totalAmount = new MutableLiveData<>();
    private final MutableLiveData<String> shopID = new MutableLiveData<>();
    private final MutableLiveData<String> userCity = new MutableLiveData<>();
    private final MutableLiveData<String> shopCity = new MutableLiveData<>();
    private final MutableLiveData<String> shopName = new MutableLiveData<>();
    private final MutableLiveData<Double> shopLon = new MutableLiveData<>();
    private final MutableLiveData<Double> shopLat = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLocProviderEnabled = new MutableLiveData<>();

    public MutableLiveData<Boolean> getIsLocProviderEnabled() {
        return isLocProviderEnabled;
    }

    public void setIsLocProviderEnabled(boolean en) {
        isLocProviderEnabled.setValue(en);
    }

    public void setShopID(String amount) {
        shopID.setValue(amount);
    }

    public void setShopCity(String city) {
        shopCity.setValue(city);
    }

    public LiveData<String> getShopCity() {
        return shopCity;
    }

    public LiveData<Double> getShopLon() {
        return shopLon;
    }

    public LiveData<Double> getShopLat() {
        return shopLat;
    }

    public void setShopLat(Double lat) {
        shopLat.setValue(lat);
    }

    public void setShopLon(Double lon) {
        shopLon.setValue(lon);
    }

    public LiveData<String> getShopID() {
        return shopID;
    }

    public void setTotalAmount(float amount) {
        totalAmount.setValue(amount);
    }

    public LiveData<Float> getTotalAmount() {
        return totalAmount;
    }

    public void setUserCity(String city) {
        userCity.setValue(city);
    }

    public LiveData<String> getUserCity() {
        return userCity;
    }

    public void setShopName(String name) {
        shopName.setValue(name);
    }

    public LiveData<String> getShopName() {
        return shopName;
    }
}
