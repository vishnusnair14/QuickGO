package com.vishnu.quickgodelivery.ui.all_orders;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.vishnu.quickgodelivery.R;
import com.vishnu.quickgodelivery.services.LocationService;

import java.io.File;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AllOrdersFragment extends Fragment {
    com.vishnu.quickgodelivery.databinding.FragmentAllOrdersBinding bind;
    private final String LOG_TAG = "OrdersFragment";
    File AppDwnAudioDir, appExternalDirectory;
    TextView serverStatusTV;
    TextView onDutyTV, offDutyTV;
    TextView locationTV;
    SharedPreferences preferences;
    UnifiedOrderAdapter orderAdapter;
    RecyclerView ordersListRecycleView;
    private List<OBSOrderModel> orderItemList;
    DecimalFormat coordinateFormat = new DecimalFormat("#.##########");

    String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.POST_NOTIFICATIONS};

    public AllOrdersFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
    }


    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        com.vishnu.quickgodelivery.databinding.FragmentAllOrdersBinding binding = com.vishnu.quickgodelivery.
                databinding.FragmentAllOrdersBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        bind = binding;

        serverStatusTV = binding.serverStatusFeedbackOrdersTextView;
        locationTV = binding.locationViewTextView;
        ordersListRecycleView = binding.allOrdersRecycleView;
        onDutyTV = root.findViewById(R.id.onDutyView_textView);
        offDutyTV = root.findViewById(R.id.offDutyView_textView);

        // OnCreate permission requests
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), permissionsToRequest.toArray(new String[0]), 1);
        }

        // Init. audio download directory
        appExternalDirectory = Environment.getExternalStoragePublicDirectory("Android/vishnu/" + requireContext().getPackageName() + "/files");
        AppDwnAudioDir = new File(appExternalDirectory, "audio_dir");
        Log.d(LOG_TAG, "appExternalDirectory: " + appExternalDirectory);

        /* updates DUTY-STATUS TV */
        if (preferences.getString("dutyStatus", "0").equals("onDuty")) {
            showOnDutyView();
            locationTV.setVisibility(View.VISIBLE);
        } else if (preferences.getString("dutyStatus", "0").equals("offDuty")) {
            showOffDutyView();

            locationTV.setVisibility(View.GONE);
        } else {
            locationTV.setVisibility(View.GONE);
        }

        if (getUserId() != null) {
            syncAllOrdersRecycleView(root);
        }

        return root;
    }

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            if (LocationService.ACTION_LOCATION_BROADCAST.equals(intent.getAction())) {
                double latitude = intent.getDoubleExtra(LocationService.EXTRA_LATITUDE, 0.00);
                double longitude = intent.getDoubleExtra(LocationService.EXTRA_LONGITUDE, 0.00);

                locationTV.setText(MessageFormat.format("{0}°N\n{1}°E",
                        coordinateFormat.format(latitude), coordinateFormat.format(longitude)));
            }
        }
    };

    @Nullable
    private String getUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            return user.getUid();
        } else {
            return null;
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private void syncAllOrdersRecycleView(@NonNull View root) {
        List<AllOrdersModel> orderList = new ArrayList<>();
        orderAdapter = new UnifiedOrderAdapter((ViewGroup) root, requireContext(), preferences, orderList);

        RecyclerView ordersRecyclerView = root.findViewById(R.id.allOrders_recycleView);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ordersRecyclerView.setAdapter(orderAdapter);

        ProgressBar loadingProgressBar = root.findViewById(R.id.loadingProgressBar);
        TextView serverStatusTV = root.findViewById(R.id.serverStatusFeedbackOrders_textView);
        loadingProgressBar.setVisibility(View.VISIBLE);

        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        CollectionReference ordersRef = FirebaseFirestore.getInstance()
                .collection("DeliveryPartners").document(userId)
                .collection("pendingOrders");

        ordersRef.addSnapshotListener((value, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                loadingProgressBar.setVisibility(View.GONE);
                return;
            }

            if (value != null && !value.isEmpty()) {
                Log.d(TAG, "pendingOrders snapshot updated: " + value.size() + " documents.");
                serverStatusTV.setText("");

                Map<String, AllOrdersModel> tempOrderMap = new HashMap<>();

                for (QueryDocumentSnapshot doc : value) {
                    if (doc.exists()) {
                        String orderType = doc.getString("order_type");
                        DocumentReference dataRef = doc.getDocumentReference("order_data_payload_reference");

                        if (dataRef != null) {
                            // Add a snapshot listener for field changes in each document
                            dataRef.addSnapshotListener((additionalDataDoc, exception) -> {
                                if (exception != null) {
                                    Log.e(LOG_TAG, "Error fetching order data", exception);
                                    return;
                                }

                                if (additionalDataDoc != null && additionalDataDoc.exists()) {
                                    AllOrdersModel allOrdersModel = new AllOrdersModel();
                                    allOrdersModel.setOrderType(orderType);

                                    Long orderTimeMillis = doc.getLong("order_time_millis");

                                    if ("obs".equals(orderType)) {
                                        OBSOrderModel orderData = additionalDataDoc.toObject(OBSOrderModel.class);
                                        allOrdersModel.setObsOrderData(orderData);
                                    } else if ("obv".equals(orderType)) {
                                        OBVOrderModel orderData = additionalDataDoc.toObject(OBVOrderModel.class);
                                        allOrdersModel.setObvOrderData(orderData);
                                        if (orderTimeMillis != null) {
                                            allOrdersModel.setOrderTimeMillis(orderTimeMillis);
                                        } else {
                                            Log.w(LOG_TAG, "order_time_millis is null for order: " + doc.getId());
                                            allOrdersModel.setOrderTimeMillis(0);
                                        }
                                    }
                                    tempOrderMap.put(doc.getId(), allOrdersModel);

                                    // Update the orderList and notify adapter of the change
                                    orderList.clear();
                                    orderList.addAll(tempOrderMap.values());
                                    orderList.sort(Comparator.comparingLong(AllOrdersModel::getOrderTimeMillis).reversed());

                                    // Ensure UI update on the main thread
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        orderAdapter.notifyDataSetChanged();
                                        loadingProgressBar.setVisibility(View.GONE);
                                    });
                                } else {
                                    Log.w(LOG_TAG, "No data found for order: " + doc.getId());
                                }
                            });
                        }
                    }
                }
            } else {
                Log.d(TAG, "No pending orders found.");
                orderList.clear();
                preferences.edit().putString("currentDeliveryOrderID", "0").apply();
                new Handler(Looper.getMainLooper()).post(() -> {
                    orderAdapter.notifyDataSetChanged();
                    serverStatusTV.setText(R.string.no_orders_on_queue);
                    startTVBlink(serverStatusTV);
                    loadingProgressBar.setVisibility(View.GONE);
                });
            }
        });
    }


    private void startTVBlink(@NonNull TextView tv) {
        Animation blinkAnimation = new AlphaAnimation(1, 0);
        blinkAnimation.setDuration(450);
        blinkAnimation.setRepeatMode(Animation.REVERSE);
        blinkAnimation.setRepeatCount(Animation.INFINITE);

        // Set the animation to the TextView
        tv.startAnimation(blinkAnimation);
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
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onStart() {
        super.onStart();

        if (orderAdapter != null) {
            orderAdapter.notifyDataSetChanged();
        }

        IntentFilter filter = new IntentFilter(LocationService.ACTION_LOCATION_BROADCAST);
        requireContext().registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            requireContext().unregisterReceiver(locationReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(LOG_TAG, "Receiver not registered", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


}
