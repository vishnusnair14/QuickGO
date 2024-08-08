package com.vishnu.quickgoorder.ui.home.recommendation;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.vishnu.quickgoorder.R;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

public class HomeRecommendationAdapter extends RecyclerView.Adapter<HomeRecommendationAdapter.ViewHolder> {

    private final List<HomeRecommendationModel> shopList;
    private Context context;
    DecimalFormat coordinateFormat = new DecimalFormat("0.0000000000");
    HomeRecommendationFragment homeRecommendationFragment;
    Bundle bundle;

    public HomeRecommendationAdapter(List<HomeRecommendationModel> shopList, Context context, HomeRecommendationFragment homeRecommendationFragment) {
        this.shopList = shopList;
        this.context = context;
        this.homeRecommendationFragment = homeRecommendationFragment;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.srv_shop_recommendation, parent, false);
        bundle = new Bundle();
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HomeRecommendationModel shop = shopList.get(position);

        Picasso.get().load(shop.getShopImageUrl()).into(holder.shopImageView);

        // Bind data to the views
        holder.shopNameTV.setText(shop.getShopName());
        if (shop.getDistanceKm() < 1) {
            holder.distanceTV.setText(String.format(Locale.getDefault(), "%.1f mtr", shop.getDistanceKm() * 1000));
        } else {
            holder.distanceTV.setText(String.format(Locale.getDefault(), "%.1f km", shop.getDistanceKm()));
        }

        holder.shopLocationTV.setText(shop.getShopStreet());
        holder.shopCoordinatesTV.setText(MessageFormat.format("{0}°N\n{1}°E",
                coordinateFormat.format(shop.getShopLat()), coordinateFormat.format(shop.getShopLon())));

        holder.shopListCardView.setOnClickListener(view -> {
            bundle.putString("shop_name", shopList.get(position).getShopName());
            bundle.putString("shop_id", shopList.get(position).getShopId());
            bundle.putString("shop_district", shopList.get(position).getShopDistrict());

            NavHostFragment.findNavController(homeRecommendationFragment).navigate(R.id.action_shopInfoFragment_to_nav_home, bundle);
        });
    }

    @Override
    public int getItemCount() {
        return shopList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView shopNameTV;
        TextView distanceTV;
        TextView shopLocationTV;
        TextView shopCoordinatesTV;
        ImageView shopImageView;
        CardView shopListCardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            shopNameTV = itemView.findViewById(R.id.shopNameShopRecommendationView_textView);
            distanceTV = itemView.findViewById(R.id.shopDistShopRecommendationView_textView);
            shopImageView = itemView.findViewById(R.id.shopImageShopRecommendationView_imageView);
            shopLocationTV = itemView.findViewById(R.id.shopPlaceLocShopRecommendationView_textView);
            shopCoordinatesTV = itemView.findViewById(R.id.shopCoordinatesShopRecommendation_textView);
            shopListCardView = itemView.findViewById(R.id.shopRecommendationInfo_cardView);
        }
    }
}
