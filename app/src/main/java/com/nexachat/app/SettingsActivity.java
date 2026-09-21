package com.nexachat.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nexachat.app.databinding.ActivitySettingsBinding;
import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.security.DisguiseManager;
import com.nexachat.app.security.HiddenChatManager;
import com.nexachat.app.security.SecurityHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences settingsPrefs;
    private HiddenChatManager hiddenChatManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        settingsPrefs = getSharedPreferences("nexachat_settings", Context.MODE_PRIVATE);
        hiddenChatManager = HiddenChatManager.getInstance(this);

        setupUI();
        loadPreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHiddenChatsBadge();
        updateCamouflageStatus();
    }

    private void updateCamouflageStatus() {
        DisguiseManager dm = DisguiseManager.getInstance();
        if (dm.isDisguiseEnabled(this)) {
            String name = dm.getDisguisedAppName(this);
            binding.tvCamouflageSettingStatus.setText("Active: Disguised as " + name + " • Long-press for NexaChat");
            binding.tvCamouflageSettingStatus.setTextColor(getColor(R.color.cyan_accent));
        } else {
            binding.tvCamouflageSettingStatus.setText("Disguise as another app or game • Long-press Face & Fingerprint unlock");
            binding.tvCamouflageSettingStatus.setTextColor(getColor(R.color.text_secondary));
        }
    }

    private void updateHiddenChatsBadge() {
        int count = hiddenChatManager.getHiddenChatCount();
        binding.tvHiddenBadge.setText(String.valueOf(count));
        if (count > 0) {
            binding.tvHiddenChatsDesc.setText(count + " private conversation" + (count == 1 ? "" : "s") + " hidden");
        } else {
            binding.tvHiddenChatsDesc.setText("View and unhide private hidden conversations");
        }
    }

    private void setupUI() {
        binding.btnSettingsBack.setOnClickListener(v -> finish());

        // Hidden Chats
        binding.btnUnhideChats.setOnClickListener(v -> {
            if (!hiddenChatManager.hasPassword()) {
                if (hiddenChatManager.getHiddenChatCount() == 0) {
                    Toast.makeText(this, "No hidden chats yet. Long-press any chat on the home screen to hide it.", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            showEnterHiddenPasswordDialog();
        });

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

        binding.btnAppCamouflageSetting.setOnClickListener(v -> {
            AppCamouflageBottomSheet camouflageSheet = AppCamouflageBottomSheet.newInstance();
            camouflageSheet.show(getSupportFragmentManager(), "app_camouflage");
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

    private void showEnterHiddenPasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_enter_hidden_password, null);
        EditText etEnterPassword = dialogView.findViewById(R.id.etEnterPassword);
        TextView tvError = dialogView.findViewById(R.id.tvEnterPasswordError);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelEnterPassword);
        Button btnVerify = dialogView.findViewById(R.id.btnVerifyEnterPassword);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnVerify.setOnClickListener(v -> {
            String input = etEnterPassword.getText().toString().trim();
            if (TextUtils.isEmpty(input)) {
                tvError.setText("Please enter your password");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            if (hiddenChatManager.verifyPassword(input)) {
                dialog.dismiss();
                startActivity(new Intent(SettingsActivity.this, HiddenChatsActivity.class));
            } else {
                tvError.setText("Incorrect password. Please try again.");
                tvError.setVisibility(View.VISIBLE);
                etEnterPassword.setText("");
            }
        });

        dialog.show();
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
