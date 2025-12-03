package com.redrak.app.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.redrak.app.R;
import com.redrak.app.adapters.WifiScanAdapter;
import com.redrak.app.databinding.FragmentNetworkBinding;

import java.util.ArrayList;
import java.util.List;

public class NetworkFragment extends Fragment implements WifiScanAdapter.OnAttackClickListener {

    private FragmentNetworkBinding binding;
    private WifiManager wifiManager;
    private final List<ScanResult> wifiResultList = new ArrayList<>();
    
    private final ActivityResultLauncher<String[]> locationPermissionRequest =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
            if (Boolean.TRUE.equals(fineLocationGranted)) {
                checkWifiStateAndScan();
            } else {
                Toast.makeText(getContext(), "Location permission is required to scan for WiFi networks.", Toast.LENGTH_LONG).show();
            }
        });

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (getActivity() == null) return;
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            try {
                getActivity().unregisterReceiver(this);
            } catch (Exception e) {}
            if (success) {
                scanWifiSuccess();
            } else {
                scanWifiFailure();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNetworkBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.rvNetworkResults.setLayoutManager(new LinearLayoutManager(getContext()));
        if (getContext() != null) {
            wifiManager = (WifiManager) requireContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }
        
        binding.btnScanWifi.setOnClickListener(v -> checkPermissionsAndScanWifi());
        binding.btnScanDevices.setOnClickListener(v -> showSniffingOption());
    }

    private void showSniffingOption() {
         if (getActivity() != null) {
            new AlertDialog.Builder(requireContext())
                .setTitle("Packet Sniffing")
                .setMessage("This will start a live packet capture on the remote host. Do you want to proceed?")
                .setPositiveButton("Start Sniffing", (dialog, which) -> {
                     getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, AttackConsoleFragment.newInstance("sniff", "", ""))
                        .addToBackStack(null)
                        .commit();
                })
                .setNegativeButton("Cancel", null)
                .show();
        }
    }

    private void checkPermissionsAndScanWifi() {
        if (getContext() != null && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkWifiStateAndScan();
        } else {
            locationPermissionRequest.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private void checkWifiStateAndScan() {
        if (wifiManager == null || getContext() == null) return;
        if (!wifiManager.isWifiEnabled()) {
            new AlertDialog.Builder(requireContext())
                .setTitle("WiFi Disabled")
                .setMessage("WiFi must be enabled to scan for networks. Enable it now?")
                .setPositiveButton("Enable", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startActivity(new Intent(Settings.Panel.ACTION_WIFI));
                    } else {
                        wifiManager.setWifiEnabled(true);
                        Toast.makeText(getContext(), "WiFi enabled. Please scan again.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        } else {
            startWifiScan();
        }
    }

    private void startWifiScan() {
        if (getContext() == null) return;
        setLoading(true, "Scanning for WiFi...");
        wifiResultList.clear();
        WifiScanAdapter wifiAdapter = new WifiScanAdapter(wifiResultList, this);
        binding.rvNetworkResults.setAdapter(wifiAdapter);
        
        IntentFilter intentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        requireContext().registerReceiver(wifiScanReceiver, intentFilter);
        
        if (!wifiManager.startScan()) {
            scanWifiFailure();
        }
    }

    private void scanWifiSuccess() {
        wifiResultList.clear();
        List<ScanResult> results = wifiManager.getScanResults();
        wifiResultList.addAll(results);
        if (binding.rvNetworkResults.getAdapter() != null) {
            binding.rvNetworkResults.getAdapter().notifyDataSetChanged();
        }
        setLoading(false, wifiResultList.isEmpty() ? "No WiFi networks found." : "");
    }

    private void scanWifiFailure() {
        try {
            if (getContext() != null) requireContext().unregisterReceiver(wifiScanReceiver);
        } catch (Exception e) {}
        wifiResultList.clear();
        if (binding != null && binding.rvNetworkResults.getAdapter() != null) {
            binding.rvNetworkResults.getAdapter().notifyDataSetChanged();
        }
        setLoading(false, "WiFi scan failed. Ensure WiFi and Location are enabled.");
    }
    
    @Override
    public void onDeauthClick(ScanResult scanResult) {
        if (getActivity() == null) return;
        new AlertDialog.Builder(requireContext())
            .setTitle("Confirm Deauth Attack")
            .setMessage("This will start a continuous deauthentication attack against " + scanResult.BSSID + ". Proceed?")
            .setPositiveButton("Proceed", (dialog, which) -> {
                getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, AttackConsoleFragment.newInstance("deauth", scanResult.BSSID, scanResult.SSID))
                    .addToBackStack(null)
                    .commit();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    @Override
    public void onEvilTwinClick(ScanResult scanResult) {
        if (getActivity() == null) return;
        new AlertDialog.Builder(requireContext())
            .setTitle("Confirm Evil Twin Attack")
            .setMessage("This will create a fake access point named '" + scanResult.SSID + "' and attempt to capture the password. This is highly disruptive. Proceed?")
            .setPositiveButton("Proceed", (dialog, which) -> {
                getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, AttackConsoleFragment.newInstance("evil-twin", scanResult.BSSID, scanResult.SSID))
                    .addToBackStack(null)
                    .commit();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void setLoading(boolean isLoading, String statusText) {
        if (binding == null) return;
        binding.networkProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.tvNetworkStatus.setText(statusText);
        binding.tvNetworkStatus.setVisibility(isLoading || !statusText.isEmpty() ? View.VISIBLE : View.GONE);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        try {
            if (getContext() != null) requireContext().unregisterReceiver(wifiScanReceiver);
        } catch (Exception e) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
