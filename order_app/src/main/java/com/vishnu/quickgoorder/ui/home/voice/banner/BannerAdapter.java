package com.vishnu.quickgoorder.ui.home.voice.banner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vishnu.quickgoorder.R;

import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {
    private final List<BannerItem> bannerItems;

    public BannerAdapter(List<BannerItem> bannerItems) {
        this.bannerItems = bannerItems;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner_text, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        BannerItem item = bannerItems.get(position);

        holder.textView.setText(item.text());
        holder.textView.setTextColor(item.textColor());
        holder.itemView.setBackgroundColor(item.backgroundColor());
    }

    @Override
    public int getItemCount() {
        return bannerItems.size();
    }

    public static class BannerViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.bannerTextView);
        }
    }
}
