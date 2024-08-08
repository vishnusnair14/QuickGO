package com.vishnu.quickgodelivery.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

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

        SwitchPreferenceCompat useTestServer_button = findPreference("use_test_server");

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