package com.vishnu.quickgoorder.ui.authentication;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.databinding.FragmentResetPswdBinding;

import java.util.Objects;

public class ResetPswdFragment extends Fragment {
    FragmentResetPswdBinding binding;
    Context context;
    Activity activity;
    //    SharedPreferences preferences;
    FirebaseAuth mAuth;

    public ResetPswdFragment(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
//        preferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentResetPswdBinding.inflate(inflater, container, false);
        View root = binding.getRoot();


        binding.resetPasswordPswdResetFragmentButton.setOnClickListener(v -> {
            if (!Objects.equals(binding.emailFieldPswdResetFragmentEditTextText.getText().toString(), "")) {
                sentPswdResetLink(binding.emailFieldPswdResetFragmentEditTextText.getText().toString());
            } else {
                binding.statusViewPswdResetFragmentEditTextText.setText(R.string.email_field_can_t_be_empty);
            }
        });

        return root;
    }

    private void sentPswdResetLink(String em) {
        mAuth.sendPasswordResetEmail(em)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(context, "Password reset email sent successful", Toast.LENGTH_SHORT).show();
                        binding.statusViewPswdResetFragmentEditTextText.setText(R.string.reset_email_sent);
                        binding.emailFieldPswdResetFragmentEditTextText.setText(null);
                        new Handler().postDelayed(() -> {
                            binding.statusViewPswdResetFragmentEditTextText.setText(null);
                        }, 5000);
                    } else {
                        Toast.makeText(context, "Unable to sent password reset email!", Toast.LENGTH_SHORT).show();
                        binding.emailFieldPswdResetFragmentEditTextText.setText(null);
                        binding.statusViewPswdResetFragmentEditTextText.setText(R.string.unable_to_sent_reset_mail);
                    }
                });
    }
}