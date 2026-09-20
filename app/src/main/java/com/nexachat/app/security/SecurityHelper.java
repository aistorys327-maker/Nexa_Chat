package com.nexachat.app.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nexachat.app.R;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;

public class SecurityHelper {
    private static final String PREF_NAME = "nexachat_security_prefs";
    private static final String KEY_PIN_HASH = "security_pin_hash";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_APP_VERIFY_ENABLED = "app_verify_enabled";

    public interface AuthCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isBiometricAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK);
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static boolean isBiometricEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, true);
    }

    public static void setBiometricEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public static boolean isAppVerificationEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_APP_VERIFY_ENABLED, false);
    }

    public static void setAppVerificationEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_APP_VERIFY_ENABLED, enabled).apply();
    }

    public static boolean hasPinSet(Context context) {
        return !TextUtils.isEmpty(getPrefs(context).getString(KEY_PIN_HASH, null));
    }

    public static void setPin(Context context, String rawPin) {
        String hash = hashString(rawPin);
        getPrefs(context).edit().putString(KEY_PIN_HASH, hash).apply();
    }

    public static boolean verifyPin(Context context, String rawPin) {
        String storedHash = getPrefs(context).getString(KEY_PIN_HASH, null);
        if (storedHash == null) {
            // Default pin is 1234 if not set
            return "1234".equals(rawPin);
        }
        return storedHash.equals(hashString(rawPin));
    }

    public static void authenticate(FragmentActivity activity, String title, String subtitle, AuthCallback callback) {
        Context context = activity.getApplicationContext();

        if (isBiometricAvailable(context) && isBiometricEnabled(context)) {
            Executor executor = ContextCompat.getMainExecutor(activity);
            BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            callback.onSuccess();
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            // Fallback to PIN dialog if user cancels biometric or it fails
                            showPinDialog(activity, title, callback);
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            // Biometric failed once, Android prompt handles retry
                        }
                    });

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButtonText("Use PIN")
                    .build();

            biometricPrompt.authenticate(promptInfo);
        } else {
            // Fallback directly to PIN
            showPinDialog(activity, title, callback);
        }
    }

    public static void showPinDialog(FragmentActivity activity, String title, AuthCallback callback) {
        View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_pin, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvPinDialogTitle);
        EditText etPin = dialogView.findViewById(R.id.etPinDialogInput);
        TextView tvError = dialogView.findViewById(R.id.tvPinDialogError);

        if (title != null) {
            tvTitle.setText(title);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(dialogView)
                .setPositiveButton("Verify", null) // Override later to prevent auto-close
                .setNegativeButton("Cancel", (d, which) -> {
                    d.dismiss();
                    callback.onFailure("Authentication cancelled");
                })
                .setCancelable(false)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String inputPin = etPin.getText().toString().trim();
                if (TextUtils.isEmpty(inputPin)) {
                    tvError.setText("Please enter your PIN");
                    tvError.setVisibility(View.VISIBLE);
                    return;
                }

                if (verifyPin(activity, inputPin)) {
                    dialog.dismiss();
                    callback.onSuccess();
                } else {
                    tvError.setText("Incorrect PIN. Try again.");
                    tvError.setVisibility(View.VISIBLE);
                    etPin.setText("");
                }
            });
        });

        dialog.show();
    }

    public static String hashPin(String input) {
        return hashString(input);
    }

    private static String hashString(String input) {
        if (input == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
