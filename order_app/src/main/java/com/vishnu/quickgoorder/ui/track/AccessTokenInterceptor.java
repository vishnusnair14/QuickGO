package com.vishnu.quickgoorder.ui.track;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AccessTokenInterceptor implements Interceptor {
    private static final String API_KEY_PARAM = "api_key";
    private static final String API_KEY_VALUE = "x2ZFZ17JYKl6oDv9Hd7AC06llyVhupSE7L4j3V2P";

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Modify the URL to include the API key as a query parameter
        HttpUrl urlWithApiKey = originalRequest.url().newBuilder()
            .addQueryParameter(API_KEY_PARAM, API_KEY_VALUE)
                .addQueryParameter("client_id", "c22974f9-e65a-4b54-bb26-6246d06d3bc3")
                .addQueryParameter("access_token", "BrNOo8Jte3ZTWqFt1KLlUDf79Wb1KuRy")
            .build();

        Request newRequest = originalRequest.newBuilder()
            .url(urlWithApiKey)
            .build();

        return chain.proceed(newRequest);
    }
}