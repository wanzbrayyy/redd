package com.redrak.app;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String token = AppStore.getInstance().getToken();
        if (token == null) startActivity(new Intent(this, LoginActivity.class));
        else startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }
}