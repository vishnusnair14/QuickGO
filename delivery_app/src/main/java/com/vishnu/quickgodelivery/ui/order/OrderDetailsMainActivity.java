package com.vishnu.quickgodelivery.ui.order;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.vishnu.quickgodelivery.R;
import com.vishnu.quickgodelivery.ui.order.info.obs.OBSOrderInformationFragment;
import com.vishnu.quickgodelivery.ui.order.info.obv.OBVOrderInformationFragment;
import com.vishnu.quickgodelivery.ui.order.others.OthersFragment;
import com.vishnu.quickgodelivery.ui.order.voice.VoiceOrdersFragment;

public class OrderDetailsMainActivity extends AppCompatActivity {
    private FirebaseUser user;
    private BottomNavigationView btmNavView;
    private String orderByVoiceType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_order_details_main);

        // Extract data from Intent
        Bundle bundle = getBundle();

        user = FirebaseAuth.getInstance().getCurrentUser();

        btmNavView = findViewById(R.id.orderDetailsView_BottomNavigationView);
        btmNavView.setSelectedItemId(R.id.order_info_nav_menu);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        VoiceOrdersFragment voiceOrdersFragment = new VoiceOrdersFragment();
        voiceOrdersFragment.setArguments(bundle);

        OBSOrderInformationFragment obsOrderInformationFragment = new OBSOrderInformationFragment();
        obsOrderInformationFragment.setArguments(bundle);

        OBVOrderInformationFragment obvOrderInformationFragment = new OBVOrderInformationFragment();
        obvOrderInformationFragment.setArguments(bundle);

        OthersFragment othersFragment = new OthersFragment();
        othersFragment.setArguments(bundle);
//
//        // Add the initial fragment
        if (orderByVoiceType.equals("obs")) {
            transaction.add(R.id.orderDetailsMainFrag_fragmentContainerView, obsOrderInformationFragment).commit();
        } else {
            transaction.add(R.id.orderDetailsMainFrag_fragmentContainerView, obvOrderInformationFragment).commit();

        }

        btmNavView.setOnItemSelectedListener(item -> {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

            int itemId = item.getItemId();
            if (itemId == R.id.voice_data_nav_menu) {
                if (!voiceOrdersFragment.isAdded()) {
                    fragmentTransaction.add(R.id.orderDetailsMainFrag_fragmentContainerView, voiceOrdersFragment);
                }
                fragmentTransaction.show(voiceOrdersFragment);

                if (orderByVoiceType.equals("obs")) {
                    if (obsOrderInformationFragment.isAdded()) {
                        fragmentTransaction.hide(obsOrderInformationFragment);
                    }
                } else {
                    if (obvOrderInformationFragment.isAdded()) {
                        fragmentTransaction.hide(obvOrderInformationFragment);
                    }
                }

                if (othersFragment.isAdded()) {
                    fragmentTransaction.hide(othersFragment);
                }

            } else if (itemId == R.id.order_info_nav_menu) {
                if (orderByVoiceType.equals("obs")) {
                    if (!obsOrderInformationFragment.isAdded()) {
                        fragmentTransaction.add(R.id.orderDetailsMainFrag_fragmentContainerView, obsOrderInformationFragment);
                    }
                    fragmentTransaction.show(obsOrderInformationFragment);
                } else {
                    if (!obvOrderInformationFragment.isAdded()) {
                        fragmentTransaction.add(R.id.orderDetailsMainFrag_fragmentContainerView, obvOrderInformationFragment);
                    }
                    fragmentTransaction.show(obvOrderInformationFragment);
                }

                if (voiceOrdersFragment.isAdded()) {
                    fragmentTransaction.hide(voiceOrdersFragment);
                }

                if (othersFragment.isAdded()) {
                    fragmentTransaction.hide(othersFragment);
                }

            } else if (itemId == R.id.others_nav_menu) {
                if (!othersFragment.isAdded()) {
                    fragmentTransaction.add(R.id.orderDetailsMainFrag_fragmentContainerView, othersFragment);
                }
                fragmentTransaction.show(othersFragment);

                if (voiceOrdersFragment.isAdded()) {
                    fragmentTransaction.hide(voiceOrdersFragment);
                }

                if (orderByVoiceType.equals("obs")) {
                    if (obsOrderInformationFragment.isAdded()) {
                        fragmentTransaction.hide(obsOrderInformationFragment);
                    }
                } else {
                    if (obvOrderInformationFragment.isAdded()) {
                        fragmentTransaction.hide(obvOrderInformationFragment);
                    }
                }
            }

            fragmentTransaction.commit();
            return true;
        });
    }


    @NonNull
    private Bundle getBundle() {
        Intent intent = getIntent();
        String orderID = intent.getStringExtra("order_id");
        String shopName = intent.getStringExtra("shop_name");
        String shopID = intent.getStringExtra("shop_id");
        String userID = intent.getStringExtra("user_id");
        orderByVoiceType = intent.getStringExtra("order_by_voice_type");
        String orderByVoiceDocID = intent.getStringExtra("order_by_voice_doc_id");
        String orderByVoiceAudioRefID = intent.getStringExtra("order_by_voice_audio_ref_id");

        // Pass data to the fragment
        Bundle bundle = new Bundle();
        bundle.putString("order_id", orderID);
        bundle.putString("shop_name", shopName);
        bundle.putString("shop_id", shopID);
        bundle.putString("user_id", userID);
        bundle.putString("order_by_voice_type", orderByVoiceType);
        bundle.putString("order_by_voice_doc_id", orderByVoiceDocID);
        bundle.putString("order_by_voice_audio_ref_id", orderByVoiceAudioRefID);
        return bundle;
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Initialize chatWSC with appropriate parameters (user, orderID, etc.)
//        chatWSC = new ChatWSC(this, orderInformationFragment, user, orderID);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close WebSocket connection when activity is destroyed
//        if (chatWSC != null) {
//            chatWSC.webSocket.close(1000, "client disconnected");
//        }
    }
}
