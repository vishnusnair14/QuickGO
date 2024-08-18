package com.vishnu.quickgoorder.ui.track.orderstatus;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ItemAnimatorOrderStatusUpdates extends DefaultItemAnimator {

    private static final long DURATION = 1200;

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        // Start with the item slightly scaled down and fully transparent
        holder.itemView.setScaleX(0.8f);
        holder.itemView.setScaleY(0.8f);
        holder.itemView.setAlpha(1f);

        // Start with the item off-screen to the right
        holder.itemView.setTranslationX(holder.itemView.getWidth());

        // Create an animator set to play the scale, alpha, and translation animations together
        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(holder.itemView, View.SCALE_X, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(holder.itemView, View.SCALE_Y, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(holder.itemView, View.ALPHA, 1f);
        ObjectAnimator translationX = ObjectAnimator.ofFloat(holder.itemView, View.TRANSLATION_X, 0);

        // Play the animations together
        animatorSet.playTogether(scaleX, scaleY, alpha, translationX);
        animatorSet.setDuration(DURATION);
        animatorSet.start();

        return super.animateAdd(holder);
    }


    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        // Apply a fade-out animation
        holder.itemView.animate()
                .scaleX(0.5f)
                .scaleY(0.5f)
                .setDuration(DURATION)
                .withEndAction(() -> holder.itemView.setAlpha(1))
                .start();
        return super.animateRemove(holder);
    }

    @Override
    public boolean animateMove(RecyclerView.ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        // Slide animation for moving items
        ObjectAnimator moveX = ObjectAnimator.ofFloat(holder.itemView, View.TRANSLATION_X, fromX, toX);
        ObjectAnimator moveY = ObjectAnimator.ofFloat(holder.itemView, View.TRANSLATION_Y, fromY, toY);
        AnimatorSet moveSet = new AnimatorSet();
        moveSet.playTogether(moveX, moveY);
        moveSet.setDuration(DURATION);
        moveSet.start();
        return super.animateMove(holder, fromX, fromY, toX, toY);
    }

    @Override
    public boolean animateChange(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder,
                                 int fromLeft, int fromTop, int toLeft, int toTop) {
        if (oldHolder != null) {
            // Keep the old item visible during the transition
            oldHolder.itemView.setAlpha(1);
            oldHolder.itemView.animate()
                    .alpha(1f) // Keep the old item fully visible
                    .setDuration(DURATION)
                    .start();
        }

        if (newHolder != null) {
            // Start the new item with full alpha
            newHolder.itemView.setAlpha(0f);

            // Fade in the new item to full visibility
            newHolder.itemView.animate()
                    .alpha(1f) // Fully visible
                    .setDuration(DURATION)
                    .start();
        }

        return super.animateChange(oldHolder, newHolder, fromLeft, fromTop, toLeft, toTop);
    }


    @Override
    public void runPendingAnimations() {
        super.runPendingAnimations();
    }

    @Override
    public void endAnimation(RecyclerView.ViewHolder item) {
        item.itemView.clearAnimation();
        super.endAnimation(item);
    }

    @Override
    public void endAnimations() {
        super.endAnimations();
    }

    @Override
    public boolean isRunning() {
        return super.isRunning();
    }

    @Override
    public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, @NonNull List<Object> payloads) {
        return true;
    }
}
