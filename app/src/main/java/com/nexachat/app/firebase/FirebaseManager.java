package com.nexachat.app.firebase;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nexachat.app.models.Conversation;
import com.nexachat.app.models.Group;
import com.nexachat.app.models.Message;
import com.nexachat.app.models.User;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    public static final String RTDB_URL = "https://nexachat-200cb-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static FirebaseManager instance;
    private final FirebaseAuth auth;
    private final FirebaseDatabase database;
    private final FirebaseStorage storage;

    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        FirebaseDatabase dbInstance;
        try {
            dbInstance = FirebaseDatabase.getInstance(RTDB_URL);
        } catch (Exception e) {
            Log.w(TAG, "Failed to initialize with RTDB URL, falling back to default instance", e);
            dbInstance = FirebaseDatabase.getInstance();
        }
        database = dbInstance;
        storage = FirebaseStorage.getInstance();
    }

    public FirebaseStorage getStorage() {
        return storage;
    }

    public StorageReference getStorageReference() {
        return storage.getReference();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    // --- Authentication ---
    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseUser getCurrentFirebaseUser() {
        return auth.getCurrentUser();
    }

    public String getCurrentUserId() {
        try {
            FirebaseUser user = auth != null ? auth.getCurrentUser() : null;
            return user != null ? user.getUid() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public boolean isUserLoggedIn() {
        try {
            return auth != null && auth.getCurrentUser() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public void signOut() {
        try {
            setOnlineStatus(false);
            if (auth != null) {
                auth.signOut();
            }
        } catch (Throwable ignored) {}
    }

    // --- Database References ---
    public FirebaseDatabase getDatabase() {
        return database;
    }

    public DatabaseReference getCallsRef() {
        return database.getReference("calls");
    }

    public DatabaseReference getUsersRef() {
        return database.getReference("users");
    }

    public DatabaseReference getUserRef(String uid) {
        String safeUid = (uid != null && !uid.trim().isEmpty()) ? uid : "unknown";
        return database.getReference("users").child(safeUid);
    }

    public DatabaseReference getUsernamesRef() {
        return database.getReference("usernames");
    }

    public DatabaseReference getPhoneNumbersRef() {
        return database.getReference("phone_numbers");
    }

    public DatabaseReference getPhoneNumberRef(String phone) {
        String clean = cleanPhoneNumber(phone);
        String safePhone = clean.isEmpty() ? "unknown" : clean;
        return database.getReference("phone_numbers").child(safePhone);
    }

    public static String cleanPhoneNumber(String raw) {
        if (raw == null) return "";
        String cleaned = raw.replaceAll("[^0-9]", "");
        return cleaned;
    }

    public static String phoneToEmail(String phone) {
        String clean = cleanPhoneNumber(phone);
        return clean + "@nexachat.app";
    }

    public static String normalizePasswordOrPin(String pinOrPass) {
        if (pinOrPass == null) return "";
        if (pinOrPass.length() < 6) {
            return pinOrPass + "_nexa";
        }
        return pinOrPass;
    }

    public DatabaseReference getConversationsRef() {
        return database.getReference("conversations");
    }

    public DatabaseReference getConversationMetaRef(String conversationId) {
        String safeId = (conversationId != null && !conversationId.trim().isEmpty()) ? conversationId : "unknown";
        return database.getReference("conversations").child(safeId).child("meta");
    }

    public DatabaseReference getMessagesRef(String conversationId) {
        String safeId = (conversationId != null && !conversationId.trim().isEmpty()) ? conversationId : "unknown";
        return database.getReference("conversations").child(safeId).child("messages");
    }

    public DatabaseReference getUserConversationsRef(String uid) {
        String safeUid = (uid != null && !uid.trim().isEmpty()) ? uid : "unknown";
        return database.getReference("user_conversations").child(safeUid);
    }

    public DatabaseReference getGroupsRef() {
        return database.getReference("groups");
    }

    public DatabaseReference getGroupRef(String groupId) {
        String safeId = (groupId != null && !groupId.trim().isEmpty()) ? groupId : "unknown";
        return database.getReference("groups").child(safeId);
    }

    public DatabaseReference getStatusRef(String uid) {
        String safeUid = (uid != null && !uid.trim().isEmpty()) ? uid : "unknown";
        return database.getReference("status").child(safeUid);
    }

    // --- Storage References ---
    public StorageReference getStorageRoot() {
        return storage.getReference();
    }

    public StorageReference getProfileImageRef(String uid) {
        return storage.getReference().child("profile_images").child(uid + ".jpg");
    }

    public DatabaseReference getCurrentUserRef() {
        String uid = getCurrentUserId();
        return uid != null ? getUserRef(uid) : null;
    }

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onFailure(Exception e);
    }

    public void uploadProfileImage(android.net.Uri uri, UploadCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null || uri == null) {
            if (callback != null) callback.onFailure(new Exception("User not authenticated"));
            return;
        }
        StorageReference ref = getProfileImageRef(uid);
        ref.putFile(uri).continueWithTask(task -> {
            if (!task.isSuccessful() && task.getException() != null) {
                throw task.getException();
            }
            return ref.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                String downloadUrl = task.getResult().toString();
                getUserRef(uid).child("profileImageUrl").setValue(downloadUrl);
                if (callback != null) callback.onSuccess(downloadUrl);
            } else {
                if (callback != null) {
                    Exception ex = task.getException() != null ? task.getException() : new Exception("Upload failed");
                    callback.onFailure(ex);
                }
            }
        });
    }

    public StorageReference getChatMediaRef(String conversationId, String subfolder, String filename) {
        return storage.getReference().child("chat_media").child(conversationId).child(subfolder).child(filename);
    }

    public StorageReference getGroupPhotoRef(String groupId) {
        return storage.getReference().child("group_photos").child(groupId + ".jpg");
    }

    // --- Deterministic Conversation ID ---
    public static String getOneToOneConversationId(String uid1, String uid2) {
        if (uid1 == null || uid2 == null) return "";
        if (uid1.compareTo(uid2) < 0) {
            return uid1 + "_" + uid2;
        } else {
            return uid2 + "_" + uid1;
        }
    }

    // --- Online Presence Setup ---
    public void setupPresence() {
        String currentUid = getCurrentUserId();
        if (currentUid == null) return;

        DatabaseReference connectedRef = database.getReference(".info/connected");
        DatabaseReference myStatusRef = getStatusRef(currentUid);
        DatabaseReference myUserRef = getUserRef(currentUid);

        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null && connected) {
                    // Set onDisconnect actions
                    Map<String, Object> offlineStatus = new HashMap<>();
                    offlineStatus.put("online", false);
                    offlineStatus.put("lastSeen", ServerValue.TIMESTAMP);

                    myStatusRef.onDisconnect().setValue(offlineStatus);
                    myUserRef.child("online").onDisconnect().setValue(false);
                    myUserRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP);

                    // Set online now
                    Map<String, Object> onlineStatus = new HashMap<>();
                    onlineStatus.put("online", true);
                    onlineStatus.put("lastSeen", ServerValue.TIMESTAMP);

                    myStatusRef.setValue(onlineStatus);
                    myUserRef.child("online").setValue(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Presence listener cancelled", error.toException());
            }
        });
    }

    public void setOnlineStatus(boolean online) {
        String currentUid = getCurrentUserId();
        if (currentUid == null) return;

        Map<String, Object> status = new HashMap<>();
        status.put("online", online);
        status.put("lastSeen", ServerValue.TIMESTAMP);

        getStatusRef(currentUid).setValue(status);
        getUserRef(currentUid).child("online").setValue(online);
        if (!online) {
            getUserRef(currentUid).child("lastSeen").setValue(ServerValue.TIMESTAMP);
        }
    }

    // --- Sync FCM Token ---
    public void updateFcmToken(String token) {
        String uid = getCurrentUserId();
        if (uid != null && token != null && !token.trim().isEmpty()) {
            getUserRef(uid).child("fcmToken").setValue(token);
        }
    }

    public void syncFcmToken() {
        String uid = getCurrentUserId();
        if (uid == null) return;

        try {
            Context context = FirebaseApp.getInstance().getApplicationContext();
            GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
            int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
            if (resultCode != ConnectionResult.SUCCESS) {
                Log.d(TAG, "Google Play Services not ready/available for FCM (code: " + resultCode + "), skipping FCM token sync.");
                return;
            }

            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    updateFcmToken(task.getResult());
                } else {
                    Log.d(TAG, "FCM token not available: " +
                            (task.getException() != null ? task.getException().getMessage() : "unsuccessful"));
                }
            });
        } catch (Throwable t) {
            Log.d(TAG, "FCM registration not supported in current environment: " + t.getMessage());
        }
    }

    // --- Send Message ---
    public Task<Void> sendMessage(String conversationId, Message message, String otherUserId, String otherUserName, String currentUserName) {
        DatabaseReference msgRef = getMessagesRef(conversationId).push();
        String messageId = msgRef.getKey();
        message.setMessageId(messageId);
        message.setConversationId(conversationId);

        String currentUid = getCurrentUserId();

        Map<String, Object> updates = new HashMap<>();
        // 1. Message object in conversation
        updates.put("conversations/" + conversationId + "/messages/" + messageId, message);

        // 2. Conversation metadata updates
        updates.put("conversations/" + conversationId + "/meta/lastMessage", message.getText());
        updates.put("conversations/" + conversationId + "/meta/lastMessageTimestamp", message.getTimestamp());
        updates.put("conversations/" + conversationId + "/meta/lastSenderId", currentUid);

        // 3. User conversations index for sender
        updates.put("user_conversations/" + currentUid + "/" + conversationId + "/conversationId", conversationId);
        updates.put("user_conversations/" + currentUid + "/" + conversationId + "/title", otherUserName);
        updates.put("user_conversations/" + currentUid + "/" + conversationId + "/otherUserId", otherUserId);
        updates.put("user_conversations/" + currentUid + "/" + conversationId + "/lastMessage", message.getText());
        updates.put("user_conversations/" + currentUid + "/" + conversationId + "/lastMessageTimestamp", message.getTimestamp());
        updates.put("user_conversations/" + currentUid + "/" + conversationId + "/lastSenderId", currentUid);

        // 4. User conversations index for recipient
        if (otherUserId != null) {
            updates.put("user_conversations/" + otherUserId + "/" + conversationId + "/conversationId", conversationId);
            updates.put("user_conversations/" + otherUserId + "/" + conversationId + "/title", currentUserName);
            updates.put("user_conversations/" + otherUserId + "/" + conversationId + "/otherUserId", currentUid);
            updates.put("user_conversations/" + otherUserId + "/" + conversationId + "/lastMessage", message.getText());
            updates.put("user_conversations/" + otherUserId + "/" + conversationId + "/lastMessageTimestamp", message.getTimestamp());
            updates.put("user_conversations/" + otherUserId + "/" + conversationId + "/lastSenderId", currentUid);
            updates.put("user_conversations/" + otherUserId + "/" + conversationId + "/unreadCount", ServerValue.increment(1));
        }

        return database.getReference().updateChildren(updates);
    }

    // --- Send Group Message ---
    public Task<Void> sendGroupMessage(String groupId, Message message, Group group) {
        DatabaseReference msgRef = getMessagesRef(groupId).push();
        String messageId = msgRef.getKey();
        message.setMessageId(messageId);
        message.setConversationId(groupId);

        String currentUid = getCurrentUserId();

        Map<String, Object> updates = new HashMap<>();
        updates.put("conversations/" + groupId + "/messages/" + messageId, message);
        updates.put("conversations/" + groupId + "/meta/lastMessage", message.getSenderName() + ": " + message.getText());
        updates.put("conversations/" + groupId + "/meta/lastMessageTimestamp", message.getTimestamp());
        updates.put("conversations/" + groupId + "/meta/lastSenderId", currentUid);

        if (group.getMembers() != null) {
            for (String memberId : group.getMembers().keySet()) {
                updates.put("user_conversations/" + memberId + "/" + groupId + "/conversationId", groupId);
                updates.put("user_conversations/" + memberId + "/" + groupId + "/title", group.getGroupName());
                updates.put("user_conversations/" + memberId + "/" + groupId + "/isGroup", true);
                updates.put("user_conversations/" + memberId + "/" + groupId + "/otherUserAvatarUrl", group.getGroupPhotoUrl());
                updates.put("user_conversations/" + memberId + "/" + groupId + "/lastMessage", message.getSenderName() + ": " + message.getText());
                updates.put("user_conversations/" + memberId + "/" + groupId + "/lastMessageTimestamp", message.getTimestamp());
                updates.put("user_conversations/" + memberId + "/" + groupId + "/lastSenderId", currentUid);
                if (!memberId.equals(currentUid)) {
                    updates.put("user_conversations/" + memberId + "/" + groupId + "/unreadCount", ServerValue.increment(1));
                }
            }
        }

        return database.getReference().updateChildren(updates);
    }
}
