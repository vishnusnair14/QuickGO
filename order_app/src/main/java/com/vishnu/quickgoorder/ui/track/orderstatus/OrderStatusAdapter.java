package com.vishnu.quickgoorder.ui.track.orderstatus;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.vishnu.quickgoorder.R;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OrderStatusAdapter extends RecyclerView.Adapter<OrderStatusAdapter.ViewHolder> {
    private final List<Map<String, String>> orderStatusList;
    private final Context context;
    private String orderID;

    public OrderStatusAdapter(List<Map<String, String>> orderStatusList, String orderID, Context context) {
        this.orderStatusList = orderStatusList;
        this.orderID = orderID;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.srv_order_status, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, String> status = orderStatusList.get(position);
        holder.statusTitleTextView.setText(status.get("title"));

        if (status.containsKey("key") && Objects.equals(status.get("key"), "1")) {
            holder.statusSubTitleTextView.setText(MessageFormat.format("{0}\n{1}",
                    orderID.substring(6), status.get("sub_title")));
        } else {
            holder.statusSubTitleTextView.setText(status.get("sub_title"));
        }

        // Ensure views are in their default state before animation
//        holder.itemView.setVisibility(View.INVISIBLE);
//        holder.itemView.setAlpha(0f);

        // Applying staggered slide-in animation with delay
//        animateItem(holder.itemView, position);

//        if (position == getItemCount() - 1) {
//            holder.statusProgressBar.setVisibility(View.VISIBLE);
//            holder.statusTitleTextView.setTextColor(ContextCompat.getColor(context, R.color.primary));
//        } else {
//            holder.statusProgressBar.setVisibility(View.GONE);
//        }
    }

    @Override
    public int getItemCount() {
        return orderStatusList.size();
    }

    private void animateItem(View itemView, int position) {
        // Define the delay for the animation
        int delay = position * 500; // Adjust the multiplier (e.g., 300ms) for the desired effect

        // Create a handler to apply the animation after a delay
        new Handler().postDelayed(() -> {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);

            // Apply the animation to the view
            itemView.startAnimation(animation);

            // Make the view visible and reset alpha after animation starts
            itemView.setVisibility(View.VISIBLE);
            itemView.setAlpha(1f);
        }, delay);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView statusTitleTextView, statusSubTitleTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            statusTitleTextView = itemView.findViewById(R.id.statusTitleTextView);
            statusSubTitleTextView = itemView.findViewById(R.id.statusSubTitleTextView);
        }
    }
}
