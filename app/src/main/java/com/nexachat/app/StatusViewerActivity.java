package com.nexachat.app;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.nexachat.app.databinding.ActivityStatusViewerBinding;
import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.models.StatusItem;
import com.nexachat.app.models.UserStatusGroup;
import com.nexachat.app.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

public class StatusViewerActivity extends AppCompatActivity {

    public static final String EXTRA_STATUS_GROUP = "extra_status_group";

    private ActivityStatusViewerBinding binding;
    private UserStatusGroup statusGroup;
    private final List<StatusItem> activeItems = new ArrayList<>();
    private int currentIndex = 0;
    private CountDownTimer storyTimer;
    private String currentUserId;
    private static final long STORY_DURATION = 5000L; // 5 seconds per story

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatusViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUserId = FirebaseManager.getInstance().getCurrentUserId();

        statusGroup = (UserStatusGroup) getIntent().getSerializableExtra(EXTRA_STATUS_GROUP);
        if (statusGroup == null || statusGroup.getStatusList() == null || statusGroup.getStatusList().isEmpty()) {
            Toast.makeText(this, "No active status to view", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        for (StatusItem item : statusGroup.getStatusList()) {
            if (!item.isExpired()) {
                activeItems.add(item);
            }
        }

        if (activeItems.isEmpty()) {
            Toast.makeText(this, "This status has expired (older than 24h).", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupGestures();
        displayCurrentStory();
    }

    private void setupGestures() {
        binding.btnStatusDismiss.setOnClickListener(v -> finish());

        binding.touchZoneRight.setOnClickListener(v -> {
            if (currentIndex < activeItems.size() - 1) {
                currentIndex++;
                displayCurrentStory();
            } else {
                finish();
            }
        });

        binding.touchZoneLeft.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                displayCurrentStory();
            } else {
                displayCurrentStory();
            }
        });

        binding.btnStatusClose.setOnClickListener(v -> {
            // Delete status option for current user
            if (currentIndex >= 0 && currentIndex < activeItems.size()) {
                StatusItem item = activeItems.get(currentIndex);
                deleteCurrentStatus(item);
            }
        });
    }

    private void displayCurrentStory() {
        if (storyTimer != null) {
            storyTimer.cancel();
        }

        if (currentIndex < 0 || currentIndex >= activeItems.size()) {
            finish();
            return;
        }

        StatusItem item = activeItems.get(currentIndex);

        // Record view if not own status
        if (currentUserId != null && !currentUserId.equals(item.getUserId())) {
            FirebaseManager.getInstance().getUsersRef().getRoot()
                    .child("statuses")
                    .child(item.getUserId())
                    .child(item.getStatusId())
                    .child("viewers")
                    .child(currentUserId)
                    .setValue(System.currentTimeMillis());
        }

        // Header info
        binding.tvStatusAuthorName.setText(item.getUserName());
        binding.tvStatusTime.setText(DateTimeUtils.formatConversationTime(item.getTimestamp()) + " • 24h update");

        if (!TextUtils.isEmpty(item.getUserAvatarUrl())) {
            Glide.with(this)
                    .load(item.getUserAvatarUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(binding.ivStatusAuthorAvatar);
        }

        // Delete button for author
        if (currentUserId != null && currentUserId.equals(item.getUserId())) {
            binding.btnStatusClose.setVisibility(View.VISIBLE);
            binding.tvStatusViewers.setVisibility(View.VISIBLE);
            int views = item.getViewersCount();
            binding.tvStatusViewers.setText("👁 " + views + (views == 1 ? " view" : " views"));
        } else {
            binding.btnStatusClose.setVisibility(View.GONE);
            binding.tvStatusViewers.setVisibility(View.GONE);
        }

        // Content
        if (StatusItem.TYPE_IMAGE.equalsIgnoreCase(item.getType()) && !TextUtils.isEmpty(item.getImageUrl())) {
            binding.layoutTextStatus.setVisibility(View.GONE);
            binding.ivStatusImage.setVisibility(View.VISIBLE);

            Glide.with(this)
                    .load(item.getImageUrl())
                    .into(binding.ivStatusImage);

            if (!TextUtils.isEmpty(item.getCaption())) {
                binding.tvStatusCaption.setVisibility(View.VISIBLE);
                binding.tvStatusCaption.setText(item.getCaption());
            } else {
                binding.tvStatusCaption.setVisibility(View.GONE);
            }
        } else {
            // Text status
            binding.ivStatusImage.setVisibility(View.GONE);
            binding.tvStatusCaption.setVisibility(View.GONE);
            binding.layoutTextStatus.setVisibility(View.VISIBLE);

            binding.tvStatusContentText.setText(item.getText());

            // Gradient background
            applyGradientBackground(item.getBackgroundColor());
        }

        startStoryProgress();
    }

    private void applyGradientBackground(String colorHex) {
        try {
            int startColor = Color.parseColor("#0284C7");
            int endColor = Color.parseColor("#060C1B");

            if (!TextUtils.isEmpty(colorHex) && colorHex.startsWith("#")) {
                startColor = Color.parseColor(colorHex);
            }

            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    new int[]{startColor, endColor}
            );
            binding.layoutTextStatus.setBackground(gd);
        } catch (Exception e) {
            binding.layoutTextStatus.setBackgroundColor(Color.parseColor("#0C182F"));
        }
    }

    private void startStoryProgress() {
        binding.statusProgressBar.setProgress(0);
        storyTimer = new CountDownTimer(STORY_DURATION, 50) {
            @Override
            public void onTick(long millisUntilFinished) {
                long elapsed = STORY_DURATION - millisUntilFinished;
                int progress = (int) ((elapsed * 100) / STORY_DURATION);
                binding.statusProgressBar.setProgress(progress);
            }

            @Override
            public void onFinish() {
                binding.statusProgressBar.setProgress(100);
                if (currentIndex < activeItems.size() - 1) {
                    currentIndex++;
                    displayCurrentStory();
                } else {
                    finish();
                }
            }
        }.start();
    }

    private void deleteCurrentStatus(StatusItem item) {
        if (storyTimer != null) storyTimer.cancel();

        FirebaseManager.getInstance().getUsersRef().getRoot()
                .child("statuses")
                .child(item.getUserId())
                .child(item.getStatusId())
                .removeValue()
                .addOnCompleteListener(task -> {
                    Toast.makeText(this, "Status deleted", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (storyTimer != null) {
            storyTimer.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (storyTimer != null) {
            storyTimer.cancel();
        }
    }
}
