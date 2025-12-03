package com.redrak.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import coil.Coil;
import coil.request.ImageRequest;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivProfileAvatar;
    private TextView tvProfileEmail;
    private EditText etProfileName;
    private Button btnChangeAvatar, btnSaveProfile;
    private ProgressBar progressBar;

    private OkHttpClient client;
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                ivProfileAvatar.setImageURI(imageUri);
                uploadAvatar(imageUri);
            }
        }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        etProfileName = findViewById(R.id.etProfileName);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        progressBar = findViewById(R.id.progress);

        client = ApiClient.getClient(this);
        
        btnChangeAvatar.setOnClickListener(v -> openGallery());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        
        loadUserProfile();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSaveProfile.setEnabled(!show);
        btnChangeAvatar.setEnabled(!show);
    }

    private void loadUserProfile() {
        showLoading(true);
        Request request = new Request.Builder().url(ApiClient.BASE_URL + "/profile").get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError("Failed to load profile: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                runOnUiThread(() -> {
                    showLoading(false);
                    if (response.isSuccessful()) {
                        try {
                            JSONObject profile = new JSONObject(responseBody);
                            etProfileName.setText(profile.optString("name"));
                            tvProfileEmail.setText("Email: " + profile.optString("email"));
                            String avatarUrl = profile.optString("avatarUrl", null);
                            if(avatarUrl != null && !avatarUrl.isEmpty()){
                                ImageRequest imageRequest = new ImageRequest.Builder(ProfileActivity.this)
                                        .data(avatarUrl)
                                        .target(ivProfileAvatar)
                                        .error(R.drawable.ic_profile_default)
                                        .build();
                                Coil.imageLoader(ProfileActivity.this).enqueue(imageRequest);
                            }
                        } catch (JSONException e) {
                            showError("Failed to parse profile data.");
                        }
                    } else {
                        showError("Failed to load profile: " + responseBody);
                    }
                });
            }
        });
    }

    private void saveProfile() {
        String name = etProfileName.getText().toString().trim();
        if (name.isEmpty()) {
            etProfileName.setError("Name cannot be empty");
            return;
        }
        showLoading(true);
        
        JSONObject payload = new JSONObject();
        try {
            payload.put("name", name);
        } catch (JSONException e) {
            showLoading(false);
            return;
        }

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder().url(ApiClient.BASE_URL + "/profile").put(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showLoading(false);
                    showError("Update failed: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                runOnUiThread(() -> {
                    showLoading(false);
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        Toast.makeText(ProfileActivity.this, json.getString("message"), Toast.LENGTH_SHORT).show();
                        if (response.isSuccessful()) {
                            AppStore.getInstance(ProfileActivity.this).setProfile(json.getJSONObject("profile").toString());
                        }
                    } catch (JSONException e) {
                        showError("Failed to parse response.");
                    }
                });
            }
        });
    }

    private void uploadAvatar(Uri imageUri) {
        File file = getFileFromUri(imageUri);
        if(file == null){
            showError("Failed to process image file.");
            return;
        }

        showLoading(true);
        
        RequestBody requestBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("avatar", file.getName(),
                    RequestBody.create(file, MediaType.parse("image/jpeg")))
            .build();

        Request request = new Request.Builder()
            .url(ApiClient.BASE_URL + "/profile/avatar")
            .post(requestBody)
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                 runOnUiThread(() -> {
                    showLoading(false);
                    showError("Upload failed: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                runOnUiThread(() -> {
                    showLoading(false);
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        Toast.makeText(ProfileActivity.this, json.getString("message"), Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        showError("Failed to parse response.");
                    }
                });
            }
        });
    }

    private File getFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File tempFile = File.createTempFile("avatar", ".jpg", getCacheDir());
            tempFile.deleteOnExit();
            OutputStream out = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            out.close();
            inputStream.close();
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}