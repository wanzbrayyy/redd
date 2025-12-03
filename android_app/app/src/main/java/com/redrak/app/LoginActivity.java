package com.redrak.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private OkHttpClient client;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progress);
        
        client = ApiClient.getClient(this);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        etEmail.setError(null);
        etPassword.setError(null);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email is required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }

        setLoading(true);

        JSONObject payload = new JSONObject();
        try {
            payload.put("email", email);
            payload.put("password", password);
        } catch (Exception e) {
            showError("Internal error");
            setLoading(false);
            return;
        }

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request req = new Request.Builder().url(ApiClient.BASE_URL + "/login").post(body).build();

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
                            showError(o.optString("message", "Login failed"));
                            return;
                        }
                        
                        String token = o.optString("token");
                        JSONObject profile = o.optJSONObject("profile");
                        AppStore.getInstance(LoginActivity.this).setToken(token);
                        AppStore.getInstance(LoginActivity.this).setProfile(profile.toString());

                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } catch (Exception e) {
                        showError("Failed to parse server response.");
                    }
                });
            }
        });
    }
    
    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
