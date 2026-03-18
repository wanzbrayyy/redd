package com.redrak.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.redrak.app.ApiClient;
import com.redrak.app.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VisionFragment extends Fragment {

    private ImageView ivPreview;
    private TextView tvResult;
    private ProgressBar progressBar;
    private OkHttpClient client;
    private String currentAction = "";

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        ivPreview.setImageURI(imageUri);
                        uploadImageForAnalysis(imageUri);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_vision, container, false);

        ivPreview = view.findViewById(R.id.ivPreview);
        tvResult = view.findViewById(R.id.tvResult);
        progressBar = view.findViewById(R.id.progressVision);
        client = ApiClient.getClient(requireContext());

        view.findViewById(R.id.btnOcr).setOnClickListener(v -> selectImage("ocr"));
        view.findViewById(R.id.btnQr).setOnClickListener(v -> selectImage("qr"));
        view.findViewById(R.id.btnExif).setOnClickListener(v -> selectImage("exif"));
        view.findViewById(R.id.btnDeepfake).setOnClickListener(v -> selectImage("deepfake"));

        return view;
    }

    private void selectImage(String action) {
        currentAction = action;
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void uploadImageForAnalysis(Uri uri) {
        File file = getFileFromUri(uri);
        if (file == null) return;

        progressBar.setVisibility(View.VISIBLE);
        tvResult.setText("Analyzing...");

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(), RequestBody.create(file, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(ApiClient.BASE_URL + "/vision/" + currentAction)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvResult.setText("Error: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String resp = response.body().string();
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvResult.setText(resp);
                });
            }
        });
    }

    private File getFileFromUri(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            File tempFile = File.createTempFile("vision", ".jpg", requireContext().getCacheDir());
            tempFile.deleteOnExit();
            try (OutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            inputStream.close();
            return tempFile;
        } catch (IOException e) {
            return null;
        }
    }
}