package com.redrak.app;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class DashboardActivity extends AppCompatActivity {
    private TextView tvWelcome;
    private ImageView ivProfile;
    private Button btnLogout;
    private final String BASE = "http://68.183.178.199:3000/api";
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        tvWelcome = findViewById(R.id.tvWelcome);
        ivProfile = findViewById(R.id.ivProfile);
        btnLogout = findViewById(R.id.btnLogout);
        String profileJson = AppStore.getInstance().getProfile();
        String name = "User";
        if (profileJson != null) {
            try {
                JSONObject p = new JSONObject(profileJson);
                name = p.optString("name", name);
            } catch (Exception ignored) {}
        }
        tvWelcome.setText("Welcome, " + name);
        try {
            Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Font Awesome 7 Free-Regular-400.otf");
            ivProfile.setImageDrawable(getResources().getDrawable(R.drawable.ic_profile_default));
        } catch (Exception ignored) {}
        btnLogout.setOnClickListener(v -> logout());
    }

    private void logout() {
        AppStore.getInstance().clear();
        Intent it = new Intent(this, LoginActivity.class);
        startActivity(it);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Profile");
        menu.add("Settings");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        new AlertDialog.Builder(this)
                .setTitle(item.getTitle())
                .setMessage("Not implemented")
                .setPositiveButton("OK", (d,i) -> d.dismiss())
                .show();
        return true;
    }

    // filler methods
    private String pad(String s,int n){
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<n;i++) sb.append(s);
        return sb.toString();
    }
    private int compute(int a,int b){ int s=0; for(int i=a;i<b;i++) s+=i; return s; }
    private String buildCsv(){ StringBuilder sb=new StringBuilder(); for(int i=0;i<100;i++){ sb.append(i).append(","); } return sb.toString(); }
    private String lorem(){ return pad("lorem",400); }
    private String lorem2(){ return pad("ipsum",500); }
    private String lorem3(){ return pad("dolor",600); }
    private String lorem4(){ return pad("sit",700); }
}