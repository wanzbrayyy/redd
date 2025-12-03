package com.redrak.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.redrak.app.AppStore;
import com.redrak.app.LoginActivity;
import com.redrak.app.ProfileActivity;
import com.redrak.app.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {
    
    private FragmentSettingsBinding binding;
    private static final int ORBOT_REQUEST_CODE = 123;
    private static final String ORBOT_PACKAGE_NAME = "org.torproject.android";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnMyProfile.setOnClickListener(v -> 
            startActivity(new Intent(requireActivity(), ProfileActivity.class))
        );
        
        binding.btnLogout.setOnClickListener(v -> {
            AppStore.getInstance(requireContext()).clear();
            Intent it = new Intent(requireActivity(), LoginActivity.class);
            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(it);
            requireActivity().finish();
        });

        binding.switchTor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startOrbotVpn();
            } else {
                stopOrbotVpn();
            }
        });
    }
    
    private void startOrbotVpn() {
        Intent intent = new Intent("org.torproject.android.intent.action.START");
        intent.setPackage(ORBOT_PACKAGE_NAME);
        intent.putExtra("org.torproject.android.intent.extra.PACKAGE_NAME", requireContext().getPackageName());
        try {
            startActivityForResult(intent, ORBOT_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Orbot is not installed. Please install it.", Toast.LENGTH_LONG).show();
            if (binding != null) binding.switchTor.setChecked(false);
        }
    }

    private void stopOrbotVpn() {
        Intent intent = new Intent("org.torproject.android.intent.action.STOP");
        intent.setPackage(ORBOT_PACKAGE_NAME);
        try {
            requireContext().sendBroadcast(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to stop Orbot.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
