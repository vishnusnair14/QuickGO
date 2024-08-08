package com.vishnu.quickgoorder.ui.home.recommendation;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ShopDataWrapper {

    @SerializedName("recommended_shop_data")
    private List<HomeRecommendationModel> recommendedShopData;

    public List<HomeRecommendationModel> getRecommendedShopData() {
        return recommendedShopData;
    }
}
