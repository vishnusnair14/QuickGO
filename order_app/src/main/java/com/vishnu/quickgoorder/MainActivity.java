package com.vishnu.quickgoorder;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavInflater;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.vishnu.quickgoorder.cloud.DbHandler;
import com.vishnu.quickgoorder.databinding.ActivityMainBinding;
import com.vishnu.quickgoorder.miscellaneous.PreferenceKeys;
import com.vishnu.quickgoorder.miscellaneous.SharedDataView;
import com.vishnu.quickgoorder.miscellaneous.Utils;
import com.vishnu.quickgoorder.service.GPSProviderService;
import com.vishnu.quickgoorder.service.LocationService;
import com.vishnu.quickgoorder.ui.home.recommendation.HomeRecommendationFragment;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MainActivity extends AppCompatActivity {
    private final String LOG_TAG = "MainActivity";
    private AppBarConfiguration mAppBarConfiguration;
    private NavController navController;
    private SharedPreferences preferences;
    DocumentReference RegisteredUsersCredentialsRef;
    DocumentReference RegisteredUsersEmailRef;
    private Vibrator vibrator;
    SharedDataView sharedDataView;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    TextView locTV;
    FirebaseUser user;
    boolean isFineLocationGranted = false;
    boolean isCoarseLocationGranted = false;
    boolean isWriteExternalStorageGranted = false;
    boolean isReadExternalStorageGranted = false;
    boolean isPostNotificationGranted = false;
    boolean isRecordAudioGranted = false;
    ActivityResultLauncher<String[]> locationPermissionRequest;
    ActivityResultLauncher<String[]> storagePermissionRequest;
    ActivityResultLauncher<String[]> audioPermissionRequest;

    //    NavOptions navOptions = new NavOptions.Builder().setPopUpTo(R.id.nav_home, false).build();

    SharedPreferences settingsPref;
    private ActivityMainBinding binding;


    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = (sharedPreferences, key) -> {
        if ("setRecommendationAsDefaultHomeView".equals(key)) {
            new Handler().postDelayed(() -> {
                setupNavSettings();
                vibrate();
            }, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar tb = findViewById(R.id.toolbar);

        setSupportActionBar(tb);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        assert user != null;

        startLocationService();
        startGPSProviderService();

        sharedDataView = new ViewModelProvider(this).get(SharedDataView.class);
        settingsPref = getSharedPreferences("settings", Context.MODE_PRIVATE);
        preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        RegisteredUsersCredentialsRef = db.collection("AuthenticationData")
                .document("RegisteredUsersCredentials");
        RegisteredUsersEmailRef = db.collection("AuthenticationData")
                .document("RegisteredUsersEmail");

        // OnCreate permission request
//        List<String> permissionsToRequest = new ArrayList<>();
//        for (String permission : permissions) {
//            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
//                permissionsToRequest.add(permission);
//            }
//        }
//
//        if (!permissionsToRequest.isEmpty()) {
//            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), 1);
//        } else {
//            Log.i(LOG_TAG, "ALL PERMISSION GRANTED");
//        }

        registerLocationPermissionResult();
        registerStoragePermissionResult();
        registerAudioPermissionResult();

        checkForPermissions();

        setupNavSettings();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        if (user != null) {
                            DbHandler.updateFCMTokenToDB(token, user.getUid());
                        }
                        Log.d(LOG_TAG, "FCM token updated: " + token);
                    } else {
                        Log.e(LOG_TAG, "Failed to get token: " + task.getException());
                    }
                });

        // Register preference change listener
        settingsPref.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

//        Initialize an SDK using below provided Client Id and Client Secret
//        PlatformConfig config = new PlatformConfig.Builder()
//                .clientId("c22974f9-e65a-4b54-bb26-6246d06d3bc3")
//                .clientSecret("BrNOo8Jte3ZTWqFt1KLlUDf79Wb1KuRy")
//                .accessTokenUrl("https://account.olamaps.io/realms/olamaps/protocol/openid-connect/")
//                .placesUrl("https://api.olamaps.io/places/v1/")
//                .routesUrl("https://api.olamaps.io/routing/v1/")
//                .build();
    }

    private void setPermissionStatusView(String type, boolean show) {
        TextView permissionStatusTV = findViewById(R.id.permissionStatusTV_textView);
        if (show) {
            permissionStatusTV.setVisibility(View.VISIBLE);
            switch (type) {
                case "loc" -> permissionStatusTV.setText("ENABLE LOCATION PERMISSION");
                case "storage" -> permissionStatusTV.setText("ENABLE STORAGE PERMISSION");
                case "audio" -> permissionStatusTV.setText("ENABLE AUDIO PERMISSION");
            }
        } else {
            permissionStatusTV.setVisibility(View.GONE);
        }

    }

    private void registerLocationPermissionResult() {
        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts
                        .RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = result.getOrDefault(
                            android.Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if (fineLocationGranted != null && fineLocationGranted) {
                        // Precise location access granted.
                        showProminentDisclosure("storage");
                        Log.d(LOG_TAG, "Precise location access granted!");
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        Log.d(LOG_TAG, "approximate location access granted!");
                    } else {
                        setPermissionStatusView("loc", true);
                        Log.d(LOG_TAG, "No location access granted!");
                    }
                }
        );
    }

    private void registerStoragePermissionResult() {
        storagePermissionRequest = registerForActivityResult(new ActivityResultContracts
                        .RequestMultiplePermissions(), result -> {
                    Boolean writeExternalStorageGranted = result.getOrDefault(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE, false);
                    Boolean readExternalStorageGranted = result.getOrDefault(
                            Manifest.permission.READ_EXTERNAL_STORAGE, false);

                    if (readExternalStorageGranted != null && readExternalStorageGranted) {
                        Log.d(LOG_TAG, "Read external storage access granted!");
                        setPermissionStatusView("None", false);
                        showProminentDisclosure("audio");
                    } else if (writeExternalStorageGranted != null && writeExternalStorageGranted) {
                        Log.d(LOG_TAG, "Write external storage access granted!");
                        setPermissionStatusView("None", false);
                    } else {
                        setPermissionStatusView("storage", true);
                        Log.d(LOG_TAG, "No storage access granted!");
                    }
                }
        );
    }

    private void registerAudioPermissionResult() {
        audioPermissionRequest = registerForActivityResult(new ActivityResultContracts
                        .RequestMultiplePermissions(), result -> {
                    Boolean recordAudioGranted = result.getOrDefault(
                            Manifest.permission.RECORD_AUDIO, false);
                    Boolean readMediaAudioGranted = result.getOrDefault(
                            Manifest.permission.READ_MEDIA_AUDIO, false);

                    if (recordAudioGranted != null && recordAudioGranted) {
                        Log.d(LOG_TAG, "Record audio access granted!");
                    } else if (readMediaAudioGranted != null && readMediaAudioGranted) {
                        Log.d(LOG_TAG, "Read media access granted!");
                        setPermissionStatusView("None", false);
                    } else {
                        setPermissionStatusView("audio", true);
                        Log.d(LOG_TAG, "No audio access granted!");
                    }
                }
        );
    }

    private void showProminentDisclosure(String type) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.prominent_disclosure, null);
        TextView headingTxt = dialogView.findViewById(R.id.prominentDisclosureHeading_textView);
        TextView subHeadingTxt1 = dialogView.findViewById(R.id.prominentDisclosureSubPoint1_textView);
        TextView subHeadingTxt2 = dialogView.findViewById(R.id.prominentDisclosureSubPoint2_textView);
        TextView subHeadingTxt3 = dialogView.findViewById(R.id.prominentDisclosureSubPoint3_textView);

        if (preferences.getInt("permissionDenyCount", 1) >= 3) {
            switch (type) {
                case "loc" -> {
                    headingTxt.setText(R.string.location_permission_required);
                    subHeadingTxt1.setText(R.string.location_permission_consent_1);
                    subHeadingTxt2.setText(R.string.location_permission_consent_2);
                    subHeadingTxt3.setText(R.string.go_to_app_settings_app_permission);
                }
                case "storage" -> {

                    headingTxt.setText("Storage permission required");
                    subHeadingTxt1.setText(R.string.storage_permission_consent_1);
                    subHeadingTxt2.setText(R.string.storage_permission_consent_2);
                    subHeadingTxt3.setText(R.string.go_to_app_settings_app_permission);
                }
                case "audio" -> {
                    headingTxt.setText("Audio record permission required");
                    subHeadingTxt1.setText(R.string.storage_permission_consent_1);
                    subHeadingTxt2.setText(R.string.storage_permission_consent_2);
                    subHeadingTxt3.setText(R.string.go_to_app_settings_app_permission);
                }
            }
        } else {
            switch (type) {
                case "loc" -> {
                    headingTxt.setText(R.string.location_permission_required);
                    subHeadingTxt1.setText(R.string.location_permission_consent_1);
                    subHeadingTxt2.setText(R.string.location_permission_consent_2);
                }
                case "storage" -> {
                    headingTxt.setText("Storage permission required");
                    subHeadingTxt1.setText(R.string.storage_permission_consent_1);
                    subHeadingTxt2.setText(R.string.storage_permission_consent_2);
                }
                case "audio" -> {
                    headingTxt.setText("Audio record permission required");
                    subHeadingTxt1.setText(R.string.storage_permission_consent_1);
                    subHeadingTxt2.setText(R.string.storage_permission_consent_2);
                }
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .setNegativeButton("Not now", (dialog, which) -> {
                    preferences.edit().putInt("permissionDenyCount", preferences.getInt("permissionDenyCount", 0) + 1).apply();
                    switch (type) {
                        case "loc" -> setPermissionStatusView("loc", true);
                        case "storage" -> setPermissionStatusView("storage", true);
                        case "audio" -> setPermissionStatusView("audio", true);
                    }
                    Log.d(LOG_TAG, "prominent disclosure cancelled");
                }).setPositiveButton("Continue", (dialog, which) -> {
                    switch (type) {
                        case "loc" -> locationPermissionRequest.launch(new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                        });
                        case "storage" -> storagePermissionRequest.launch(new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        });
                        case "audio" -> audioPermissionRequest.launch(new String[]{
                                Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.READ_MEDIA_AUDIO
                        });
                    }
                }).show();

    }

    private void checkForPermissions() {
        Log.d(LOG_TAG, "checking permissions");
        isFineLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        isCoarseLocationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        isWriteExternalStorageGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        isReadExternalStorageGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        isRecordAudioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        isPostNotificationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        if (!isFineLocationGranted && !isCoarseLocationGranted) {
            showProminentDisclosure("loc");
        } else if (isFineLocationGranted && isCoarseLocationGranted) {
            if (!isWriteExternalStorageGranted || !isReadExternalStorageGranted) {
                showProminentDisclosure("storage");
            } else if (!isRecordAudioGranted) {
                showProminentDisclosure("audio");
            } else {
                Log.d(LOG_TAG, "All permissions granted");
            }
        }

    }

//    setPermissionStatusView("None", false);
//                Toast.makeText(this, "All permission granted", Toast.LENGTH_SHORT).show();

    private void setupNavSettings() {
        // *** LEFT DRAWER MENU ***
        DrawerLayout navDrawerLayout = binding.drawerLayout;
        NavigationView leftDrawerMenu = binding.navView;
        View headerView = leftDrawerMenu.getHeaderView(0);

        locTV = findViewById(R.id.locationView_textView);

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupWithNavController(leftDrawerMenu, navController);

        // Load the navigation graph
        NavInflater navInflater = navController.getNavInflater();
        NavGraph navGraph = navInflater.inflate(R.navigation.mobile_navigation);

        boolean setRecommendationAsDefaultHomeView = settingsPref.getBoolean("setRecommendationAsDefaultHomeView", false);

        if (setRecommendationAsDefaultHomeView) {
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_recommendation,
                    R.id.nav_settings, R.id.nav_mcart)
                    .setOpenableLayout(navDrawerLayout).build();
            navGraph.setStartDestination(R.id.nav_recommendation);
        } else {
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_orderbyvoice,
                    R.id.nav_settings, R.id.nav_mcart)
                    .setOpenableLayout(navDrawerLayout).build();
            navGraph.setStartDestination(R.id.nav_orderbyvoice);
        }
        navController.setGraph(navGraph);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        TextView emailIdTV = headerView.findViewById(R.id.userEmailIDView_textBox);
        TextView userIdTV = headerView.findViewById(R.id.userUIDView_textBox);

        if (mAuth.getCurrentUser() != null) {
            if (mAuth.getCurrentUser().isEmailVerified()) {
                emailIdTV.setText(MessageFormat.format("{0} {1}",
                        mAuth.getCurrentUser().getEmail(), "(verified)"));
                userIdTV.setText(mAuth.getCurrentUser().getUid());
            } else {
                emailIdTV.setText(mAuth.getCurrentUser().getEmail());
                userIdTV.setText(mAuth.getCurrentUser().getUid());
            }
        } else {
            emailIdTV.setText("");
            userIdTV.setText("");
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_ENABLE_BROADCAST);
        startService(serviceIntent);
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_DISABLE_BROADCAST);
        stopService(serviceIntent);
    }

    private void startGPSProviderService() {
        Intent serviceIntent = new Intent(this, GPSProviderService.class);
        startService(serviceIntent);
    }

    private void stopGPSProviderService() {
        Intent serviceIntent = new Intent(this, GPSProviderService.class);
        stopService(serviceIntent);
    }


    private void executeDelAccPrefs() {
        Map<String, Object> updates = new HashMap<>();

        assert user != null;
        updates.put(user.getUid().trim(), FieldValue.delete());

        user.delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        preferences.edit().clear().apply();
                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to delete account" +
                                task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });


        /* Delete user credentials from "RegisteredUsersCredentials" db bucket */
        RegisteredUsersCredentialsRef.update(updates)
                .addOnSuccessListener(aVoid -> Log.d(LOG_TAG, "Credentials deleted successfully"))
                .addOnFailureListener(e -> Log.w(LOG_TAG, "Error deleting credentials", e));

        /* Delete registered email from "RegisteredUsersEmail" db bucket */
        RegisteredUsersEmailRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                List<String> emailAddresses = (List<String>) documentSnapshot.get("email_addresses");

                if (emailAddresses != null) {
                    emailAddresses.remove(user.getEmail());

                    updates.put("email_addresses", emailAddresses);

                    // Perform the update operation
                    RegisteredUsersEmailRef.update(updates)
                            .addOnSuccessListener(aVoid -> Log.d(LOG_TAG, "Email removed successfully"))
                            .addOnFailureListener(e -> Log.w(LOG_TAG, "Error removing email", e));
                } else {
                    Log.d(LOG_TAG, "Array field 'email_addresses' is null");
                }
            } else {
                Log.d(LOG_TAG, "Document does not exist");
            }
        });

        preferences.edit().putString("username", null).apply();
        preferences.edit().putString("password", null).apply();
        preferences.edit().putBoolean("isRemembered", false).apply();
        preferences.edit().putBoolean("isInitialLogin", true).apply();
        preferences.edit().putString(PreferenceKeys.HOME_RECOMMENDATION_SELECTED_ADDRESS_KEY, "None").apply();
        preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_KEY, "None").apply();
        preferences.edit().putString("selectedDeliveryAddressKey", "").apply();
        preferences.edit().putString("selectedDeliveryAddressLat", "").apply();
        preferences.edit().putString("selectedDeliveryAddressLon", "").apply();
        preferences.edit().putString(PreferenceKeys.HOME_RECOMMENDATION_SELECTED_ADDRESS_TYPE, "Select an address for delivery").apply();
        preferences.edit().putString(PreferenceKeys.HOME_RECOMMENDATION_SELECTED_ADDRESS_FULL_ADDRESS, "").apply();
        preferences.edit().putBoolean(PreferenceKeys.IS_SET_TO_CURRENT_LOCATION, false).apply();
        preferences.edit().putString(PreferenceKeys.HOME_RECOMMENDATION_CURRENT_SHOP_PINCODE, "0").apply();
        preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_FRAGMENT_ORDER_ID, "0").apply();
        preferences.edit().putString(PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_AUDIO_REF_ID, "0").apply();
        preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_FRAGMENT_AUDIO_REF_ID, "0").apply();
        preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_TYPE, "Select an address").apply();
        preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_FULL_ADDRESS, "Tap on a delivery address to make it as default").apply();
        preferences.edit().putBoolean("setRecommendationAsDefaultHomeView", false).apply();

        try {
            HomeRecommendationFragment.writeShopDataToFile(this, "{}");
            Utils.deleteAddressDataInFile(this);
            Utils.deleteVoiceOrdersFolder(this);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
        finish();
    }

    private void showDelAccDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Account");
        builder.setMessage("Removes all your data and identity from our databases. " +
                "This action can't be undone. Do you want to proceed?");
        builder.setPositiveButton("YES", (dialog, which) -> {
            // Perform account-deletion action
            executeDelAccPrefs();
        });
        builder.setNegativeButton("NO", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void executeLogoutPrefs() {
        FirebaseAuth.getInstance().signOut();
        preferences.edit().putString("username", null).apply();
        preferences.edit().putString("password", null).apply();
        preferences.edit().putBoolean("isRemembered", false).apply();
        preferences.edit().putBoolean("isAlreadyScanned", false).apply();
        preferences.edit().putBoolean("isInitialLogin", true).apply();
        preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_KEY, "None").apply();
        preferences.edit().putString(PreferenceKeys.HOME_RECOMMENDATION_SELECTED_ADDRESS_KEY, "None").apply();
        preferences.edit().putString("selectedDeliveryAddressKey", "").apply();
        preferences.edit().putString("selectedDeliveryAddressLat", "").apply();
        preferences.edit().putString("selectedDeliveryAddressLon", "").apply();
        preferences.edit().putString(PreferenceKeys.HOME_RECOMMENDATION_SELECTED_ADDRESS_TYPE, "Select an address for delivery").apply();
        preferences.edit().putString(PreferenceKeys.HOME_RECOMMENDATION_SELECTED_ADDRESS_FULL_ADDRESS, "").apply();
        preferences.edit().putBoolean(PreferenceKeys.IS_SET_TO_CURRENT_LOCATION, false).apply();
        preferences.edit().putString(PreferenceKeys.HOME_RECOMMENDATION_CURRENT_SHOP_PINCODE, "0").apply();
        preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_FRAGMENT_ORDER_ID, "0").apply();
        preferences.edit().putString(PreferenceKeys.HOME_RECOMMENDATION_FRAGMENT_AUDIO_REF_ID, "0").apply();
        preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_FRAGMENT_AUDIO_REF_ID, "0").apply();
        preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_TYPE, "Select an address").apply();
        preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_FULL_ADDRESS, "Tap on a delivery address to make it as default").apply();
        preferences.edit().putBoolean("setRecommendationAsDefaultHomeView", false).apply();

        try {
            HomeRecommendationFragment.writeShopDataToFile(this, "{}");
            Utils.deleteAddressDataInFile(this);
            Utils.deleteVoiceOrdersFolder(this);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }

        vibrate();
        Toast.makeText(this, "Logout successful", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Logout", (dialog, which) -> {
            // Perform logout action
            executeLogoutPrefs();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void vibrate() {
        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.EFFECT_TICK));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_toolbar_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.cartToolbarButton) {
//            if (Objects.requireNonNull(navController.getCurrentDestination()).getId() != R.id.nav_mcart) {
//                navController.navigate(R.id.nav_mcart, null, navOptions);
//                Navigation.findNavController(this, R.id.action_nav_shop_to_nav_mcart);
            navController.navigate(R.id.nav_mcart);
            return true;

        } else if (item.getItemId() == R.id.action_logout) {
            showLogoutDialog();
            return true;
        } else if (item.getItemId() == R.id.action_delete_account) {
            showDelAccDialog();
            return true;
        }
//        return super.onOptionsItemSelected(item);
        return false;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupNavSettings();
    }

    @Override
    protected void onStart() {
        super.onStart();
//        setupNavSettings();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        settingsPref.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        stopLocationService();
        stopGPSProviderService();
    }
}

