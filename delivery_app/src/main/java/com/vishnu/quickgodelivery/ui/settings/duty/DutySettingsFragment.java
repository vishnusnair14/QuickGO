package com.vishnu.quickgodelivery.ui.settings.duty;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.vishnu.quickgodelivery.R;
import com.vishnu.quickgodelivery.databinding.FragmentDutySettingsBinding;


public class DutySettingsFragment extends Fragment {

    FragmentDutySettingsBinding binding;

    public DutySettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentDutySettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        return root;
    }
}