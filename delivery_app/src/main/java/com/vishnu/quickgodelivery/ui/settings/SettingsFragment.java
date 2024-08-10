package com.vishnu.quickgodelivery.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.vishnu.quickgodelivery.R;
import com.vishnu.quickgodelivery.server.ApiServiceGenerator;


public class SettingsFragment extends PreferenceFragmentCompat {
    SharedPreferences preferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_root_preferences, rootKey);
        preferences = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE);

        Preference manageAccountDetails_button = findPreference("manageAccountDetails_pref");
        Preference manageDutyPref_button = findPreference("manageDutyPreferences_pref");

        SwitchPreferenceCompat useTestServer_button = findPreference("use_test_server");


        if (manageAccountDetails_button != null) {
            manageAccountDetails_button.setOnPreferenceClickListener(preference -> {
                NavHostFragment.findNavController(this).navigate(R.id.action_nav_settings_to_manageAccountFragment);
                return true;
            });
        }

        if (manageDutyPref_button != null) {
            manageDutyPref_button.setOnPreferenceClickListener(preference -> {
                NavHostFragment.findNavController(this).navigate(R.id.action_nav_settings_to_dutySettingsFragment);
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

    }
}