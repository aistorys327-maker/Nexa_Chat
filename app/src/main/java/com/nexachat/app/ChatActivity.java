package com.nexachat.app;

import android.Manifest;
import android.content.Intent;
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
import androidx.appcompat.widget.PopupMenu;
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
import com.nexachat.app.databinding.ActivityChatBinding;
import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.models.Message;
import com.nexachat.app.models.User;
import com.nexachat.app.security.ChatLockManager;
import com.nexachat.app.security.SecurityHelper;
import com.nexachat.app.utils.AudioPlayerHelper;
import com.nexachat.app.utils.AudioRecorderHelper;
import com.nexachat.app.utils.DateTimeUtils;
import com.nexachat.app.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private String conversationId;
    private String otherUserId;
    private String otherUserName;
    private String otherUserAvatar;
    private String currentUserId;
    private String currentUserName = "Me";
    private String currentUserAvatar;

    private MessageAdapter messageAdapter;
    private DatabaseReference messagesRef;
    private ChildEventListener messagesListener;
    private DatabaseReference presenceRef;
    private ValueEventListener presenceListener;

    private ChatLockManager chatLockManager;
    private AudioRecorderHelper audioRecorder;
    private final Handler recordingTimerHandler = new Handler(Looper.getMainLooper());
    private Uri cameraImageUri;

    // Permissions & Pickers
    private ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickFileLauncher;
    private ActivityResultLauncher<String> recordAudioPermissionLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        conversationId = getIntent().getStringExtra("conversationId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        otherUserName = getIntent().getStringExtra("title");
        otherUserAvatar = getIntent().getStringExtra("avatarUrl");

        currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        if (currentUserId == null) {
            finish();
            return;
        }

        chatLockManager = ChatLockManager.getInstance(this);

        // Security Check: If chat is locked and not unlocked in this session, authenticate first
        if (chatLockManager.isChatLocked(conversationId) && !chatLockManager.isSessionUnlocked(conversationId)) {
            SecurityHelper.authenticate(this, "Secured Chat", "Authenticate to view this conversation",
                    new SecurityHelper.AuthCallback() {
                        @Override
                        public void onSuccess() {
                            chatLockManager.markSessionUnlocked(conversationId);
                            initChatUI();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            Toast.makeText(ChatActivity.this, "Authentication required", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
        } else {
            initChatUI();
        }
    }

    private void initChatUI() {
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        audioRecorder = new AudioRecorderHelper();

        setupKeyboardAndInsets();
        setupTopBar();
        setupRecyclerView();
        setupComposer();
        setupActivityLaunchers();
        loadCurrentUserInfo();
        observePresence();
        listenForMessages();
        resetUnreadCount();
    }

    private void setupKeyboardAndInsets() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        ViewCompat.setOnApplyWindowInsetsListener(binding.chatAppBar, (v, windowInsets) -> {
            Insets sysBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingStart(), sysBars.top + (int) (8 * getResources().getDisplayMetrics().density),
                    v.getPaddingEnd(), (int) (12 * getResources().getDisplayMetrics().density));
            return windowInsets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomComposerContainer, (v, windowInsets) -> {
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
                    binding.rvMessages.postDelayed(() ->
                            binding.rvMessages.scrollToPosition(messageAdapter.getItemCount() - 1), 60);
                }
            } catch (Throwable ignored) {}
            return windowInsets;
        });

        binding.rvMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom && messageAdapter != null && messageAdapter.getItemCount() > 0) {
                binding.rvMessages.post(() -> binding.rvMessages.scrollToPosition(messageAdapter.getItemCount() - 1));
            }
        });

        binding.etMessageInput.setOnClickListener(v -> {
            if (messageAdapter != null && messageAdapter.getItemCount() > 0) {
                binding.rvMessages.postDelayed(() ->
                        binding.rvMessages.scrollToPosition(messageAdapter.getItemCount() - 1), 120);
            }
        });

        binding.etMessageInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && messageAdapter != null && messageAdapter.getItemCount() > 0) {
                binding.rvMessages.postDelayed(() ->
                        binding.rvMessages.scrollToPosition(messageAdapter.getItemCount() - 1), 120);
            }
        });
    }

    private void setupTopBar() {
        binding.tvChatUserName.setText(otherUserName != null ? otherUserName : "Chat");
        binding.tvChatUserStatus.setText("Connecting...");

        if (!TextUtils.isEmpty(otherUserAvatar)) {
            Glide.with(this)
                    .load(otherUserAvatar)
                    .placeholder(R.drawable.circle_avatar_placeholder)
                    .circleCrop()
                    .into(binding.ivChatAvatar);
        }

        updateLockIcon();

        binding.btnChatBack.setOnClickListener(v -> finish());

        binding.btnChatAudioCall.setOnClickListener(v -> startCall(com.nexachat.app.models.CallSession.TYPE_AUDIO));
        binding.btnChatVideoCall.setOnClickListener(v -> startCall(com.nexachat.app.models.CallSession.TYPE_VIDEO));

        binding.btnChatMenu.setOnClickListener(v -> showChatMenu());
    }

    private void startCall(String callType) {
        String callId = "call_" + System.currentTimeMillis();
        com.nexachat.app.models.CallSession session = new com.nexachat.app.models.CallSession(
                callId,
                currentUserId,
                currentUserName,
                currentUserAvatar,
                otherUserId,
                otherUserName,
                callType
        );

        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra(CallActivity.EXTRA_CALL_SESSION, session);
        intent.putExtra(CallActivity.EXTRA_IS_INCOMING, false);
        startActivity(intent);
    }

    private void updateLockIcon() {
        boolean isLocked = chatLockManager.isChatLocked(conversationId);
        binding.ivChatLockIcon.setVisibility(isLocked ? View.VISIBLE : View.GONE);
    }

    private void showChatMenu() {
        PopupMenu popup = new PopupMenu(this, binding.btnChatMenu);
        boolean isLocked = chatLockManager.isChatLocked(conversationId);
        popup.getMenu().add(0, 1, 0, isLocked ? "Unlock Chat" : "Lock Chat");
        popup.getMenu().add(0, 2, 1, "Clear Chat History");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                if (isLocked) {
                    chatLockManager.setChatLocked(conversationId, false);
                    Toast.makeText(this, "Chat unlocked", Toast.LENGTH_SHORT).show();
                } else {
                    chatLockManager.setChatLocked(conversationId, true);
                    chatLockManager.markSessionUnlocked(conversationId);
                    Toast.makeText(this, "Chat locked with security PIN / Biometric", Toast.LENGTH_SHORT).show();
                }
                updateLockIcon();
                return true;
            } else if (item.getItemId() == 2) {
                clearChatHistory();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void clearChatHistory() {
        FirebaseManager.getInstance().getMessagesRef(conversationId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    messageAdapter.setMessages(null);
                    Toast.makeText(this, "Chat history cleared", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(this, false);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(messageAdapter);
    }

    private void setupComposer() {
        binding.etMessageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s != null && s.toString().trim().length() > 0;
                binding.ivSendIcon.setVisibility(hasText ? View.VISIBLE : View.GONE);
                binding.ivMicIcon.setVisibility(hasText ? View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Toggle initial icons
        binding.ivSendIcon.setVisibility(View.GONE);
        binding.ivMicIcon.setVisibility(View.VISIBLE);

        binding.btnSendContainer.setOnClickListener(v -> {
            String text = binding.etMessageInput.getText().toString().trim();
            if (!TextUtils.isEmpty(text)) {
                sendTextMessage(text);
                binding.etMessageInput.setText("");
            } else {
                handleMicClick();
            }
        });

        binding.btnAttachFile.setOnClickListener(v -> pickFileLauncher.launch("*/*"));

        binding.btnAttachCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        binding.tvCancelRecording.setOnClickListener(v -> cancelAudioRecording());
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
                        Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
                    }
                });

        recordAudioPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startAudioRecording();
                    } else {
                        Toast.makeText(this, "Microphone permission is required for voice notes", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void launchCamera() {
        try {
            File photoFile = new File(getExternalFilesDir("Pictures"), "img_" + System.currentTimeMillis() + ".jpg");
            cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            takePictureLauncher.launch(cameraImageUri);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to prepare camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            binding.layoutRecordingOverlay.setVisibility(View.VISIBLE);
            binding.ivMicIcon.setImageResource(R.drawable.ic_pause);
            startRecordingTimer();
        } else {
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopAndSendAudio() {
        recordingTimerHandler.removeCallbacksAndMessages(null);
        int duration = audioRecorder.getElapsedDurationSeconds();
        File audioFile = audioRecorder.stopRecording();
        binding.layoutRecordingOverlay.setVisibility(View.GONE);
        binding.ivMicIcon.setImageResource(R.drawable.ic_mic);

        if (audioFile != null && audioFile.exists() && duration >= 1) {
            uploadAndSendAudio(Uri.fromFile(audioFile), duration);
        } else if (duration < 1) {
            Toast.makeText(this, "Audio too short", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelAudioRecording() {
        recordingTimerHandler.removeCallbacksAndMessages(null);
        audioRecorder.cancelRecording();
        binding.layoutRecordingOverlay.setVisibility(View.GONE);
        binding.ivMicIcon.setImageResource(R.drawable.ic_mic);
    }

    private void startRecordingTimer() {
        recordingTimerHandler.post(new Runnable() {
            @Override
            public void run() {
                if (audioRecorder.isRecording()) {
                    int sec = audioRecorder.getElapsedDurationSeconds();
                    binding.tvRecordingTimer.setText(DateTimeUtils.formatDuration(sec));
                    recordingTimerHandler.postDelayed(this, 500);
                }
            }
        });
    }

    private void sendTextMessage(String text) {
        Message msg = new Message(null, conversationId, currentUserId, currentUserName, text);
        FirebaseManager.getInstance().sendMessage(conversationId, msg, otherUserId, otherUserName, currentUserName);
    }

    private void uploadAndSendMedia(Uri uri, String mediaType) {
        binding.pbUploadProgress.setVisibility(View.VISIBLE);

        String subfolder = Message.TYPE_IMAGE.equals(mediaType) ? "images" : "files";
        String filename = UUID.randomUUID().toString() + "_" + FileUtils.getFileName(this, uri);
        StorageReference ref = FirebaseManager.getInstance().getChatMediaRef(conversationId, subfolder, filename);

        ref.putFile(uri).continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) {
                throw task.getException();
            }
            return ref.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            binding.pbUploadProgress.setVisibility(View.GONE);
            if (task.isSuccessful() && task.getResult() != null) {
                String downloadUrl = task.getResult().toString();
                Message msg = new Message(null, conversationId, currentUserId, currentUserName,
                        Message.TYPE_IMAGE.equals(mediaType) ? "Photo" : FileUtils.getFileName(this, uri));
                msg.setMessageType(mediaType);
                msg.setMediaUrl(downloadUrl);
                msg.setFileName(FileUtils.getFileName(this, uri));
                msg.setFileSize(FileUtils.getFileSize(this, uri));

                FirebaseManager.getInstance().sendMessage(conversationId, msg, otherUserId, otherUserName, currentUserName);
            } else {
                Toast.makeText(this, "Upload failed: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadAndSendAudio(Uri uri, int durationSeconds) {
        binding.pbUploadProgress.setVisibility(View.VISIBLE);

        String filename = UUID.randomUUID().toString() + ".m4a";
        StorageReference ref = FirebaseManager.getInstance().getChatMediaRef(conversationId, "voice", filename);

        ref.putFile(uri).continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) {
                throw task.getException();
            }
            return ref.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            binding.pbUploadProgress.setVisibility(View.GONE);
            if (task.isSuccessful() && task.getResult() != null) {
                String downloadUrl = task.getResult().toString();
                Message msg = new Message(null, conversationId, currentUserId, currentUserName, "Voice note");
                msg.setMessageType(Message.TYPE_AUDIO);
                msg.setMediaUrl(downloadUrl);
                msg.setAudioDurationSeconds(durationSeconds);

                FirebaseManager.getInstance().sendMessage(conversationId, msg, otherUserId, otherUserName, currentUserName);
            } else {
                Toast.makeText(this, "Voice note upload failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCurrentUserInfo() {
        FirebaseManager.getInstance().getUserRef(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    if (user.getFullName() != null) {
                        currentUserName = user.getFullName();
                    }
                    currentUserAvatar = user.getProfileImageUrl();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void observePresence() {
        if (otherUserId == null) return;

        presenceRef = FirebaseManager.getInstance().getStatusRef(otherUserId);
        presenceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isOnline = snapshot.child("online").getValue(Boolean.class);
                Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);

                boolean online = isOnline != null && isOnline;
                binding.viewOnlineDot.setVisibility(online ? View.VISIBLE : View.GONE);
                binding.tvChatUserStatus.setText(DateTimeUtils.formatLastSeen(lastSeen != null ? lastSeen : 0, online));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        presenceRef.addValueEventListener(presenceListener);
    }

    private void listenForMessages() {
        messagesRef = FirebaseManager.getInstance().getMessagesRef(conversationId);
        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message msg = snapshot.getValue(Message.class);
                if (msg != null) {
                    messageAdapter.addMessage(msg);
                    binding.rvMessages.scrollToPosition(messageAdapter.getItemCount() - 1);

                    // If incoming, mark as read
                    if (!currentUserId.equals(msg.getSenderId()) && !Message.STATUS_READ.equals(msg.getStatus())) {
                        snapshot.getRef().child("status").setValue(Message.STATUS_READ);
                    }
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
        if (conversationId != null && currentUserId != null) {
            FirebaseManager.getInstance().getUserConversationsRef(currentUserId)
                    .child(conversationId).child("unreadCount").setValue(0);
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
        if (presenceRef != null && presenceListener != null) {
            presenceRef.removeEventListener(presenceListener);
        }
    }
}
