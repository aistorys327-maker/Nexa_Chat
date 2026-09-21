package com.nexachat.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.nexachat.app.adapters.HiddenChatAdapter;
import com.nexachat.app.databinding.ActivityHiddenChatsBinding;
import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.models.Conversation;
import com.nexachat.app.security.HiddenChatManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class HiddenChatsActivity extends AppCompatActivity implements HiddenChatAdapter.OnHiddenChatActionListener {

    private ActivityHiddenChatsBinding binding;
    private HiddenChatAdapter adapter;
    private HiddenChatManager hiddenChatManager;
    private final List<Conversation> hiddenConversations = new ArrayList<>();
    private DatabaseReference conversationsRef;
    private ValueEventListener conversationsListener;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHiddenChatsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        hiddenChatManager = HiddenChatManager.getInstance(this);
        currentUserId = FirebaseManager.getInstance().getCurrentUserId();

        if (currentUserId == null) {
            finish();
            return;
        }

        setupUI();
        loadHiddenConversations();
    }

    private void setupUI() {
        binding.btnBackHidden.setOnClickListener(v -> finish());

        adapter = new HiddenChatAdapter(this, this);
        binding.rvHiddenChats.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHiddenChats.setAdapter(adapter);

        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void loadHiddenConversations() {
        binding.progressBarHidden.setVisibility(View.VISIBLE);
        conversationsRef = FirebaseManager.getInstance().getUserConversationsRef(currentUserId);
        conversationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.progressBarHidden.setVisibility(View.GONE);
                hiddenConversations.clear();
                Set<String> hiddenIds = hiddenChatManager.getHiddenChatIds();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Conversation conv = child.getValue(Conversation.class);
                    if (conv != null && hiddenIds.contains(conv.getConversationId())) {
                        hiddenConversations.add(conv);
                    }
                }

                // Sort descending by timestamp
                Collections.sort(hiddenConversations, (c1, c2) ->
                        Long.compare(c2.getLastMessageTimestamp(), c1.getLastMessageTimestamp()));

                adapter.setConversations(hiddenConversations);
                binding.emptyHiddenChatsLayout.setVisibility(hiddenConversations.isEmpty() ? View.VISIBLE : View.GONE);
                binding.tvHiddenSubtitle.setText(hiddenConversations.size() + " Protected Chat" +
                        (hiddenConversations.size() == 1 ? "" : "s"));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBarHidden.setVisibility(View.GONE);
            }
        };
        conversationsRef.addValueEventListener(conversationsListener);
    }

    @Override
    public void onChatClick(Conversation conversation) {
        // Options: Open Chat or Unhide
        String[] options = new String[]{"Open Protected Chat", "Unhide to Main Chats"};
        new MaterialAlertDialogBuilder(this)
                .setTitle(conversation.getTitle() != null ? conversation.getTitle() : "Protected Chat")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openConversation(conversation);
                    } else {
                        confirmUnhide(conversation);
                    }
                })
                .show();
    }

    @Override
    public void onUnhideClick(Conversation conversation) {
        confirmUnhide(conversation);
    }

    private void confirmUnhide(Conversation conversation) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Unhide Chat")
                .setMessage("Are you sure you want to unhide '" +
                        (conversation.getTitle() != null ? conversation.getTitle() : "this chat") +
                        "'? It will be visible on your main Chats tab again.")
                .setPositiveButton("Unhide", (dialog, which) -> {
                    hiddenChatManager.setChatHidden(conversation.getConversationId(), false);
                    Toast.makeText(this, "Chat unhidden successfully", Toast.LENGTH_SHORT).show();
                    loadHiddenConversations();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openConversation(Conversation conversation) {
        if (conversation.isGroup()) {
            Intent intent = new Intent(this, GroupChatActivity.class);
            intent.putExtra("groupId", conversation.getConversationId());
            intent.putExtra("groupName", conversation.getTitle());
            intent.putExtra("groupPhoto", conversation.getOtherUserAvatarUrl());
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("conversationId", conversation.getConversationId());
            intent.putExtra("otherUserId", conversation.getOtherUserId());
            intent.putExtra("title", conversation.getTitle());
            intent.putExtra("avatarUrl", conversation.getOtherUserAvatarUrl());
            startActivity(intent);
        }
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_hidden_password, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvSetPasswordTitle);
        TextView tvDesc = dialogView.findViewById(R.id.tvSetPasswordDesc);
        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
        TextView tvError = dialogView.findViewById(R.id.tvSetPasswordError);
        Button btnCancel = dialogView.findViewById(R.id.btnCancelSetPassword);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmSetPassword);

        tvTitle.setText("Change Hidden Password");
        tvDesc.setText("Enter a new master password for all your hidden conversations.");
        btnConfirm.setText("Update Password");

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            String p1 = etNewPassword.getText().toString().trim();
            String p2 = etConfirmPassword.getText().toString().trim();

            if (p1.length() < 4) {
                tvError.setText("Password must be at least 4 characters");
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            if (!p1.equals(p2)) {
                tvError.setText("Passwords do not match");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            hiddenChatManager.setPassword(p1);
            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (conversationsRef != null && conversationsListener != null) {
            conversationsRef.removeEventListener(conversationsListener);
        }
    }
}
