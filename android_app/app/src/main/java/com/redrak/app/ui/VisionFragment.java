package com.redrak.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.redrak.app.ApiClient;
import com.redrak.app.adapters.SimpleTextAdapter;
import com.redrak.app.databinding.FragmentVisionBinding;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VisionFragment extends Fragment {

    private FragmentVisionBinding binding;
    private SimpleTextAdapter adapter;
    private OkHttpClient client;
    private final List<String> resultList = new ArrayList<>();
    private JSONArray detectedFacesJson = new JSONArray();

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                if (imageUri != null) {
                    binding.ivPreview.setImageURI(imageUri);
                    analyzeFace(imageUri);
                }
            }
        }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentVisionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        client = ApiClient.getClient(requireContext());
        adapter = new SimpleTextAdapter(resultList);
        binding.rvFaceResults.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvFaceResults.setAdapter(adapter);

        binding.btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });
    }

    private void analyzeFace(Uri imageUri) {
        File file = getFileFromUri(imageUri);
        if (file == null) {
            Toast.makeText(getContext(), "Could not read image file", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        resultList.clear();
        detectedFacesJson = new JSONArray();
        binding.ivPreview.setFaces(detectedFacesJson);
        adapter.notifyDataSetChanged();

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(),
                        RequestBody.create(file, MediaType.parse("image/jpeg")))
                .build();

        Request request = new Request.Builder()
                .url(ApiClient.BASE_URL + "/analyze/face")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    setLoading(false);
                    resultList.add("Error: " + e.getMessage());
                    adapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (getActivity() == null) return;
                final String body = response.body().string();
                getActivity().runOnUiThread(() -> {
                    setLoading(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(body);
                        parseApiResponse(jsonResponse);
                        binding.ivPreview.setFaces(detectedFacesJson);
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        resultList.add("Error parsing response.");
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    private void parseApiResponse(JSONObject response) {
        Iterator<String> providerKeys = response.keys();
        if (!providerKeys.hasNext()) {
            resultList.add("Invalid response structure from server.");
            return;
        }
        
        String provider = providerKeys.next();
        JSONObject providerData = response.optJSONObject(provider);
        if(providerData == null) {
            resultList.add("No valid provider data found.");
            return;
        }
        
        JSONArray items = providerData.optJSONArray("items");
        if(items == null || items.length() == 0) {
            resultList.add("No faces detected.");
            return;
        }

        this.detectedFacesJson = items;

        resultList.add("Detected " + items.length() + " face(s) via " + provider + ":");
        for (int i = 0; i < items.length(); i++) {
            JSONObject face = items.optJSONObject(i);
            if(face == null) continue;
            
            resultList.add("\n--- Face " + (i + 1) + " ---");
            resultList.add(String.format("Confidence: %.2f%%", face.optDouble("confidence", 0.0) * 100));
            
            JSONArray emotions = face.optJSONArray("emotions");
            if (emotions != null) resultList.add("Emotion: " + getHighestAttribute(emotions));
            
            JSONArray ages = face.optJSONArray("age");
            if (ages != null && ages.length() > 0) resultList.add("Age Range: " + ages.optJSONObject(0).optString("attribute"));
            
            JSONArray genders = face.optJSONArray("gender");
            if(genders != null) resultList.add("Gender: " + getHighestAttribute(genders));
        }
    }
    
    private String getHighestAttribute(JSONArray array) {
        double maxConfidence = -1;
        String dominantAttribute = "Unknown";
        if(array == null) return dominantAttribute;

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null) {
                double confidence = obj.optDouble("confidence", 0.0);
                if (confidence > maxConfidence) {
                    maxConfidence = confidence;
                    dominantAttribute = obj.optString("attribute", "Unknown");
                }
            }
        }
        return String.format("%s (%.1f%%)", dominantAttribute, maxConfidence * 100);
    }

    private void setLoading(boolean isLoading) {
        binding.visionProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnSelectImage.setEnabled(!isLoading);
    }
    
    private File getFileFromUri(Uri uri) {
        if (getContext() == null) return null;
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if(inputStream == null) return null;
            File tempFile = File.createTempFile("face", ".jpg", requireContext().getCacheDir());
            tempFile.deleteOnExit();
            try (OutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }
            inputStream.close();
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
