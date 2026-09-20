package com.nexachat.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.nexachat.app.adapters.MessageAdapter;
import com.nexachat.app.databinding.ActivityGroupChatBinding;
import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.models.Group;
import com.nexachat.app.models.Message;
import com.nexachat.app.models.User;
import com.nexachat.app.utils.AudioPlayerHelper;
import com.nexachat.app.utils.AudioRecorderHelper;
import com.nexachat.app.utils.DateTimeUtils;
import com.nexachat.app.utils.FileUtils;

import java.io.File;
import java.util.UUID;

public class GroupChatActivity extends AppCompatActivity {

    private ActivityGroupChatBinding binding;
    private String groupId;
    private String groupName;
    private String groupPhoto;
    private Group group;
    private String currentUserId;
    private String currentUserName = "Me";

    private MessageAdapter messageAdapter;
    private DatabaseReference messagesRef;
    private ChildEventListener messagesListener;

    private AudioRecorderHelper audioRecorder;
    private final Handler recordingTimerHandler = new Handler(Looper.getMainLooper());
    private Uri cameraImageUri;

    private ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickFileLauncher;
    private ActivityResultLauncher<String> recordAudioPermissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        groupId = getIntent().getStringExtra("groupId");
        groupName = getIntent().getStringExtra("groupName");
        groupPhoto = getIntent().getStringExtra("groupPhoto");

        currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        if (currentUserId == null || groupId == null) {
            finish();
            return;
        }

        audioRecorder = new AudioRecorderHelper();

        setupKeyboardAndInsets();
        setupTopBar();
        setupRecyclerView();
        setupComposer();
        setupActivityLaunchers();
        loadGroupData();
        loadCurrentUserInfo();
        listenForMessages();
        resetUnreadCount();
    }

    private void setupKeyboardAndInsets() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        ViewCompat.setOnApplyWindowInsetsListener(binding.groupAppBar, (v, windowInsets) -> {
            Insets sysBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingStart(), sysBars.top + (int) (8 * getResources().getDisplayMetrics().density),
                    v.getPaddingEnd(), (int) (12 * getResources().getDisplayMetrics().density));
            return windowInsets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.groupBottomComposer, (v, windowInsets) -> {
            try {
                Insets imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
                Insets navInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());

                int bottomInset = Math.max(imeInsets.bottom, navInsets.bottom);
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                if (lp != null && lp.bottomMargin != bottomInset) {
                    lp.bottomMargin = bottomInset;
                    v.setLayoutParams(lp);
                }

                if (imeInsets.bottom > 0 && messageAdapter != null && messageAdapter.getItemCount() > 0) {
                    binding.rvGroupMessages.postDelayed(() ->
                            binding.rvGroupMessages.scrollToPosition(messageAdapter.getItemCount() - 1), 60);
                }
            } catch (Throwable ignored) {}
            return windowInsets;
        });

        binding.rvGroupMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom && messageAdapter != null && messageAdapter.getItemCount() > 0) {
                binding.rvGroupMessages.post(() -> binding.rvGroupMessages.scrollToPosition(messageAdapter.getItemCount() - 1));
            }
        });

        binding.etGroupMessageInput.setOnClickListener(v -> {
            if (messageAdapter != null && messageAdapter.getItemCount() > 0) {
                binding.rvGroupMessages.postDelayed(() ->
                        binding.rvGroupMessages.scrollToPosition(messageAdapter.getItemCount() - 1), 120);
            }
        });

        binding.etGroupMessageInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && messageAdapter != null && messageAdapter.getItemCount() > 0) {
                binding.rvGroupMessages.postDelayed(() ->
                        binding.rvGroupMessages.scrollToPosition(messageAdapter.getItemCount() - 1), 120);
            }
        });
    }

    private void setupTopBar() {
        binding.tvGroupName.setText(groupName != null ? groupName : "Group");
        binding.tvGroupMembersCount.setText("Loading members...");

        if (!TextUtils.isEmpty(groupPhoto)) {
            Glide.with(this)
                    .load(groupPhoto)
                    .placeholder(R.drawable.circle_avatar_placeholder)
                    .circleCrop()
                    .into(binding.ivGroupAvatar);
        }

        binding.btnGroupBack.setOnClickListener(v -> finish());
    }

    private void loadGroupData() {
        FirebaseManager.getInstance().getGroupRef(groupId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                group = snapshot.getValue(Group.class);
                if (group != null) {
                    binding.tvGroupName.setText(group.getGroupName());
                    int memberCount = group.getMembers() != null ? group.getMembers().size() : 1;
                    binding.tvGroupMembersCount.setText(memberCount + " members");

                    if (!TextUtils.isEmpty(group.getGroupPhotoUrl())) {
                        Glide.with(GroupChatActivity.this)
                                .load(group.getGroupPhotoUrl())
                                .placeholder(R.drawable.circle_avatar_placeholder)
                                .circleCrop()
                                .into(binding.ivGroupAvatar);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(this, true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvGroupMessages.setLayoutManager(layoutManager);
        binding.rvGroupMessages.setAdapter(messageAdapter);
    }

    private void setupComposer() {
        binding.etGroupMessageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s != null && s.toString().trim().length() > 0;
                binding.ivGroupSendIcon.setVisibility(hasText ? View.VISIBLE : View.GONE);
                binding.ivGroupMicIcon.setVisibility(hasText ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.ivGroupSendIcon.setVisibility(View.GONE);
        binding.ivGroupMicIcon.setVisibility(View.VISIBLE);

        binding.btnGroupSendContainer.setOnClickListener(v -> {
            String text = binding.etGroupMessageInput.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                sendTextMessage(text);
                binding.etGroupMessageInput.setText("");
            } else {
                handleMicClick();
            }
        });

        binding.btnGroupAttachFile.setOnClickListener(v -> pickFileLauncher.launch("*/*"));

        binding.btnGroupAttachCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        binding.tvGroupCancelRecording.setOnClickListener(v -> cancelAudioRecording());
    }

    private void setupActivityLaunchers() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        uploadAndSendMedia(uri, Message.TYPE_IMAGE);
                    }
                });

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraImageUri != null) {
                        uploadAndSendMedia(cameraImageUri, Message.TYPE_IMAGE);
                    }
                });

        pickFileLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadAndSendMedia(uri, Message.TYPE_FILE);
                    }
                });

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                    }
                });

        recordAudioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startAudioRecording();
                    } else {
                        Toast.makeText(this, "Microphone permission required for voice notes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void launchCamera() {
        try {
            File photoFile = new File(getExternalFilesDir("Pictures"), "grp_" + System.currentTimeMillis() + ".jpg");
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            takePictureLauncher.launch(cameraImageUri);
        } catch (Exception e) {
            Toast.makeText(this, "Camera preparation failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleMicClick() {
        if (audioRecorder.isRecording()) {
            stopAndSendAudio();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                startAudioRecording();
            } else {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        }
    }

    private void startAudioRecording() {
        if (audioRecorder.startRecording(this)) {
            binding.layoutGroupRecordingOverlay.setVisibility(View.VISIBLE);
            binding.ivGroupMicIcon.setImageResource(R.drawable.ic_pause);
            startRecordingTimer();
        } else {
            Toast.makeText(this, "Failed to record audio", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAndSendAudio() {
        recordingTimerHandler.removeCallbacksAndMessages(null);
        int duration = audioRecorder.getElapsedDurationSeconds();
        File audioFile = audioRecorder.stopRecording();
        binding.layoutGroupRecordingOverlay.setVisibility(View.GONE);
        binding.ivGroupMicIcon.setImageResource(R.drawable.ic_mic);

        if (audioFile != null && audioFile.exists() && duration >= 1) {
            uploadAndSendAudio(Uri.fromFile(audioFile), duration);
        }
    }

    private void cancelAudioRecording() {
        recordingTimerHandler.removeCallbacksAndMessages(null);
        audioRecorder.cancelRecording();
        binding.layoutGroupRecordingOverlay.setVisibility(View.GONE);
        binding.ivGroupMicIcon.setImageResource(R.drawable.ic_mic);
    }

    private void startRecordingTimer() {
        recordingTimerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (audioRecorder.isRecording()) {
                    int sec = audioRecorder.getElapsedDurationSeconds();
                    binding.tvGroupRecordingTimer.setText(DateTimeUtils.formatDuration(sec));
                    recordingTimerHandler.postDelayed(this, 500);
                }
            }
        });
    }

    private void sendTextMessage(String text) {
        if (group == null) return;
        Message msg = new Message(null, groupId, currentUserId, currentUserName, text);
        FirebaseManager.getInstance().sendGroupMessage(groupId, msg, group);
    }

    private void uploadAndSendMedia(Uri uri, String mediaType) {
        if (group == null) return;
        binding.pbGroupUploadProgress.setVisibility(View.VISIBLE);

        String subfolder = Message.TYPE_IMAGE.equals(mediaType) ? "images" : "files";
        String filename = UUID.randomUUID().toString() + "_" + FileUtils.getFileName(this, uri);
        StorageReference ref = FirebaseManager.getInstance().getChatMediaRef(groupId, subfolder, filename);

        ref.putFile(uri).continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) {
                throw task.getException();
            }
            return ref.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            binding.pbGroupUploadProgress.setVisibility(View.GONE);
            if (task.isSuccessful() && task.getResult() != null) {
                String downloadUrl = task.getResult().toString();
                Message msg = new Message(null, groupId, currentUserId, currentUserName,
                        Message.TYPE_IMAGE.equals(mediaType) ? "Photo" : FileUtils.getFileName(this, uri));
                msg.setMessageType(mediaType);
                msg.setMediaUrl(downloadUrl);
                msg.setFileName(FileUtils.getFileName(this, uri));
                msg.setFileSize(FileUtils.getFileSize(this, uri));

                FirebaseManager.getInstance().sendGroupMessage(groupId, msg, group);
            } else {
                Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadAndSendAudio(Uri uri, int durationSeconds) {
        if (group == null) return;
        binding.pbGroupUploadProgress.setVisibility(View.VISIBLE);

        String filename = UUID.randomUUID().toString() + ".m4a";
        StorageReference ref = FirebaseManager.getInstance().getChatMediaRef(groupId, "voice", filename);

        ref.putFile(uri).continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) {
                throw task.getException();
            }
            return ref.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            binding.pbGroupUploadProgress.setVisibility(View.GONE);
            if (task.isSuccessful() && task.getResult() != null) {
                String downloadUrl = task.getResult().toString();
                Message msg = new Message(null, groupId, currentUserId, currentUserName, "Voice note");
                msg.setMessageType(Message.TYPE_AUDIO);
                msg.setMediaUrl(downloadUrl);
                msg.setAudioDurationSeconds(durationSeconds);

                FirebaseManager.getInstance().sendGroupMessage(groupId, msg, group);
            } else {
                Toast.makeText(this, "Audio upload failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCurrentUserInfo() {
        FirebaseManager.getInstance().getUserRef(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null && user.getFullName() != null) {
                    currentUserName = user.getFullName();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void listenForMessages() {
        messagesRef = FirebaseManager.getInstance().getMessagesRef(groupId);
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message msg = snapshot.getValue(Message.class);
                if (msg != null) {
                    messageAdapter.addMessage(msg);
                    binding.rvGroupMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        messagesRef.addChildEventListener(messagesListener);
    }

    private void resetUnreadCount() {
        if (groupId != null && currentUserId != null) {
            FirebaseManager.getInstance().getUserConversationsRef(currentUserId)
                    .child(groupId).child("unreadCount").setValue(0);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        AudioPlayerHelper.getInstance().stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AudioPlayerHelper.getInstance().stop();
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }
}
