package com.nexachat.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nexachat.app.databinding.ActivitySecurityBinding;
import com.nexachat.app.security.ChatLockManager;
import com.nexachat.app.security.SecurityHelper;

public class SecurityActivity extends AppCompatActivity {

    private ActivitySecurityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySecurityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
    }

    private void setupUI() {
        binding.btnSecurityBack.setOnClickListener(v -> finish());

        boolean isBiometricSupported = SecurityHelper.isBiometricAvailable(this);
        if (!isBiometricSupported) {
            binding.switchBiometric.setEnabled(false);
            binding.switchBiometric.setChecked(false);
        } else {
            binding.switchBiometric.setChecked(SecurityHelper.isBiometricEnabled(this));
            binding.switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SecurityHelper.setBiometricEnabled(this, isChecked);
                Toast.makeText(this, isChecked ? "Biometric authentication enabled" : "Biometric authentication disabled", Toast.LENGTH_SHORT).show();
            });
        }

        binding.btnTestBiometric.setOnClickListener(v -> {
            SecurityHelper.authenticate(this, "Biometric Test", "Verify biometric sensor response",
                    new SecurityHelper.AuthCallback() {
                        @Override
                        public void onSuccess() {
                            binding.tvSecurityStatus.setText("Biometric verification successful!");
                            binding.tvSecurityStatus.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            binding.tvSecurityStatus.setText("Verification failed: " + errorMessage);
                            binding.tvSecurityStatus.setVisibility(View.VISIBLE);
                        }
                    });
        });

        binding.btnSavePin.setOnClickListener(v -> {
            String pin = binding.etSecurityPin.getText().toString().trim();
            if (pin.length() >= 4) {
                SecurityHelper.setPin(this, pin);
                binding.etSecurityPin.setText("");
                binding.tvSecurityStatus.setText("Security PIN updated successfully!");
                binding.tvSecurityStatus.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Security PIN updated", Toast.LENGTH_SHORT).show();
            } else {
                binding.etSecurityPin.setError("PIN must be 4 digits");
            }
        });
    }
}
