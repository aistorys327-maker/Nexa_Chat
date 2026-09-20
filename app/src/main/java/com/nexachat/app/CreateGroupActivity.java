package com.nexachat.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.nexachat.app.adapters.SelectMemberAdapter;
import com.nexachat.app.databinding.ActivityCreateGroupBinding;
import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.models.Group;
import com.nexachat.app.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CreateGroupActivity extends AppCompatActivity {

    private ActivityCreateGroupBinding binding;
    private SelectMemberAdapter adapter;
    private Uri selectedGroupPhotoUri;
    private String currentUserId;
    private ActivityResultLauncher<PickVisualMediaRequest> pickPhotoLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        if (currentUserId == null) {
            finish();
            return;
        }

        setupUI();
        loadAvailableMembers();
    }

    private void setupUI() {
        binding.btnCreateGroupBack.setOnClickListener(v -> finish());

        adapter = new SelectMemberAdapter(this);
        binding.rvSelectMembers.setLayoutManager(new LinearLayoutManager(this));
        binding.rvSelectMembers.setAdapter(adapter);

        pickPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        selectedGroupPhotoUri = uri;
                        binding.ivNewGroupAvatar.setImageURI(uri);
                    }
                });

        binding.btnSelectGroupPhoto.setOnClickListener(v ->
                pickPhotoLauncher.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build())
        );

        binding.btnCreateGroupSubmit.setOnClickListener(v -> validateAndCreateGroup());
    }

    private void loadAvailableMembers() {
        FirebaseManager.getInstance().getUsersRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    User user = child.getValue(User.class);
                    if (user != null && !user.getUid().equals(currentUserId)) {
                        list.add(user);
                    }
                }
                adapter.setUsers(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void validateAndCreateGroup() {
        String groupName = binding.etNewGroupName.getText().toString().trim();
        Set<String> selectedMembers = adapter.getSelectedUserIds();

        if (TextUtils.isEmpty(groupName)) {
            binding.etNewGroupName.setError("Please enter a group name");
            binding.etNewGroupName.requestFocus();
            return;
        }

        if (selectedMembers.isEmpty()) {
            Toast.makeText(this, "Please select at least 1 member for the group", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        DatabaseReference newGroupRef = FirebaseManager.getInstance().getGroupsRef().push();
        String groupId = newGroupRef.getKey();

        if (selectedGroupPhotoUri != null) {
            uploadPhotoAndSaveGroup(groupId, groupName, selectedMembers);
        } else {
            saveGroupToDatabase(groupId, groupName, null, selectedMembers);
        }
    }

    private void uploadPhotoAndSaveGroup(String groupId, String groupName, Set<String> members) {
        StorageReference photoRef = FirebaseManager.getInstance().getGroupPhotoRef(groupId);
        photoRef.putFile(selectedGroupPhotoUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return photoRef.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        saveGroupToDatabase(groupId, groupName, task.getResult().toString(), members);
                    } else {
                        // Fallback without photo if upload fails
                        saveGroupToDatabase(groupId, groupName, null, members);
                    }
                });
    }

    private void saveGroupToDatabase(String groupId, String groupName, String photoUrl, Set<String> members) {
        Group group = new Group(groupId, groupName, photoUrl, currentUserId, System.currentTimeMillis());

        Map<String, Boolean> membersMap = new HashMap<>();
        membersMap.put(currentUserId, true);
        for (String m : members) {
            membersMap.put(m, true);
        }
        group.setMembers(membersMap);

        Map<String, Boolean> adminsMap = new HashMap<>();
        adminsMap.put(currentUserId, true);
        group.setAdminIds(adminsMap);

        Map<String, Object> updates = new HashMap<>();
        updates.put("groups/" + groupId, group);
        updates.put("conversations/" + groupId + "/meta/conversationId", groupId);
        updates.put("conversations/" + groupId + "/meta/title", groupName);
        updates.put("conversations/" + groupId + "/meta/isGroup", true);
        updates.put("conversations/" + groupId + "/meta/lastMessage", "Group created");
        updates.put("conversations/" + groupId + "/meta/lastMessageTimestamp", System.currentTimeMillis());

        for (String memberId : membersMap.keySet()) {
            updates.put("user_conversations/" + memberId + "/" + groupId + "/conversationId", groupId);
            updates.put("user_conversations/" + memberId + "/" + groupId + "/title", groupName);
            updates.put("user_conversations/" + memberId + "/" + groupId + "/isGroup", true);
            updates.put("user_conversations/" + memberId + "/" + groupId + "/otherUserAvatarUrl", photoUrl);
            updates.put("user_conversations/" + memberId + "/" + groupId + "/lastMessage", "Group created");
            updates.put("user_conversations/" + memberId + "/" + groupId + "/lastMessageTimestamp", System.currentTimeMillis());
            updates.put("user_conversations/" + memberId + "/" + groupId + "/unreadCount", 0);
        }

        FirebaseManager.getInstance().getUsersRef().getRoot().updateChildren(updates)
                .addOnCompleteListener(task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Group created successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(CreateGroupActivity.this, GroupChatActivity.class);
                        intent.putExtra("groupId", groupId);
                        intent.putExtra("groupName", groupName);
                        intent.putExtra("groupPhoto", photoUrl);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to create group", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        binding.btnCreateGroupSubmit.setEnabled(!isLoading);
        binding.btnCreateGroupSubmit.setText(isLoading ? "Creating..." : "Create");
    }
}
