package com.vishnu.quickgoorder;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.vishnu.quickgoorder.ui.authentication.signInWithEmailPswd.AuthenticationActivity;
import com.vishnu.quickgoorder.ui.authentication.signInWithPhoneNumber.PhoneAuthActivity;

public class GreetingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_greeting);

        // all bindings init. here
        Button getStatedBtn = findViewById(R.id.getStarted_button);

        getStatedBtn.setOnClickListener(view -> {
            Intent SignInActivityIntent = new Intent(GreetingActivity.this, AuthenticationActivity.class);
            startActivity(SignInActivityIntent);
        });

//        signInWithEmailBtn.setOnClickListener(view -> {
//            Intent SignInActivityIntent = new Intent(GreetingActivity.this, AuthenticationActivity.class);
//            startActivity(SignInActivityIntent);
//        });

//        if (R.integer.DebugStateForAPP > 1) {
//            signInWithPhnoBtn.performClick();
//            Log.i(TAG, "DebugStateForAPP.called: MainEntryActivity.continue_button");
//        }
    }
}