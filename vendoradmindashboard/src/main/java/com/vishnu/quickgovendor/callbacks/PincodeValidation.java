package com.vishnu.quickgovendor.callbacks;

public interface PincodeValidation {
    void onSuccess(boolean isPinValid, String postOffName, String cityName);
}

