package com.vishnu.quickgoorder.ui.track;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.ola.maps.mapslibrary.models.OlaMapsConfig;
import com.ola.maps.navigation.ui.v5.MapStatusCallback;
import com.ola.maps.navigation.v5.navigation.OlaMapView;
import com.vishnu.quickgoorder.R;

import okhttp3.OkHttpClient;

public class MapActivity extends AppCompatActivity implements MapStatusCallback {
    private OlaMapView olaMapView;
    MapStatusCallback mapStatusCallback;

    OlaMapsConfig olaMapsConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_map);

        olaMapView = findViewById(R.id.olaMapView);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AccessTokenInterceptor())
                .build();

        // Initialize OlaMapView

        olaMapView.onCreate(savedInstanceState);

        olaMapView.initialize(
                mapStatusCallback = this,
                olaMapsConfig = new OlaMapsConfig.Builder()
                        .setApplicationContext(getApplicationContext())
                        .setMapBaseUrl("https://api.olamaps.io")

                        .setInterceptor(new AccessTokenInterceptor())
                        .setClientId("789fc588-3b8c-4bc7-abdd-6a22c2bed56d")
                        .setMinZoomLevel(3.0)
                        .setMaxZoomLevel(21.0)
                        .build()
        );

    }

    @Override
    public void onMapReady() {

    }

    @Override
    public void onMapLoadFailed(String s) {

    }
}