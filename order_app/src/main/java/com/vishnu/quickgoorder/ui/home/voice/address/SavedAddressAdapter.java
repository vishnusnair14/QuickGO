package com.vishnu.quickgoorder.ui.home.voice.address;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.databinding.FragmentHomeOrderByVoiceBinding;
import com.vishnu.quickgoorder.miscellaneous.PreferenceKeys;

import java.text.MessageFormat;
import java.util.List;

public class SavedAddressAdapter extends RecyclerView.Adapter<SavedAddressAdapter.AddressViewHolder> {

    private final String LOG_TAG = "SavedAddressDataAdapter";
    private final SharedPreferences preferences;
    FragmentHomeOrderByVoiceBinding binding;
    private final List<SavedAddressModel> savedAddressModelList;
    private final BottomSheetDialog setDeliveryAddrBtmView;

    public SavedAddressAdapter(FragmentHomeOrderByVoiceBinding binding,
                               SharedPreferences preferences,
                               List<SavedAddressModel> savedAddressModelList,
                               BottomSheetDialog setDeliveryAddrBtmView) {
        this.binding = binding;
        this.savedAddressModelList = savedAddressModelList;
        this.preferences = preferences;
        this.setDeliveryAddrBtmView = setDeliveryAddrBtmView;
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.srv_address_list, parent, false);
        return new AddressViewHolder(itemView, this, preferences,
                binding, setDeliveryAddrBtmView);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        SavedAddressModel address = savedAddressModelList.get(position);
        holder.bind(address, position);
    }


    @Override
    public int getItemCount() {
        return savedAddressModelList.size();
    }

    public static class AddressViewHolder extends RecyclerView.ViewHolder {
        private final String LOG_TAG = "AddressDataAdapter";
        private final TextView fullAddressTV;
        private final TextView addressLocationTV;
        private final TextView addressCityPincodeTV;
        private final CardView addressCardView;
        private final TextView addressNameViewTV;
        private final ImageView addrTypeIconView;
        private final ConstraintLayout addrViewLayout;
        private SavedAddressAdapter adapter;
        private FirebaseUser user;
        private final SharedPreferences preferences;
        FragmentHomeOrderByVoiceBinding binding;
        private final BottomSheetDialog setDeliveryAddrBtmView;

        public AddressViewHolder(@NonNull View itemView, SavedAddressAdapter adapter,
                                 SharedPreferences preferences,
                                 FragmentHomeOrderByVoiceBinding binding, BottomSheetDialog setDeliveryAddrBtmView) {
            super(itemView);
            this.adapter = adapter;
            this.preferences = preferences;
            this.binding = binding;
            this.setDeliveryAddrBtmView = setDeliveryAddrBtmView;

            user = FirebaseAuth.getInstance().getCurrentUser();
            fullAddressTV = itemView.findViewById(R.id.srvselectAddrForStorePrefFullAddressView_textView);
            addressCardView = itemView.findViewById(R.id.setDefaultDeliveryAddress_cardView);
            addressCityPincodeTV = itemView.findViewById(R.id.addressIDView_textView);
            addressLocationTV = itemView.findViewById(R.id.srvselectAddrForStorePrefAddressLocCordView_textView);
            addrTypeIconView = itemView.findViewById(R.id.addressTypeIconView_imageView);
            addrViewLayout = itemView.findViewById(R.id.addressCardViewlayout_constraintlayout);
            addressNameViewTV = itemView.findViewById(R.id.srvDeliveryAddressNameView_textView);
        }

        public void bind(SavedAddressModel address, int pos) {
            if (address.getAddressType().trim().equals("Home")) {
                addrTypeIconView.setImageResource(R.drawable.baseline_home_24);
            } else if (address.getAddressType().trim().equals("Work")) {
                addrTypeIconView.setImageResource(R.drawable.baseline_business_24);
            } else if (address.getAddressType().trim().equals("Other")) {
                addrTypeIconView.setImageResource(R.drawable.baseline_pending_24);
            } else {
                addrTypeIconView.setImageResource(R.drawable.baseline_pending_24);
            }

            // Set the values to their respective TextViews
            fullAddressTV.setText(MessageFormat.format("{0}", address.getStreetAddress()));
            addressCityPincodeTV.setText(MessageFormat.format("{0}{1} | {2}", address.getDistrict().substring(0, 1).toUpperCase(),
                    address.getDistrict().substring(1), address.getPincode()));
            addressLocationTV.setText(MessageFormat.format("{0}°N {1}°E", address.getAddressLat(), address.getAddressLon()));
            addressNameViewTV.setText(MessageFormat.format("{0}", address.getName()));

            if (preferences.getString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_KEY, "None").equals(address.getPhoneNo())) {
                addrViewLayout.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.selectedAddressCardView));
            }

            // Set default delivery address view
            addressCardView.setOnClickListener(v -> {
                preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_KEY, address.getPhoneNo()).apply();
                preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_TYPE, address.getAddressType()).apply();
                preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_STREET_ADDRESS, address.getStreetAddress()).apply();
                preferences.edit().putString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_FULL_ADDRESS, address.getFullAddress()).apply();
//                    preferences.edit().putString("selectedAddressLat", String.valueOf(address.getAddressLat())).apply();
//                    preferences.edit().putString("selectedAddressLon", String.valueOf(address.getAddressLon())).apply();
//                    preferences.edit().putString("selectedAddressCity", address.getCity()).apply();
//                    preferences.edit().putString("selectedAddressPincode", address.getPincode()).apply();
//                preferences.edit().putBoolean("isSetToCurrentLoc", false).apply();
                binding.selectedAddressTypeViewTextView.setText(preferences.getString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_TYPE, "Select an address"));
                binding.selectedFullAddressViewTextView.setText(preferences.getString(PreferenceKeys.HOME_ORDER_BY_VOICE_SELECTED_ADDRESS_FULL_ADDRESS, "Tap on any saved address to make it as default"));

                setDeliveryAddrBtmView.hide();
                setDeliveryAddrBtmView.dismiss();
            });
        }


        public void showStorePrefActiveBtmView(Context context, Fragment fragment) {
            // Inflate the custom layout for the bottom sheet
            View view = LayoutInflater.from(context).inflate(R.layout.bottomview_store_pref_isactive, null);

            // Create the BottomSheetDialog
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
            bottomSheetDialog.setContentView(view);

            // Set the title and message
            TextView titleTextView = view.findViewById(R.id.titleTextView);
            TextView messageTextView = view.findViewById(R.id.messageTextView);
            titleTextView.setText(R.string.store_preference_is_active);
            messageTextView.setText(R.string.order_from_the_preferred);

            // Set the click listeners for the buttons
            Button goToSettingsButton = view.findViewById(R.id.goToSettingsButton);
            goToSettingsButton.setOnClickListener(v -> {
                // Create a bundle to pass the argument
                Bundle bundle = new Bundle();
                bundle.putString("highlightOption", "preferred_store");

                // Navigate to settings
                NavHostFragment.findNavController(fragment).navigate(R.id.action_nav_shop_to_nav_settings, bundle);
                bottomSheetDialog.dismiss();
            });

            Button cancelButton = view.findViewById(R.id.cancelButton);
            cancelButton.setOnClickListener(v -> {
                // Dismiss the dialog
                bottomSheetDialog.dismiss();
            });

            // Show the BottomSheetDialog
            bottomSheetDialog.show();
        }
    }
}


