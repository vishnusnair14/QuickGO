package com.vishnu.quickgodelivery.miscellaneous;

import android.content.Context;
import android.media.SoundPool;

import com.vishnu.quickgodelivery.R;

public class SoundManager {
    private static SoundPool soundPool;
    private static int onOrderAccepted;
    private static int onOrderSavedForNext;

    // Initialize SoundPool and load the sound
    public static void initialize(Context context) {
        if (soundPool == null) {
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(1)
                    .build();
            onOrderAccepted = soundPool.load(context, R.raw.on_order_accepted, 1);
            onOrderSavedForNext = soundPool.load(context, R.raw.on_order_saved_for_next, 1);
        }
    }

    public static void playOnOrderAccepted() {
        if (soundPool != null && onOrderAccepted != 0) {
            soundPool.play(onOrderAccepted, 1, 1, 0, 0, 1);
        }
    }

    public static void playOnOrderSavedForNext() {
        if (soundPool != null && onOrderSavedForNext != 0) {
            soundPool.play(onOrderSavedForNext, 1, 1, 0, 0, 1);
        }
    }

    // Release resources
    public static void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
