package com.nexachat.app;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.nexachat.app.databinding.ActivityProfileBinding;
import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.models.User;
import com.nexachat.app.utils.DateTimeUtils;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private String currentUserId;
    private User currentUser;
    private ActivityResultLauncher<PickVisualMediaRequest> pickPhotoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        if (currentUserId == null) {
            finish();
            return;
        }

        setupUI();
        loadUserProfile();
    }

    private void setupUI() {
        binding.btnProfileBack.setOnClickListener(v -> finish());
        binding.btnProfileSettings.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, SettingsActivity.class));
        });

        pickPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        uploadProfilePhoto(uri);
                    }
                });

        binding.btnChangePhoto.setOnClickListener(v ->
                pickPhotoLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build())
        );

        binding.btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        binding.btnProfileSecurity.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, SecurityActivity.class));
        });

        binding.btnLogout.setOnClickListener(v -> {
            FirebaseManager.getInstance().signOut();
            android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserProfile() {
        FirebaseManager.getInstance().getUserRef(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    binding.tvProfileFullName.setText(currentUser.getFullName());
                    binding.tvProfileUsername.setText("@" + currentUser.getUsername());
                    binding.tvProfileEmail.setText(currentUser.getEmail());
                    binding.tvProfilePresence.setText(currentUser.isOnline() ? "Online" : "Offline");

                    if (currentUser.getCreatedAt() > 0) {
                        binding.tvProfileCreatedAt.setText(DateTimeUtils.formatConversationTime(currentUser.getCreatedAt()));
                    }

                    if (!TextUtils.isEmpty(currentUser.getProfileImageUrl())) {
                        Glide.with(ProfileActivity.this)
                                .load(currentUser.getProfileImageUrl())
                                .placeholder(R.drawable.circle_avatar_placeholder)
                                .circleCrop()
                                .into(binding.ivProfilePicture);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void uploadProfilePhoto(Uri uri) {
        Toast.makeText(this, "Uploading profile photo...", Toast.LENGTH_SHORT).show();
        StorageReference ref = FirebaseManager.getInstance().getProfileImageRef(currentUserId);

        ref.putFile(uri).continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) {
                throw task.getException();
            }
            return ref.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String downloadUrl = task.getResult().toString();
                FirebaseManager.getInstance().getUserRef(currentUserId)
                        .child("profileImageUrl").setValue(downloadUrl);
                Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to upload photo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditProfileDialog() {
        if (currentUser == null) return;

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        EditText etFullName = dialogView.findViewById(R.id.etEditFullName);
        EditText etBio = dialogView.findViewById(R.id.etEditBio);

        etFullName.setText(currentUser.getFullName());
        etBio.setText(currentUser.getBio());

        new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etFullName.getText().toString().trim();
                    String newBio = etBio.getText().toString().trim();

                    if (!TextUtils.isEmpty(newName)) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("fullName", newName);
                        updates.put("bio", newBio);
                        FirebaseManager.getInstance().getUserRef(currentUserId).updateChildren(updates);
                        Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
