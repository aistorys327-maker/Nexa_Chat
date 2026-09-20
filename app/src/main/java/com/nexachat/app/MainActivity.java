package com.nexachat.app;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.nexachat.app.adapters.ContactsAdapter;
import com.nexachat.app.adapters.ConversationAdapter;
import com.nexachat.app.adapters.GroupAdapter;
import com.nexachat.app.adapters.StatusAdapter;
import com.nexachat.app.databinding.ActivityMainBinding;
import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.models.CallSession;
import com.nexachat.app.models.ContactItem;
import com.nexachat.app.models.Conversation;
import com.nexachat.app.models.StatusItem;
import com.nexachat.app.models.User;
import com.nexachat.app.models.UserStatusGroup;
import com.nexachat.app.security.ChatLockManager;
import com.nexachat.app.security.SecurityHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements ContactsAdapter.OnContactClickListener {

    private ActivityMainBinding binding;
    private String currentUserId;
    private User currentUser;

    // Tab indices: 0 = Chat, 1 = Update, 2 = Group, 3 = Calls
    private int currentTab = 0;

    // 1. Chats Tab
    private ConversationAdapter conversationAdapter;
    private final List<Conversation> allConversations = new ArrayList<>();
    private DatabaseReference conversationsRef;
    private ValueEventListener conversationsListener;

    // 2. Updates Tab
    private StatusAdapter statusAdapter;
    private final List<UserStatusGroup> allStatusGroups = new ArrayList<>();
    private DatabaseReference statusesRef;
    private ValueEventListener statusesListener;
    private UserStatusGroup myStatusGroup;

    // 3. Groups Tab
    private GroupAdapter groupAdapter;
    private final List<Conversation> groupConversations = new ArrayList<>();

    // 4. Calls / Mobile Contacts Tab
    private ContactsAdapter contactsAdapter;
    private final List<ContactItem> rawDeviceContacts = new ArrayList<>();
    private final Map<String, User> registeredPhoneToUserMap = new HashMap<>();
    private final List<ContactItem> onAppContacts = new ArrayList<>();
    private final List<ContactItem> inviteContacts = new ArrayList<>();
    private final List<ContactItem> combinedContacts = new ArrayList<>();
    private int callsFilterIndex = 0; // 0 = All, 1 = On App, 2 = Invite

    private ActivityResultLauncher<String> notificationPermissionLauncher;
    private ActivityResultLauncher<String> contactsPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. App Disguise / Camouflage Check FIRST before doing any user operations
        try {
            if (com.nexachat.app.security.DisguiseManager.getInstance().isDisguiseEnabled(this) &&
                    !getIntent().getBooleanExtra("unlocked_via_face", false)) {
                Intent disguiseIntent = new Intent(this, CamouflageGatewayActivity.class);
                startActivity(disguiseIntent);
                finish();
                return;
            }
        } catch (Throwable t) {
            Log.e("MainActivity", "Error in disguise check", t);
        }

        // 2. Authentication guard
        if (!FirebaseManager.getInstance().isUserLoggedIn() || FirebaseManager.getInstance().getCurrentUserId() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupPermissions();
        setupNavigation();
        setupChatsTab();
        setupUpdatesTab();
        setupGroupsTab();
        setupCallsTab();

        loadCurrentUserData();
        listenForConversations();
        listenForStatuses();

        // Switch to initial tab (Chat)
        switchTab(0);
    }

    private void setupPermissions() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {}
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        contactsPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        binding.cardCallsPermission.setVisibility(View.GONE);
                        loadDeviceContactsAndSync();
                    } else {
                        binding.cardCallsPermission.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Contacts permission is needed to show mobile contacts", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupNavigation() {
        // Header profile button
        binding.profileButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        // Header settings button (profile ka bagal ma setting icon)
        binding.btnSettings.setOnClickListener(v -> {
            SettingsBottomSheet.newInstance().show(getSupportFragmentManager(), "settings_sheet");
        });

        // Bottom Navigation Tabs
        binding.tabChats.setOnClickListener(v -> switchTab(0));
        binding.tabUpdates.setOnClickListener(v -> switchTab(1));
        binding.tabGroups.setOnClickListener(v -> switchTab(2));
        binding.tabCalls.setOnClickListener(v -> switchTab(3));
    }

    private void switchTab(int tabIndex) {
        currentTab = tabIndex;

        // Reset all navigation icons and texts
        int colorAccent = ContextCompat.getColor(this, R.color.cyan_accent);
        int colorSecondary = ContextCompat.getColor(this, R.color.text_secondary);

        binding.ivTabChats.setColorFilter(tabIndex == 0 ? colorAccent : colorSecondary);
        binding.tvTabChats.setTextColor(tabIndex == 0 ? colorAccent : colorSecondary);

        binding.ivTabUpdates.setColorFilter(tabIndex == 1 ? colorAccent : colorSecondary);
        binding.tvTabUpdates.setTextColor(tabIndex == 1 ? colorAccent : colorSecondary);

        binding.ivTabGroups.setColorFilter(tabIndex == 2 ? colorAccent : colorSecondary);
        binding.tvTabGroups.setTextColor(tabIndex == 2 ? colorAccent : colorSecondary);

        binding.ivTabCalls.setColorFilter(tabIndex == 3 ? colorAccent : colorSecondary);
        binding.tvTabCalls.setTextColor(tabIndex == 3 ? colorAccent : colorSecondary);

        // Visibility of view containers
        binding.chatsViewContainer.setVisibility(tabIndex == 0 ? View.VISIBLE : View.GONE);
        binding.updatesViewContainer.setVisibility(tabIndex == 1 ? View.VISIBLE : View.GONE);
        binding.groupsViewContainer.setVisibility(tabIndex == 2 ? View.VISIBLE : View.GONE);
        binding.callsViewContainer.setVisibility(tabIndex == 3 ? View.VISIBLE : View.GONE);

        // Update Header Tagline
        switch (tabIndex) {
            case 0:
                binding.tvAppTagline.setText("Messages & Conversations");
                break;
            case 1:
                binding.tvAppTagline.setText("Status Updates • 24h Expiry");
                break;
            case 2:
                binding.tvAppTagline.setText("Your Groups & Communities");
                break;
            case 3:
                binding.tvAppTagline.setText("Calls & Mobile Contacts");
                checkAndLoadContactsIfNeeded();
                break;
        }
    }

    // ==========================================
    // 1. CHATS TAB
    // ==========================================
    private void setupChatsTab() {
        conversationAdapter = new ConversationAdapter(this, conversation -> {
            ChatLockManager lockManager = ChatLockManager.getInstance(MainActivity.this);
            if (lockManager.isChatLocked(conversation.getConversationId()) && !lockManager.isSessionUnlocked(conversation.getConversationId())) {
                SecurityHelper.authenticate(MainActivity.this, "Secured Conversation", "Verify your identity to open this chat",
                        new SecurityHelper.AuthCallback() {
                            @Override
                            public void onSuccess() {
                                lockManager.markSessionUnlocked(conversation.getConversationId());
                                openConversation(conversation);
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Toast.makeText(MainActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                openConversation(conversation);
            }
        });

        binding.rvConversations.setLayoutManager(new LinearLayoutManager(this));
        binding.rvConversations.setAdapter(conversationAdapter);

        binding.etSearchConversations.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterConversations(s != null ? s.toString().trim() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.swipeRefreshChats.setOnRefreshListener(() -> {
            binding.swipeRefreshChats.setRefreshing(false);
        });

        binding.btnEmptyStartChat.setOnClickListener(v -> switchTab(3)); // Switch to calls & contacts tab
        binding.fabNewChat.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, NewChatActivity.class));
        });
    }

    private void openConversation(Conversation conversation) {
        if (conversation.isGroup()) {
            Intent intent = new Intent(MainActivity.this, GroupChatActivity.class);
            intent.putExtra("groupId", conversation.getConversationId());
            intent.putExtra("groupName", conversation.getTitle());
            intent.putExtra("groupPhoto", conversation.getOtherUserAvatarUrl());
            startActivity(intent);
        } else {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("conversationId", conversation.getConversationId());
            intent.putExtra("otherUserId", conversation.getOtherUserId());
            intent.putExtra("title", conversation.getTitle());
            intent.putExtra("avatarUrl", conversation.getOtherUserAvatarUrl());
            startActivity(intent);
        }
    }

    private void listenForConversations() {
        binding.swipeRefreshChats.setRefreshing(true);
        conversationsRef = FirebaseManager.getInstance().getUserConversationsRef(currentUserId);
        conversationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.swipeRefreshChats.setRefreshing(false);
                allConversations.clear();
                groupConversations.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Conversation conv = child.getValue(Conversation.class);
                    if (conv != null) {
                        allConversations.add(conv);
                        if (conv.isGroup()) {
                            groupConversations.add(conv);
                        }
                    }
                }

                // Sort descending by timestamp
                Collections.sort(allConversations, (c1, c2) ->
                        Long.compare(c2.getLastMessageTimestamp(), c1.getLastMessageTimestamp()));
                Collections.sort(groupConversations, (c1, c2) ->
                        Long.compare(c2.getLastMessageTimestamp(), c1.getLastMessageTimestamp()));

                filterConversations(binding.etSearchConversations.getText().toString().trim());
                filterGroups(binding.etSearchGroups.getText().toString().trim());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.swipeRefreshChats.setRefreshing(false);
            }
        };
        conversationsRef.addValueEventListener(conversationsListener);
    }

    private void filterConversations(String query) {
        List<Conversation> filtered = new ArrayList<>();
        String lower = query.toLowerCase();

        for (Conversation c : allConversations) {
            if (TextUtils.isEmpty(query)) {
                filtered.add(c);
            } else {
                boolean titleMatches = c.getTitle() != null && c.getTitle().toLowerCase().contains(lower);
                boolean messageMatches = c.getLastMessage() != null && c.getLastMessage().toLowerCase().contains(lower);
                if (titleMatches || messageMatches) {
                    filtered.add(c);
                }
            }
        }

        conversationAdapter.setConversations(filtered);
        binding.emptyChatsLayout.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ==========================================
    // 2. UPDATES / STATUS TAB
    // ==========================================
    private void setupUpdatesTab() {
        statusAdapter = new StatusAdapter(this, group -> {
            Intent intent = new Intent(MainActivity.this, StatusViewerActivity.class);
            intent.putExtra(StatusViewerActivity.EXTRA_STATUS_GROUP, group);
            startActivity(intent);
        });

        binding.rvRecentUpdates.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRecentUpdates.setAdapter(statusAdapter);

        // My Status Card tap
        binding.cardMyStatus.setOnClickListener(v -> {
            if (myStatusGroup != null && !myStatusGroup.getStatusList().isEmpty()) {
                // View own status
                Intent intent = new Intent(MainActivity.this, StatusViewerActivity.class);
                intent.putExtra(StatusViewerActivity.EXTRA_STATUS_GROUP, myStatusGroup);
                startActivity(intent);
            } else {
                // Add new status
                startActivity(new Intent(MainActivity.this, AddStatusActivity.class));
            }
        });

        binding.btnQuickTextStatus.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddStatusActivity.class));
        });

        binding.btnQuickPhotoStatus.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddStatusActivity.class));
        });

        binding.fabNewStatus.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AddStatusActivity.class));
        });
    }

    private void listenForStatuses() {
        statusesRef = FirebaseManager.getInstance().getUsersRef().getRoot().child("statuses");
        statusesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allStatusGroups.clear();
                myStatusGroup = null;

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String userId = userSnap.getKey();
                    if (userId == null) continue;

                    UserStatusGroup group = new UserStatusGroup();
                    group.setUserId(userId);

                    for (DataSnapshot itemSnap : userSnap.getChildren()) {
                        StatusItem item = itemSnap.getValue(StatusItem.class);
                        if (item != null) {
                            // Enforce 24 hour expiry requirement!
                            if (!item.isExpired()) {
                                group.addStatus(item);
                                if (group.getUserName() == null) group.setUserName(item.getUserName());
                                if (group.getUserAvatarUrl() == null) group.setUserAvatarUrl(item.getUserAvatarUrl());
                            }
                        }
                    }

                    if (!group.getStatusList().isEmpty()) {
                        if (userId.equals(currentUserId)) {
                            myStatusGroup = group;
                        } else {
                            allStatusGroups.add(group);
                        }
                    }
                }

                // Sort other status groups descending by latest timestamp
                Collections.sort(allStatusGroups, (g1, g2) -> Long.compare(g2.getLatestTimestamp(), g1.getLatestTimestamp()));

                updateMyStatusUI();
                statusAdapter.updateList(allStatusGroups);
                binding.emptyUpdatesLayout.setVisibility(allStatusGroups.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        statusesRef.addValueEventListener(statusesListener);
    }

    private void updateMyStatusUI() {
        if (myStatusGroup != null && !myStatusGroup.getStatusList().isEmpty()) {
            binding.tvMyStatusSubtitle.setText("Active status • Tap to view");
            binding.ivMyStatusAddBadge.setVisibility(View.GONE);
            binding.layoutMyStatusRing.setBackgroundResource(R.drawable.circle_status_ring_active);
        } else {
            binding.tvMyStatusSubtitle.setText(R.string.subtitle_tap_add_status);
            binding.ivMyStatusAddBadge.setVisibility(View.VISIBLE);
            binding.layoutMyStatusRing.setBackgroundResource(R.drawable.circle_avatar_placeholder);
        }
    }

    // ==========================================
    // 3. GROUPS TAB
    // ==========================================
    private void setupGroupsTab() {
        groupAdapter = new GroupAdapter(this, group -> {
            Intent intent = new Intent(MainActivity.this, GroupChatActivity.class);
            intent.putExtra("groupId", group.getConversationId());
            intent.putExtra("groupName", group.getTitle());
            intent.putExtra("groupPhoto", group.getOtherUserAvatarUrl());
            startActivity(intent);
        });

        binding.rvGroups.setLayoutManager(new LinearLayoutManager(this));
        binding.rvGroups.setAdapter(groupAdapter);

        binding.btnCreateGroupBanner.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CreateGroupActivity.class));
        });

        binding.btnEmptyCreateGroup.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CreateGroupActivity.class));
        });

        binding.fabNewGroup.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CreateGroupActivity.class));
        });

        binding.etSearchGroups.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterGroups(s != null ? s.toString().trim() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterGroups(String query) {
        List<Conversation> filtered = new ArrayList<>();
        String lower = query.toLowerCase();

        for (Conversation g : groupConversations) {
            if (TextUtils.isEmpty(query)) {
                filtered.add(g);
            } else {
                boolean nameMatches = g.getTitle() != null && g.getTitle().toLowerCase().contains(lower);
                boolean msgMatches = g.getLastMessage() != null && g.getLastMessage().toLowerCase().contains(lower);
                if (nameMatches || msgMatches) {
                    filtered.add(g);
                }
            }
        }

        groupAdapter.setGroups(filtered);
        binding.emptyGroupsLayout.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ==========================================
    // 4. CALLS & MOBILE CONTACTS TAB
    // ==========================================
    private void setupCallsTab() {
        contactsAdapter = new ContactsAdapter(this, this);
        binding.rvCallsContacts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvCallsContacts.setAdapter(contactsAdapter);

        binding.btnCallsGrantPermission.setOnClickListener(v -> {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        });

        binding.swipeRefreshCalls.setOnRefreshListener(() -> {
            loadDeviceContactsAndSync();
        });

        // Filter chips
        binding.chipCallsAll.setOnClickListener(v -> setCallsFilter(0));
        binding.chipCallsOnApp.setOnClickListener(v -> setCallsFilter(1));
        binding.chipCallsInvite.setOnClickListener(v -> setCallsFilter(2));

        binding.etSearchCallsContacts.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCallsContacts(s != null ? s.toString().trim() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setCallsFilter(int index) {
        callsFilterIndex = index;

        binding.chipCallsAll.setBackgroundResource(index == 0 ? R.drawable.bg_pill_active : R.drawable.bg_pill_inactive);
        binding.chipCallsAll.setTextColor(ContextCompat.getColor(this, index == 0 ? R.color.bg_dark_navy : R.color.text_secondary));

        binding.chipCallsOnApp.setBackgroundResource(index == 1 ? R.drawable.bg_pill_active : R.drawable.bg_pill_inactive);
        binding.chipCallsOnApp.setTextColor(ContextCompat.getColor(this, index == 1 ? R.color.bg_dark_navy : R.color.text_secondary));

        binding.chipCallsInvite.setBackgroundResource(index == 2 ? R.drawable.bg_pill_active : R.drawable.bg_pill_inactive);
        binding.chipCallsInvite.setTextColor(ContextCompat.getColor(this, index == 2 ? R.color.bg_dark_navy : R.color.text_secondary));

        filterCallsContacts(binding.etSearchCallsContacts.getText().toString().trim());
    }

    private void checkAndLoadContactsIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            binding.cardCallsPermission.setVisibility(View.VISIBLE);
        } else {
            binding.cardCallsPermission.setVisibility(View.GONE);
            if (rawDeviceContacts.isEmpty()) {
                loadDeviceContactsAndSync();
            }
        }
    }

    private void loadDeviceContactsAndSync() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            binding.swipeRefreshCalls.setRefreshing(false);
            binding.cardCallsPermission.setVisibility(View.VISIBLE);
            return;
        }

        binding.swipeRefreshCalls.setRefreshing(true);
        binding.pbCallsLoading.setVisibility(View.VISIBLE);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<ContactItem> fetched = new ArrayList<>();
            Set<String> seenNumbers = new HashSet<>();

            ContentResolver cr = getContentResolver();
            Cursor cursor = null;
            try {
                cursor = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                ContactsContract.CommonDataKinds.Phone.NUMBER,
                                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                        },
                        null,
                        null,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                );

                if (cursor != null && cursor.moveToFirst()) {
                    int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    int numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    int photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI);

                    do {
                        String name = cursor.getString(nameIdx);
                        String rawNumber = cursor.getString(numIdx);
                        String photoUri = photoIdx != -1 ? cursor.getString(photoIdx) : null;

                        if (TextUtils.isEmpty(rawNumber) || TextUtils.isEmpty(name)) continue;

                        String clean = cleanPhoneNumber(rawNumber);
                        if (!seenNumbers.contains(clean)) {
                            seenNumbers.add(clean);
                            fetched.add(new ContactItem(name, rawNumber, photoUri, false, null));
                        }
                    } while (cursor.moveToNext());
                }
            } catch (Exception ignored) {
            } finally {
                if (cursor != null) cursor.close();
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                rawDeviceContacts.clear();
                rawDeviceContacts.addAll(fetched);
                syncWithFirebaseUsers();
            });
        });
    }

    private void syncWithFirebaseUsers() {
        FirebaseManager.getInstance().getUsersRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.swipeRefreshCalls.setRefreshing(false);
                binding.pbCallsLoading.setVisibility(View.GONE);

                registeredPhoneToUserMap.clear();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    User user = userSnap.getValue(User.class);
                    if (user != null && !user.getUid().equals(currentUserId)) {
                        if (!TextUtils.isEmpty(user.getPhoneNumber())) {
                            registeredPhoneToUserMap.put(cleanPhoneNumber(user.getPhoneNumber()), user);
                        }
                    }
                }

                onAppContacts.clear();
                inviteContacts.clear();
                combinedContacts.clear();

                for (ContactItem item : rawDeviceContacts) {
                    String clean = cleanPhoneNumber(item.getPhoneNumber());
                    User matchedUser = findMatchedUser(clean);

                    if (matchedUser != null) {
                        item.setOnApp(true);
                        item.setUser(matchedUser);
                        onAppContacts.add(item);
                    } else {
                        item.setOnApp(false);
                        item.setUser(null);
                        inviteContacts.add(item);
                    }
                }

                // In combined, contacts on NexaChat appear first!
                combinedContacts.addAll(onAppContacts);
                combinedContacts.addAll(inviteContacts);

                filterCallsContacts(binding.etSearchCallsContacts.getText().toString().trim());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.swipeRefreshCalls.setRefreshing(false);
                binding.pbCallsLoading.setVisibility(View.GONE);
            }
        });
    }

    private User findMatchedUser(String cleanContactNumber) {
        if (registeredPhoneToUserMap.containsKey(cleanContactNumber)) {
            return registeredPhoneToUserMap.get(cleanContactNumber);
        }
        for (Map.Entry<String, User> entry : registeredPhoneToUserMap.entrySet()) {
            String registered = entry.getKey();
            if (cleanContactNumber.endsWith(registered) || registered.endsWith(cleanContactNumber)) {
                if (cleanContactNumber.length() >= 8 && registered.length() >= 8) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String cleanPhoneNumber(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9+]", "");
    }

    private void filterCallsContacts(String query) {
        List<ContactItem> source;
        if (callsFilterIndex == 1) {
            source = onAppContacts;
        } else if (callsFilterIndex == 2) {
            source = inviteContacts;
        } else {
            source = combinedContacts;
        }

        List<ContactItem> filtered = new ArrayList<>();
        String lower = query.toLowerCase();

        for (ContactItem item : source) {
            boolean matches = TextUtils.isEmpty(query)
                    || item.getName().toLowerCase().contains(lower)
                    || item.getPhoneNumber().toLowerCase().contains(lower);

            if (!matches && item.getUser() != null) {
                if (!TextUtils.isEmpty(item.getUser().getUsername()) && item.getUser().getUsername().toLowerCase().contains(lower)) {
                    matches = true;
                }
            }

            if (matches) {
                filtered.add(item);
            }
        }

        contactsAdapter.updateList(filtered);
        binding.emptyCallsLayout.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ContactsAdapter Callback: Message click -> Opens Chat & Ensures conversation is added to Chats tab
    @Override
    public void onMessage(ContactItem contact) {
        if (contact.getUser() == null || currentUserId == null) return;
        User otherUser = contact.getUser();

        String conversationId = FirebaseManager.getOneToOneConversationId(currentUserId, otherUser.getUid());

        // Immediately add to user_conversations for both users so it appears in the Chats tab!
        Map<String, Object> updates = new HashMap<>();
        updates.put("user_conversations/" + currentUserId + "/" + conversationId + "/conversationId", conversationId);
        updates.put("user_conversations/" + currentUserId + "/" + conversationId + "/title", otherUser.getFullName());
        updates.put("user_conversations/" + currentUserId + "/" + conversationId + "/otherUserId", otherUser.getUid());
        updates.put("user_conversations/" + currentUserId + "/" + conversationId + "/otherUserAvatarUrl", otherUser.getProfileImageUrl());
        updates.put("user_conversations/" + currentUserId + "/" + conversationId + "/otherUsername", otherUser.getUsername());
        updates.put("user_conversations/" + currentUserId + "/" + conversationId + "/isGroup", false);
        updates.put("user_conversations/" + currentUserId + "/" + conversationId + "/lastMessage", "Started conversation");
        updates.put("user_conversations/" + currentUserId + "/" + conversationId + "/lastMessageTimestamp", System.currentTimeMillis());

        updates.put("user_conversations/" + otherUser.getUid() + "/" + conversationId + "/conversationId", conversationId);
        updates.put("user_conversations/" + otherUser.getUid() + "/" + conversationId + "/title", currentUser != null ? currentUser.getFullName() : "NexaChat User");
        updates.put("user_conversations/" + otherUser.getUid() + "/" + conversationId + "/otherUserId", currentUserId);
        updates.put("user_conversations/" + otherUser.getUid() + "/" + conversationId + "/otherUserAvatarUrl", currentUser != null ? currentUser.getProfileImageUrl() : "");
        updates.put("user_conversations/" + otherUser.getUid() + "/" + conversationId + "/isGroup", false);
        updates.put("user_conversations/" + otherUser.getUid() + "/" + conversationId + "/lastMessage", "Started conversation");
        updates.put("user_conversations/" + otherUser.getUid() + "/" + conversationId + "/lastMessageTimestamp", System.currentTimeMillis());

        FirebaseManager.getInstance().getUsersRef().getRoot().updateChildren(updates);

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("conversationId", conversationId);
        intent.putExtra("otherUserId", otherUser.getUid());
        intent.putExtra("title", otherUser.getFullName());
        intent.putExtra("avatarUrl", otherUser.getProfileImageUrl());
        startActivity(intent);
    }

    // ContactsAdapter Callback: Audio Call
    @Override
    public void onAudioCall(ContactItem contact) {
        startCall(contact, CallSession.TYPE_AUDIO);
    }

    // ContactsAdapter Callback: Video Call
    @Override
    public void onVideoCall(ContactItem contact) {
        startCall(contact, CallSession.TYPE_VIDEO);
    }

    private void startCall(ContactItem contact, String callType) {
        if (contact.getUser() == null || currentUserId == null) return;
        User otherUser = contact.getUser();

        String myName = currentUser != null ? currentUser.getFullName() : "NexaChat User";
        String myAvatar = currentUser != null ? currentUser.getProfileImageUrl() : "";
        String callId = "call_" + System.currentTimeMillis();

        CallSession session = new CallSession(
                callId,
                currentUserId,
                myName,
                myAvatar,
                otherUser.getUid(),
                otherUser.getFullName(),
                callType
        );

        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra(CallActivity.EXTRA_CALL_SESSION, session);
        intent.putExtra(CallActivity.EXTRA_IS_INCOMING, false);
        startActivity(intent);
    }

    // ContactsAdapter Callback: Invite via native Android Share Sheet
    @Override
    public void onInvite(ContactItem contact) {
        String inviteMsg = "Hey " + contact.getName() + "! Join me on NexaChat for ultra-fast, secure encrypted messaging & calling. Download the app here: https://nexachat.app";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Join me on NexaChat");
        shareIntent.putExtra(Intent.EXTRA_TEXT, inviteMsg);
        startActivity(Intent.createChooser(shareIntent, "Invite " + contact.getName() + " via"));
    }

    // ==========================================
    // USER DATA
    // ==========================================
    private void loadCurrentUserData() {
        FirebaseManager.getInstance().getUserRef(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    if (!TextUtils.isEmpty(currentUser.getProfileImageUrl())) {
                        Glide.with(MainActivity.this)
                                .load(currentUser.getProfileImageUrl())
                                .placeholder(R.drawable.circle_avatar_placeholder)
                                .circleCrop()
                                .into(binding.ivHeaderAvatar);

                        Glide.with(MainActivity.this)
                                .load(currentUser.getProfileImageUrl())
                                .placeholder(R.drawable.ic_person)
                                .circleCrop()
                                .into(binding.ivMyStatusAvatar);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (conversationAdapter != null) conversationAdapter.notifyDataSetChanged();
        if (groupAdapter != null) groupAdapter.notifyDataSetChanged();
        if (statusAdapter != null) statusAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (conversationsRef != null && conversationsListener != null) {
            conversationsRef.removeEventListener(conversationsListener);
        }
        if (statusesRef != null && statusesListener != null) {
            statusesRef.removeEventListener(statusesListener);
        }
    }
}
