package com.vishnu.quickgovendor;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {
    Button sentResetEmailBtn;
    EditText resetPswdEmailIDET;
    TextView forgotPswdFdbkTV;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        sentResetEmailBtn = findViewById(R.id.sentPasswordResetLink_button);
        resetPswdEmailIDET = findViewById(R.id.emailIDFieldResetPass_editText);
        forgotPswdFdbkTV = findViewById(R.id.resetPassFeedback_textView);

        sentResetEmailBtn.setOnClickListener(v -> {
            if (resetPswdEmailIDET.getText().toString().length() != 0) {
                sendPasswordResetEmail(resetPswdEmailIDET.getText().toString());
            } else {
                Toast.makeText(this, "Enter the email address", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ForgotPasswordActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                forgotPswdFdbkTV.setText(R.string.reset_email_sent_successfully);
            } else {
                Toast.makeText(ForgotPasswordActivity.this, "Failed to send password reset email", Toast.LENGTH_SHORT).show();
            }
        });
    }
}