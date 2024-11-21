package com.vishnu.quickgoorder.ui.settings.address.set_location;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.databinding.FragmentSetLocationOnMapBinding;
import com.vishnu.quickgoorder.service.LocationService;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class SetLocationOnMapFragment extends Fragment implements OnMapReadyCallback {
    private final String LOG_TAG = "SetLocationOnMapFragment";
    FragmentSetLocationOnMapBinding binding;
    private MapView mapView;
    private GoogleMap gMap;
    private Marker locationMarker;
    private TextView btnSaveLocation;
    private double latitude = 0;
    private double longitude = 0;
    private static final int GPS_REQUEST_CODE = 1001;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private double lat_from_map;
    private double lon_from_map;
    private static final String ARG_PARAM1 = "lat_from_map";
    private static final String ARG_PARAM2 = "lon_from_map";
    private FusedLocationProviderClient fusedLocationClient;

    public SetLocationOnMapFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentSetLocationOnMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mapView = binding.mapView2;
        btnSaveLocation = binding.btnSaveLocation;

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        btnSaveLocation.setOnClickListener(v -> saveLocationAndOpenAddressFrag());

        return root;
    }

    private boolean isWithinGeofence(double lat, double lon, LatLng center, double radiusInMeters) {
        double earthRadius = 6371000; // Radius of Earth in meters

        double dLat = Math.toRadians(lat - center.latitude);
        double dLon = Math.toRadians(lon - center.longitude);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(center.latitude)) * Math.cos(Math.toRadians(lat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;

        return distance <= radiusInMeters;
    }

    private void saveLocationAndOpenAddressFrag() {
        if (locationMarker != null) {
            LatLng position = locationMarker.getPosition();
            double latitude = position.latitude;
            double longitude = position.longitude;

            String address = getAddressFromCoordinates(latitude, longitude);

            // Define your geofence boundary (center and radius or polygon points)
            LatLng geofenceCenter = new LatLng(12.313790336550827, 76.64005029671401);
            double geofenceRadiusInMeters = 10000;

            if (isWithinGeofence(latitude, longitude, geofenceCenter, geofenceRadiusInMeters)) {
                // Save or use the address as needed
                lat_from_map = latitude;
                lon_from_map = longitude;
                Toast.makeText(getContext(), "Location captured: " + address, Toast.LENGTH_SHORT).show();

                Bundle args = new Bundle();
                args.putDouble(ARG_PARAM1, lat_from_map);
                args.putDouble(ARG_PARAM2, lon_from_map);
                if (lat_from_map != 0 && lon_from_map != 0) {
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_nav_setAddressLocation_to_nav_addAddress, args);
                    locationMarker = null;
                } else {
                    Toast.makeText(requireContext(), "Error fetching coordinates, try refresh", Toast.LENGTH_SHORT).show();
                }
            } else {
                showBottomSheetDialog();
            }
        } else {
            Toast.makeText(getContext(), "Please select a location", Toast.LENGTH_SHORT).show();
        }
    }

    private void showBottomSheetDialog() {
        // Create BottomSheetDialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());

        // Inflate custom layout
        View bottomSheetView = getLayoutInflater().inflate(R.layout.location_outside_deliv_area, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.setCancelable(false);

        // Set message
//        TextView messageTextView = bottomSheetView.findViewById(R.id.messageTextView);
//        messageTextView.setText(R.string.location_is_outside_deliverable_area);

        // Set dismiss button click listener
        Button dismissButton = bottomSheetView.findViewById(R.id.dismissButton);
        dismissButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        // Show the dialog
        bottomSheetDialog.show();
    }

    private String getAddressFromCoordinates(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            Log.d(LOG_TAG, e.toString());
        }
        return "No address found";
    }

    private void checkAndPromptForGPS() {
        // Create a LocationRequest to request high-accuracy location
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
                .setMinUpdateIntervalMillis(5000L)
                .build();

        // Build a LocationSettingsRequest
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        // Check location settings with the SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(requireActivity());
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(requireActivity(), locationSettingsResponse -> {
            // GPS is enabled, you can proceed with accessing location
            new Handler().postDelayed(this::zoomToUserLocation, 2000);
        });

        task.addOnFailureListener(requireActivity(), e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    // Show the dialog to enable GPS
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(requireActivity(), GPS_REQUEST_CODE);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Handle error in showing the dialog
                }
            }
        });
    }

    private void zoomToUserLocation() {
        // Check if location permission is granted
        if (isAdded()) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // Request location permissions if not granted
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);

                return;
            }

            // Proceed to get the user's last known location
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    // Get user's current location
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

                    // Set map camera to the location with zoom level 15
                    gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 20));
                }
            });

        }
    }


    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            if (LocationService.ACTION_LOCATION_BROADCAST.equals(intent.getAction())) {
                latitude = intent.getDoubleExtra(LocationService.EXTRA_LATITUDE, 0.00);
                longitude = intent.getDoubleExtra(LocationService.EXTRA_LONGITUDE, 0.00);

            }
        }
    };


    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        checkAndPromptForGPS();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(LocationService.ACTION_LOCATION_BROADCAST);
        requireContext().registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStop() {
        super.onStop();
        requireContext().unregisterReceiver(locationReceiver);
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

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        gMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        gMap.getUiSettings().setZoomControlsEnabled(true);
        gMap.getUiSettings().setCompassEnabled(true);


        // Check if we have permission to access location
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            gMap.setMyLocationEnabled(true);
            gMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
        gMap.getUiSettings().setMyLocationButtonEnabled(true);
        gMap.setMyLocationEnabled(true);
        gMap.getUiSettings().setScrollGesturesEnabled(true);
        gMap.getUiSettings().setTiltGesturesEnabled(true);
        gMap.getUiSettings().setRotateGesturesEnabled(true);
        gMap.getUiSettings().setZoomGesturesEnabled(true);

        zoomToUserLocation();

        // Add a marker and listen for map taps to place a new marker
        gMap.setOnMapClickListener(latLng -> {
            if (locationMarker != null) locationMarker.remove();
            locationMarker = gMap.addMarker(new MarkerOptions().position(latLng).title("Items will be delivered here"));
            if (locationMarker != null) locationMarker.setDraggable(true);
            gMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        });
    }
}