package com.vishnu.quickgoorder.ui.miscellaneous;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.vishnu.quickgoorder.R;

public class MiscellaneousFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.miscellaneous_root_preferences, rootKey);
    }
}

