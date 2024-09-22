package com.vishnu.quickgoorder.ui.authentication.signInWithEmailPswd;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.callbacks.ActivityFinisher;

public class AuthenticationActivity extends AppCompatActivity implements ActivityFinisher {
    private static final String LOG_TAG = "LoginFragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        LoginFragment loginFragment = new LoginFragment(this, this);
        RegisterFragment registerFragment = new RegisterFragment(this, this);
        ResetPswdFragment resetPswdFragment = new ResetPswdFragment(this, this);

        transaction.add(R.id.fragmentContainer, loginFragment).commit();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

            if (item.getItemId() == R.id.login_nav_menu) {
                if (!loginFragment.isAdded()) {
                    fragmentTransaction.add(R.id.fragmentContainer, loginFragment);
                }
                fragmentTransaction.show(loginFragment);
                fragmentTransaction.hide(registerFragment);
                fragmentTransaction.hide(resetPswdFragment);
            } else if (item.getItemId() == R.id.register_nav_menu) {
                if (!registerFragment.isAdded()) {
                    fragmentTransaction.add(R.id.fragmentContainer, registerFragment);
                }
                fragmentTransaction.show(registerFragment);
                fragmentTransaction.hide(loginFragment);
                fragmentTransaction.hide(resetPswdFragment);
            } else if (item.getItemId() == R.id.reset_pswd_nav_menu) {
                if (!resetPswdFragment.isAdded()) {
                    fragmentTransaction.add(R.id.fragmentContainer, resetPswdFragment);
                }
                fragmentTransaction.show(resetPswdFragment);
                fragmentTransaction.hide(loginFragment);
                fragmentTransaction.hide(registerFragment);
            }

            fragmentTransaction.commit();
            return true;
        });
    }

    @Override
    public void onFinishActivity() {
        finish(); // Finish the activity
    }

}
