package com.vishnu.quickgodelivery.ui.order.info.obv;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.squareup.picasso.Picasso;
import com.vishnu.quickgodelivery.R;
import com.vishnu.quickgodelivery.miscellaneous.Utils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

public class StorePrefAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private BottomSheetDialog shopDetailsBtmView;
    private final List<StorePrefDataModel> storePrefDataModelList;

    public StorePrefAdapter(Context context, List<StorePrefDataModel> storePrefDataModelList) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.storePrefDataModelList = storePrefDataModelList;
    }

    @Override
    public int getCount() {
        return storePrefDataModelList.size();
    }

    @Override
    public Object getItem(int position) {
        return storePrefDataModelList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        StorePrefDataModel storePrefDataModel = storePrefDataModelList.get(position);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.srv_store_pref_list, parent, false);
            holder = new ViewHolder(convertView);

            Picasso.get().load(storePrefDataModel.getShopImageUrl()).into(holder.shopImage);
            holder.shopName.setText(storePrefDataModel.getShopName());
            holder.shopAddress.setText(String.format("%s | %s%s",
                    storePrefDataModel.getShopStreet(),
                    storePrefDataModel.getShopDistrict().substring(0, 1).toUpperCase(),
                    storePrefDataModel.getShopDistrict().substring(1)));

            holder.viewAllDetails.setOnClickListener(v -> {
                Log.d("StorePrefAdapter", "ViewAllDetails clicked");
                showShopDetailsBtmView(storePrefDataModel);
                Utils.vibrate(context, 50, 2);
            });

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        return convertView;
    }

    private void showShopDetailsBtmView(StorePrefDataModel storePrefDataModel) {
        View shopDetailView = LayoutInflater.from(context).inflate(
                R.layout.bottomview_store_pref_shop_details, null, false);

        TextView shopNameTV1 = shopDetailView.findViewById(R.id.btmview_ShopInfoShopName_textView);
        TextView shopPlaceDistrictTV1 = shopDetailView.findViewById(R.id.btmview_ShopInfoShopStreetDistrict_textView);

        TextView shopInfoTV1 = shopDetailView.findViewById(R.id.btmview_ShopInfo1_textView);
        TextView shopInfoTV2 = shopDetailView.findViewById(R.id.btmview_ShopInfo2_textView);
        TextView shopInfoTV3 = shopDetailView.findViewById(R.id.btmview_ShopInfo3_textView);

        shopNameTV1.setText(storePrefDataModel.getShopName());
        shopPlaceDistrictTV1.setText(MessageFormat.format("{0} | {1}",
                storePrefDataModel.getShopStreet(), storePrefDataModel.getShopDistrict()));

        String info1 = "";
        String info2 = "";
        String info3 = "";

        shopInfoTV1.setText(info1);
        shopInfoTV2.setText(info2);
        shopInfoTV3.setText(info3);

        if (shopDetailsBtmView == null) {
            shopDetailsBtmView = new BottomSheetDialog(context);
            shopDetailsBtmView.setContentView(shopDetailView);
            shopDetailsBtmView.setCanceledOnTouchOutside(false);
            Objects.requireNonNull(shopDetailsBtmView.getWindow()).setGravity(Gravity.TOP);
        } else {
            shopDetailsBtmView.show();
        }

    }

    static class ViewHolder {
        ImageView shopImage;
        TextView shopName;
        TextView shopAddress;
        TextView viewAllDetails;

        ViewHolder(View view) {
            shopImage = view.findViewById(R.id.srvStorePrefShopImage_imageView);
            shopName = view.findViewById(R.id.srvStorePrefShopName_textView);
            shopAddress = view.findViewById(R.id.srvStorePrefShopDetails_textView);
            viewAllDetails = view.findViewById(R.id.srvStorePrefShowAllDetails_textView);

        }
    }
}
