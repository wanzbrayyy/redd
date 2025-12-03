package com.redrak.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

public class DashboardActivity extends AppCompatActivity {
    private TextView tvWelcome;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        tvWelcome = findViewById(R.id.tvWelcome);
        ImageView ivProfile = findViewById(R.id.ivProfile);
        Button btnLogout = findViewById(R.id.btnLogout);

        ivProfile.setImageDrawable(getResources().getDrawable(R.drawable.ic_profile_default));
        btnLogout.setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWelcomeMessage();
    }

    private void loadWelcomeMessage() {
        String profileJson = AppStore.getInstance(this).getProfile();
        String name = "User";
        if (profileJson != null) {
            try {
                JSONObject p = new JSONObject(profileJson);
                name = p.optString("name", name);
            } catch (Exception ignored) {}
        }
        tvWelcome.setText("Welcome, " + name);
    }

    private void logout() {
        AppStore.getInstance(this).clear();
        Intent it = new Intent(this, LoginActivity.class);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(it);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}