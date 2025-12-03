package com.redrak.app;

import android.content.Context;
import androidx.annotation.NonNull;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private final Context context;

    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request.Builder requestBuilder = chain.request().newBuilder();
        String token = AppStore.getInstance(context).getToken();
        if (token != null) {
            requestBuilder.addHeader("Authorization", "Bearer " + token);
        }
        return chain.proceed(requestBuilder.build());
    }
}
