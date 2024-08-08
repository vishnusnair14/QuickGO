package com.vishnu.quickgovendor.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vishnu.quickgovendor.R;
import com.vishnu.quickgovendor.databinding.FragmentHomeBinding;


public class HomeFragment extends Fragment {
    private final String LOG_TAG = this.getClass().getSimpleName();
    private FirebaseAuth mAuth;
    TextView vendorIDTV, vendorNameTV;
    Button addNewProductBtn;
    Button editProductBtn;
    Button editShopDataBtn;
    Button dbAggViewBtn;
    Button logoutBtn;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        com.vishnu.quickgovendor.databinding.FragmentHomeBinding binding = FragmentHomeBinding.inflate(inflater, container, false);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        assert user != null;
        Log.i(LOG_TAG, user.getUid());

        vendorIDTV = binding.vendorIDTextView1;
        vendorNameTV = binding.vendorNameTextView;
        addNewProductBtn = binding.addNewProductRedirectButton;
        editProductBtn = binding.editProductRedirectButton;
        editShopDataBtn = binding.editShopDataRedirectButton;
        dbAggViewBtn = binding.dbAggViewButton;
        logoutBtn = binding.logoutVendorButton;

        vendorIDTV.setText(user.getUid());
        vendorNameTV.setText(user.getEmail());

        addNewProductBtn.setOnClickListener(v -> NavHostFragment.findNavController(requireParentFragment()).navigate(R.id.action_nav_home_to_addProductFragment));
        editProductBtn.setOnClickListener(v -> NavHostFragment.findNavController(requireParentFragment()).navigate(R.id.action_nav_home_to_editProductFragment));
        editShopDataBtn.setOnClickListener(v -> NavHostFragment.findNavController(requireParentFragment()).navigate(R.id.action_nav_home_to_editShopDataFragment));

        logoutBtn.setOnClickListener(v -> {
            try {
                mAuth.signOut();
                if (mAuth.getCurrentUser() == null) {
                    Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
                    editProductBtn.setEnabled(false);
                    addNewProductBtn.setEnabled(false);
                    editShopDataBtn.setEnabled(false);
                    dbAggViewBtn.setEnabled(false);

                    vendorIDTV.setTextSize(11);
                    vendorIDTV.setText(R.string.user_logged_out);
                    vendorNameTV.setTextSize(11);
                    vendorNameTV.setText(R.string.not_found);
                    new Handler().postDelayed(() -> requireActivity().getOnBackPressedDispatcher().onBackPressed(), 1500);
                } else {
                    Toast.makeText(requireContext(), "Sign-out not performed correctly!", Toast.LENGTH_SHORT).show();
                    editProductBtn.setEnabled(true);
                    addNewProductBtn.setEnabled(true);
                    editShopDataBtn.setEnabled(true);
                    dbAggViewBtn.setEnabled(true);
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error signing out,try again!", Toast.LENGTH_SHORT).show();
                editProductBtn.setEnabled(true);
                addNewProductBtn.setEnabled(true);
                editShopDataBtn.setEnabled(true);
                dbAggViewBtn.setEnabled(true);
            }
        });

        View root = binding.getRoot();

        return root;
    }
}

