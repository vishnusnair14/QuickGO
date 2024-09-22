package com.vishnu.quickgoorder.server.sapi;

import com.google.gson.JsonObject;
import com.vishnu.quickgoorder.server.models.DeleteVoiceOrderFile;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface APIService {

    @POST("save_preferences/")
    Call<JsonObject> saveSelectedStores(@Body JsonObject selectedShops);

    @POST("place-order-obv")
    Call<JsonObject> placeOrderOBV(
            @Body RequestBody orderData
    );

    @GET("delete-user-account")
    Call<JsonObject> deleteUserAccount(
            @Query("client_id") String clientID,
            @Query("client_type") String clientType);

    @GET("place-order-obs/{order_id}/{user_id}/{user_email}/{user_phno}" +
            "/{order_by_voice_type}/{order_by_voice_doc_id}/{order_by_voice_audio_ref_id}/{shop_id}/{shop_city}" +
            "/{shop_pincode}/{curr_lat}/{curr_lon}")
    Call<JsonObject> placeOrderOBS(
            @Path("order_id") String OrderID,
            @Path("user_id") String userID,
            @Path("user_email") String userEmail,
            @Path("user_phno") String userPhno,
            @Path("order_by_voice_type") String orderByVoiceType,
            @Path("order_by_voice_doc_id") String orderByVoiceDocID,
            @Path("order_by_voice_audio_ref_id") String orderByVoiceAudioRefID,
            @Path("shop_id") String shopID,
            @Path("shop_city") String shopCity,
            @Path("shop_pincode") String shopPincode,
            @Path("curr_lat") String currLat,
            @Path("curr_lon") String currLon);

    @GET("recommend-shop/{user_la}/{user_lo}/{user_state}/{user_district}/{user_pincode}")
    Call<JsonObject> getShopRecommendations(
            @Path("user_la") double latitude,
            @Path("user_lo") double longitude,
            @Path("user_state") String user_state,
            @Path("user_district") String user_district,
            @Path("user_pincode") String user_pincode);

    @GET("delete-address/")
    Call<JsonObject> deleteAddress(
            @Query("user_id") String userId,
            @Query("address_id") String addressId
    );

    @GET("get-saved-address/")
    Call<JsonObject> getSavedAddresses(
            @Query("user_id") String userID);

    @POST("delete-voice-order-cart")
    Call<JsonObject> deleteVoiceOrderFromCart(
            @Body DeleteVoiceOrderFile deleteVoiceOrderFile);

    @GET("fetch-store-pref-data/{user_id}/{phno_enc}")
    Call<JsonObject> fetchStorePrefData(
            @Path("user_id") String userID,
            @Path("phno_enc") String phnoEnc);

    @GET("delete-store-pref-data/{user_id}/{phno_enc}")
    Call<JsonObject> deleteStorePreferenceData(
            @Path("user_id") String userID,
            @Path("phno_enc") String phnoEnc);

    @GET("create-order-rz/{amount}")
    Call<JsonObject> createPaymentOrder(
            @Path("amount") float amount);

    @GET("verify-payment-sign/{order_id}/{razorpay_payment_id}/{razorpay_signature}")
    Call<JsonObject> verifyPaymentSignature(
            @Path("order_id") String OrderID,
            @Path("razorpay_payment_id") String razorpayPaymentID,
            @Path("razorpay_signature") String razorpaySignature);

    @Multipart
    @POST("add-new-address")
    Call<JsonObject> addNewAddress(
            @Part("address_data") RequestBody addressData
    );

    @GET("get-voice-order-data/{user_id}/{order_by_voice_type}/{order_by_voice_doc_id}" +
            "/{order_by_voice_audio_ref_id}/{shop_id}")
    Call<JsonObject> getVoiceOrderCartData(
            @Path("user_id") String userId,
            @Path("order_by_voice_type") String orderByVoiceType,
            @Path("order_by_voice_doc_id") String orderByVoiceDocID,
            @Path("order_by_voice_audio_ref_id") String orderByVoiceAudioRefID,
            @Path("shop_id") String shopID);

    @Multipart
    @POST("handle-address-decision")
    Call<JsonObject> addressUpdateDecision(
            @Part("address_decision_data") RequestBody addressData
    );
}


