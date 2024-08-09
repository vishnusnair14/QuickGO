package com.vishnu.quickgodelivery.ui.order.info.obv;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
        if (shopDetailsBtmView == null) {
            shopDetailsBtmView = new BottomSheetDialog(context);
            View shopDetailView = LayoutInflater.from(context).inflate(
                    R.layout.bottomview_store_pref_shop_details, null, false);

            shopDetailsBtmView.setContentView(shopDetailView);
            Objects.requireNonNull(shopDetailsBtmView.getWindow()).setGravity(Gravity.TOP);
        }

        // Retrieve the existing content view directly from the BottomSheetDialog
        View shopDetailView = shopDetailsBtmView.findViewById(R.id.btmview_ShopInfoShopName_textView);

        if (shopDetailView != null) {
            // Get the root view of the inflated layout
            shopDetailView = shopDetailView.getRootView();

            // Now use shopDetailView to find and update other views
            TextView shopNameTV1 = shopDetailView.findViewById(R.id.btmview_ShopInfoShopName_textView);
            TextView shopPlaceDistrictTV1 = shopDetailView.findViewById(R.id.btmview_ShopInfoShopStreetDistrict_textView);

            Button hideBtn = shopDetailView.findViewById(R.id.btmview_ShopInfoHideBtn_button);
            Button showOnMapBtn = shopDetailView.findViewById(R.id.btmview_ShopInfoShowOnMap_button);
            TextView shopInfoTV1 = shopDetailView.findViewById(R.id.btmview_ShopInfo1_textView);
            TextView shopInfoTV2 = shopDetailView.findViewById(R.id.btmview_ShopInfo2_textView);
            TextView shopInfoTV3 = shopDetailView.findViewById(R.id.btmview_ShopInfo3_textView);

            shopNameTV1.setText(storePrefDataModel.getShopName().toUpperCase());
            shopPlaceDistrictTV1.setText(MessageFormat.format("{0}{1} | {2}{3}",
                    storePrefDataModel.getShopStreet().substring(0, 1).toUpperCase(),
                    storePrefDataModel.getShopStreet().substring(1),
                    storePrefDataModel.getShopDistrict().substring(0, 1).toUpperCase(),
                    storePrefDataModel.getShopDistrict().substring(1)));

            // Update information
            shopInfoTV1.setText("");
            shopInfoTV2.setText("");
            shopInfoTV3.setText("");

            showOnMapBtn.setOnClickListener(v->{

            });
            hideBtn.setOnClickListener(v -> shopDetailsBtmView.dismiss());

            // Show the dialog
            shopDetailsBtmView.show();
        } else {
            // Handle the case where the view is not found
            Log.e("showShopDetailsBtmView", "View not found");
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
