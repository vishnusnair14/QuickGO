package com.vishnu.quickgovendor.services;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import androidx.annotation.NonNull;

public class GPSLocationProvider implements LocationListener {
    Context context;
    private final LocationUpdateListener locationUpdateListener;

    public GPSLocationProvider(Context context, LocationUpdateListener callback) {
        this.context = context;
        this.locationUpdateListener = callback;
    }

    @Override
    public void onLocationChanged(Location location) {
        // Handle new location updates
        locationUpdateListener.onLocationUpdated(location.getLatitude(), location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Handle changes in location provider status (e.g., GPS enabled/disabled)
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        // Handle when the location provider is enabled
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        // Handle when the location provider is disabled
    }

}
