package com.redrak.app.ui;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.redrak.app.ApiClient;
import com.redrak.app.databinding.FragmentAttackConsoleBinding;
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
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class AttackConsoleFragment extends Fragment {

    private FragmentAttackConsoleBinding binding;
    private OkHttpClient client;
    private WebSocket webSocket;

    private String attackType;
    private String bssid;
    private String ssid;
    private int channel;

    public static AttackConsoleFragment newInstance(String attackType, String bssid, String ssid) {
        AttackConsoleFragment fragment = new AttackConsoleFragment();
        Bundle args = new Bundle();
        args.putString("attackType", attackType);
        args.putString("bssid", bssid);
        args.putString("ssid", ssid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            attackType = getArguments().getString("attackType");
            bssid = getArguments().getString("bssid");
            ssid = getArguments().getString("ssid");
            channel = getArguments().getInt("channel", 6);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAttackConsoleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        client = ApiClient.getClient(requireContext());
        binding.tvConsoleOutput.setMovementMethod(new ScrollingMovementMethod());
        connectWebSocket();
    }

    private void connectWebSocket() {
        if (ApiClient.BASE_URL == null) return;
        String wsUrl = ApiClient.BASE_URL.replace("http", "ws").replace("/api", "");
        Request request = new Request.Builder().url(wsUrl).build();
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                updateUiOnMainThread(() -> {
                    binding.tvAttackStatus.setText("Connection Established. Starting attack...");
                    initiateAttack();
                });
            }

            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                updateUiOnMainThread(() -> {
                    try {
                        JSONObject message = new JSONObject(text);
                        String type = message.optString("type");
                        
                        if ("console".equals(type)) {
                            binding.tvConsoleOutput.append(message.optString("data"));
                        } else if ("password".equals(type)) {
                            JSONObject data = message.optJSONObject("data");
                            binding.passwordLayout.setVisibility(View.VISIBLE);
                            binding.tvCapturedPassword.setText(data.optString("password"));
                            binding.tvAttackStatus.setText("Password Captured for " + data.optString("ssid") + "!");
                        }
                    } catch (JSONException e) {
                        binding.tvConsoleOutput.append(text);
                    }
                });
            }
            
            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                 updateUiOnMainThread(() -> binding.tvAttackStatus.setText("Connection closing..."));
            }

            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                updateUiOnMainThread(() -> binding.tvAttackStatus.setText("Connection Failed: " + t.getMessage()));
            }
        });
    }

    private void initiateAttack() {
        String url;
        JSONObject payload = new JSONObject();
        
        try {
            switch (attackType) {
                case "deauth":
                    url = ApiClient.BASE_URL + "/network/deauth";
                    payload.put("bssid", bssid);
                    binding.tvAttackStatus.setText("Executing Deauth on " + bssid);
                    break;
                case "evil-twin":
                    url = ApiClient.BASE_URL + "/network/evil-twin";
                    payload.put("bssid", bssid);
                    payload.put("ssid", ssid);
                    payload.put("channel", channel);
                    binding.tvAttackStatus.setText("Executing Evil Twin on " + ssid);
                    break;
                case "sniff":
                    url = ApiClient.BASE_URL + "/network/sniff";
                    binding.tvAttackStatus.setText("Executing Packet Sniffing...");
                    break;
                default:
                    return;
            }
        } catch (JSONException e) {
            return;
        }

        RequestBody body = RequestBody.create(payload.toString(), MediaType.parse("application/json"));
        Request request = new Request.Builder().url(url).post(body).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {}
        });
    }

    private void stopAttack() {
        Request request = new Request.Builder().url(ApiClient.BASE_URL + "/network/stop-attack").post(RequestBody.create(new byte[0])).build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {}
        });
    }

    private void updateUiOnMainThread(Runnable task) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(task);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webSocket != null) {
            webSocket.close(1000, "Fragment destroyed");
        }
        stopAttack();
        binding = null;
    }
}
