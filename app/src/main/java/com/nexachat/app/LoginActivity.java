package com.nexachat.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.nexachat.app.databinding.ActivityLoginBinding;
import com.nexachat.app.firebase.FirebaseManager;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If already logged in, redirect directly to MainActivity
        if (FirebaseManager.getInstance().isUserLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
    }

    private void setupListeners() {
        binding.ivTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                binding.etLoginPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility_off);
            } else {
                binding.etLoginPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility);
            }
            binding.etLoginPassword.setSelection(binding.etLoginPassword.length());
        });

        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        binding.tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(this, ForgotPasswordActivity.class));
        });

        binding.tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void attemptLogin() {
        String phoneInput = binding.etLoginPhone.getText().toString().trim();
        String password = binding.etLoginPassword.getText().toString().trim();

        binding.tvLoginError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(phoneInput)) {
            binding.etLoginPhone.setError(getString(R.string.error_empty_field));
            binding.etLoginPhone.requestFocus();
            return;
        }

        String cleanPhone = FirebaseManager.cleanPhoneNumber(phoneInput);
        if (!phoneInput.contains("@") && (TextUtils.isEmpty(cleanPhone) || cleanPhone.length() < 7)) {
            binding.etLoginPhone.setError("Please enter a valid phone number.");
            binding.etLoginPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.etLoginPassword.setError(getString(R.string.error_empty_field));
            binding.etLoginPassword.requestFocus();
            return;
        }

        setLoading(true);

        String internalEmail = phoneInput.contains("@") ? phoneInput : FirebaseManager.phoneToEmail(cleanPhone);
        String internalPassword = FirebaseManager.normalizePasswordOrPin(password);

        FirebaseAuth.getInstance().signInWithEmailAndPassword(internalEmail, internalPassword)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        setLoading(false);
                        FirebaseManager.getInstance().setupPresence();
                        FirebaseManager.getInstance().syncFcmToken();
                        Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // If direct attempt failed and cleanPhone was entered, check if phone_numbers index maps to another email
                        if (!phoneInput.contains("@")) {
                            checkPhoneMappingAndSignIn(cleanPhone, internalPassword, task.getException());
                        } else {
                            setLoading(false);
                            handleLoginFailure(task.getException());
                        }
                    }
                });
    }

    private void checkPhoneMappingAndSignIn(String cleanPhone, String password, Exception originalError) {
        FirebaseManager.getInstance().getPhoneNumberRef(cleanPhone).addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String uid = snapshot.child("uid").getValue(String.class);
                    // Attempt sign-in with cleanPhone email
                    String fallbackEmail = cleanPhone + "@nexachat.app";
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(fallbackEmail, password)
                            .addOnCompleteListener(LoginActivity.this, fallbackTask -> {
                                setLoading(false);
                                if (fallbackTask.isSuccessful()) {
                                    FirebaseManager.getInstance().setupPresence();
                                    FirebaseManager.getInstance().syncFcmToken();
                                    Toast.makeText(LoginActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    handleLoginFailure(fallbackTask.getException());
                                }
                            });
                } else {
                    setLoading(false);
                    handleLoginFailure(originalError);
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                setLoading(false);
                handleLoginFailure(originalError);
            }
        });
    }

    private void handleLoginFailure(Exception exception) {
        String errorMsg = "Login failed. Please check your phone number and PIN.";
        if (exception instanceof FirebaseAuthInvalidUserException) {
            errorMsg = "No account found with this phone number. Please sign up.";
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            errorMsg = "Incorrect PIN or password. Please try again.";
        } else if (exception != null && exception.getMessage() != null) {
            errorMsg = exception.getMessage();
        }
        binding.tvLoginError.setText(errorMsg);
        binding.tvLoginError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean isLoading) {
        binding.btnLogin.setEnabled(!isLoading);
        binding.btnLogin.setText(isLoading ? "" : getString(R.string.btn_login));
        binding.pbLoginLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
