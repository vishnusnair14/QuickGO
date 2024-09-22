package com.vishnu.quickgoorder.ui.home.recommendation.items;

//import static com.google.common.io.Resources.getResource;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.vishnu.quickgoorder.R;
import com.vishnu.quickgoorder.cloud.DbHandler;

import java.util.List;
import java.util.Locale;

public class ItemAdapter extends BaseAdapter {
    private final List<ItemModel> itemList;
    private final LayoutInflater inflater;
    private final DbHandler dbHandler;
    Vibrator vibrator;
    String SHOP_ID;
    Context context;

    public ItemAdapter(Context context, DbHandler dbHandler, Vibrator vibrator,
                       List<ItemModel> itemList, String SHOP_ID) {
        this.itemList = itemList;
        this.inflater = LayoutInflater.from(context);
        this.SHOP_ID = SHOP_ID;
        this.dbHandler = dbHandler;
        this.vibrator = vibrator;
        this.context = context;
    }

    @Override
    public int getCount() {
        return itemList.size();
    }

    @Override
    public Object getItem(int position) {
        return itemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        ItemModel item = itemList.get(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.srv_home_item, parent, false);
            holder = new ViewHolder(convertView);

            // Set data to views
            Picasso.get().load(item.getItem_image_url()).into(holder.itemImageView);
            holder.itemNameTextView.setText(item.getItem_name());
            holder.itemPriceTextView.setText(String.format(Locale.ENGLISH, "₹%,d/%s",
                    item.getItem_price(), item.getItem_price_unit()));

            holder.itemIDTV.setText(item.getItem_id());

            holder.itemAddToCartButton.setOnClickListener(view -> {
                holder.itemAddToCartButton.setText(R.string.adding);
                dbHandler.addItemToManualCartDB(view, item, vibrator, SHOP_ID, holder.itemAddToCartButton);
            });

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        return convertView;
    }


    static class ViewHolder {
        ImageView itemImageView;
        TextView itemNameTextView, itemPriceTextView;
        TextView itemIDTV;
        public TextView itemAddToCartButton;

        public ViewHolder(View itemView) {
            itemImageView = itemView.findViewById(R.id.vegieImageHomeView_imageView);
            itemIDTV = itemView.findViewById(R.id.itemIDView_textView);
            itemNameTextView = itemView.findViewById(R.id.itemDescriptionHomeView_textView);
            itemPriceTextView = itemView.findViewById(R.id.itemPriceHomeView_textView);
            itemAddToCartButton = itemView.findViewById(R.id.addToCartHomeView_button);
        }
    }

}
