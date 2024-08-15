package com.vishnu.quickgoorder.miscellaneous;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

public class CustomItemAnimator extends DefaultItemAnimator {

    private static final long PAUSE_DURATION = 300;

    @Override
    public boolean animateRemove(@NonNull RecyclerView.ViewHolder holder) {
        // Custom removal animation with a pause before moving items up
        View view = holder.itemView;

        // Fade out the item first
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        fadeOut.setDuration(getRemoveDuration());

        // Add a pause before the rest of the items move up
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Trigger default removal after the pause
                ValueAnimator pauseAnimator = ValueAnimator.ofFloat(0f, 1f);
                pauseAnimator.setDuration(PAUSE_DURATION);
                pauseAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        dispatchRemoveFinished(holder);
                        view.setAlpha(1f);
                        dispatchAnimationsIfNeeded();
                    }
                });
                pauseAnimator.start();
            }
        });

        fadeOut.start();
        return true;
    }

    @Override
    public void onRemoveFinished(RecyclerView.ViewHolder item) {
        super.onRemoveFinished(item);
        // Override this if you want to handle something after the removal animation ends
    }

    @Override
    public void onRemoveStarting(RecyclerView.ViewHolder item) {
        super.onRemoveStarting(item);
        // Override this if you want to add custom animations when removal starts
    }

    private void dispatchAnimationsIfNeeded() {
        // Check if there are any pending animations and finish them
        if (!isRunning()) {
            dispatchAnimationsFinished();
        }
    }
}
