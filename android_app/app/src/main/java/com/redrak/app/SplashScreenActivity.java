package com.redrak.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {
@Override
protected void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
setContentView(R.layout.activity_splash);
new Handler(Looper.getMainLooper()).postDelayed(() -> {
        String token = AppStore.getInstance(this).getToken();
        Intent intent;
        if (token == null) {
            intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
        } else {
            intent = new Intent(SplashScreenActivity.this, MainActivity.class);
        }
        startActivity(intent);
        finish();
    }, 1500);
}

}
