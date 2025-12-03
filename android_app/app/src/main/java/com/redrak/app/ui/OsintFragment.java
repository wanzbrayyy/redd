package com.redrak.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.redrak.app.ApiClient;
import com.redrak.app.adapters.OsintResultAdapter;
import com.redrak.app.databinding.FragmentOsintBinding;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OsintFragment extends Fragment {

    private FragmentOsintBinding binding;
    private OkHttpClient client;
    private OsintResultAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOsintBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        client = ApiClient.getClient(getContext());
        setupRecyclerView();

        binding.btnSearch.setOnClickListener(v -> performSearch());
        binding.etSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }
    
    private void setupRecyclerView() {
        adapter = new OsintResultAdapter(getContext());
        binding.rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvResults.setAdapter(adapter);
    }

    private void performSearch() {
        String query = binding.etSearchQuery.getText().toString().trim();
        if (query.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a search query", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        JSONObject payload = new JSONObject();
        try {
            payload.put("query", query);
        } catch (JSONException e) {
            e.printStackTrace();
            showLoading(false);
            return;
        }
        
        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(ApiClient.BASE_URL + "/search/osint")
                .post(body)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    binding.tvStatus.setText("Network Error: " + e.getMessage());
                    binding.tvStatus.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (getActivity() == null) return;
                final String responseBody = response.body().string();
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        if (response.isSuccessful()) {
                            if (jsonResponse.has("List") && jsonResponse.getJSONObject("List").length() > 0) {
                                binding.rvResults.setVisibility(View.VISIBLE);
                                binding.tvStatus.setVisibility(View.GONE);
                                adapter.updateData(jsonResponse.getJSONObject("List"));
                            } else {
                                binding.tvStatus.setText("No results found for your query.");
                                binding.tvStatus.setVisibility(View.VISIBLE);
                                binding.rvResults.setVisibility(View.GONE);
                            }
                        } else {
                            binding.tvStatus.setText("Error: " + jsonResponse.optString("message", "Unknown error"));
                            binding.tvStatus.setVisibility(View.VISIBLE);
                        }
                    } catch (JSONException e) {
                        binding.tvStatus.setText("Failed to parse server response.");
                        binding.tvStatus.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }
    
    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnSearch.setEnabled(!isLoading);
        if (isLoading) {
            binding.tvStatus.setVisibility(View.GONE);
            binding.rvResults.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
