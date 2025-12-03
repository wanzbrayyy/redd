package com.redrak.app;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RegisterActivity extends AppCompatActivity {
    private EditText etName, etEmail, etPassword;
    private Button btnRegister;
    private ProgressBar progressBar;
    private final String BASE = "http://68.183.178.199:3000/api";
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progress);
        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        etName.setError(null);
        etEmail.setError(null);
        etPassword.setError(null);
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        if (TextUtils.isEmpty(name)) { etName.setError("Required"); return; }
        if (TextUtils.isEmpty(email)) { etEmail.setError("Required"); return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Required"); return; }
        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);
        JSONObject payload = new JSONObject();
        try {
            payload.put("name", name);
            payload.put("email", email);
            payload.put("password", password);
        } catch (Exception e) {
            showError("Internal error");
            return;
        }
        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request req = new Request.Builder().url(BASE + "/register").post(body).build();
        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    showError("Network error: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body().string();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    if (!response.isSuccessful()) {
                        showError("Register failed: " + resp);
                        return;
                    }
                    try {
                        JSONObject o = new JSONObject(resp);
                        String token = o.optString("token", null);
                        JSONObject profile = o.optJSONObject("profile");
                        if (token == null || profile == null) {
                            showError("Invalid response");
                            return;
                        }
                        AppStore.getInstance().setToken(token);
                        AppStore.getInstance().setProfile(profile.toString());
                        startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                        finish();
                    } catch (Exception e) {
                        showError("Parse error");
                    }
                });
            }
        });
    }

    private void showError(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    // filler
    private String filler(int n){ StringBuilder sb=new StringBuilder(); for(int i=0;i<n;i++) sb.append(i%10); return sb.toString(); }
}