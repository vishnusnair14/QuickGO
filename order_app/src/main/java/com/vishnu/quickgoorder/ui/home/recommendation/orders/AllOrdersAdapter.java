package com.vishnu.quickgoorder.ui.home.recommendation.orders;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.ui.track.OrderTrackActivity;

import java.text.MessageFormat;
import java.util.List;


public class AllOrdersAdapter extends RecyclerView.Adapter<AllOrdersAdapter.ViewHolder> {
    final Activity activity;
    private final List<AllOrdersModel> itemList;
    SharedPreferences preferences;
    Intent orderTrackIntent;

    public AllOrdersAdapter(Activity activity, SharedPreferences preferences, List<AllOrdersModel> itemList) {
        this.activity = activity;
        this.itemList = itemList;
        this.preferences = preferences;
        this.orderTrackIntent = new Intent(activity, OrderTrackActivity.class);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.srv_placed_orders, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AllOrdersModel item = itemList.get(position);
        holder.shopNameTv.setText(item.getShop_name().toUpperCase());
        holder.orderNoTv.setText(MessageFormat.format("Order No: {0}", item.getOrder_id().substring(6)));
        holder.orderTimeTV.setText(MessageFormat.format("Order Time: {0}", item.getOrder_time()));
        holder.deliveryAddressTV.setText(MessageFormat.format("Delivery Address: {0}", item.getDelivery_full_address()));
//        holder.deliveryAddrLocCordTV.setText(MessageFormat.format("Address co-ordinates: {0}°N {1}°E", item.getDelivery_loc_coordinates(),
//                item.getDelivery_loc_coordinates()));

        holder.orderAcceptedView.setVisibility(View.VISIBLE);
        holder.orderAcceptedView.setTextColor(Color.parseColor(item.getOrder_status_fg_color()));
        holder.orderAcceptedView.setBackgroundColor(Color.parseColor(item.getOrder_status_bg_color()));
        holder.orderAcceptedView.setText(item.getOrder_status_label());


        holder.allOrderCarView.setOnClickListener(v -> {
//            preferences.edit().putString("orderToTrackOrderID", item.getOrder_id()).apply();
            orderTrackIntent.putExtra("orderToTrackOrderID", item.getOrder_id());
            activity.startActivity(orderTrackIntent);
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public void updateOrder(int position, AllOrdersModel updatedOrder) {
        if (position >= 0 && position < itemList.size()) {
            itemList.set(position, updatedOrder);
            notifyItemChanged(position);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderNoTv;
        TextView shopNameTv;
        TextView orderTimeTV;
        CardView allOrderCarView;
        TextView orderAcceptedView;
        TextView deliveryAddressTV;
        TextView deliveryAddrLocCordTV;

        public ViewHolder(View itemView) {
            super(itemView);
            orderNoTv = itemView.findViewById(R.id.srvOrdersOrderNoView_textView);
            shopNameTv = itemView.findViewById(R.id.srvPlacedOrdersShopNameView_textView);
            orderTimeTV = itemView.findViewById(R.id.srvOrdersOrderTimeView_textView);
            allOrderCarView = itemView.findViewById(R.id.allPlacedOrders_cardView);
            orderAcceptedView = itemView.findViewById(R.id.orderAcceptedView_textView);
            deliveryAddressTV = itemView.findViewById(R.id.srvOrdersDeliveryAddress_textView);
            deliveryAddrLocCordTV = itemView.findViewById(R.id.srvOrdersDeliveryLocationCoords_textView);
        }
    }
}


