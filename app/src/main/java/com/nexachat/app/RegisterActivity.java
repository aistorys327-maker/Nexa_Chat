package com.nexachat.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.nexachat.app.databinding.ActivityRegisterBinding;
import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.models.User;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
    }

    private void setupListeners() {
        binding.ivRegisterBack.setOnClickListener(v -> finish());
        binding.tvGoToLogin.setOnClickListener(v -> finish());

        binding.ivToggleRegPassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                binding.etRegisterPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                binding.ivToggleRegPassword.setImageResource(R.drawable.ic_visibility_off);
            } else {
                binding.etRegisterPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                binding.ivToggleRegPassword.setImageResource(R.drawable.ic_visibility);
            }
            binding.etRegisterPassword.setSelection(binding.etRegisterPassword.length());
        });

        binding.ivToggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            if (isConfirmPasswordVisible) {
                binding.etRegisterConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                binding.ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_off);
            } else {
                binding.etRegisterConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                binding.ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility);
            }
            binding.etRegisterConfirmPassword.setSelection(binding.etRegisterConfirmPassword.length());
        });

        binding.btnRegisterSubmit.setOnClickListener(v -> validateAndRegister());
    }

    private void validateAndRegister() {
        String fullName = binding.etRegisterFullName.getText().toString().trim();
        String username = binding.etRegisterUsername.getText().toString().trim().toLowerCase();
        String rawPhone = binding.etRegisterPhone.getText().toString().trim();
        String cleanPhone = FirebaseManager.cleanPhoneNumber(rawPhone);
        String pin = binding.etRegisterPassword.getText().toString().trim();
        String confirmPin = binding.etRegisterConfirmPassword.getText().toString().trim();

        binding.tvRegisterError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(fullName)) {
            binding.etRegisterFullName.setError(getString(R.string.error_empty_field));
            binding.etRegisterFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(username)) {
            binding.etRegisterUsername.setError(getString(R.string.error_empty_field));
            binding.etRegisterUsername.requestFocus();
            return;
        }

        if (username.length() < 3 || !username.matches("^[a-zA-Z0-9_.]+$")) {
            binding.etRegisterUsername.setError("Username must be at least 3 characters and contain only letters, numbers, underscores, or dots.");
            binding.etRegisterUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(cleanPhone) || cleanPhone.length() < 7) {
            binding.etRegisterPhone.setError("Please enter a valid phone number.");
            binding.etRegisterPhone.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(pin)) {
            binding.etRegisterPassword.setError(getString(R.string.error_empty_field));
            binding.etRegisterPassword.requestFocus();
            return;
        }

        if (pin.length() < 4) {
            binding.etRegisterPassword.setError("PIN or password must be at least 4 digits.");
            binding.etRegisterPassword.requestFocus();
            return;
        }

        if (!pin.equals(confirmPin)) {
            binding.etRegisterConfirmPassword.setError(getString(R.string.error_passwords_mismatch));
            binding.etRegisterConfirmPassword.requestFocus();
            return;
        }

        setLoading(true);

        // Step 1: Check if username is already taken
        DatabaseReference usernameRef = FirebaseManager.getInstance().getUsernamesRef().child(username);
        usernameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    setLoading(false);
                    binding.tvRegisterError.setText("This username is already taken. Please choose another.");
                    binding.tvRegisterError.setVisibility(View.VISIBLE);
                    binding.etRegisterUsername.requestFocus();
                } else {
                    // Step 2: Check if phone number is already registered
                    checkPhoneAvailability(fullName, username, rawPhone, cleanPhone, pin);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                binding.tvRegisterError.setText("Database error: " + error.getMessage());
                binding.tvRegisterError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void checkPhoneAvailability(String fullName, String username, String rawPhone, String cleanPhone, String pin) {
        FirebaseManager.getInstance().getPhoneNumberRef(cleanPhone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    setLoading(false);
                    binding.tvRegisterError.setText("This phone number is already registered. Please log in.");
                    binding.tvRegisterError.setVisibility(View.VISIBLE);
                    binding.etRegisterPhone.requestFocus();
                } else {
                    createFirebaseAccount(fullName, username, rawPhone, cleanPhone, pin);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                binding.tvRegisterError.setText("Database error: " + error.getMessage());
                binding.tvRegisterError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void createFirebaseAccount(String fullName, String username, String rawPhone, String cleanPhone, String pin) {
        String internalEmail = FirebaseManager.phoneToEmail(cleanPhone);
        String internalPassword = FirebaseManager.normalizePasswordOrPin(pin);

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(internalEmail, internalPassword)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FirebaseUser firebaseUser = task.getResult().getUser();
                        if (firebaseUser != null) {
                            saveUserProfile(firebaseUser.getUid(), fullName, username, rawPhone, cleanPhone);
                        } else {
                            setLoading(false);
                            showError("Failed to retrieve user ID.");
                        }
                    } else {
                        setLoading(false);
                        String errorMsg = "Registration failed.";
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            errorMsg = "An account with this phone number already exists.";
                        } else if (task.getException() != null && task.getException().getMessage() != null) {
                            errorMsg = task.getException().getMessage();
                        }
                        showError(errorMsg);
                    }
                });
    }

    private void saveUserProfile(String uid, String fullName, String username, String rawPhone, String cleanPhone) {
        User user = new User(uid, fullName, username, cleanPhone + "@nexachat.app", null, "Hey there! I am using NexaChat.", System.currentTimeMillis());
        user.setPhoneNumber(rawPhone);

        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("users/" + uid, user);
        childUpdates.put("usernames/" + username, uid);
        
        Map<String, Object> phoneData = new HashMap<>();
        phoneData.put("uid", uid);
        phoneData.put("fullName", fullName);
        phoneData.put("username", username);
        phoneData.put("phoneNumber", rawPhone);
        phoneData.put("cleanPhone", cleanPhone);
        childUpdates.put("phone_numbers/" + cleanPhone, phoneData);

        // Also index last 10 digits if cleanPhone has country code (e.g. 919876543210 -> 9876543210)
        if (cleanPhone.length() > 10) {
            String last10 = cleanPhone.substring(cleanPhone.length() - 10);
            childUpdates.put("phone_numbers/" + last10, phoneData);
        }

        FirebaseManager.getInstance().getUsersRef().getRoot().updateChildren(childUpdates)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseManager.getInstance().setupPresence();
                        FirebaseManager.getInstance().syncFcmToken();
                        Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        showError("Failed to save profile: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void showError(String message) {
        binding.tvRegisterError.setText(message);
        binding.tvRegisterError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean isLoading) {
        binding.btnRegisterSubmit.setEnabled(!isLoading);
        binding.btnRegisterSubmit.setText(isLoading ? "" : getString(R.string.btn_register));
        binding.pbRegisterLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
