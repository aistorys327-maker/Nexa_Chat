package com.nexachat.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nexachat.app.databinding.ActivitySettingsBinding;
import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.security.SecurityHelper;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences settingsPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        settingsPrefs = getSharedPreferences("nexachat_settings", Context.MODE_PRIVATE);

        setupUI();
        loadPreferences();
    }

    private void setupUI() {
        binding.btnSettingsBack.setOnClickListener(v -> finish());

        // Privacy Switches
        binding.switchOnlineStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsPrefs.edit().putBoolean("show_online_status", isChecked).apply();
            FirebaseManager.getInstance().setOnlineStatus(isChecked);
        });

        binding.switchLastSeen.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsPrefs.edit().putBoolean("show_last_seen", isChecked).apply();
        });

        binding.switchReadReceipts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsPrefs.edit().putBoolean("read_receipts", isChecked).apply();
        });

        binding.switchNotificationPreview.setOnCheckedChangeListener((buttonView, isChecked) -> {
            settingsPrefs.edit().putBoolean("show_notification_preview", isChecked).apply();
        });

        binding.switchAppVerification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SecurityHelper.setAppVerificationEnabled(this, isChecked);
        });

        binding.btnConfigureSecurity.setOnClickListener(v -> {
            startActivity(new Intent(this, SecurityActivity.class));
        });

        binding.btnViewPrivacyPolicy.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Privacy Policy")
                    .setMessage("NexaChat values your privacy.\n\nAll messaging data is stored within your dedicated Firebase Realtime Database. NexaChat does not collect or sell your personal data. Chat locking and app security features are enforced locally on your device with biometric and PIN protection.")
                    .setPositiveButton("Close", null)
                    .show();
        });

        binding.btnViewTerms.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Terms of Service")
                    .setMessage("By using NexaChat, you agree to respectful and safe communication. Do not use the platform for unlawful, harmful, or abusive content.")
                    .setPositiveButton("Close", null)
                    .show();
        });
    }

    private void loadPreferences() {
        boolean showOnline = settingsPrefs.getBoolean("show_online_status", true);
        boolean showLastSeen = settingsPrefs.getBoolean("show_last_seen", true);
        boolean readReceipts = settingsPrefs.getBoolean("read_receipts", true);
        boolean showPreview = settingsPrefs.getBoolean("show_notification_preview", true);
        boolean appVerification = SecurityHelper.isAppVerificationEnabled(this);

        binding.switchOnlineStatus.setChecked(showOnline);
        binding.switchLastSeen.setChecked(showLastSeen);
        binding.switchReadReceipts.setChecked(readReceipts);
        binding.switchNotificationPreview.setChecked(showPreview);
        binding.switchAppVerification.setChecked(appVerification);
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Log Out")
                .setMessage("Are you sure you want to log out of NexaChat?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    FirebaseManager.getInstance().signOut();
                    Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
