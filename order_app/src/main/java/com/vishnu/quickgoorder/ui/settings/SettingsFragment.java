package com.vishnu.quickgoorder.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.server.sapi.ApiServiceGenerator;

public class SettingsFragment extends PreferenceFragmentCompat {
    SharedPreferences preferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_root_preferences, rootKey);
        preferences = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);

        Preference addNewAddress_button = findPreference("addNewAddress_pref");
        Preference showAllAddress_button = findPreference("showAllAddress_pref");
        Preference setStorePref_button = findPreference("setStorePreferences_pref");
        SwitchPreferenceCompat orderByShopRecommendation_button = findPreference("orderByShopRecommendation_pref");

        SwitchPreferenceCompat useTestServer_button = findPreference("use_test_server");

        if (addNewAddress_button != null) {
            addNewAddress_button.setOnPreferenceClickListener(preference -> {
                NavHostFragment.findNavController(this).navigate(R.id.action_nav_settings_to_nav_addAddress);
                return true;
            });
        }

        if (showAllAddress_button != null) {
            showAllAddress_button.setOnPreferenceClickListener(preference -> {
                NavHostFragment.findNavController(this).navigate(R.id.action_nav_settings_to_savedAddressFragment3);
                return true;
            });
        }

        if (setStorePref_button != null) {
            setStorePref_button.setOnPreferenceClickListener(preference -> {
                NavHostFragment.findNavController(this).navigate(R.id.action_nav_settings_to_nav_store_pref);
                return true;
            });
        }

        if (useTestServer_button != null) {
            useTestServer_button.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean useTestServer = (Boolean) newValue;
                PreferencesManager.setBaseUrl(requireContext(), useTestServer);
                ApiServiceGenerator.resetApiService();
                Toast.makeText(requireContext(), "Changed to " + (useTestServer ? "test" : "production") + " environment.", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        if (orderByShopRecommendation_button != null) {
            orderByShopRecommendation_button.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean orderByShopRecommendation = (Boolean) newValue;
                if (orderByShopRecommendation) {
                    preferences.edit().putBoolean("setRecommendationAsDefaultHomeView", true).apply();
                    Toast.makeText(requireContext(), "Now you can order from recommended shops.", Toast.LENGTH_LONG).show();
                } else {
                    preferences.edit().putBoolean("setRecommendationAsDefaultHomeView", false).apply();
                    Toast.makeText(requireContext(), "Now you have enabled, store preference feature.", Toast.LENGTH_LONG).show();
                }
                return true;
            });
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Retrieve the argument
        Bundle args = getArguments();
        if (args != null) {
            String highlightOption = args.getString("highlightOption");
            if ("preferred_store".equals(highlightOption)) {
                highlightPreferredStoreOption();
            }
        }
    }

    private void highlightPreferredStoreOption() {
        // Implement your logic to highlight the preferred store option
        Preference orderFromStorePreference = findPreference("orderFromStorePreference_pref");
        if (orderFromStorePreference != null) {
            orderFromStorePreference.setIcon(R.drawable.baseline_toggle_off_24);
            orderFromStorePreference.setTitle("Order from preferred shops (TURN OFF)");
            orderFromStorePreference.setSummary("When enabled, all the orders will be automatically placed to the nearby stores based on user's pre-defined preferences.");
        }
    }
}
