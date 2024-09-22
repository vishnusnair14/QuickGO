package com.vishnu.quickgoorder.ui.settings.address.manage_address;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.miscellaneous.SoundManager;
import com.vishnu.quickgoorder.miscellaneous.Utils;
import com.vishnu.quickgoorder.server.sapi.APIService;
import com.vishnu.quickgoorder.server.sapi.ApiServiceGenerator;

import java.text.MessageFormat;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SavedAddressAdapter extends RecyclerView.Adapter<SavedAddressAdapter.AddressViewHolder> {

    private final String LOG_TAG = "SavedAddressDataAdapter";
    private final List<SavedAddressModel> savedAddressModelList;
    private final Context context;


    public SavedAddressAdapter(Context context, List<SavedAddressModel> savedAddressModelList) {
        this.context = context;
        this.savedAddressModelList = savedAddressModelList;
        SoundManager.initialize(context);
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.srv_saved_address, parent, false);
        return new AddressViewHolder(itemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        SavedAddressModel address = savedAddressModelList.get(position);
        holder.bind(address, position);
    }


    @SuppressLint("NotifyDataSetChanged")
    public void deleteAddress(String userId, String addressId, int pos) {
        APIService apiService = ApiServiceGenerator.getApiService(context);
        Call<JsonObject> call0489 = apiService.deleteAddress(userId, addressId);

        call0489.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(LOG_TAG, response.body().toString());
                    savedAddressModelList.remove(pos);
                    notifyItemRemoved(pos);
                    notifyItemRangeChanged(pos, getItemCount());
                    Utils.deleteAddressDataCacheFile(context);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Utils.vibrate(context, 0, VibrationEffect.EFFECT_TICK);
                    }

                    SoundManager.playOnDelete();

                } else {
                    Log.d(LOG_TAG, "No response");
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Log.d(LOG_TAG, "No response failed");
            }
        });
    }

    @Override
    public int getItemCount() {
        return savedAddressModelList.size();
    }

    public static class AddressViewHolder extends RecyclerView.ViewHolder {
        private final String LOG_TAG = "AddressDataAdapter";
        private final TextView phoneNoTv;
        private final TextView pincodeTV;
        private final TextView fullAddressTV;
        private final TextView deleteAddressTV;
        private final TextView locCordsTV;
        private SavedAddressAdapter adapter;
        private FirebaseUser user;

        public AddressViewHolder(@NonNull View itemView, SavedAddressAdapter adapter) {
            super(itemView);
            this.adapter = adapter;
            user = FirebaseAuth.getInstance().getCurrentUser();
            fullAddressTV = itemView.findViewById(R.id.addressView_textView);
            phoneNoTv = itemView.findViewById(R.id.phoneNoView_textView);
            pincodeTV = itemView.findViewById(R.id.pincodeView_textView);
            deleteAddressTV = itemView.findViewById(R.id.deleteAddressView_textView);
            locCordsTV = itemView.findViewById(R.id.locationCoordinatesView_textView);
        }

        public void bind(SavedAddressModel address, int pos) {
            fullAddressTV.setText(address.getFullAddress());
            phoneNoTv.setText(address.getPhoneNo());
            pincodeTV.setText(MessageFormat.format("{0}{1} | {2}",
                    address.getDistrict().substring(0, 1).toUpperCase(),
                    address.getDistrict().substring(1), address.getPincode()));
            locCordsTV.setText(MessageFormat.format("{0}°N {1}°E", address.getAddressLat(), address.getAddressLon()));

            deleteAddressTV.setOnClickListener(v ->
                    Toast.makeText(adapter.context, "Long press to delete...", Toast.LENGTH_SHORT).show());

            deleteAddressTV.setOnLongClickListener(v -> {
                adapter.deleteAddress(user.getUid(), address.getPhoneNo(), pos);
                return false;
            });

        }
    }

}


