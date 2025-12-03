package com.redrak.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {
    private EditText etName, etEmail, etPassword;
    private Button btnRegister;
    private ProgressBar progressBar;
    private OkHttpClient client;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progress);
        
        client = ApiClient.getClient(this);
        
        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        etName.setError(null);
        etEmail.setError(null);
        etPassword.setError(null);
        
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(name)) { etName.setError("Name is required"); return; }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.setError("Valid email is required"); return; }
        if (password.length() < 6) { etPassword.setError("Password must be at least 6 characters"); return; }
        
        setLoading(true);

        JSONObject payload = new JSONObject();
        try {
            payload.put("name", name);
            payload.put("email", email);
            payload.put("password", password);
        } catch (Exception e) {
            showError("Internal error");
            setLoading(false);
            return;
        }
        
        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request req = new Request.Builder().url(ApiClient.BASE_URL + "/register").post(body).build();
        
        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError("Network error: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String respBody = response.body().string();
                runOnUiThread(() -> {
                    setLoading(false);
                    try {
                        JSONObject o = new JSONObject(respBody);
                        if (!response.isSuccessful()) {
                            showError(o.optString("message", "Registration failed"));
                            return;
                        }

                        String token = o.optString("token");
                        JSONObject profile = o.optJSONObject("profile");
                        AppStore.getInstance(RegisterActivity.this).setToken(token);
                        AppStore.getInstance(RegisterActivity.this).setProfile(profile.toString());
                        
                        startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                        finishAffinity();
                    } catch (Exception e) {
                        showError("Failed to parse server response.");
                    }
                });
            }
        });
    }
    
    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
    }

    private void showError(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}