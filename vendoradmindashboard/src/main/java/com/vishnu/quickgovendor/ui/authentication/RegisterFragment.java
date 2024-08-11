package com.vishnu.quickgovendor.ui.authentication;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.gson.JsonObject;
import com.vishnu.quickgovendor.R;
import com.vishnu.quickgovendor.callbacks.PincodeValidation;
import com.vishnu.quickgovendor.databinding.FragmentRegistrationBinding;
import com.vishnu.quickgovendor.server.APIService;
import com.vishnu.quickgovendor.server.ApiServiceGenerator;
import com.vishnu.quickgovendor.services.GPSLocationProvider;
import com.vishnu.quickgovendor.services.LocationUpdateListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterFragment extends Fragment implements LocationUpdateListener {
    private static final String LOG_TAG = "RegisterFragment";
    FragmentRegistrationBinding binding;
    EditText emailET;
    EditText passwordET;
    EditText reenterPasswordET;
    Context context;
    Activity activity;
    Button registerBtn;
    private BottomSheetDialog registrationStatusBtmDialog;
    TextView statusTV;
    private FirebaseAuth mAuth;
    Map<String, Object> userDataMap;
    Map<String, Object> userDataKeyMap;
    DocumentReference registeredUsersCredentialsRef;
    DocumentReference DeviceInfoRef;
    DocumentReference registeredUsersEmailRef;
    private EditText shopNameET;
    private EditText signUpShopStreetET;
    private ImageView selectedImageIV;
    private FirebaseFirestore db;
    SharedPreferences preferences;
    private Uri selectedImageUri;
    private double shop_lat, shop_lon;
    TextView realtimeLocTV;
    private StorageReference storageRef;
    private BottomSheetDialog bottomSheetDialog;
    private LocationManager locationManager;
    TextView btmViewStatusTV;
    TextView btmViewRegIDTV;
    EditText shopPhnoET;
    ProgressBar btmViewProgressBar;
    TextView postOffNameTV;
    EditText pinCodeET;
    private String pincode;
    TextView stateDistTV;
    private View root;
    String selectedDistrict;
    String selectedState;
    String filePath;
    private Vibrator vibrator;
    private GPSLocationProvider gpsLocationProvider;
    private Spinner spinnerCountry, spinnerState, spinnerDistrict;

    Button enableLocationBtn;
    DecimalFormat coordinateFormat = new DecimalFormat("0.0000000000");
    private final String NOT_FOUND = "NOT FOUND!";
    String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        selectedImageIV.setImageURI(selectedImageUri);
                        String filePath = getRealPathFromURI(selectedImageUri);
                        setFilePath(filePath);
                    } else {
                        Toast.makeText(requireContext(), "Failed to get image path", Toast.LENGTH_SHORT).show();
                    }
                }
            });


    public RegisterFragment(Activity activity, Context context) {
        this.activity = activity;
        this.context = context;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        binding = FragmentRegistrationBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference().child("vendorAccountImages");
        preferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), permissionsToRequest.toArray(new String[0]), 1);
        } else {
            Log.i(LOG_TAG, "ALL PERMISSION GRANTED");
        }

        locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);
        gpsLocationProvider = new GPSLocationProvider(requireContext(), this);
        vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);

        DeviceInfoRef = db.collection("UserInformation").document("DeviceInfo");
        registeredUsersCredentialsRef = db.collection("AuthenticationData").document("RegisteredUsersCredentials");
        registeredUsersEmailRef = db.collection("AuthenticationData").document("RegisteredUsersEmail");

        userDataKeyMap = new HashMap<>();
        userDataMap = new HashMap<>();

        shopNameET = binding.shopNameSignUpEditText;
        emailET = binding.emailIdFieldSignUpEditText;
        passwordET = binding.passwordFieldSignUpEditText;
        signUpShopStreetET = binding.shopStreetSignupEditText;
        registerBtn = binding.customerRegisterButton;
        stateDistTV = binding.stateDistrictViewSignUpTextView;
        selectedImageIV = binding.selectedImageSignupImageView;
        statusTV = binding.statusViewTextView;
        shopPhnoET = binding.shopPhoneNoSignupEditTextPhone;
        realtimeLocTV = binding.realtimeCoordinatesViewTextew;
        postOffNameTV = binding.postOffNameViewSignUpTextView;
        pinCodeET = binding.pincodeSignUpEditText;

        spinnerCountry = binding.spinnerCountry;
        spinnerState = binding.spinnerState;
        spinnerDistrict = binding.spinnerDistrict;


        // Populate country spinner from resources
        ArrayAdapter<CharSequence> countryAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.countries_array,
                R.layout.spinner_item);
        countryAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerCountry.setAdapter(countryAdapter);


        // Set listener for country spinner
        spinnerCountry.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCountry = parent.getItemAtPosition(position).toString();
                populateStateSpinner(selectedCountry);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

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

        registerBtn.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                pincode = pinCodeET.getText().toString();
                if (TextUtils.isEmpty(pincode)) {
                    Toast.makeText(requireContext(), "Please enter pin code!", Toast.LENGTH_SHORT).show();
                } else if (pincode.length() < 6) {
                    Toast.makeText(requireContext(), "Please enter a valid 6-digit pin code!", Toast.LENGTH_SHORT).show();
                } else if (emailET.getText().toString().isEmpty()) {
                    Toast.makeText(context, "Please enter the email ID", Toast.LENGTH_SHORT).show();
                } else if (passwordET.getText().toString().isEmpty()) {
                    Toast.makeText(context, "Please enter the password", Toast.LENGTH_SHORT).show();
                } else if (shopPhnoET.getText().toString().isEmpty()) {
                    Toast.makeText(context, "Please enter the phone number", Toast.LENGTH_SHORT).show();
                } else if (!(shopPhnoET.getText().toString().length() >= 10)) {
                    Toast.makeText(context, "Phone no should be 10 digits!", Toast.LENGTH_SHORT).show();
                } else if (!(passwordET.getText().toString().length() >= 6)) {
                    Toast.makeText(context, "Password length should be more that 6 chars!", Toast.LENGTH_SHORT).show();
                } else {
                    selectedDistrict = spinnerDistrict.getSelectedItem() != null ? spinnerDistrict.getSelectedItem().toString() : "0";
                    selectedState = spinnerState.getSelectedItem() != null ? spinnerState.getSelectedItem().toString() : "0";

                    if (selectedDistrict.equals("— Select —") || selectedDistrict.isEmpty()) {
                        Toast.makeText(context, "Select district", Toast.LENGTH_SHORT).show();
                    } else if (selectedState.equals("— Select —") || selectedState.isEmpty()) {
                        Toast.makeText(context, "Select state", Toast.LENGTH_SHORT).show();
                    } else {
                        validatePinCode(pincode, (isPinValid, postOffName, cityName) -> {
                            if (isPinValid) {
                                registerNewShop();
                            } else {
                                Toast.makeText(requireContext(), "Unable to validate pincode!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up image selection
        selectedImageIV.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });


        if (isLocationNotEnabled(requireContext())) {
            showLocNotEnabledBtmView(root);
        } else {
            startLocationUpdates();
        }

        startLocationUpdates();
        populateStateSpinner("India");
        populateDistrictSpinner("Kerala");
        return root;
    }

    private void populateStateSpinner(String country) {
        int statesArrayId = switch (country) {
            case "India" -> R.array.india_states_array;
            default -> 0;
            // Add more cases if there are more countries
        };

        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                statesArrayId,
                R.layout.spinner_item);
        stateAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerState.setAdapter(stateAdapter);

        // Reset district spinner
        spinnerDistrict.setAdapter(null);
    }


    private void populateDistrictSpinner(String state) {
        int districtsArrayId = switch (state) {
            case "Kerala" -> R.array.kerala_districts_array;
            case "Karnataka" -> R.array.karnataka_districts_array;
            default -> R.array.none_array;
            // Add more cases if there are more states
        };

        ArrayAdapter<CharSequence> districtAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                districtsArrayId,
                R.layout.spinner_item);
        districtAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerDistrict.setAdapter(districtAdapter);
    }

    private void showRegistrationStatusBtmView() {
        View registrationStatusBtmView = LayoutInflater.from(requireContext()).inflate(
                R.layout.bottomview_register_new_shop_feedback, (ViewGroup) root, false);

        /* Create a BottomSheetDialog with TOP gravity */
        registrationStatusBtmDialog = new BottomSheetDialog(requireContext());
        registrationStatusBtmDialog.setContentView(registrationStatusBtmView);
        registrationStatusBtmDialog.setCanceledOnTouchOutside(false);

        Objects.requireNonNull(registrationStatusBtmDialog.getWindow()).setGravity(Gravity.TOP);

        btmViewStatusTV = registrationStatusBtmView.findViewById(R.id.btmViewShopRegistrationStatusView_textView);
        btmViewRegIDTV = registrationStatusBtmView.findViewById(R.id.btmViewShopRegistrationRegID_textView);
        btmViewProgressBar = registrationStatusBtmView.findViewById(R.id.btmViewProgressBar_progressBar);

        registrationStatusBtmDialog.show();
    }


    private String getRealPathFromURI(Uri contentUri) {
        String path = null;
        if (Objects.requireNonNull(contentUri.getPath()).startsWith("/storage")) {
            path = contentUri.getPath();
        } else {
            String[] proj = {MediaStore.Images.Media.DATA};
            Cursor cursor = requireActivity().getContentResolver().query(contentUri, proj, null, null, null);
            if (cursor != null) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    path = cursor.getString(columnIndex);
                }
                cursor.close();
            }
        }
        return path;
    }


    @NonNull
    private JsonObject getShopData() {
        JsonObject jsonData = new JsonObject();
        jsonData.addProperty("shop_name", shopNameET.getText().toString());
        jsonData.addProperty("shop_reg_email", emailET.getText().toString());
        jsonData.addProperty("shop_reg_password", passwordET.getText().toString());
        jsonData.addProperty("shop_phone", shopPhnoET.getText().toString());
        jsonData.addProperty("shop_image_url", "image_url");
        jsonData.addProperty("shop_lat", shop_lat);
        jsonData.addProperty("shop_lon", shop_lon);
        jsonData.addProperty("shop_state", selectedState);
        jsonData.addProperty("shop_district", selectedDistrict);
        jsonData.addProperty("shop_pincode", pincode);
        jsonData.addProperty("shop_street", signUpShopStreetET.getText().toString());
        return jsonData;
    }

    private void sentCreateShopRequest(File imageFile) {
        showRegistrationStatusBtmView();
        RequestBody requestFile = RequestBody.create(imageFile, MediaType.parse("image/jpeg"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

        // Prepare JSON data
        JsonObject jsonData = getShopData();
        RequestBody shopData = RequestBody.create(jsonData.toString(), MediaType.parse("application/json"));

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call = apiService.registerShop(body, shopData);

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    // Handle successful response
                    if (response.body() != null) {
                        if (response.body().get("status").getAsBoolean()) {
                            btmViewProgressBar.setVisibility(View.GONE);

                            mAuth.signInWithEmailAndPassword(emailET.getText().toString(),
                                    passwordET.getText().toString()).addOnCompleteListener(requireActivity(), task -> {
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    Log.d(LOG_TAG, "signInWithEmail:success");
                                    FirebaseUser user = mAuth.getCurrentUser();

                                    assert user != null;
                                    btmViewRegIDTV.setText(user.getUid());

                                    preferences.edit().putString("username", emailET.getText().toString()).apply();
                                    preferences.edit().putString("password", passwordET.getText().toString()).apply();
                                    preferences.edit().putBoolean("isInitialLogin", false).apply();
                                    preferences.edit().putBoolean("isRemembered", true).apply();

                                    new Handler().postDelayed(() -> {
                                        registrationStatusBtmDialog.hide();
                                        registrationStatusBtmDialog.dismiss();

                                        requireActivity().onBackPressed();
                                    }, 2000);

                                    Toast.makeText(requireContext(), "Authentication successful.",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    // If sign in fails, display a message to the user.
                                    btmViewProgressBar.setVisibility(View.GONE);
                                    btmViewStatusTV.setText(R.string.sign_failed);
                                    Log.w(LOG_TAG, "signInWithEmail:failure", task.getException());
                                    Toast.makeText(requireContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    Log.d("MainActivity", "Shop created successfully");
                } else {
                    btmViewProgressBar.setVisibility(View.GONE);
                    btmViewStatusTV.setText(R.string.failed_to_register_shop_server_busy_404);
                    // Handle unsuccessful response
                    Log.d("MainActivity", "Failed to create shop");
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.e(LOG_TAG, "Image upload error: " + t.getMessage());
            }
        });
    }

    private void registerNewShop() {
        registeredUsersEmailRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Object emailAddressesObj = documentSnapshot.get("email_addresses");
                        if (emailAddressesObj instanceof List<?> emailAddressesList) {
                            for (Object emailObj : emailAddressesList) {
                                if (emailObj instanceof String email) {
                                    if (email.equals(emailET.getText().toString())) {
                                        statusTV.setText(R.string.email_address_has_already_taken);
                                        emailET.setText("");
                                        Log.d(LOG_TAG, "Email " + emailET.getText().toString() + " exists in db");
                                        return;
                                    }
                                }
                            }
                            if (selectedImageUri != null && selectedImageUri.getPath() != null) {
                                sentCreateShopRequest(new File(getFilePath()));
                            } else {
                                Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show();
                            }
                            Log.d(LOG_TAG, "Email " + emailET.getText().toString() + " does not exist in db");
                        } else {
                            Log.e(LOG_TAG, "Email addresses is not a list");
                        }
                    } else {
                        Log.e(LOG_TAG, "Document 'RegisteredUsersEmail' does not exist");
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error checking email in db", e));
    }

    private void startLocationUpdates() {
        // Register the listener with the Location Manager to receive location updates
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, gpsLocationProvider);
        }
    }

    private void showLocationSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
    }

    private void showLocNotEnabledBtmView(View fragmentView) {
        // Inflate the menu layout
        View locEnableView = LayoutInflater.from(requireContext()).inflate(
                R.layout.bottomview_enable_devlocation, (ViewGroup) fragmentView, false);

        // Create a BottomSheetDialog with TOP gravity
        bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetDialog.setContentView(locEnableView);
        Objects.requireNonNull(bottomSheetDialog.getWindow()).setGravity(Gravity.TOP);

        enableLocationBtn = locEnableView.findViewById(R.id.enableDeviceLocation_button);

        enableLocationBtn.setOnClickListener(v -> {
            bottomSheetDialog.hide();
            showLocationSettings(requireContext());
        });

        bottomSheetDialog.show();

    }

    private boolean isLocationNotEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Check if either GPS or network provider is enabled
        return locationManager == null ||
                (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                        !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private void validatePinCode(String pincode, PincodeValidation isValid) {
        RequestQueue queue = Volley.newRequestQueue(requireContext());

        String PINCODE_VAL_URL = "http://www.postalpincode.in/api/pincode/";
        JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, PINCODE_VAL_URL +
                pincode.trim(), null, response -> {
            try {
                JSONArray postOfficeInfoArray = response.getJSONArray("PostOffice");
                if (response.getString("Status").equals("Error")) {
                    postOffNameTV.setText(R.string.invalid_pincode);
                } else {
                    JSONObject obj = postOfficeInfoArray.getJSONObject(0);

                    Log.i("AddAddressFragment", obj.getString("Name") + "-" +
                            obj.getString("District") + "-" + obj.getString("State"));

                    stateDistTV.setText(MessageFormat.format("{0}|{1}", obj.getString("District"), obj.getString("State")));

                    if (!obj.getString("Name").isEmpty()) {
                        postOffNameTV.setText(obj.getString("Name"));
                        isValid.onSuccess(true, obj.getString("Name"), obj.getString("District"));
                    } else {
                        postOffNameTV.setText(R.string.invalid_pincode);
                    }
                }
            } catch (JSONException e) {
                isValid.onSuccess(false, NOT_FOUND, NOT_FOUND);
                Log.e(LOG_TAG, e.toString());
                postOffNameTV.setText(R.string.invalid_pincode);
                stateDistTV.setText("");
                Log.i("AddAddressFragment", String.valueOf(e));
            }
        }, error -> {
            Toast.makeText(requireContext(), "Pincode is not valid!", Toast.LENGTH_SHORT).show();
            isValid.onSuccess(false, NOT_FOUND, NOT_FOUND);
            postOffNameTV.setText(R.string.invalid_pincode);
            stateDistTV.setText("");
            Log.i("AddAddressFragment", String.valueOf(error));
        });
        queue.add(objectRequest);
    }


    private boolean isGmailAppInstalled() {
        PackageManager pm = requireActivity().getPackageManager();
        Intent gmailIntent = new Intent(Intent.ACTION_MAIN);
        gmailIntent.addCategory(Intent.CATEGORY_APP_EMAIL);
        ResolveInfo resolveInfo = pm.resolveActivity(gmailIntent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null && resolveInfo.activityInfo.packageName.equals("com.google.android.gm");
    }

    private void showOpenGmailDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Account verification");
        builder.setMessage("Please verify your email address. Do you want to open Gmail?");
        builder.setCancelable(false);

        builder.setPositiveButton("Open Gmail", (dialog, which) -> {
            // Perform account-deletion action
            if (isGmailAppInstalled()) {
                // Open the Gmail app to the main screen
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_APP_EMAIL);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Error opening Gmail", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Gmail app is not installed", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancel", ((dialog, which) -> dialog.dismiss()));
        builder.show();
    }

    private void vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(85, 2));
        }
    }

//    private void saveDeviceInfoToFirestore(FirebaseUser user) {
//        Map<String, Object> deviceInfo = getStringObjectMap(user);
//        Map<String, Object> dataMap = new HashMap<>();
//
//        dataMap.put(Objects.requireNonNull(user.getEmail()).replace('.', '_'), deviceInfo);
//
//        // Add the device information to Firestore
//        DeviceInfoRef.get().addOnSuccessListener(documentSnapshot -> {
//            if (documentSnapshot.exists()) {
//                // Update the map-type data
//                DeviceInfoRef.update(dataMap)
//                        .addOnSuccessListener(aVoid -> Log.d(LOG_TAG, "Device info updated successfully"))
//                        .addOnFailureListener(e -> Log.e(LOG_TAG, "Error updating device info", e));
//            } else {
//                DeviceInfoRef.set(dataMap)
//                        .addOnSuccessListener(aVoid -> Log.d(LOG_TAG, "Device info added successfully"))
//                        .addOnFailureListener(e -> Log.e(LOG_TAG, "Error adding device info", e));
//            }
//
//        });
//
//    }

//    @NonNull
//    private Map<String, Object> getStringObjectMap(FirebaseUser user) {
//        String manufacturer = Build.MANUFACTURER;
//        String model = Build.MODEL;
//        String androidVersion = Build.VERSION.RELEASE;
//        int apiLevel = Build.VERSION.SDK_INT;
//
//        // Create a map to store the device information
//        Map<String, Object> deviceInfo = new HashMap<>();
//        deviceInfo.put("manufacturer", manufacturer);
//        deviceInfo.put("user_id", user.getUid());
//        deviceInfo.put("registered_email", user.getEmail());
//        deviceInfo.put("model", model);
//        deviceInfo.put("androidVersion", androidVersion);
//        deviceInfo.put("apiLevel", apiLevel);
//        deviceInfo.put("reg_timestamp", FieldValue.serverTimestamp());
//        return deviceInfo;
//    }


    @Override
    public void onLocationUpdated(double latitude, double longitude) {
        this.shop_lat = latitude;
        this.shop_lon = longitude;
        realtimeLocTV.setText((MessageFormat.format("{0}°N\n{1}°E", coordinateFormat.format(latitude),
                coordinateFormat.format(longitude))));
        vibrate();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (registrationStatusBtmDialog != null) {
            registrationStatusBtmDialog.hide();
            registrationStatusBtmDialog.dismiss();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (registrationStatusBtmDialog != null) {
            registrationStatusBtmDialog.hide();
            registrationStatusBtmDialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (registrationStatusBtmDialog != null) {
            registrationStatusBtmDialog.hide();
            registrationStatusBtmDialog.dismiss();
        }
    }
}