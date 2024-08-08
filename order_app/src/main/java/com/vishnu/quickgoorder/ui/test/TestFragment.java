package com.vishnu.quickgoorder.ui.test;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.vishnu.quickgoorder.databinding.FragmentTestBinding;

public class TestFragment extends Fragment {
    FragmentTestBinding binding;

    public TestFragment() {
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

        binding = FragmentTestBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        return root;
    }
}