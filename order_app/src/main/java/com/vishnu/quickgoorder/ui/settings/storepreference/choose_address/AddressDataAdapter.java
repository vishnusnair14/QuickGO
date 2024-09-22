package com.vishnu.quickgoorder.ui.settings.storepreference.choose_address;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.JsonObject;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.callbacks.StorePref;
import com.vishnu.quickgoorder.crypto.DESCore;
import com.vishnu.quickgoorder.miscellaneous.PreferenceKeys;
import com.vishnu.quickgoorder.server.sapi.APIService;
import com.vishnu.quickgoorder.server.sapi.ApiServiceGenerator;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddressDataAdapter extends RecyclerView.Adapter<AddressDataAdapter.AddressViewHolder> {
    private final String LOG_TAG = "AddressDataAdapter";
    private final List<AddressData> addressList;
    private final Context context;
    private final String user;
    private SharedPreferences preferences;

    public AddressDataAdapter(List<AddressData> addressList, Context context, String user, SharedPreferences preferences) {
        this.addressList = addressList;
        this.context = context;
        this.user = user;
        this.preferences = preferences;
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.srv_store_pref_address_list, parent, false);
        return new AddressViewHolder(context, user, itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        AddressData address = addressList.get(position);
        holder.bind(address, preferences);
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    public static class AddressViewHolder extends RecyclerView.ViewHolder {
        private final String LOG_TAG = "AddressDataAdapter";
        private final TextView addressNameTV;
        private final TextView fullAddressTV;
        private final ImageView addressTypeIV;
        private final Context context;
        private final String userID;
        private final TextView phnoTV;
        private final TextView locCoordsTV;
        private final TextView defaultAddrViewTV;

        public AddressViewHolder(Context context, String userID, @NonNull View itemView) {
            super(itemView);
            this.context = context;
            this.userID = userID;

            addressNameTV = itemView.findViewById(R.id.srvSelectAddrForStorePrefAddrName_textView);
            fullAddressTV = itemView.findViewById(R.id.srvselectAddrForStorePrefFullAddressView_textView);
            addressTypeIV = itemView.findViewById(R.id.addressTypeIconView_imageView);
            phnoTV = itemView.findViewById(R.id.addressIDView_textView);
            locCoordsTV = itemView.findViewById(R.id.srvselectAddrForStorePrefAddressLocCordView_textView);
            defaultAddrViewTV = itemView.findViewById(R.id.srv_store_pref_default_addr_view_textView);
        }

        public void bind(@NonNull AddressData address, SharedPreferences preferences) {
            addressNameTV.setText(address.getName());
            fullAddressTV.setText(address.getFullAddress());
            phnoTV.setText(address.getPhoneNo());
            locCoordsTV.setText(MessageFormat.format("{0}°N, {1}°E", address.getAddressLat(), address.getAddressLon()));

            if (address.getPhoneNo().equals(preferences.getString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_KEY, "0"))) {
                defaultAddrViewTV.setVisibility(View.VISIBLE);
            } else {
                defaultAddrViewTV.setVisibility(View.GONE);
            }

            if (address.getAddressType().trim().equals("Home")) {
                addressTypeIV.setImageResource(R.drawable.baseline_home_24);
            } else if (address.getAddressType().trim().equals("Work")) {
                addressTypeIV.setImageResource(R.drawable.baseline_business_24);
            } else if (address.getAddressType().trim().equals("Other")) {
                addressTypeIV.setImageResource(R.drawable.baseline_pending_24);
            } else {
                addressTypeIV.setImageResource(R.drawable.baseline_pending_24);
            }

            // Handle item tap
            itemView.setOnClickListener(v -> {
                try {
                    showCheckStorePrefDataExistenceBtmView(address);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        private void showCheckStorePrefDataExistenceBtmView(@NonNull AddressData addressData) {
            View savedStorePrefView = LayoutInflater.from(context).inflate(
                    R.layout.bottomview_store_pref_data_existence, null, false);

            BottomSheetDialog savedStorePrefBtmView = new BottomSheetDialog(context);
            savedStorePrefBtmView.setContentView(savedStorePrefView);
            savedStorePrefBtmView.setCanceledOnTouchOutside(false);
            Objects.requireNonNull(savedStorePrefBtmView.getWindow()).setGravity(Gravity.TOP);


            TextView actionBtn = savedStorePrefView.findViewById(R.id.btmviewStorePrefDataFoundOverwriteBtn_button);
            ProgressBar progressBar = savedStorePrefView.findViewById(R.id.btmviewStorePrefDataLoadingView_progressBar);
            TextView cancelBtn = savedStorePrefView.findViewById(R.id.btmviewStorePrefDataFoundCancelBtn_button);
            TextView mainTitle = savedStorePrefView.findViewById(R.id.btmviewStorePrefDataHeading_button);
            TextView subTitle = savedStorePrefView.findViewById(R.id.btmviewStorePrefDataSubTitle_button);

            cancelBtn.setOnClickListener(v -> {
                savedStorePrefBtmView.hide();
                savedStorePrefBtmView.dismiss();
            });

            actionBtn.setOnClickListener(v -> {
                savedStorePrefBtmView.hide();
                savedStorePrefBtmView.dismiss();

                overwriteStorePrefData(addressData);
            });

            cancelBtn.setVisibility(View.INVISIBLE);
            actionBtn.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);

            mainTitle.setText(R.string.store_preference);
            subTitle.setText(MessageFormat.format("Please wait while we check for any " +
                    "existing store preference data for {0}...", addressData.getPhoneNo()));

            savedStorePrefBtmView.show();

            new Handler().postDelayed(() -> {
                try {
                    checkIsPrefSavedForAddress(addressData.getPhoneNo(), hasData -> {
                        if (hasData == 1) {
                            cancelBtn.setVisibility(View.VISIBLE);
                            actionBtn.setVisibility(View.VISIBLE);

                            mainTitle.setText(R.string.data_already_exists);
                            subTitle.setText(R.string.store_preference_data_already_exists_for_this_phone);

                            actionBtn.setText(R.string.overwrite);
                            cancelBtn.setText(R.string.erase);

                            savedStorePrefBtmView.setCanceledOnTouchOutside(true);

                            cancelBtn.setOnClickListener(v -> {
                                try {
                                    eraseStorePreferenceData(addressData.getPhoneNo(), savedStorePrefBtmView);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });

                        } else if (hasData == 0) {
                            cancelBtn.setVisibility(View.VISIBLE);
                            actionBtn.setVisibility(View.VISIBLE);

                            mainTitle.setText(R.string.store_preference);
                            subTitle.setText(R.string.no_store_preference_data_exists_for_this_phone);

                            actionBtn.setText(R.string.add_new);

                        } else if (hasData == -1) {
                            actionBtn.setText(R.string.try_again);

                            mainTitle.setText(R.string.server_offline);
                            subTitle.setText(R.string.unable_to_check_data_at_the_moment_the_server_);

                            cancelBtn.setVisibility(View.VISIBLE);
                            actionBtn.setVisibility(View.VISIBLE);

                            actionBtn.setOnClickListener(v -> {
                                savedStorePrefBtmView.dismiss();
                                showCheckStorePrefDataExistenceBtmView(addressData);
                            });
                        }
                        progressBar.setVisibility(View.INVISIBLE);
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, 450);
        }

        private void overwriteStorePrefData(@NonNull AddressData addressData) {
            Bundle args = new Bundle();
            args.putDouble("addr_lat", addressData.getAddressLat());
            args.putDouble("addr_lon", addressData.getAddressLon());
            args.putString("addr_state", addressData.getState());
            args.putString("addr_district", addressData.getDistrict());
            args.putString("addr_pincode", addressData.getPincode());
            args.putString("addr_phone", addressData.getPhoneNo());

            Navigation.findNavController(itemView).navigate(
                    R.id.action_nav_store_pref_address_to_nav_store_pref_store, args);

            Log.d(LOG_TAG, "redirecting to find nearby shops for address, "
                    + addressData.getAddressLat() + "°N " + addressData.getAddressLon() + "°E");
        }


        private void checkIsPrefSavedForAddress(String phno, StorePref storePref) throws Exception {
            APIService apiService = ApiServiceGenerator.getApiService(context);
            Call<JsonObject> call0455 = apiService.fetchStorePrefData(userID, DESCore.encrypt(phno));

            call0455.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject responseBody = response.body();

                        boolean hasDataSaved;
                        if (responseBody.has("has_data")) {
                            hasDataSaved = responseBody.get("has_data").getAsBoolean();

                            if (hasDataSaved) {
                                storePref.isDataFound(1);

                                if (responseBody.has("data")) {
                                    JsonObject storePefData = responseBody.get("data").getAsJsonObject();
                                }

                            } else {
                                storePref.isDataFound(0);
//                                Toast.makeText(context, responseBody.get("message").getAsString(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            storePref.isDataFound(-1);
                            Toast.makeText(context, responseBody.get("message").getAsString(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        storePref.isDataFound(-1);
                        Log.e(LOG_TAG, "Failed to fetch address data" + response.message());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    storePref.isDataFound(-1);
                    Log.e(LOG_TAG, "Failed to fetch address data", t);
                }
            });
        }

        private void eraseStorePreferenceData(String phno, BottomSheetDialog bottomSheetDialog) throws Exception {
            APIService apiService = ApiServiceGenerator.getApiService(context);
            Call<JsonObject> call0455 = apiService.deleteStorePreferenceData(userID, DESCore.encrypt(phno));

            call0455.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject responseBody = response.body();

                        if (responseBody.has("is_deleted")) {
                            boolean isDeleted = responseBody.get("is_deleted").getAsBoolean();
                            if (isDeleted) {
                                if (bottomSheetDialog != null) {
                                    bottomSheetDialog.dismiss();
                                    bottomSheetDialog.hide();
                                    Toast.makeText(context, "Data erased successfully.", Toast.LENGTH_SHORT).show();
                                }
                            }

                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    Log.e(LOG_TAG, "Failed to fetch address data", t);
                }
            });


        }
    }
}
