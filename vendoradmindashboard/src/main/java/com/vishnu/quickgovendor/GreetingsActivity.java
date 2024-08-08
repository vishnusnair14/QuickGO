package com.vishnu.quickgovendor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.vishnu.quickgovendor.ui.authentication.AuthenticationActivity;

public class GreetingsActivity extends AppCompatActivity {
    Button continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_greetings);

        continueButton = findViewById(R.id.continue_button);
        Intent intent = new Intent(this, AuthenticationActivity.class);


        continueButton.setOnClickListener(v -> startActivity(intent));

        continueButton.performClick();
    }
}