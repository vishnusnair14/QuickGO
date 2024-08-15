package com.vishnu.quickgoorder.miscellaneous;

import android.content.Context;
import android.media.SoundPool;

import com.vishnu.quickgoorder.R;

public class SoundManager {
    private static SoundPool soundPool;
    private static int onButtonHold;
    private static int onButtonRelease;
    private static int onDeleted;

    // Initialize SoundPool and load the sound
    public static void initialize(Context context) {
        if (soundPool == null) {
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(1)
                    .build();
            onButtonHold = soundPool.load(context, R.raw.on_button_hold, 1);
            onButtonRelease = soundPool.load(context, R.raw.on_button_release, 1);
            onDeleted = soundPool.load(context, R.raw.on_deleted, 1);
        }
    }

    public static void playOnButtonHold() {
        if (soundPool != null && onButtonHold != 0) {
            soundPool.play(onButtonHold, 1, 1, 0, 0, 1);
        }
    }

    public static void playOnDelete() {
        if (soundPool != null && onDeleted != 0) {
            soundPool.play(onDeleted, 1, 1, 0, 0, 1);
        }
    }

    public static void playOnButtonRelease() {
        if (soundPool != null && onButtonRelease != 0) {
            soundPool.play(onButtonRelease, 1, 1, 0, 0, 1);
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
