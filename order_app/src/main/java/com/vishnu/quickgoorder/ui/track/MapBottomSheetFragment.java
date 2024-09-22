package com.vishnu.quickgoorder.ui.track;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.vishnu.quickgoorder.R;

public class MapBottomSheetFragment extends BottomSheetDialogFragment {

    private GoogleMap mMap;
    private MapView mapView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map_bottom_sheet, container, false);

        mapView = view.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this::onMapReady);

        return view;
    }

    private void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Set up the map with the delivery partner's location
        LatLng deliveryPartnerLocation = new LatLng(10.786942293903314, 76.65547228986703);
        mMap.addMarker(new MarkerOptions().position(deliveryPartnerLocation).title("Delivery Partner"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(deliveryPartnerLocation, 10));
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
