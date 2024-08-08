package com.vishnu.quickgovendor.callbacks;

import java.util.Map;

public interface GeocodeCallback {
    void onSuccess(Map<String, String> resultMap);
    void onFailure(String errorMessage);
}
