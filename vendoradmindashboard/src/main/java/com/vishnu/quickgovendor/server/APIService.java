package com.vishnu.quickgovendor.server;

import com.google.gson.JsonObject;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface APIService {

    @Multipart
    @POST("register-vendor-account")
    Call<JsonObject> registerShop(
            @Part MultipartBody.Part image,
            @Part("account_data") RequestBody shopData
    );
}
