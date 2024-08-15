package com.vishnu.quickgodelivery;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;
import com.vishnu.quickgodelivery.callbacks.DutyStatusData;
import com.vishnu.quickgodelivery.cloud.DbHandler;
import com.vishnu.quickgodelivery.databinding.ActivityMainBinding;
import com.vishnu.quickgodelivery.miscellaneous.StartDutyModel;
import com.vishnu.quickgodelivery.miscellaneous.EndDutyModel;
import com.vishnu.quickgodelivery.miscellaneous.SharedDataView;
import com.vishnu.quickgodelivery.miscellaneous.Utils;
import com.vishnu.quickgodelivery.server.APIService;
import com.vishnu.quickgodelivery.server.ApiServiceGenerator;
import com.vishnu.quickgodelivery.services.DutyNotification;
import com.vishnu.quickgodelivery.services.GPSProviderService;
import com.vishnu.quickgodelivery.services.LocationService;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    FirebaseFirestore db;
    private FirebaseUser user;
    private final String LOG_TAG = "MainActivity";
    Intent dutyNotificationService;
    NotificationManager dutyNotificationManager;
    NotificationChannel dutyChannel;
    private Vibrator vibrator;
    private boolean isGpsEnabled = false;
    private BottomSheetDialog enableLocBtmDialog;
    DocumentReference RegisteredUsersCredentialsRef;
    DocumentReference RegisteredUsersEmailRef;
    TextView locationTV;
    TextView statusTV;
    private SharedPreferences preferences;
    SwitchCompat dutyToggle;
    private double dp_lat;
    private double dp_lon;
    TextView drawerDutyStatusView;
    SharedDataView sharedDataView;
    private DrawerLayout drawerLayout;
    TextView onDutyTV, offDutyTV;
    private ActivityResultLauncher<Intent> gpsActivityResultLauncher;
    private static final int GPS_CHECK_INTERVAL = 1000;
    private Handler gpsCheckHandler;
    private AppBarConfiguration mAppBarConfiguration;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermissions();

        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();


        sharedDataView = new ViewModelProvider(this).get(SharedDataView.class);
        preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        ActivityMainBinding activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());

        gpsCheckHandler = new Handler(Looper.getMainLooper());

        drawerLayout = activityMainBinding.drawerLayout;
        NavigationView leftDrawerMenu = activityMainBinding.navView;
        View headerView = leftDrawerMenu.getHeaderView(0);

        onDutyTV = findViewById(R.id.onDutyView_textView);
        offDutyTV = findViewById(R.id.offDutyView_textView);
        locationTV = findViewById(R.id.locationView_textView);
        statusTV = findViewById(R.id.serverStatusFeedbackOrders_textView);

        locationTV.setVisibility(View.GONE);

        RegisteredUsersCredentialsRef = db.collection("AuthenticationData")
                .document("RegisteredUsersCredentials");
        RegisteredUsersEmailRef = db.collection("AuthenticationData")
                .document("RegisteredUsersEmail");

        dutyToggle = Objects.requireNonNull(leftDrawerMenu.getMenu().findItem(R.id.nav_startDuty)
                .getActionView()).findViewById(R.id.start_duty_toggle_button);

        drawerDutyStatusView = Objects.requireNonNull(leftDrawerMenu.getMenu().findItem(R.id.nav_startDuty)
                .getActionView()).findViewById(R.id.mainDrawerDutyView_textView);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_orders, R.id.nav_settings)
                .setOpenableLayout(drawerLayout).build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(leftDrawerMenu, navController);

        // duty notification channel
        dutyChannel = new NotificationChannel("CHANNEL_ID", "Duty Status", NotificationManager.IMPORTANCE_DEFAULT);
        dutyNotificationManager = getSystemService(NotificationManager.class);
        dutyNotificationManager.createNotificationChannel(dutyChannel);
        dutyNotificationService = new Intent(this, DutyNotification.class);

//        // FETCH CURRENT DUTY STATUS
//        if (preferences.getBoolean("isOnDuty", false)) {
//            startLocationService();
//            dutyNotificationService.putExtra("DUTY_NOTIFICATION_TEXT", "You're still on duty");
//            startService(dutyNotificationService);
//            showOnDutyView();
//            locationTV.setVisibility(View.VISIBLE);
//            drawerDutyStatusView.setText(R.string.on_duty);
//
//            if (dutyToggle != null) {
//                if (!dutyToggle.isChecked()) {
//                    dutyToggle.setChecked(true);
//                }
//            }
//        } else if (!preferences.getBoolean("isOnDuty", false)) {
//            stopLocationService();
//            locationTV.setVisibility(View.GONE);
//            showOffDutyView();
//            drawerDutyStatusView.setText(R.string.off_duty);
//
//            if (dutyToggle != null) {
//                if (dutyToggle.isChecked()) {
//                    dutyToggle.setChecked(false);
//                }
//            }
//        }

//        getCurrentDutyStatusData(data -> {
//            if (data != null) {
//                String duty_mode = data.get("duty_mode").getAsString().trim();
//                if (duty_mode.equals("on_duty")) {
//                    startLocationService();
//                    showOnDutyView();
//                    drawerDutyStatusView.setText(R.string.on_duty);
//                    locationTV.setVisibility(View.VISIBLE);
//                    Toast.makeText(this, "You are on duty", Toast.LENGTH_SHORT).show();
//
////                    if (dutyToggle != null) {
////                        if (!dutyToggle.isChecked()) {
////                            dutyToggle.setChecked(true);
////                        }
////                    }
//                    dutyToggle.setSelected(true);
//
//                    if (preferences.getBoolean("isOnDuty", false)) {
//                        dutyNotificationService.putExtra("DUTY_NOTIFICATION_TEXT", "You're still on duty");
//                        startService(dutyNotificationService);
//                    }
//                } else if (duty_mode.equals("off_duty")) {
//                    showOffDutyView();
//                    drawerDutyStatusView.setText(R.string.off_duty);
//                    locationTV.setVisibility(View.GONE);
//                    Toast.makeText(this, "You are off duty", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//
//        // TODO: clean here make it std.
//        // DUTY START-STOP BUTTON LISTENER
//        if (dutyToggle != null) {
//            dutyToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
//                if (Utils.isNetworkConnected(MainActivity.this)) {
//                    if (isChecked) {
//                        if (Utils.isGPSEnabled(MainActivity.this)) {
//                            if (dp_lat != 0 && dp_lon != 0) {
//                                closeLeftDrawer();
//                                sentDutyStartRequest();
//                            } else {
//                                closeLeftDrawer();
//                                startGpsCheck();
//                            }
//                        } else {
//                            closeLeftDrawer();
//                            dutyToggle.setChecked(false);
//                            locationTV.setVisibility(View.GONE);
//                            showEnableLocationBtmView(true);
//                        }
//                    } else {
//                        sentDutyEndRequest(user.getUid(), preferences.getString("decodedCityForDuty", "0"));
//                        closeLeftDrawer();
//                    }
//                } else {
//                    Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
//                    dutyToggle.setChecked(!isChecked);
//                }
//            });
//        } else {
//            Toast.makeText(this, "Unable to perform action.", Toast.LENGTH_SHORT).show();
//        }


        // Initialize the ActivityResultLauncher

        checkDutyStatus();

        gpsActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Utils.isGPSEnabled(MainActivity.this)) {
                        if (dp_lat != 0 && dp_lon != 0) {
                            closeLeftDrawer();
                            dutyToggle.setChecked(true);
                            startLocationService();
                            startDutyNotificationService();
                            Toast.makeText(this, "GPS Enabled, Duty Started", Toast.LENGTH_SHORT).show();
                        } else {
                            closeLeftDrawer();
                            startGpsCheck();
                        }
                    } else {
                        dutyToggle.setChecked(false);
//                        stopLocationService();
                        Toast.makeText(this, "GPS is not enabled, Duty cannot be started", Toast.LENGTH_SHORT).show();
                    }
                }
        );


        // GET FCM APP TOKEN
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

        // GETS & SETS PROFILE VIEW
        ImageView profilePhotoImageView = headerView.findViewById(R.id.profilePhoto_imageView);
        TextView nameTV = headerView.findViewById(R.id.profileUsername_textView);
        TextView emailIdTV = headerView.findViewById(R.id.userEmailIDView_textBox);
        TextView userIdTV = headerView.findViewById(R.id.userUIDView_textBox);

        // sets
        if (mAuth.getCurrentUser() != null) {
            Picasso.get().load(user.getPhotoUrl()).into(profilePhotoImageView);
            nameTV.setText(MessageFormat.format("WELCOME {0}",
                    Objects.requireNonNull(Objects.requireNonNull(user.getDisplayName()).toUpperCase())));

            if (mAuth.getCurrentUser().isEmailVerified()) {
                emailIdTV.setText(MessageFormat.format("{0} {1}",
                        user.getEmail(), "(verified)"));
                userIdTV.setText(user.getUid());
            } else {
                emailIdTV.setText(user.getEmail());
                userIdTV.setText(user.getUid());
            }
        } else {
            emailIdTV.setText("");
            userIdTV.setText("");
        }
    }


    private void initializeDutyToggle() {
        // Get the stored duty state and update the UI accordingly
        boolean isOnDuty = preferences.getBoolean("isOnDuty", false);

        if (isOnDuty) {
            startLocationService();
            showOnDutyView();
            drawerDutyStatusView.setText(R.string.on_duty);
            locationTV.setVisibility(View.VISIBLE);
            startDutyNotificationService();
        } else {
            showOffDutyView();
            drawerDutyStatusView.setText(R.string.off_duty);
            locationTV.setVisibility(View.GONE);
        }

        // Temporarily remove listener before setting state
        dutyToggle.setOnCheckedChangeListener(null);
        dutyToggle.setChecked(isOnDuty);
        dutyToggle.setSelected(isOnDuty);
        dutyToggle.setOnCheckedChangeListener(dutyToggleListener);

        // Log the restored state
        Log.d("DutyToggle", "Restored state: " + (isOnDuty ? "On Duty" : "Off Duty"));
    }

    // Listener for the duty toggle button
    private final CompoundButton.OnCheckedChangeListener dutyToggleListener = (buttonView, isChecked) -> {
        if (Utils.isNetworkConnected(MainActivity.this)) {
            if (isChecked) {
                if (Utils.isGPSEnabled(MainActivity.this)) {
                    if (dp_lat != 0 && dp_lon != 0) {
                        closeLeftDrawer();
                        sentDutyStartRequest();
                        startDutyNotificationService();

                        preferences.edit().putBoolean("isOnDuty", true).apply();
                        Log.d("DutyToggle", "Duty started.");
                    } else {
                        closeLeftDrawer();
                        startGpsCheck();
                    }
                } else {
                    closeLeftDrawer();
                    dutyToggle.setChecked(false);
                    locationTV.setVisibility(View.GONE);
                    showEnableLocationBtmView(true);
                }
            } else {
                sentDutyEndRequest(user.getUid(), preferences.getString("decodedCityForDuty", "0"));
                closeLeftDrawer();

                preferences.edit().putBoolean("isOnDuty", false).apply();
                Log.d("DutyToggle", "Duty ended.");
            }
        } else {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_SHORT).show();
            dutyToggle.setChecked(!isChecked);  // Revert the toggle state
        }
    };

    private void startDutyNotificationService() {
        dutyNotificationService.putExtra("DUTY_NOTIFICATION_TEXT", "You're still on duty");
        startService(dutyNotificationService);
    }

    // Call this method in onCreate or wherever you need to initialize the UI
    private void checkDutyStatus() {
        getCurrentDutyStatusData(data -> {
            if (data != null) {
                String duty_mode = data.get("duty_mode").getAsString().trim();
                if (duty_mode.equals("on_duty")) {
                    preferences.edit().putBoolean("isOnDuty", true).apply();
                } else if (duty_mode.equals("off_duty")) {
                    preferences.edit().putBoolean("isOnDuty", false).apply();
                } else {
                    preferences.edit().putBoolean("isOnDuty", false).apply();
                }

                // Initialize the duty toggle state
                initializeDutyToggle();
            }
        });

        if (dutyToggle != null) {
            dutyToggle.setOnCheckedChangeListener(dutyToggleListener);
        } else {
            Toast.makeText(this, "Unable to perform action.", Toast.LENGTH_SHORT).show();
        }
    }


    private final BroadcastReceiver gpsStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GPSProviderService.ACTION_GPS_STATUS_CHANGED.equals(intent.getAction())) {
                boolean isGPSEnabled = intent.getBooleanExtra(GPSProviderService.EXTRA_IS_GPS_ENABLED, false);
                if (isGPSEnabled && !isGpsEnabled) {
                    Toast.makeText(context, "GPS Enabled", Toast.LENGTH_SHORT).show();

                    if (enableLocBtmDialog != null && enableLocBtmDialog.isShowing()) {
                        enableLocBtmDialog.hide();
                    }
                    isGpsEnabled = true;
                } else if (!isGPSEnabled && isGpsEnabled) {
                    Toast.makeText(context, "GPS Disabled", Toast.LENGTH_SHORT).show();
                    if (dutyToggle.isChecked()) {
                        showEnableLocationBtmView(false);
                    }
                    isGpsEnabled = false;
                }
            }
        }
    };

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocationService.ACTION_LOCATION_BROADCAST.equals(intent.getAction())) {
                dp_lat = intent.getDoubleExtra(LocationService.EXTRA_LATITUDE, 0.00);
                dp_lon = intent.getDoubleExtra(LocationService.EXTRA_LONGITUDE, 0.00);
            }
        }
    };

    private void closeLeftDrawer() {
        if (drawerLayout != null) {
            new Handler().postDelayed(() -> drawerLayout.close(), 600);
        }
    }

    private void getCurrentDutyStatusData(DutyStatusData dutyStatusData) {
        statusTV.setVisibility(View.GONE);
        APIService apiService = ApiServiceGenerator.getApiService(this);
        Call<JsonObject> call = apiService.getDutyStatus(user.getUid());

        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (response.body().has("has_data") && response.body().get("has_data").getAsBoolean()) {
                        if (response.body().has("data")) {
                            dutyStatusData.data(response.body().get("data").getAsJsonObject());
                        }
                        dutyStatusData.data(null);
                    } else {
                        dutyStatusData.data(null);
                        Toast.makeText(MainActivity.this, response.body().get("message").getAsString(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    dutyStatusData.data(null);
                    Toast.makeText(MainActivity.this, "Duty status check failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                dutyStatusData.data(null);
                Toast.makeText(MainActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startGpsCheck() {
        // Show a loading indicator or message to the user
        View gpsCheckBtmView = LayoutInflater.from(this).inflate(
                R.layout.init_gps_dialog, null, false);

        // Create a BottomSheetDialog with TOP gravity
        BottomSheetDialog gpsCheckBtmDialog = new BottomSheetDialog(this);
        gpsCheckBtmDialog.setContentView(gpsCheckBtmView);
        gpsCheckBtmDialog.setCanceledOnTouchOutside(false);
        Objects.requireNonNull(gpsCheckBtmDialog.getWindow()).setGravity(Gravity.TOP);

        if (!gpsCheckBtmDialog.isShowing()) {
            gpsCheckBtmDialog.show();
        }

        // GPS coordinates are not valid, check again after the interval
        Runnable gpsCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (dp_lat != 0.0 && dp_lon != 0.0) {
                    gpsCheckBtmDialog.hide();
                    gpsCheckBtmDialog.dismiss();
                    sentDutyStartRequest();
                } else {
                    // GPS coordinates are not valid, check again after the interval
                    gpsCheckHandler.postDelayed(this, GPS_CHECK_INTERVAL);
                }
            }
        };
        gpsCheckHandler.post(gpsCheckRunnable);
    }

    public void sentDutyStartRequest() {
        View sentDutyStartRequestBtmView = LayoutInflater.from(this).inflate(
                R.layout.bottomview_duty_start_status, null, false);

        // Create a BottomSheetDialog with TOP gravity
        BottomSheetDialog sentDutyStartRequestBtmDialog = new BottomSheetDialog(this);
        sentDutyStartRequestBtmDialog.setContentView(sentDutyStartRequestBtmView);
        sentDutyStartRequestBtmDialog.setCancelable(false);
        sentDutyStartRequestBtmDialog.setCanceledOnTouchOutside(false);
        Objects.requireNonNull(sentDutyStartRequestBtmDialog.getWindow()).setGravity(Gravity.TOP);

        if (!sentDutyStartRequestBtmDialog.isShowing()) {
            sentDutyStartRequestBtmDialog.show();
        }
        StartDutyModel startDutyData = new StartDutyModel(user.getDisplayName(), user.getUid(), dp_lat, dp_lon);

        APIService apiService = ApiServiceGenerator.getApiService(this);
        Call<JsonObject> call2388 = apiService.startDuty(startDutyData);

        call2388.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        if (response.body().has("isDutyStarted") && response.body().get("isDutyStarted").getAsBoolean()) {
                            locationTV.setVisibility(View.VISIBLE);
                            statusTV.setText(R.string.waiting_for_orders);

                            showOnDutyView();
                            drawerDutyStatusView.setText(R.string.on_duty);
                            preferences.edit().putBoolean("isOnDuty", true).apply();

                            dutyNotificationService.putExtra("DUTY_NOTIFICATION_TEXT", "Duty started, You'll get orders soon");
                            startService(dutyNotificationService);
                            Toast.makeText(MainActivity.this, response.body().get("message").getAsString(), Toast.LENGTH_SHORT).show();
                            Log.i(LOG_TAG, "Duty started, You'll get orders soon");

                            sentDutyStartRequestBtmDialog.hide();
                            sentDutyStartRequestBtmDialog.dismiss();

                        } else if (response.body().has("exception")) {
                            showOffDutyView();
                            dutyToggle.setChecked(false);
                            preferences.edit().putBoolean("isOnDuty", false).apply();
                            Log.e(LOG_TAG, "Unable to start duty: " + response.body().get("exception").getAsString());
                            Toast.makeText(MainActivity.this, response.body().get("message").getAsString(), Toast.LENGTH_LONG).show();
                        } else {
                            showOffDutyView();
                            dutyToggle.setChecked(false);
                            preferences.edit().putBoolean("isOnDuty", false).apply();
                            Log.e(LOG_TAG, "Unable to start duty, error occurred!");
                            Toast.makeText(MainActivity.this, response.body().get("message").getAsString(), Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    showOffDutyView();
                    dutyToggle.setChecked(false);
                    preferences.edit().putBoolean("isOnDuty", false).apply();
                    sentDutyStartRequestBtmDialog.hide();
                    sentDutyStartRequestBtmDialog.dismiss();
                    Log.e(LOG_TAG, "Duty response failed: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                showOffDutyView();
                sentDutyStartRequestBtmDialog.hide();
                sentDutyStartRequestBtmDialog.dismiss();
                preferences.edit().putBoolean("isOnDuty", false).apply();
                dutyToggle.setChecked(false);
                Toast.makeText(MainActivity.this, "Error: Failed to sent duty start request:" + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(LOG_TAG, "Error: Failed to sent duty start request: " + t.getMessage());
            }
        });
    }

    public void sentDutyEndRequest(String dpID, String cityName) {
        View sentDutyEndRequestBtmView = LayoutInflater.from(this).inflate(
                R.layout.bottomview_duty_end_status, null, false);

        // Create a BottomSheetDialog with TOP gravity
        BottomSheetDialog sentDutyEndRequestBtmDialog = new BottomSheetDialog(this);
        sentDutyEndRequestBtmDialog.setContentView(sentDutyEndRequestBtmView);
        sentDutyEndRequestBtmDialog.setCanceledOnTouchOutside(false);
        sentDutyEndRequestBtmDialog.setCancelable(false);
        Objects.requireNonNull(sentDutyEndRequestBtmDialog.getWindow()).setGravity(Gravity.TOP);

        if (!sentDutyEndRequestBtmDialog.isShowing()) {
            sentDutyEndRequestBtmDialog.show();
        }

        APIService apiService = ApiServiceGenerator.getApiService(this);
        EndDutyModel stopRequestModel = new EndDutyModel(dpID, cityName);
        Call<JsonObject> call2388 = apiService.endDuty(stopRequestModel);
        call2388.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        if (response.body().has("isDutyEnded") && response.body().get("isDutyEnded").getAsBoolean()) {
                            Log.d(LOG_TAG, "Duty stop request passed to server");

//                            stopLocationService();
                            statusTV.setVisibility(View.GONE);
                            locationTV.setVisibility(View.GONE);

                            showOffDutyView();
                            drawerDutyStatusView.setText(R.string.off_duty);
                            dutyNotificationService.putExtra("DUTY_NOTIFICATION_TEXT", "Duty stopped!");
                            startService(dutyNotificationService);

                            Toast.makeText(MainActivity.this, response.body().get("message").getAsString(), Toast.LENGTH_SHORT).show();
                            preferences.edit().putBoolean("isOnDuty", false).apply();
                            Log.i(LOG_TAG, "Duty stopped");

                            try {
                                sentDutyEndRequestBtmDialog.hide();
                                sentDutyEndRequestBtmDialog.dismiss();
                            } catch (Exception e) {
                                Log.e(LOG_TAG, e.toString());
                            }

                        } else if (response.body().has("exception")) {
                            dutyToggle.setChecked(true);
                            Log.e(LOG_TAG, "Unable to start duty: " + response.body().get("exception").getAsString());
                            Toast.makeText(MainActivity.this, response.body().get("message").getAsString(), Toast.LENGTH_LONG).show();
                        } else {
                            dutyToggle.setChecked(true);
                            Log.e(LOG_TAG, "Unable to start duty, error occurred!");
                            Toast.makeText(MainActivity.this, response.body().get("message").getAsString(), Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    dutyToggle.setChecked(true);
                    try {
                        sentDutyEndRequestBtmDialog.hide();
                        sentDutyEndRequestBtmDialog.dismiss();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.toString());
                    }
                    Log.e(LOG_TAG, "Duty response failed: " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                try {
                    sentDutyEndRequestBtmDialog.hide();
                    sentDutyEndRequestBtmDialog.dismiss();
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.toString());
                }
                dutyToggle.setChecked(true);
                Log.e(LOG_TAG, "Error: Failed to update duty info: " + t.getMessage());
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CALL_PHONE,
                android.Manifest.permission.FOREGROUND_SERVICE,
        };

        // Check each permission in the array and request if not granted
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]), 1);
        }
    }

    private void showLocationSettings(boolean startDutyOnCallback) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

        if (startDutyOnCallback) {
            gpsActivityResultLauncher.launch(intent);
        } else {
            startActivity(intent);
        }
    }

    private void showEnableLocationBtmView(boolean startDutyOnCallback) {
        View enableLocationBottomView = LayoutInflater.from(this).inflate(
                R.layout.bottomview_enable_location, null, false);

        // Create a BottomSheetDialog with TOP gravity
        enableLocBtmDialog = new BottomSheetDialog(this);
        enableLocBtmDialog.setContentView(enableLocationBottomView);
        enableLocBtmDialog.setCanceledOnTouchOutside(false);
        Objects.requireNonNull(enableLocBtmDialog.getWindow()).setGravity(Gravity.TOP);

        Button enableLocationBtn = enableLocationBottomView.findViewById(R.id.enableDeviceLocation1_button);

        enableLocationBtn.setOnClickListener(v -> {
            enableLocBtmDialog.hide();
            showLocationSettings(startDutyOnCallback);
        });

        if (!enableLocBtmDialog.isShowing()) {
            enableLocBtmDialog.show();
        }
    }


    private void startLocationService() {
        if (!LocationService.isRunning) {
            Intent serviceIntent = new Intent(this, LocationService.class);
            serviceIntent.setAction(LocationService.ACTION_ENABLE_BROADCAST);
            startService(serviceIntent);
        } else {
//            Toast.makeText(this, "Location service is already running", Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, "Location service is already running");
        }
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        serviceIntent.setAction(LocationService.ACTION_DISABLE_BROADCAST);
        stopService(serviceIntent);
    }

    private void startGPSProviderService() {
        if (!GPSProviderService.isRunning) {
            Intent serviceIntent = new Intent(this, GPSProviderService.class);
            startService(serviceIntent);
        } else {
//            Toast.makeText(this, "GPS provider service is already running.", Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, "GPS provider service is already running");
        }
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
                        Toast.makeText(this, "Failed to delete account" + task.getException(), Toast.LENGTH_SHORT).show();
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
        preferences.edit().putString("currentDeliveryOrderID", "0").apply();
        preferences.edit().putString("decodedCityForDuty", "0").apply();
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

    private void startVibration() {
        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.EFFECT_TICK));
    }

    private void executeLogoutPrefs() {
        FirebaseAuth.getInstance().signOut();
        preferences.edit().putString("username", null).apply();
        preferences.edit().putString("password", null).apply();
        preferences.edit().putBoolean("isRemembered", false).apply();
        preferences.edit().putBoolean("isAlreadyScanned", false).apply();
        preferences.edit().putBoolean("isInitialLogin", true).apply();
        preferences.edit().putString("currentDeliveryOrderID", "0").apply();
        preferences.edit().putString("decodedCityForDuty", "0").apply();

        startVibration();
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


    private void showOnDutyView() {
        offDutyTV.setVisibility(View.GONE);
        onDutyTV.setVisibility(View.VISIBLE);
    }

    private void showOffDutyView() {
        onDutyTV.setVisibility(View.GONE);
        offDutyTV.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
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
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(gpsStatusReceiver);
            unregisterReceiver(locationReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(LOG_TAG, "Receiver not registered", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter providerFilter = new IntentFilter(GPSProviderService.ACTION_GPS_STATUS_CHANGED);
        registerReceiver(gpsStatusReceiver, providerFilter, Context.RECEIVER_NOT_EXPORTED);

        IntentFilter filter = new IntentFilter(LocationService.ACTION_LOCATION_BROADCAST);
        registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        startLocationService();
        startGPSProviderService();

        if (Utils.isGPSEnabled(MainActivity.this)) {
            dutyToggle.setEnabled(true);
        } else {
            showEnableLocationBtmView(false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

//        stopService(dutyNotificationService);
        unregisterReceiver(gpsStatusReceiver);
        unregisterReceiver(locationReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (enableLocBtmDialog != null) {
            enableLocBtmDialog.hide();
            enableLocBtmDialog.dismiss();
        }
        stopService(dutyNotificationService);
        stopLocationService();
        stopGPSProviderService();
    }

}
