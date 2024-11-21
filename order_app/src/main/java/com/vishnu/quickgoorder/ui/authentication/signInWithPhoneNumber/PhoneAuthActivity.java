package com.vishnu.quickgoorder.ui.authentication.signInWithPhoneNumber;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.vishnu.quickgoorder.MainActivity;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.callbacks.ActivityFinisher;

import java.util.concurrent.TimeUnit;

public class PhoneAuthActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private EditText phoneNumberField, otpField;
    private Button sendOtpButton, verifyOtpButton;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private LinearLayout loginViewLayout;
    private TextView statusViewTV;
    private TextView singInWithEmailTV;
    private ActivityFinisher mListener;
    private ProgressBar statusProgressBar;
    private SharedPreferences preferences;
    Intent mainActivity;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_auth);

        FirebaseApp.initializeApp(this);

        // Initialize App Check
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();
        firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());

        mAuth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        mainActivity = new Intent(this, MainActivity.class);

        loginViewLayout = findViewById(R.id.loginViewLayout_linearLayout);
        statusProgressBar = findViewById(R.id.progressBar9801);
        statusViewTV = findViewById(R.id.statusView_textBox);
        phoneNumberField = findViewById(R.id.phoneNumberFieldLoginFragment_editTextText);
        otpField = findViewById(R.id.otpFieldLoginFragment_editTextText);
        sendOtpButton = findViewById(R.id.buttonSendOtp);
        verifyOtpButton = findViewById(R.id.buttonVerifyOtp);
        singInWithEmailTV = findViewById(R.id.signInWithEmail_textView);

        // Send OTP Button Click Listener
        sendOtpButton.setOnClickListener(v -> {
            String phoneNumber = phoneNumberField.getText().toString().trim();

            if (!phoneNumber.isEmpty()) {
                // Assume phoneNumber is already in E.164 format
                if (isValidPhoneNumber(formatToE164(phoneNumber, "IN"))) {
                    loginViewLayout.setVisibility(View.GONE);
                    statusViewTV.setVisibility(View.VISIBLE);
                    statusViewTV.setText("please wait...");
                    statusProgressBar.setVisibility(View.VISIBLE);
//                    Toast.makeText(this, formatToE164(phoneNumber, "IN"), Toast.LENGTH_SHORT).show();
                    startPhoneNumberVerification(formatToE164(phoneNumber, "IN"));
                } else {
                    Toast.makeText(this, "phone number not valid", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(PhoneAuthActivity.this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            }
        });

        // Verify OTP Button Click Listener
        verifyOtpButton.setOnClickListener(v -> {
            String otpCode = otpField.getText().toString().trim();

            if (!otpCode.isEmpty()) {
                if (mVerificationId != null) {
                    loginViewLayout.setVisibility(View.GONE);
                    statusViewTV.setVisibility(View.VISIBLE);
                    statusViewTV.setText("please wait...");
                    statusProgressBar.setVisibility(View.VISIBLE);
                    verifyPhoneNumberWithCode(mVerificationId, otpCode);
                } else {
                    Toast.makeText(this, "Can't verify OTP", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(PhoneAuthActivity.this, "Please enter the OTP", Toast.LENGTH_SHORT).show();
            }
        });
        singInWithEmailTV.setOnClickListener(v -> {
            Intent i = new Intent(this, AuthenticationActivity.class);
            startActivity(i);
        });
        init();
    }

    public boolean isValidPhoneNumber(String phoneNumber) {
        String e164Regex = "\\+?[1-9]\\d{1,14}";
        return phoneNumber.matches(e164Regex);
    }

    public String formatToE164(String phoneNumber, String countryCode) {
        try {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            Phonenumber.PhoneNumber number = phoneUtil.parse(phoneNumber, countryCode);
            return phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (Exception e) {
            Log.d("TAG", e.toString());
            return null;
        }
    }

    private void showCheckView(int txt, int cl) {
        loginViewLayout.setVisibility(View.GONE);
        statusProgressBar.setVisibility(View.VISIBLE);
        statusViewTV.setVisibility(View.VISIBLE);
        statusViewTV.setTextColor(getResources().getColor(cl, null));
        statusViewTV.setText(txt);
    }

    private void showLoginView() {
        loginViewLayout.setVisibility(View.VISIBLE);
        statusProgressBar.setVisibility(View.GONE);
        statusViewTV.setVisibility(View.GONE);
    }


    private void init() {
        showCheckView(R.string.checking, R.color.checking);

        new Handler().postDelayed(() -> {
            if (!preferences.getBoolean("isRememberedPhoneAuth", false) || user == null) {
                showLoginView();
            } else {
                statusProgressBar.setVisibility(View.VISIBLE);
                showCheckView(R.string.signing_in, R.color.signing_in);
                new Handler().postDelayed(() -> {
                    startActivity(mainActivity);
//                    loginViewLayout.setVisibility(View.VISIBLE);
                    finish();
                }, 100);
            }
        }, 100);
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        preferences.edit().putBoolean("isRememberedPhoneAuth", true).apply();
                        startActivity(mainActivity);
                        finish();
                        Toast.makeText(PhoneAuthActivity.this, "Authentication Successful", Toast.LENGTH_SHORT).show();
                    } else {
                        // Sign in failed, display a message and update the UI
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            // The verification code entered was invalid
                            Toast.makeText(PhoneAuthActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    // Auto verification or instant verification is done here
                    Log.d("OTP", "Verification completed with auto-detection.");
                    signInWithPhoneAuthCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    // Handle verification failed
                    Toast.makeText(PhoneAuthActivity.this, "Verification Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    // Save verification ID and resending token so we can use them later
                    mVerificationId = verificationId;
                    mResendToken = token;
                    loginViewLayout.setVisibility(View.VISIBLE);
                    statusViewTV.setVisibility(View.GONE);
                    statusViewTV.setText("");
                    statusProgressBar.setVisibility(View.GONE);
                    Toast.makeText(PhoneAuthActivity.this, "OTP Sent", Toast.LENGTH_SHORT).show();
                }
            };
}




