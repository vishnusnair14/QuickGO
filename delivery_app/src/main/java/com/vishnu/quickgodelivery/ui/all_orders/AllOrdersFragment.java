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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
import java.util.List;
import java.util.Objects;

public class AllOrdersFragment extends Fragment {
    com.vishnu.quickgodelivery.databinding.FragmentAllOrdersBinding bind;
    private final String LOG_TAG = "OrdersFragment";
    File AppDwnAudioDir, appExternalDirectory;
    TextView serverStatusTV;
    TextView onDutyTV, offDutyTV;
    TextView locationTV;
    SharedPreferences preferences;
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

        com.vishnu.quickgodelivery.databinding.FragmentAllOrdersBinding binding = com.vishnu.quickgodelivery.databinding.FragmentAllOrdersBinding.inflate(inflater, container, false);
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
// TODO
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
        public void onReceive(Context context, Intent intent) {
            if (LocationService.ACTION_LOCATION_BROADCAST.equals(intent.getAction())) {
                double latitude = intent.getDoubleExtra(LocationService.EXTRA_LATITUDE, 0.00);
                double longitude = intent.getDoubleExtra(LocationService.EXTRA_LONGITUDE, 0.00);

                locationTV.setText(MessageFormat.format("{0}°N\n{1}°E",
                        coordinateFormat.format(latitude), coordinateFormat.format(longitude)));
            }
        }
    };

    private String getUserId() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            return user.getUid();
        } else {
            return null;
        }
    }


//    @SuppressLint("NotifyDataSetChanged")
//    private void syncAllOrdersRecycleView(View root) {
//        orderItemList = new ArrayList<>();
//        OBSOrdersAdapter = new OBSOrdersAdapter(this, (ViewGroup) root,
//                getContext(), preferences, orderItemList);
//
//        ordersListRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));
//        ordersListRecycleView.setAdapter(OBSOrdersAdapter);
//
//        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
//        CollectionReference ordersRef = FirebaseFirestore.getInstance()
//                .collection("DeliveryPartners").document(userId)
//                .collection("pendingOrders");
//
//        ordersRef.addSnapshotListener((value, e) -> {
//            if (e != null) {
//                Log.w(TAG, "Listen failed.", e);
//                return;
//            }
//
//            if (value != null && !value.isEmpty()) {
//                serverStatusTV.setText("");
//                orderItemList.clear();
//
//                for (QueryDocumentSnapshot doc : value) {
//                    if (doc.exists()) {
//                        Log.d(LOG_TAG, "All orders data: " + doc.getData());
//                        BaseOrderModel order = doc.toObject(BaseOrderModel.class);
//
//                        String orderType = doc.getString("order_type");
//                        DocumentReference dataRef;
//
//                        dataRef = doc.getDocumentReference("order_data_payload_reference");
//
//                        if (orderType != null && orderType.equals("obs")) {
//
//                            if (dataRef != null) {
//                                dataRef.get().addOnSuccessListener(additionalDataDoc -> {
//                                    if (additionalDataDoc.exists()) {
//                                        Log.d(LOG_TAG, "Additional data: " + additionalDataDoc.getData());
//                                        OBSOrderModel orderData = additionalDataDoc.toObject(OBSOrderModel.class);
//                                        orderItemList.add(orderData);
//                                        OBSOrdersAdapter.notifyDataSetChanged();
//                                    } else {
//                                        Log.w(LOG_TAG, "No additional data found for order: " + doc.getId());
//                                    }
//                                    OBSOrdersAdapter.notifyDataSetChanged();
//                                }).addOnFailureListener(exception -> {
//                                    Log.e(LOG_TAG, "Error fetching additional data", exception);
//                                });
//                            } else {
////                            orderItemList.add(order);
//                                OBSOrdersAdapter.notifyDataSetChanged();
//                                Log.e(LOG_TAG, "dataRef is null");
//                            }
//                        } else {
//                            Toast.makeText(requireContext(), "It is an OBV type order", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                }
//            } else {
//                orderItemList.clear();
//                preferences.edit().putString("currentDeliveryOrderID", "0").apply();
//                OBSOrdersAdapter.notifyDataSetChanged();
//                serverStatusTV.setText(R.string.no_orders_on_queue);
//
//                startTVBlink(serverStatusTV);
//            }
//        });
//    }

//    @SuppressLint("NotifyDataSetChanged")
//    private void syncAllOrdersRecycleView(View root) {
//        orderItemList = new ArrayList<>();
//        OBSOrderAdapter = new OBSOrderAdapter(this, (ViewGroup) root, getContext(),
//                preferences, orderItemList);
//
//        RecyclerView ordersListRecycleView = root.findViewById(R.id.allOrdersListView_recycleView);
//        ordersListRecycleView.setLayoutManager(new LinearLayoutManager(getContext()));
//        ordersListRecycleView.setAdapter(OBSOrderAdapter);
//
//        ProgressBar loadingProgressBar = root.findViewById(R.id.loadingProgressBar);
//        TextView serverStatusTV = root.findViewById(R.id.serverStatusFeedbackOrders_textView);
//        loadingProgressBar.setVisibility(View.VISIBLE); // Show loading indicator
//
//        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
//        CollectionReference ordersRef = FirebaseFirestore.getInstance()
//                .collection("DeliveryPartners").document(userId)
//                .collection("pendingOrders");
//
//        ordersRef.addSnapshotListener((value, e) -> {
//            if (e != null) {
//                Log.w(TAG, "Listen failed.", e);
//                loadingProgressBar.setVisibility(View.GONE); // Hide loading indicator on error
//                return;
//            }
//
//            if (value != null && !value.isEmpty()) {
//                serverStatusTV.setText("");
//                orderItemList.clear();
//
//                List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
//
//                for (QueryDocumentSnapshot doc : value) {
//                    if (doc.exists()) {
//                        Log.d(LOG_TAG, "All orders data: " + doc.getData());
//                        String orderType = doc.getString("order_type");
//                        DocumentReference dataRef = doc.getDocumentReference("order_data_payload_reference");
//
//                        if ("obs".equals(orderType) && dataRef != null) {
//                            Task<DocumentSnapshot> task = dataRef.get();
//                            tasks.add(task);
//                            task.addOnSuccessListener(additionalDataDoc -> {
//                                if (additionalDataDoc.exists()) {
//                                    Log.d(LOG_TAG, "OBS data: " + additionalDataDoc.getData());
//                                    OBSOrderModel orderData = additionalDataDoc.toObject(OBSOrderModel.class);
//                                    orderItemList.add(orderData);
//                                } else {
//                                    Log.w(LOG_TAG, "No OBS data found for order: " + doc.getId());
//                                }
//                            }).addOnFailureListener(exception -> {
//                                Log.e(LOG_TAG, "Error fetching OBS data", exception);
//                            });
//                        } else if ("obv".equals(orderType) && dataRef != null) {
//                            Task<DocumentSnapshot> task = dataRef.get();
//                            tasks.add(task);
//                            task.addOnSuccessListener(additionalDataDoc -> {
//                                if (additionalDataDoc.exists()) {
//                                    Log.d(LOG_TAG, "OBV data: " + additionalDataDoc.getData());
//                                    OBVOrderModel orderData = additionalDataDoc.toObject(OBVOrderModel.class);
//                                    orderItemList.add(orderData);
//                                } else {
//                                    Log.w(LOG_TAG, "No OBV data found for order: " + doc.getId());
//                                }
//                            }).addOnFailureListener(exception -> {
//                                Log.e(LOG_TAG, "Error fetching OBV data", exception);
//                            });
//                        }
//                    }
//                }
//
//                Tasks.whenAllComplete(tasks).addOnCompleteListener(task -> {
//                    OBSOrderAdapter.notifyDataSetChanged();
//                    loadingProgressBar.setVisibility(View.GONE); // Hide loading indicator after data is loaded
//                });
//            } else {
//                orderItemList.clear();
//                preferences.edit().putString("currentDeliveryOrderID", "0").apply();
//                OBSOrderAdapter.notifyDataSetChanged();
//                serverStatusTV.setText(R.string.no_orders_on_queue);
//
//                startTVBlink(serverStatusTV);
//                loadingProgressBar.setVisibility(View.GONE); // Hide loading indicator if no data
//            }
//        });
//    }

    @SuppressLint("NotifyDataSetChanged")
    private void syncAllOrdersRecycleView(View root) {
        List<AllOrdersModel> orderList = new ArrayList<>();
        UnifiedOrderAdapter orderAdapter = new UnifiedOrderAdapter((ViewGroup) root, requireContext(), preferences, orderList);

        RecyclerView ordersRecyclerView = root.findViewById(R.id.allOrders_recycleView);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ordersRecyclerView.setAdapter(orderAdapter);

        ProgressBar loadingProgressBar = root.findViewById(R.id.loadingProgressBar);
        TextView serverStatusTV = root.findViewById(R.id.serverStatusFeedbackOrders_textView);
        loadingProgressBar.setVisibility(View.VISIBLE); // Show loading indicator

        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        CollectionReference ordersRef = FirebaseFirestore.getInstance()
                .collection("DeliveryPartners").document(userId)
                .collection("pendingOrders");

        ordersRef.addSnapshotListener((value, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                loadingProgressBar.setVisibility(View.GONE); // Hide loading indicator on error
                return;
            }

            if (value != null && !value.isEmpty()) {
                serverStatusTV.setText("");
                orderList.clear();

                List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

                for (QueryDocumentSnapshot doc : value) {
                    if (doc.exists()) {
                        Log.d(LOG_TAG, "All orders data: " + doc.getData());
                        String orderType = doc.getString("order_type");
                        DocumentReference dataRef = doc.getDocumentReference("order_data_payload_reference");

                        if (dataRef != null) {
                            Task<DocumentSnapshot> task = dataRef.get();
                            tasks.add(task);
                            task.addOnSuccessListener(additionalDataDoc -> {
                                if (additionalDataDoc.exists()) {
                                    AllOrdersModel allOrdersModel = new AllOrdersModel();
                                    allOrdersModel.setOrderType(orderType);
                                    if ("obs".equals(orderType)) {
                                        OBSOrderModel orderData = additionalDataDoc.toObject(OBSOrderModel.class);
                                        allOrdersModel.setObsOrderData(orderData);
                                    } else if ("obv".equals(orderType)) {
                                        OBVOrderModel orderData = additionalDataDoc.toObject(OBVOrderModel.class);
                                        allOrdersModel.setObvOrderData(orderData);
                                    }
                                    orderList.add(allOrdersModel);
                                } else {
                                    Log.w(LOG_TAG, "No data found for order: " + doc.getId());
                                }
                            }).addOnFailureListener(exception -> {
                                Log.e(LOG_TAG, "Error fetching order data", exception);
                            });
                        }
                    }
                }

                Tasks.whenAllComplete(tasks).addOnCompleteListener(task -> {
                    orderAdapter.notifyDataSetChanged();
                    loadingProgressBar.setVisibility(View.GONE); // Hide loading indicator after data is loaded
                });
            } else {
                orderList.clear();
                preferences.edit().putString("currentDeliveryOrderID", "0").apply();
                orderAdapter.notifyDataSetChanged();
                serverStatusTV.setText(R.string.no_orders_on_queue);

                startTVBlink(serverStatusTV);
                loadingProgressBar.setVisibility(View.GONE); // Hide loading indicator if no data
            }
        });
    }


    private void startTVBlink(TextView tv) {
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
//        if (allOrdersAdapter != null) {
//            allOrdersAdapter.notifyDataSetChanged();
//        }
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
