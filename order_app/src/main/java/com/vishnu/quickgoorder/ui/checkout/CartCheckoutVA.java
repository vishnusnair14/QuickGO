package com.vishnu.quickgoorder.ui.checkout;

import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vishnu.quickgoorder.R;

import java.util.List;
import java.util.Locale;


public class CartCheckoutVA extends RecyclerView.Adapter<CartCheckoutVA.ViewHolder> {
    private final List<CartCheckoutVM> itemList;
    private final String LOG_TAG = this.getClass().getSimpleName();
    private float grandTotal = 0;
    private SharedPreferences preferences;
    private final TextView grandTotalTV;

    public CartCheckoutVA(List<CartCheckoutVM> itemList, TextView grandTotalTV, SharedPreferences preferences) {
        this.itemList = itemList;
        this.grandTotalTV = grandTotalTV;
        this.preferences = preferences;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.srv_checkout_item_summary, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CartCheckoutVM cartCheckoutVM = itemList.get(position);

        holder.itemNameTV.setText(cartCheckoutVM.getItem_name());
        holder.itemQtyTV.setText(String.format(Locale.ENGLISH, "%d", cartCheckoutVM.getItem_qty()));
        holder.itemPriceTV.setText(String.format(Locale.ENGLISH, "%d", cartCheckoutVM.getItem_price()));
        holder.itemFinalPriceTV.setText(String.format(Locale.ENGLISH, "₹ %d/-", cartCheckoutVM.getItem_final_price()));

        grandTotal = grandTotal + cartCheckoutVM.getGrand_total();
        grandTotalTV.setText(String.format(Locale.ENGLISH, "GRAND TOTAL: ₹ %f/-", grandTotal));

        preferences.edit().putFloat("final_amount_payable", grandTotal).apply();
        Log.i(LOG_TAG, "GRAND TOTAL:" + grandTotal);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemNameTV, itemPriceTV, itemQtyTV, itemFinalPriceTV;

        public ViewHolder(View itemView) {
            super(itemView);
            itemNameTV = itemView.findViewById(R.id.itemNameCheckoutView_textView);
            itemPriceTV = itemView.findViewById(R.id.itemPriceCheckoutView_textView);
            itemQtyTV = itemView.findViewById(R.id.itemQuantityCheckoutView_textView);
            itemFinalPriceTV = itemView.findViewById(R.id.itemFinalPriceCheckoutView_textView);
        }
    }
}

