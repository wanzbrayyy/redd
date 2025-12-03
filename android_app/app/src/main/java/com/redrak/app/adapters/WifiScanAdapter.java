package com.redrak.app.adapters;

import android.net.wifi.ScanResult;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.redrak.app.databinding.ListItemWifiBinding;
import java.util.List;

public class WifiScanAdapter extends RecyclerView.Adapter<WifiScanAdapter.ViewHolder> {

    public interface OnAttackClickListener {
        void onDeauthClick(ScanResult scanResult);
        void onEvilTwinClick(ScanResult scanResult);
    }

    private final List<ScanResult> scanResults;
    private final OnAttackClickListener attackClickListener;

    public WifiScanAdapter(List<ScanResult> scanResults, OnAttackClickListener listener) {
        this.scanResults = scanResults;
        this.attackClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ListItemWifiBinding binding = ListItemWifiBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanResult result = scanResults.get(position);
        holder.bind(result, attackClickListener);
    }

    @Override
    public int getItemCount() {
        return scanResults.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ListItemWifiBinding binding;

        ViewHolder(ListItemWifiBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(final ScanResult result, final OnAttackClickListener listener) {
            String ssid = result.SSID == null || result.SSID.isEmpty() ? "<Hidden>" : result.SSID;
            binding.tvSsid.setText(ssid);
            binding.tvBssid.setText("BSSID: " + result.BSSID);
            
            int channel = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                channel = result.channelWidth; // Not exactly channel, but provides info
            }
            binding.tvStrength.setText("Strength: " + result.level + " dBm");

            binding.btnDeauth.setOnClickListener(v -> listener.onDeauthClick(result));
            binding.btnEvilTwin.setOnClickListener(v -> listener.onEvilTwinClick(result));
        }
    }
}
