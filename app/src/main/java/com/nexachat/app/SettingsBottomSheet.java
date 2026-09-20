package com.nexachat.app;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.nexachat.app.databinding.BottomSheetSettingsBinding;
import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.models.User;
import com.nexachat.app.security.DisguiseManager;
import com.nexachat.app.security.SecurityHelper;

public class SettingsBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetSettingsBinding binding;
    private User currentUser;
    private ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher;

    public static SettingsBottomSheet newInstance() {
        return new SettingsBottomSheet();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        photoPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        uploadProfilePhoto(uri);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadUserData();
        setupCamouflageCard();
        setupBasicFeatureToggles();
        setupActionListeners();
    }

    private void loadUserData() {
        if (!FirebaseManager.getInstance().isUserLoggedIn()) return;

        DatabaseReference userRef = FirebaseManager.getInstance().getCurrentUserRef();
        if (userRef == null) return;

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;
                currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    populateUserUI(currentUser);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void populateUserUI(User user) {
        binding.tvSettingsFullName.setText(!TextUtils.isEmpty(user.getFullName()) ? user.getFullName() : "NexaChat User");
        binding.tvSettingsUsername.setText(!TextUtils.isEmpty(user.getUsername()) ? "@" + user.getUsername() : "");
        binding.tvSettingsBio.setText(!TextUtils.isEmpty(user.getBio()) ? user.getBio() : "Hey there! I am using NexaChat.");

        if (!TextUtils.isEmpty(user.getProfileImageUrl())) {
            Glide.with(this)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.ivSettingsProfilePic);
        }

        // Hide Number switch & status
        boolean isHidden = user.isPhoneNumberHidden();
        binding.switchHideNumber.setChecked(isHidden);
        updateHideNumberStatus(isHidden, user.getPhoneNumber());
    }

    private void updateHideNumberStatus(boolean isHidden, String phoneNumber) {
        if (isHidden) {
            binding.tvPhoneNumberStatus.setText("Private • Hidden from contacts and searches");
            binding.tvPhoneNumberStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.cyan_accent));
            binding.ivHideNumberIcon.setImageResource(R.drawable.ic_phone_hidden);
        } else {
            String display = !TextUtils.isEmpty(phoneNumber) ? phoneNumber : "Visible to contacts";
            binding.tvPhoneNumberStatus.setText("Visible: " + display);
            binding.tvPhoneNumberStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            binding.ivHideNumberIcon.setImageResource(R.drawable.ic_phone);
        }
    }

    private void setupCamouflageCard() {
        Context context = requireContext();
        DisguiseManager dm = DisguiseManager.getInstance();

        if (dm.isDisguiseEnabled(context)) {
            String name = dm.getDisguisedAppName(context);
            binding.tvDisguiseCurrentBadge.setText("ACTIVE: Disguised as " + name + " (Face ID Lock)");
            binding.tvDisguiseCurrentBadge.setTextColor(ContextCompat.getColor(context, R.color.cyan_accent));
        } else {
            binding.tvDisguiseCurrentBadge.setText("OFF: Tap to disguise as Calculator or Game");
            binding.tvDisguiseCurrentBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        }
    }

    private void setupBasicFeatureToggles() {
        Context context = requireContext();
        SharedPreferences prefs = context.getSharedPreferences("nexachat_privacy_prefs", Context.MODE_PRIVATE);

        // Online Status
        binding.switchOnlineStatus.setChecked(prefs.getBoolean("online_status_enabled", true));
        binding.switchOnlineStatus.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean("online_status_enabled", isChecked).apply();
            DatabaseReference userRef = FirebaseManager.getInstance().getCurrentUserRef();
            if (userRef != null) {
                userRef.child("online").setValue(isChecked);
            }
        });

        // Last Seen
        binding.switchLastSeen.setChecked(prefs.getBoolean("last_seen_enabled", true));
        binding.switchLastSeen.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean("last_seen_enabled", isChecked).apply();
        });

        // Read Receipts
        binding.switchReadReceipts.setChecked(prefs.getBoolean("read_receipts_enabled", true));
        binding.switchReadReceipts.setOnCheckedChangeListener((v, isChecked) -> {
            prefs.edit().putBoolean("read_receipts_enabled", isChecked).apply();
        });

        // App Lock Biometrics
        binding.switchAppLock.setChecked(SecurityHelper.isAppVerificationEnabled(context));
        binding.switchAppLock.setOnCheckedChangeListener((v, isChecked) -> {
            SecurityHelper.setAppVerificationEnabled(context, isChecked);
            Toast.makeText(context, isChecked ? "App Lock Enabled" : "App Lock Disabled", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupActionListeners() {
        binding.btnCloseSettings.setOnClickListener(v -> dismiss());

        // Change Profile Photo
        View.OnClickListener photoListener = v -> {
            photoPickerLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        };
        binding.frameSettingsAvatar.setOnClickListener(photoListener);
        binding.ivChangePhotoBadge.setOnClickListener(photoListener);

        // Edit Name & Bio
        binding.btnEditName.setOnClickListener(v -> showEditProfileDialog());
        binding.cardProfileHeader.setOnClickListener(v -> showEditProfileDialog());

        // Hide Number toggle
        binding.switchHideNumber.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentUser != null) {
                currentUser.setPhoneNumberHidden(isChecked);
            }
            updateHideNumberStatus(isChecked, currentUser != null ? currentUser.getPhoneNumber() : "");

            // Persist to Firebase
            DatabaseReference userRef = FirebaseManager.getInstance().getCurrentUserRef();
            if (userRef != null) {
                userRef.child("phoneNumberHidden").setValue(isChecked);
            }

            Context context = getContext();
            if (context != null) {
                context.getSharedPreferences("nexachat_privacy_prefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("hide_phone_number", isChecked).apply();
                Toast.makeText(context, isChecked ? "Phone number is now hidden" : "Phone number is now visible", Toast.LENGTH_SHORT).show();
            }
        });

        // Add App / Camouflage card click
        binding.cardAddAppCamouflage.setOnClickListener(v -> {
            dismiss();
            AppCamouflageBottomSheet camouflageSheet = AppCamouflageBottomSheet.newInstance();
            camouflageSheet.show(getParentFragmentManager(), "app_camouflage");
        });

        // Security settings button
        binding.btnOpenSecuritySettings.setOnClickListener(v -> {
            dismiss();
            startActivity(new Intent(requireContext(), SecurityActivity.class));
        });

        // Logout button
        binding.btnSettingsLogout.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Log Out")
                    .setMessage("Are you sure you want to log out of NexaChat?")
                    .setPositiveButton("Log Out", (dialog, which) -> {
                        FirebaseManager.getInstance().signOut();
                        Intent intent = new Intent(requireContext(), LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void showEditProfileDialog() {
        Context context = requireContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        final EditText etName = new EditText(context);
        etName.setHint("Full Name");
        if (currentUser != null && !TextUtils.isEmpty(currentUser.getFullName())) {
            etName.setText(currentUser.getFullName());
        }
        layout.addView(etName);

        final EditText etBio = new EditText(context);
        etBio.setHint("About / Bio");
        if (currentUser != null && !TextUtils.isEmpty(currentUser.getBio())) {
            etBio.setText(currentUser.getBio());
        }
        layout.addView(etBio);

        new MaterialAlertDialogBuilder(context)
                .setTitle("Edit Profile")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newBio = etBio.getText().toString().trim();
                    if (!TextUtils.isEmpty(newName)) {
                        DatabaseReference userRef = FirebaseManager.getInstance().getCurrentUserRef();
                        if (userRef != null) {
                            userRef.child("fullName").setValue(newName);
                            userRef.child("bio").setValue(newBio);
                        }
                        binding.tvSettingsFullName.setText(newName);
                        binding.tvSettingsBio.setText(newBio);
                        if (currentUser != null) {
                            currentUser.setFullName(newName);
                            currentUser.setBio(newBio);
                        }
                        Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void uploadProfilePhoto(Uri uri) {
        Context context = getContext();
        if (context == null) return;

        Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(binding.ivSettingsProfilePic);

        // Upload to Firebase Storage
        Toast.makeText(context, "Uploading profile photo...", Toast.LENGTH_SHORT).show();
        FirebaseManager.getInstance().uploadProfileImage(uri, new FirebaseManager.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Profile photo updated!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Failed to upload: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
