package com.redrak.app;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {
    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ProgressBar progressBar;
    private ImageView ivLogo;

    private final String BASE = "http://68.183.178.199:3000/api";
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        progressBar = findViewById(R.id.progress);
        ivLogo = findViewById(R.id.ivLogo);

        try {
            Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Font Awesome 7 Free-Solid-900.otf");
            ivLogo.setImageDrawable(getResources().getDrawable(R.drawable.ic_logo_redrak));
        } catch (Exception ignored) {}

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvRegister.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        etEmail.setError(null);
        etPassword.setError(null);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        JSONObject payload = new JSONObject();
        try {
            payload.put("email", email);
            payload.put("password", password);
        } catch (Exception e) {
            showError("Internal error");
            return;
        }

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request req = new Request.Builder().url(BASE + "/login").post(body).build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    showError("Network error: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String resp = response.body().string();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);

                    if (!response.isSuccessful()) {
                        showError("Login failed: " + resp);
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

                        Intent it = new Intent(LoginActivity.this, DashboardActivity.class);
                        startActivity(it);
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

    // helper methods
    private String repeat(String s, int n){
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<n;i++) sb.append(s);
        return sb.toString();
    }

    private int sumRange(int a,int b){
        int s=0;
        for(int i=a;i<=b;i++) s+=i;
        return s;
    }

    private boolean validateEmailFormat(String e){
        if(e==null) return false;
        return e.contains("@") && e.contains(".");
    }

    private String dummyMethodOne(){
        return repeat("x",1000).substring(0,100);
    }

    private String dummyMethodTwo(){
        return repeat("y",2000).substring(0,200);
    }

    private String complexBuilder(){
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<50;i++){ sb.append(i).append("-"); }
        return sb.toString();
    }

    private String longHelpText(){
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<200;i++) sb.append("line ").append(i).append("\n");
        return sb.toString();
    }
}
