package com.nexachat.app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.nexachat.app.databinding.ActivityForgotPasswordBinding;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.ivForgotBack.setOnClickListener(v -> finish());
        binding.btnSendResetLink.setOnClickListener(v -> sendPasswordReset());
    }

    private void sendPasswordReset() {
        String input = binding.etForgotEmail.getText().toString().trim();

        if (TextUtils.isEmpty(input)) {
            binding.etForgotEmail.setError(getString(R.string.error_empty_field));
            binding.etForgotEmail.requestFocus();
            return;
        }

        setLoading(true);

        if (input.contains("@")) {
            FirebaseAuth.getInstance().sendPasswordResetEmail(input)
                    .addOnCompleteListener(task -> {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            binding.tvForgotStatus.setText("Password reset link has been sent to " + input + ". Please check your inbox or spam folder.");
                            binding.tvForgotStatus.setTextColor(getColor(R.color.cyan_accent));
                            binding.tvForgotStatus.setVisibility(View.VISIBLE);
                            binding.btnSendResetLink.setEnabled(false);
                            Toast.makeText(ForgotPasswordActivity.this, "Reset link sent!", Toast.LENGTH_SHORT).show();
                        } else {
                            String errorMsg = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email.";
                            binding.tvForgotStatus.setText(errorMsg);
                            binding.tvForgotStatus.setTextColor(getColor(R.color.error_red));
                            binding.tvForgotStatus.setVisibility(View.VISIBLE);
                        }
                    });
        } else {
            String cleanPhone = com.nexachat.app.firebase.FirebaseManager.cleanPhoneNumber(input);
            com.nexachat.app.firebase.FirebaseManager.getInstance().getPhoneNumberRef(cleanPhone)
                    .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                        @Override
                        public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                            setLoading(false);
                            if (snapshot.exists()) {
                                String username = snapshot.child("username").getValue(String.class);
                                binding.tvForgotStatus.setText("Account found for @" + username + " (" + input + "). You can log in using your 4-6 digit security PIN.");
                                binding.tvForgotStatus.setTextColor(getColor(R.color.cyan_accent));
                                binding.tvForgotStatus.setVisibility(View.VISIBLE);
                            } else {
                                binding.tvForgotStatus.setText("No account found registered with phone number " + input + ". Please check and try again.");
                                binding.tvForgotStatus.setTextColor(getColor(R.color.error_red));
                                binding.tvForgotStatus.setVisibility(View.VISIBLE);
                            }
                        }

                        @Override
                        public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                            setLoading(false);
                            binding.tvForgotStatus.setText("Database error: " + error.getMessage());
                            binding.tvForgotStatus.setTextColor(getColor(R.color.error_red));
                            binding.tvForgotStatus.setVisibility(View.VISIBLE);
                        }
                    });
        }
    }

    private void setLoading(boolean isLoading) {
        binding.btnSendResetLink.setEnabled(!isLoading);
        binding.btnSendResetLink.setText(isLoading ? "" : getString(R.string.btn_send_reset));
        binding.pbForgotLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
