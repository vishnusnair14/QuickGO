package com.vishnu.quickgoorder.ui.settings.address.add_address;

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
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.ResolvableApiException;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.callbacks.PincodeValidation;
import com.vishnu.quickgoorder.databinding.FragmentAddAddressBinding;
import com.vishnu.quickgoorder.miscellaneous.Utils;
import com.vishnu.quickgoorder.server.sapi.APIService;
import com.vishnu.quickgoorder.server.sapi.ApiServiceGenerator;
import com.vishnu.quickgoorder.service.LocationService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddAddressFragment extends Fragment {
    private final String LOG_TAG = "AddAddressFragment";
    private TextView postOffValNameTV;
    private EditText pinCodeEditText;
    private EditText phoneNoET;
    private String pincode, phoneno;
    private String pincodeFromDistrict;
    TextView cityStateTV;
    private double latitude = 0;
    private double longitude = 0;
    private final String NOT_FOUND = "NOT FOUND!";
    private String addressType = "Others";
    CheckBox setAsDefaultAddressCheckBox;
    String selectedDistrict;
    String selectedState;
    private TextView locationTV;
    private final DecimalFormat coordinateFormat = new DecimalFormat("#.##########");
    private FragmentAddAddressBinding binding;
    private FirebaseUser user;
    private Spinner spinnerState, spinnerDistrict;
    private static final int GPS_REQUEST_CODE = 1001;

    private static final String ARG_PARAM1 = "lat_from_map";
    private static final String ARG_PARAM2 = "lon_from_map";

    private double address_lat;
    private double address_lon;
    NavController navController;
    FragmentManager fragmentManager;

    public AddAddressFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            address_lat = getArguments().getDouble(ARG_PARAM1, 0);
            address_lon = getArguments().getDouble(ARG_PARAM2, 0);
        }
        navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main);
        fragmentManager = requireActivity().getSupportFragmentManager();

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentAddAddressBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        user = FirebaseAuth.getInstance().getCurrentUser();

        TextView addNewAddressButton = binding.addNewAddressButton;
        pinCodeEditText = binding.pincodeTextBox;
        cityStateTV = binding.cityStateViewTextView;
        postOffValNameTV = binding.postOffValidateNameTextView;
        setAsDefaultAddressCheckBox = binding.setAsDefaultAddressBoolCheckBox;
        RadioGroup addressTyperadioGroup = binding.addressTypeRadioGroup;
        phoneNoET = binding.phoneNumberEditTextText;
        locationTV = binding.addFragLocationViewTextView;
        spinnerState = binding.spinnerState;
        spinnerDistrict = binding.spinnerDistrict;

        // Populate state spinner
        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.india_states_array, R.layout.spinner_item);
        stateAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerState.setAdapter(stateAdapter);

        // Set listener for state spinner
        spinnerState.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedState = parent.getItemAtPosition(position).toString();
                populateDistrictSpinner(selectedState);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Initial population
        populateDistrictSpinner("Select State");

        // ADD-ADDRESS button action def.
        addNewAddressButton.setOnClickListener(view -> {
            pincode = pinCodeEditText.getText().toString();
            phoneno = phoneNoET.getText().toString();
            if (TextUtils.isEmpty(pincode)) {
                Toast.makeText(requireContext(), "Please enter pin code", Toast.LENGTH_SHORT).show();
            } else if (pincode.length() < 6) {
                Toast.makeText(requireContext(), "Please enter a valid 6-digit pin code", Toast.LENGTH_SHORT).show();
            } else if (phoneno.length() < 10) {
                Toast.makeText(requireContext(), "Please enter a valid 10-digit phone number", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(phoneno)) {
                Toast.makeText(requireContext(), "Please enter phone no", Toast.LENGTH_SHORT).show();
            } else {
                selectedDistrict = spinnerDistrict.getSelectedItem() != null ? spinnerDistrict.getSelectedItem().toString() : "0";
                selectedState = spinnerState.getSelectedItem() != null ? spinnerState.getSelectedItem().toString() : "0";

                if (selectedDistrict.equals("Select District") || selectedDistrict.isEmpty()) {
                    Toast.makeText(requireContext(), "Please select district", Toast.LENGTH_SHORT).show();
                } else if (selectedState.equals("Select State") || selectedState.isEmpty()) {
                    Toast.makeText(requireContext(), "Please select state", Toast.LENGTH_SHORT).show();
                } else {
                    validatePinCode(pincode, (isPinValid, postOffName) -> {
                        if (isPinValid) {
                            sentAddressAddRequest(postOffName, setAsDefaultAddressCheckBox.isChecked());
                        } else {
                            Toast.makeText(requireContext(), "Unable to validate pincode, at the moment!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        addressTyperadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.homeType_radioButton) {
                addressType = "Home";
            } else if (checkedId == R.id.workType_radioButton) {
                addressType = "Work";
            } else if (checkedId == R.id.otherType_radioButton) {
                addressType = "Other";
            } else {
                addressType = "OTHER";
            }
        });

        return root;
    }


    private void checkAndPromptForGPS() {
        // Create a LocationRequest to request high-accuracy location
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
                .setMinUpdateIntervalMillis(5000L) // Fastest interval
                .build();

        // Build a LocationSettingsRequest
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        // Check location settings with the SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(requireActivity());
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(requireActivity(), locationSettingsResponse -> {
            // GPS is enabled, you can proceed with accessing location

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

    private void populateDistrictSpinner(@NonNull String state) {
        int districtsArrayId = switch (state) {
            case "Karnataka" -> R.array.karnataka_districts_array;
            case "Kerala" -> R.array.kerala_districts_array;
            default -> R.array.empty_array
            ;
        };

        ArrayAdapter<CharSequence> districtAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                districtsArrayId,
                R.layout.spinner_item);
        districtAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerDistrict.setAdapter(districtAdapter);
    }

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            if (LocationService.ACTION_LOCATION_BROADCAST.equals(intent.getAction())) {
                latitude = intent.getDoubleExtra(LocationService.EXTRA_LATITUDE, 0.00);
                longitude = intent.getDoubleExtra(LocationService.EXTRA_LONGITUDE, 0.00);
                if (locationTV.getVisibility() == View.GONE) {
                    locationTV.setVisibility(View.VISIBLE);
                }

                locationTV.setText(MessageFormat.format("{0}°N {1}°E",
                        coordinateFormat.format(latitude),
                        coordinateFormat.format(longitude)));
            }
        }
    };


    private void sentAddressAddRequest(String postOffName, boolean isAddDefault) {
//        RequestBody requestFile = RequestBody.create(imageFile, MediaType.parse("image/jpeg"));
//        MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

        // Prepare JSON data
        JsonObject jsonData = getAddressData(postOffName, isAddDefault);
        RequestBody data = RequestBody.create(jsonData.toString(), MediaType.parse("application/json"));

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call = apiService.addNewAddress(data);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    // Handle successful response
                    if (response.body() != null) {
                        if (response.body().has("exists")) {
                            if (response.body().get("exists").getAsBoolean()) {
                                showDecisionDialog();
                            }
                        } else if (response.body().has("message") && response.body().has("success")) {
                            if (response.body().get("success").getAsBoolean()) {
                                Utils.deleteAddressDataCacheFile(requireContext());
                                Toast.makeText(requireContext(), response.body().get("message").getAsString(), Toast.LENGTH_SHORT).show();
//                                new Handler().postDelayed(() ->
//                                        NavHostFragment.findNavController(AddAddressFragment.this)
//                                                .navigate(R.id.action_nav_addAddress_to_nav_settings), 1200);
                                clearFieldData();

                            }
                        }

                        Log.d("MainActivity", "Shop created successfully");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e("LOG_TAG", "Image upload error: " + t.getMessage());
            }
        });
    }

    private void sentAddressDecisionRequest(String _decision) {
        JsonObject jsonData = getAddressDecisionData(_decision);
        RequestBody data = RequestBody.create(jsonData.toString(), MediaType.parse("application/json"));

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call2301 = apiService.addressUpdateDecision(data);

        call2301.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    // Handle successful response
                    if (response.body() != null) {
                        if (response.body().has("success")) {
                            if (response.body().get("success").getAsBoolean()) {
                                Utils.deleteAddressDataCacheFile(requireContext());
                                Toast.makeText(requireContext(), response.body().get("message").getAsString(), Toast.LENGTH_SHORT).show();
//                                new Handler().postDelayed(() ->
//                                        NavHostFragment.findNavController(AddAddressFragment.this)
//                                                .navigate(R.id.action_nav_addAddress_to_nav_settings), 1200);
                                clearFieldData();

                            }
                        }
                        Log.d("MainActivity", "Shop created successfully");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e("LOG_TAG", "Image upload error: " + t.getMessage());
            }
        });
    }

    private void clearFieldData() {
        phoneNoET.setText("");
        pinCodeEditText.setText("");
        cityStateTV.setText("");
        postOffValNameTV.setText("");
        locationTV.setText("");
    }

    private void showDecisionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Address Exists");
        builder.setMessage("An address already exists for this phone number. Do you want to overwrite it?");
        builder.setPositiveButton("Yes", (dialog, id) -> sentAddressDecisionRequest("update"));
        builder.setNegativeButton("No", (dialog, id) -> {
            sentAddressDecisionRequest("cancel");
            dialog.dismiss();
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void validatePinCode(@NonNull String pincode, PincodeValidation isValid) {
        RequestQueue queue = Volley.newRequestQueue(requireContext());

        String PINCODE_VAL_URL = "http://www.postalpincode.in/api/pincode/";
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, PINCODE_VAL_URL + pincode.trim(), null, response -> {
            try {
                JSONArray postOfficeInfoArray = response.getJSONArray("PostOffice");
                if (response.getString("Status").equals("Error")) {
                    postOffValNameTV.setText(R.string.invalid_pincode);
                } else {
                    JSONObject obj = postOfficeInfoArray.getJSONObject(0);

                    Log.i("AddAddressFragment", obj.getString("Name") + "-" + obj.getString("District") + "-" + obj.getString("State"));

                    pincodeFromDistrict = obj.getString("District");
//                    state = obj.getString("State");
                    cityStateTV.setText(MessageFormat.format("{0}, {1}", obj.getString("District"), obj.getString("State")));

                    if (!obj.getString("Name").isEmpty()) {
                        postOffValNameTV.setText(MessageFormat.format("{0} (PO)", obj.getString("Name")));
                        isValid.onSuccess(true, obj.getString("Name"));
                    } else {
                        postOffValNameTV.setText(R.string.invalid_pincode);
                    }
                }
            } catch (JSONException e) {
                isValid.onSuccess(false, NOT_FOUND);
                Log.e("AddAddressFragment", e.toString());
                postOffValNameTV.setText(R.string.invalid_pincode);
                cityStateTV.setText("");
                Log.i("AddAddressFragment", String.valueOf(e));
            }
        }, error -> {
            Toast.makeText(requireContext(), "Pincode is not valid!", Toast.LENGTH_SHORT).show();
            isValid.onSuccess(false, NOT_FOUND);
            postOffValNameTV.setText(R.string.invalid_pincode);
            cityStateTV.setText("");
            Log.i("AddAddressFragment", String.valueOf(error));
        });
        queue.add(objectRequest);
    }


    @NonNull
    private JsonObject getAddressData(String postOffName, boolean isAddDef) {

        String name_address, street_address, pincode_address, landmark_address, full_address;

        name_address = binding.nameNewAddressEditText.getText().toString();
        street_address = binding.streetNewAddressEditText.getText().toString();
        pincode_address = binding.pincodeTextBox.getText().toString();
        landmark_address = binding.landmarkNewAddressEditText.getText().toString();

        full_address = name_address + ",\n" + street_address + ", " + landmark_address.trim() + ", "
                + selectedDistrict.substring(0, 1).toUpperCase() + selectedDistrict.substring(1) + ", "
                + pincode_address + ", " + selectedState.substring(0, 1).toUpperCase() + selectedState.substring(1);

        JsonObject jsonData = new JsonObject();
        jsonData.addProperty("user_id", user.getUid());
        jsonData.addProperty("name", name_address);
        jsonData.addProperty("street_address", street_address);
        jsonData.addProperty("district", selectedDistrict.toLowerCase());
        jsonData.addProperty("state", selectedState.toLowerCase());
        jsonData.addProperty("district_from_pincode", pincodeFromDistrict);
        jsonData.addProperty("pincode", pincode_address);
        jsonData.addProperty("landmark", landmark_address);
        jsonData.addProperty("full_address", full_address);
        jsonData.addProperty("is_default", isAddDef);
        jsonData.addProperty("post_office_name", postOffName);
        jsonData.addProperty("address_type", addressType);
        jsonData.addProperty("address_lat", address_lat);
        jsonData.addProperty("address_lon", address_lon);
        jsonData.addProperty("phone_no", phoneNoET.getText().toString());
        return jsonData;
    }

    @NonNull
    private JsonObject getAddressDecisionData(String _decision) {
        JsonObject jsonData = new JsonObject();
        jsonData.addProperty("user_id", user.getUid());
        jsonData.addProperty("decision", _decision);
        jsonData.addProperty("phone_no", phoneNoET.getText().toString());
        return jsonData;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAndPromptForGPS();
    }

    @Override
    public void onPause() {
        super.onPause();
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
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

}
