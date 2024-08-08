package com.vishnu.quickgodelivery.miscellaneous;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedDataView extends ViewModel {
    private final MutableLiveData<Double> dpLat = new MutableLiveData<>();
    private final MutableLiveData<Double> dpLon = new MutableLiveData<>();
    private final MutableLiveData<String> shopName = new MutableLiveData<>();
    private final MutableLiveData<String> voiceOrderID = new MutableLiveData<>();
    private final MutableLiveData<String> userID = new MutableLiveData<>();
    private final MutableLiveData<String> shopID = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLocationProviderEnabled = new MutableLiveData<>();
    private final MutableLiveData<String> orderKey = new MutableLiveData<>();

    public LiveData<Boolean> getIsLocationProviderEnabled() {
        return isLocationProviderEnabled;
    }

    public void setIsLocationProviderEnabled(boolean state) {
        isLocationProviderEnabled.setValue(state);
    }

    public LiveData<String> getShopName() {
        return shopName;
    }

    public LiveData<String> getUserID() {
        return userID;
    }

    public LiveData<String> getShopID() {
        return shopID;
    }

    public LiveData<String> getOrderKey() {
        return orderKey;
    }

    public LiveData<String> getVoiceOrderID() {
        return voiceOrderID;
    }


    public LiveData<Double> getDpLat() {
        return dpLat;
    }

    public LiveData<Double> getDpLon() {
        return dpLon;
    }

    public void setDpLon(double lon) {
        dpLon.setValue(lon);
    }


    public void setDpLat(double lat) {
        dpLat.setValue(lat);
    }

    public void setShopName(String sname) {
        shopName.setValue(sname);
    }

    public void setVoiceOrderID(String voiceOrderID) {
        this.voiceOrderID.setValue(voiceOrderID);
    }

    public void setOrderKey(String key) {
        this.orderKey.setValue(key);
    }


    public void setUserID(String cid) {
        userID.setValue(cid);
    }

    public void setShopID(String sid) {
        shopID.setValue(sid);
    }


}
