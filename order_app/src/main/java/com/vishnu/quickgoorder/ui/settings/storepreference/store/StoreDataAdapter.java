package com.vishnu.quickgoorder.ui.settings.storepreference.store;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.vishnu.quickgoorder.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StoreDataAdapter extends BaseAdapter {

    private List<StoreData> storeList;
    private Context context;
    private Set<Integer> selectedPreferences; // To track selected preferences

    public StoreDataAdapter(Context context, List<StoreData> storeList) {
        this.context = context;
        this.storeList = storeList;
        this.selectedPreferences = new HashSet<>();

        // Initialize selectedPreferences with already selected preferences
        for (int i = 0; i < storeList.size(); i++) {
            StoreData store = storeList.get(i);
            int preference = store.getShop_preference();
            if (preference <= 0 || preference > storeList.size()) {
                preference = i + 1;
                store.setShop_preference(preference);
            }
            selectedPreferences.add(preference);
        }
    }

    @Override
    public int getCount() {
        return storeList.size();
    }

    @Override
    public Object getItem(int position) {
        return storeList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public List<StoreData> getSelectedStores() {
        List<StoreData> selectedShops = new ArrayList<>();
        for (StoreData shop : storeList) {
            if (shop.getShop_preference() > 0) {
                selectedShops.add(shop);
            }
        }
        return selectedShops;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.srv_shop_pref_lists, parent, false);
            holder = new ViewHolder();
            holder.textViewShopName = convertView.findViewById(R.id.tvShopName);
            holder.spinnerPreference = convertView.findViewById(R.id.spinner2);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Bind data to views
        StoreData store = storeList.get(position);
        holder.textViewShopName.setText(store.getShopName());

        // Set up Spinner adapter with dynamic list
        List<CharSequence> preferenceList = new ArrayList<>();
        for (int i = 1; i <= storeList.size(); i++) {
            if (store.getShop_preference() == i || !selectedPreferences.contains(i)) {
                preferenceList.add(String.valueOf(i));
            }
        }

        ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, preferenceList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        holder.spinnerPreference.setAdapter(spinnerAdapter);

        if (store.getShop_preference() > 0) {
            holder.spinnerPreference.setSelection(preferenceList.indexOf(String.valueOf(store.getShop_preference())));
        } else {
            holder.spinnerPreference.setSelection(0);
        }

        holder.spinnerPreference.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                int selectedPreference = Integer.parseInt(parent.getItemAtPosition(pos).toString());
                if (store.getShop_preference() != selectedPreference) {

                    // Update the selected preferences set
                    selectedPreferences.remove(store.getShop_preference());
                    store.setShop_preference(selectedPreference);
                    selectedPreferences.add(selectedPreference);
                    notifyDataSetChanged();
                    SetStorePrefFragment.checkForDuplicatePreferences();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle nothing selected
            }
        });

        return convertView;
    }

    static class ViewHolder {
        TextView textViewShopName;
        Spinner spinnerPreference;
    }
}
