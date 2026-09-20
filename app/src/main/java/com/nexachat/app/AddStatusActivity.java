package com.nexachat.app;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.nexachat.app.databinding.ActivityAddStatusBinding;
import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.models.StatusItem;
import com.nexachat.app.models.User;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddStatusActivity extends AppCompatActivity {

    private ActivityAddStatusBinding binding;
    private String currentUserId;
    private User currentUser;
    private boolean isPhotoMode = false;
    private Uri selectedImageUri;
    private String selectedColorHex = "#00B0FF";

    private ActivityResultLauncher<PickVisualMediaRequest> pickPhotoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddStatusBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        if (currentUserId == null) {
            finish();
            return;
        }

        setupPhotoPicker();
        setupUI();
        loadCurrentUser();
    }

    private void setupPhotoPicker() {
        pickPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        binding.layoutPhotoPlaceholder.setVisibility(View.GONE);
                        binding.ivSelectedStatusPhoto.setVisibility(View.VISIBLE);
                        binding.ivSelectedStatusPhoto.setImageURI(uri);
                    }
                });
    }

    private void setupUI() {
        binding.btnAddStatusBack.setOnClickListener(v -> finish());

        // Mode toggles
        binding.btnModeText.setOnClickListener(v -> setMode(false));
        binding.btnModePhoto.setOnClickListener(v -> setMode(true));

        // Photo pick action
        binding.btnPickStatusPhoto.setOnClickListener(v -> {
            pickPhotoLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        // Color palette clicks
        binding.colorSkyBlue.setOnClickListener(v -> applyPalette("#00B0FF"));
        binding.colorDeepBlue.setOnClickListener(v -> applyPalette("#0284C7"));
        binding.colorCyanNeon.setOnClickListener(v -> applyPalette("#00E5FF"));
        binding.colorMidnight.setOnClickListener(v -> applyPalette("#0D1B36"));
        binding.colorPurpleBlue.setOnClickListener(v -> applyPalette("#4F46E5"));

        applyPalette(selectedColorHex);

        binding.btnPostStatus.setOnClickListener(v -> postStatus());
    }

    private void setMode(boolean photoMode) {
        isPhotoMode = photoMode;
        if (photoMode) {
            binding.btnModePhoto.setBackgroundResource(R.drawable.bg_pill_active);
            binding.btnModePhoto.setTextColor(ContextCompat.getColor(this, R.color.bg_dark_navy));

            binding.btnModeText.setBackgroundResource(R.drawable.bg_pill_inactive);
            binding.btnModeText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            binding.cardTextStatus.setVisibility(View.GONE);
            binding.colorPaletteLayout.setVisibility(View.GONE);
            binding.cardPhotoStatus.setVisibility(View.VISIBLE);
        } else {
            binding.btnModeText.setBackgroundResource(R.drawable.bg_pill_active);
            binding.btnModeText.setTextColor(ContextCompat.getColor(this, R.color.bg_dark_navy));

            binding.btnModePhoto.setBackgroundResource(R.drawable.bg_pill_inactive);
            binding.btnModePhoto.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            binding.cardPhotoStatus.setVisibility(View.GONE);
            binding.cardTextStatus.setVisibility(View.VISIBLE);
            binding.colorPaletteLayout.setVisibility(View.VISIBLE);
        }
    }

    private void applyPalette(String hex) {
        selectedColorHex = hex;
        try {
            int c = Color.parseColor(hex);
            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{c, Color.parseColor("#060C1B")}
            );
            gd.setCornerRadius(36f);
            binding.cardTextStatus.setBackground(gd);
        } catch (Exception ignored) {}
    }

    private void loadCurrentUser() {
        FirebaseManager.getInstance().getUserRef(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = snapshot.getValue(User.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void postStatus() {
        if (isPhotoMode) {
            if (selectedImageUri == null) {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
                return;
            }
            uploadPhotoStatus();
        } else {
            String text = binding.etStatusText.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                binding.etStatusText.setError("Please write something");
                binding.etStatusText.requestFocus();
                return;
            }
            saveStatusToFirebase(StatusItem.TYPE_TEXT, text, null, null, selectedColorHex);
        }
    }

    private void uploadPhotoStatus() {
        setLoading(true);
        String statusId = "status_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 5);
        StorageReference storageRef = FirebaseManager.getInstance().getStorage()
                .getReference("status_images/" + currentUserId + "/" + statusId + ".jpg");

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            String caption = binding.etPhotoCaption.getText().toString().trim();
                            saveStatusToFirebase(StatusItem.TYPE_IMAGE, null, uri.toString(), caption, selectedColorHex);
                        })
                        .addOnFailureListener(e -> {
                            setLoading(false);
                            Toast.makeText(this, "Failed to get image link", Toast.LENGTH_SHORT).show();
                        }))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveStatusToFirebase(String type, String text, String imageUrl, String caption, String bgColor) {
        setLoading(true);
        String statusId = "status_" + System.currentTimeMillis();
        long now = System.currentTimeMillis();

        String authorName = currentUser != null ? currentUser.getFullName() : "NexaChat User";
        String authorAvatar = currentUser != null ? currentUser.getProfileImageUrl() : "";

        StatusItem item = new StatusItem(statusId, currentUserId, authorName, authorAvatar, now, type, text, imageUrl, bgColor, caption);

        Map<String, Object> updates = new HashMap<>();
        updates.put("statuses/" + currentUserId + "/" + statusId, item);
        updates.put("users/" + currentUserId + "/latestStatusTimestamp", now);

        FirebaseManager.getInstance().getUsersRef().getRoot().updateChildren(updates)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Status posted! Disappears after 24 hours.", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to post status.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        binding.pbPostingStatus.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnPostStatus.setEnabled(!isLoading);
        binding.btnPostStatus.setText(isLoading ? "" : "Post");
    }
}
