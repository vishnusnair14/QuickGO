package com.vishnu.quickgodelivery.ui.authentication;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
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

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.JsonObject;
import com.vishnu.quickgodelivery.R;
import com.vishnu.quickgodelivery.databinding.FragmentRegistrationBinding;
import com.vishnu.quickgodelivery.server.sapi.APIService;
import com.vishnu.quickgodelivery.server.sapi.ApiServiceGenerator;

import java.io.File;
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

public class RegisterFragment extends Fragment {
    private static final String LOG_TAG = "RegisterFragment";
    FragmentRegistrationBinding binding;
    EditText nameET;
    EditText emailET;
    EditText pincodeET;
    EditText passwordET;
    EditText phoneET;
    EditText reenterPasswordET;
    Context context;
    Activity activity;
    Button registerBtn;
    TextView statusTV;
    private FirebaseAuth mAuth;
    View root;
    ProgressBar btmViewProgressBar;
    private BottomSheetDialog registrationStatusBtmDialog;
    Map<String, Object> userDataMap;
    TextView btmViewRegIDTV;
    private Uri selectedImageUri;
    Map<String, Object> userDataKeyMap;
    DocumentReference registeredUsersEmailRef;
    DocumentReference registeredUsersCredentialsRef;
    private ImageView selectedImageIV;
    String selectedDistrict;
    String selectedState;
    private SharedPreferences preferences;
    TextView btmViewStatusTV;
    String filePath;
    private FirebaseFirestore db;
    private Spinner spinnerLocality, spinnerState, spinnerDistrict;
    private OnBackPressedCallback onBackPressedCallback;


    public RegisterFragment(Activity activity, Context context) {
        this.activity = activity;
        this.context = context;
    }


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

    public String getFilePath() {
        return filePath;
    }


    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        // Initialize the callback and handle back press
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Implement back press logic here
                if (registrationStatusBtmDialog != null && registrationStatusBtmDialog.isShowing()) {
                    registrationStatusBtmDialog.dismiss();
                } else {
                    // Handle other back press cases or pop the fragment
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            }
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        binding = FragmentRegistrationBinding.inflate(inflater, container, false);
        root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        registeredUsersCredentialsRef = db.collection("AuthenticationData").document("RegisteredUsersCredentials");
        registeredUsersEmailRef = db.collection("AuthenticationData").document("RegisteredUsersEmail");

        preferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        userDataKeyMap = new HashMap<>();
        userDataMap = new HashMap<>();

        emailET = binding.emailFieldRegisterFragmentEditTextText;
        nameET = binding.nameFieldRegisterFragmentEditTextText;
        phoneET = binding.phoneFieldRegisterFragmentEditTextText;
        passwordET = binding.passwordFieldRegisterFragmentEditTextText;
        pincodeET = binding.pincodeFieldRegisterFragmentEditTextText;
        reenterPasswordET = binding.reenterPasswordFieldRegisterFragmentEditTextText;
        registerBtn = binding.registerRegisterFragmentButton;
        statusTV = binding.statusViewRegisterFragmentEditTextText;
        selectedImageIV = binding.profileImageDeliveryRegisterFragmentImageView;
        spinnerState = binding.spinnerState;
        spinnerLocality = binding.spinnerLocality;
        spinnerDistrict = binding.spinnerDistrict;

        selectedImageIV.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.india_states_array,
                R.layout.spinner_item);
        stateAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerState.setAdapter(stateAdapter);

        // Set listener for country spinner
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

        // Set listener for state spinner
        spinnerDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDistrict = parent.getItemAtPosition(position).toString();
                populateLocalitySpinner(selectedDistrict);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });


        emailET.setOnClickListener(v -> statusTV.setText(""));
        passwordET.setOnClickListener(v -> statusTV.setText(""));

        registerBtn.setOnClickListener(v -> {
            String email = emailET.getText().toString().trim();
            String password = passwordET.getText().toString().trim();
            String reenterPassword = reenterPasswordET.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || reenterPassword.isEmpty()) {
                if (email.isEmpty()) {
                    statusTV.setText(R.string.please_enter_the_email_id);
                } else if (password.isEmpty()) {
                    statusTV.setText(R.string.please_enter_the_password);
                } else {
                    statusTV.setText(R.string.re_enter_password);
                }
            } else if (password.length() < 6) {
                statusTV.setText(R.string.length_should_be_6_or_more);
            } else if (!password.equals(reenterPassword)) {
                statusTV.setText(R.string.password_not_match);
            } else {
                statusTV.setText(R.string.please_wait);
                selectedDistrict = spinnerDistrict.getSelectedItem() != null ? spinnerDistrict.getSelectedItem().toString() : "0";
                selectedState = spinnerState.getSelectedItem() != null ? spinnerState.getSelectedItem().toString() : "0";


                if (selectedDistrict == null || selectedDistrict.equals("Select District") || selectedDistrict.isEmpty()) {
                    Toast.makeText(context, "Select District", Toast.LENGTH_SHORT).show();
                } else if (selectedState.equals("Select State") || selectedState.isEmpty()) {
                    Toast.makeText(context, "Select state", Toast.LENGTH_SHORT).show();
                } else {
                    registerNewUser(email);
                }

            }
        });
//        populateDistrictSpinner("Kerala");

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Add the callback to the OnBackPressedDispatcher
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);
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

        spinnerLocality.setAdapter(null);
    }

    private void populateLocalitySpinner(String district) {
        int statesArrayId = switch (district) {
            case "Mysuru" -> R.array.mysuru_locality_array;
            case "Palakkad" -> R.array.palakkad_locality_array;
            default -> R.array.none_array;
            // Add more cases if there are more countries
        };

        ArrayAdapter<CharSequence> stateAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                statesArrayId,
                R.layout.spinner_item);
        stateAdapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerLocality.setAdapter(stateAdapter);
    }


    private void registerNewUser(String emailToCheck) {
        registeredUsersEmailRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Object emailAddressesObj = documentSnapshot.get("email_addresses");
                        if (emailAddressesObj instanceof List<?> emailAddressesList) {
                            for (Object emailObj : emailAddressesList) {
                                if (emailObj instanceof String email) {
                                    if (email.equals(emailToCheck)) {
                                        statusTV.setText(R.string.email_address_has_already_taken);
                                        emailET.setText("");
                                        Log.d(LOG_TAG, "Email " + emailToCheck + " exists in db");
                                        return;
                                    }
                                }
                            }
                            Log.d(LOG_TAG, "Email " + emailToCheck + " does not exist in db");
                            if (selectedImageUri != null && selectedImageUri.getPath() != null) {
//                                addUser(nameET.getText().toString(), emailET.getText().toString(), passwordET.getText().toString());
                                sentCreateShopRequest(new File(getFilePath()));
                            } else {
                                Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(LOG_TAG, "Email addresses is not a list");
                        }
                    } else {
                        Log.e(LOG_TAG, "Document 'RegisteredUsersEmail' does not exist");
                    }
                })
                .addOnFailureListener(e -> Log.e(LOG_TAG, "Error checking email in db", e));
    }

    @NonNull
    private JsonObject getRegistrationData() {
        JsonObject jsonData = new JsonObject();
        jsonData.addProperty("user_name", nameET.getText().toString());
        jsonData.addProperty("user_email", emailET.getText().toString());
        jsonData.addProperty("user_password", passwordET.getText().toString());
        jsonData.addProperty("user_pincode", pincodeET.getText().toString());
        jsonData.addProperty("user_profile_image_url", "image_url");
        jsonData.addProperty("user_state", selectedState);
        jsonData.addProperty("user_district", selectedDistrict);
        jsonData.addProperty("user_phone", phoneET.getText().toString());
        return jsonData;
    }

    private void sentCreateShopRequest(File imageFile) {
        showRegistrationStatusBtmView();
        RequestBody requestFile = RequestBody.create(imageFile, MediaType.parse("image/jpeg"));
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

        // Prepare JSON data
        JsonObject jsonData = getRegistrationData();
        RequestBody registrationData = RequestBody.create(jsonData.toString(), MediaType.parse("application/json"));

        APIService apiService = ApiServiceGenerator.getApiService(requireContext());
        Call<JsonObject> call = apiService.registerUser(body, registrationData);

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

                                        onBackPressedCallback.handleOnBackPressed();

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

    private void showRegistrationStatusBtmView() {
        View registrationStatusBtmView = LayoutInflater.from(requireContext()).inflate(
                R.layout.bottomview_registration_progress, (ViewGroup) root, false);

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


    @NonNull
    private Map<String, Object> getStringObjectMap(FirebaseUser user) {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String androidVersion = Build.VERSION.RELEASE;
        int apiLevel = Build.VERSION.SDK_INT;

        // Create a map to store the device information
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("manufacturer", manufacturer);
        deviceInfo.put("user_id", user.getUid());
        deviceInfo.put("registered_email", user.getEmail());
        deviceInfo.put("model", model);
        deviceInfo.put("androidVersion", androidVersion);
        deviceInfo.put("apiLevel", apiLevel);
        deviceInfo.put("reg_timestamp", FieldValue.serverTimestamp());
        return deviceInfo;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove the callback when the view is destroyed
        onBackPressedCallback.remove();
    }


}