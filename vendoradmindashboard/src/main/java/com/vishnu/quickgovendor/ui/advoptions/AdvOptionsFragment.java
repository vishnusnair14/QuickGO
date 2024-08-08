package com.vishnu.quickgovendor.ui.advoptions;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vishnu.quickgovendor.R;
import com.vishnu.quickgovendor.databinding.FragmentAdvOptionsBinding;


import java.util.ArrayList;
import java.util.List;

public class AdvOptionsFragment extends Fragment {
    private FirebaseAuth mAuth;
    String[] permissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        com.vishnu.quickgovendor.databinding.FragmentAdvOptionsBinding binding = FragmentAdvOptionsBinding.inflate(inflater, container, false);

        Button deleteAccountBtn = binding.deleteAdvOptnButton;
        Button sendMsg = binding.logoutAdvOptnButton;

        // OnCreate permission request
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), permissionsToRequest.toArray(new String[0]), 1);
        }

        // action listeners
        deleteAccountBtn.setOnClickListener(v -> showDelAccDialog());

        View root = binding.getRoot();
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {

        }
    }

    private void showDelAccDialog() {
        AlertDialog.Builder delAccountBuilder = new AlertDialog.Builder(requireContext());

        delAccountBuilder.setView(R.layout.acc_delete_dialog);
        delAccountBuilder.setPositiveButton("Yes", (dialog, which) -> deleteAccountPermanently());
        delAccountBuilder.setNegativeButton("No", (dialog, which) -> Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_SHORT).show());

        AlertDialog dialog = delAccountBuilder.create();
        dialog.show();
    }

    private void deleteAccountPermanently() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseAuth.getInstance().signOut();
                    Toast.makeText(requireContext(), "Your account has been deleted permanently", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Unable to delete your account at the moment!", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(requireContext(), "Null ref. at user instance!", Toast.LENGTH_SHORT).show();

        }
    }
}
