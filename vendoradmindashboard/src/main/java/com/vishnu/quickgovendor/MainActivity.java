package com.vishnu.quickgovendor;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;
import com.vishnu.quickgovendor.databinding.ActivityMainBinding;

import java.text.MessageFormat;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private NavController navController;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    NavOptions navOptions = new NavOptions.Builder().setPopUpTo(R.id.nav_home, false).build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityMainBinding activityMainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(activityMainBinding.getRoot());

        Toolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        DrawerLayout navDrawerLayout = activityMainBinding.drawerLayout;
        NavigationView leftNavDrawerMenu = activityMainBinding.navView;
        View headerView = leftNavDrawerMenu.getHeaderView(0);

        // GETS & SETS PROFILE VIEW
        ImageView profilePhotoImageView = headerView.findViewById(R.id.vendorAppProfilePhoto_imageView);
        TextView nameTV = headerView.findViewById(R.id.vendorAppProfileUsername_textView);
        TextView emailIdTV = headerView.findViewById(R.id.vendorAppUserEmailIDView_textBox);
        TextView userIdTV = headerView.findViewById(R.id.vendorAppUserUIDView_textBox);

        // sets
        if (mAuth.getCurrentUser() != null) {
            Picasso.get().load(user.getPhotoUrl()).into(profilePhotoImageView);
            nameTV.setText(MessageFormat.format("WELCOME {0}",
                    Objects.requireNonNull(user.getDisplayName())));

            if (mAuth.getCurrentUser().isEmailVerified()) {
                emailIdTV.setText(MessageFormat.format("{0} {1}",
                        user.getEmail(), "(verified)"));
                userIdTV.setText(user.getUid());
            } else {
                emailIdTV.setText(user.getEmail());
                userIdTV.setText(user.getUid());
            }
        } else {
            emailIdTV.setText("");
            userIdTV.setText("");
        }

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_settings, R.id.nav_advancedOptions)
                .setOpenableLayout(navDrawerLayout).build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(leftNavDrawerMenu, navController);

        leftNavDrawerMenu.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() != Objects.requireNonNull(navController.getCurrentDestination()).getId()) {
                NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);

                assert navHostFragment != null;
                NavController navController = navHostFragment.getNavController();

                navController.navigate(item.getItemId(), null, navOptions);
                navDrawerLayout.close();
                return true;
            } else {
                Log.i("MainActivity", "ON-SAME-DESTINATION." + item + "." + item.getItemId());
                navDrawerLayout.close();
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}