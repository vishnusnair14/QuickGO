package com.vishnu.quickgoorder.ui.authentication.signInWithPhoneNumber;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vishnu.quickgoorder.MainActivity;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.callbacks.ActivityFinisher;
import com.vishnu.quickgoorder.databinding.FragmentLoginBinding;


public class LoginFragment extends Fragment {
    private static final String LOG_TAG = "LoginFragment";
    private final Activity activity;
    private final Context context;
    FragmentLoginBinding binding;
    EditText emailET;
    SharedPreferences preferences;
    EditText passwordET;
    Button loginBtn;
    TextView statusTV;
    Intent mainActivity;
    FirebaseAuth mAuth;
    private ActivityFinisher mListener;
    LinearLayout loginViewLayout;
    CheckBox rememberCredCB;
    private FirebaseFirestore db;
    private FirebaseUser user;

    public LoginFragment(Activity activity, Context context) {
        this.activity = activity;
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();

        preferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        emailET = binding.emailFieldLoginFragmentEditTextText;
        passwordET = binding.passwordFieldLoginFragmentEditTextText;
        loginBtn = binding.loginLoginFragmentButton;
        rememberCredCB = binding.rememberMeLoginFragmentCheckBox;
        statusTV = binding.statusViewLoginFragmentTextView;
        loginViewLayout = binding.loginViewLayoutLinearLayout;

        showCheckView(R.string.checking, R.color.checking);

        rememberCredCB.setChecked(true);

//        String username = preferences.getString("username", "");
//        String password = preferences.getString("password", "");
//
//        emailET.setTextColor(ContextCompat.getColor(requireContext(), R.color.emailET_value_restore));
//        passwordET.setTextColor(ContextCompat.getColor(requireContext(), R.color.pswdET_value_restore));
//
//        emailET.setText(username);
//        passwordET.setText(password);

        mainActivity = new Intent(activity, MainActivity.class);

        loginBtn.setOnClickListener(v -> {
            if (!emailET.getText().toString().isEmpty() && !passwordET.getText().toString().isEmpty()) {
                binding.loginStatusProgressBar.setVisibility(View.VISIBLE);
                statusTV.setText(R.string.please_wait);
                if (passwordET.getText().toString().length() >= 6) {
                    signIn(emailET.getText().toString(), passwordET.getText().toString());
                } else {
                    passwordET.setText("");
                    binding.loginStatusProgressBar.setVisibility(View.GONE);
                    statusTV.setText(R.string.length_should_be_6_or_more);
                }
            } else if (emailET.getText().toString().isEmpty() && passwordET.getText().toString().isEmpty()) {
                binding.loginStatusProgressBar.setVisibility(View.GONE);
                statusTV.setText(R.string.fields_can_t_be_empty);
            } else if (emailET.getText().toString().isEmpty()) {
                binding.loginStatusProgressBar.setVisibility(View.GONE);
                statusTV.setText(R.string.please_enter_the_email_id);
            } else if (passwordET.getText().toString().isEmpty()) {
                binding.loginStatusProgressBar.setVisibility(View.GONE);
                statusTV.setText(R.string.please_enter_the_password);
            }
        });

        init();
        return root;
    }

    private void init() {
        showCheckView(R.string.checking, R.color.checking);
        new Handler().postDelayed(() -> {
            if (!preferences.getBoolean("isRemembered", false) || user == null) {
                showLoginView();
            } else {
                showCheckView(R.string.signing_in, R.color.signing_in);
                binding.loginStatusProgressBar.setVisibility(View.GONE);
                new Handler().postDelayed(() -> {
                    startActivity(mainActivity);
                    mListener.onFinishActivity();
                }, 100);
            }
        }, 100);
    }

    private void showLoginView() {
        loginViewLayout.setVisibility(View.VISIBLE);
        binding.statusViewTextBox.setVisibility(View.GONE);
        binding.loginStatusProgressBar.setVisibility(View.GONE);
        binding.progressBar987.setVisibility(View.GONE);
        binding.loginStatusProgressBar.setVisibility(View.GONE);
    }

    private void showCheckView(int txt, int cl) {
        loginViewLayout.setVisibility(View.GONE);
        binding.statusViewTextBox.setVisibility(View.VISIBLE);
        binding.statusViewTextBox.setTextColor(getResources().getColor(cl, null));
        binding.statusViewTextBox.setText(txt);
        binding.progressBar987.setVisibility(View.VISIBLE);
    }

    private void sendEmailVerification(@NonNull FirebaseUser user, TextView statusTV) {
        user.sendEmailVerification().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(LOG_TAG, "Verification email has sent successfully.");
                Toast.makeText(context, "Verification email has sent successfully", Toast.LENGTH_SHORT).show();
                statusTV.setText(R.string.email_verification_link_sent);
                // You can show a message to the user to check their email for verification
            } else {
                Toast.makeText(context, "Failed to send email verification", Toast.LENGTH_SHORT).show();
                Log.e(LOG_TAG, "Failed to send email verification.", task.getException());
            }
        });
    }

    private void signIn(@NonNull String email, @NonNull String password) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(activity, task -> {
            if (task.isSuccessful()) {
                // Sign in success, update UI with the signed-in user's information
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in and email is verified
                    // Proceed to your main activity or dashboard
                    if (rememberCredCB.isChecked()) {
                        preferences.edit().putString("username", email).apply();
                        preferences.edit().putString("password", password).apply();
                        preferences.edit().putBoolean("isInitialLogin", false).apply();
                        preferences.edit().putBoolean("isRemembered", true).apply();
                    } else {
                        preferences.edit().putString("username", "").apply();
                        preferences.edit().putString("password", "").apply();
                        preferences.edit().putBoolean("isInitialLogin", false).apply();
                        preferences.edit().putBoolean("isRemembered", false).apply();
                    }

                    startActivity(mainActivity);

                    binding.loginStatusProgressBar.setVisibility(View.GONE);
                    binding.statusViewLoginFragmentTextView.setText("");
                    emailET.setText("");
                    passwordET.setText("");
                    rememberCredCB.setChecked(true);
                } else {
                    // Email is not verified, show a message or handle accordingly
                    statusTV.setText(R.string.please_verify_your_email_id);
                    preferences.edit().putBoolean("isRegisteredUser", false).apply();
                    Toast.makeText(context, "Please verify your email id", Toast.LENGTH_SHORT).show();
                }
            } else {
                // If sign in fails, display a message to the user.
                Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show();
                binding.loginStatusProgressBar.setVisibility(View.GONE);
                binding.statusViewLoginFragmentTextView.setText("");
                Log.w(LOG_TAG, "signInWithEmail:failure");
            }
        });
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ActivityFinisher) {
            mListener = (ActivityFinisher) context;
        } else {
            throw new RuntimeException(context + " must implement ActivityFinishListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        statusTV.setText("");
    }
}