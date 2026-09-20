package com.nexachat.app;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.nexachat.app.adapters.ContactsAdapter;
import com.nexachat.app.calls.CallManager;
import com.nexachat.app.databinding.ActivityNewChatBinding;
import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.models.CallSession;
import com.nexachat.app.models.ContactItem;
import com.nexachat.app.models.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NewChatActivity extends AppCompatActivity implements ContactsAdapter.OnContactClickListener {

    private ActivityNewChatBinding binding;
    private ContactsAdapter adapter;
    private String currentUserId;
    private User currentUser;

    private final List<ContactItem> rawDeviceContacts = new ArrayList<>();
    private final Map<String, User> registeredPhoneToUserMap = new HashMap<>();
    private final List<User> allRegisteredNexaUsers = new ArrayList<>();

    private final List<ContactItem> onAppContacts = new ArrayList<>();
    private final List<ContactItem> inviteContacts = new ArrayList<>();
    private final List<ContactItem> allNexaUserContacts = new ArrayList<>();

    private enum TabFilter { ON_APP, INVITE, ALL_USERS }
    private TabFilter currentTab = TabFilter.ON_APP;

    private final ActivityResultLauncher<String> requestContactsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    binding.cardContactsPermission.setVisibility(View.GONE);
                    readDeviceContactsAndSync();
                } else {
                    binding.cardContactsPermission.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Contacts permission denied. You can still search users.", Toast.LENGTH_SHORT).show();
                    loadRegisteredUsers();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        setupUI();
        loadCurrentUserProfile();
        checkContactsPermissionAndLoad();
    }

    private void setupUI() {
        binding.btnNewChatBack.setOnClickListener(v -> finish());

        binding.btnNewGroupOption.setOnClickListener(v -> {
            startActivity(new Intent(NewChatActivity.this, CreateGroupActivity.class));
            finish();
        });

        binding.btnGrantContactsPermission.setOnClickListener(v -> {
            requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        });

        adapter = new ContactsAdapter(this, this);
        binding.rvContacts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvContacts.setAdapter(adapter);

        binding.pillOnApp.setOnClickListener(v -> switchTab(TabFilter.ON_APP));
        binding.pillInvite.setOnClickListener(v -> switchTab(TabFilter.INVITE));
        binding.pillAllUsers.setOnClickListener(v -> switchTab(TabFilter.ALL_USERS));

        binding.etSearchUsers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilterAndDisplay();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void switchTab(TabFilter tab) {
        currentTab = tab;
        updatePillsUI();
        applyFilterAndDisplay();
    }

    private void updatePillsUI() {
        setPillState(binding.pillOnApp, currentTab == TabFilter.ON_APP);
        setPillState(binding.pillInvite, currentTab == TabFilter.INVITE);
        setPillState(binding.pillAllUsers, currentTab == TabFilter.ALL_USERS);
    }

    private void setPillState(TextView pill, boolean active) {
        if (active) {
            pill.setBackgroundResource(R.drawable.bg_pill_active);
            pill.setTextColor(ContextCompat.getColor(this, R.color.bg_dark_navy));
        } else {
            pill.setBackgroundResource(R.drawable.bg_pill_inactive);
            pill.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private void loadCurrentUserProfile() {
        if (currentUserId != null) {
            FirebaseManager.getInstance().getUserRef(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    currentUser = snapshot.getValue(User.class);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void checkContactsPermissionAndLoad() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            binding.cardContactsPermission.setVisibility(View.GONE);
            readDeviceContactsAndSync();
        } else {
            binding.cardContactsPermission.setVisibility(View.VISIBLE);
            loadRegisteredUsers();
        }
    }

    private void readDeviceContactsAndSync() {
        binding.pbSearchLoading.setVisibility(View.VISIBLE);
        new Thread(() -> {
            rawDeviceContacts.clear();
            Set<String> seenPhones = new HashSet<>();

            ContentResolver resolver = getContentResolver();
            Cursor cursor = null;
            try {
                cursor = resolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                ContactsContract.CommonDataKinds.Phone.NUMBER
                        },
                        null, null,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                );

                if (cursor != null) {
                    int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    int numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                    while (cursor.moveToNext()) {
                        String name = nameIdx != -1 ? cursor.getString(nameIdx) : "Contact";
                        String number = numIdx != -1 ? cursor.getString(numIdx) : "";
                        String clean = FirebaseManager.cleanPhoneNumber(number);

                        if (!TextUtils.isEmpty(clean) && !seenPhones.contains(clean)) {
                            seenPhones.add(clean);
                            rawDeviceContacts.add(new ContactItem(name, number, clean, false, null));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            runOnUiThread(this::loadRegisteredUsers);
        }).start();
    }

    private void loadRegisteredUsers() {
        binding.pbSearchLoading.setVisibility(View.VISIBLE);

        FirebaseManager.getInstance().getUsersRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                registeredPhoneToUserMap.clear();
                allRegisteredNexaUsers.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    User user = child.getValue(User.class);
                    if (user != null && !user.getUid().equals(currentUserId)) {
                        allRegisteredNexaUsers.add(user);

                        if (!TextUtils.isEmpty(user.getPhoneNumber())) {
                            String clean = FirebaseManager.cleanPhoneNumber(user.getPhoneNumber());
                            if (!TextUtils.isEmpty(clean)) {
                                registeredPhoneToUserMap.put(clean, user);
                                if (clean.length() > 10) {
                                    registeredPhoneToUserMap.put(clean.substring(clean.length() - 10), user);
                                }
                            }
                        }
                    }
                }

                buildContactCategories();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.pbSearchLoading.setVisibility(View.GONE);
                Toast.makeText(NewChatActivity.this, "Failed to sync: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buildContactCategories() {
        onAppContacts.clear();
        inviteContacts.clear();
        allNexaUserContacts.clear();

        // 1. Process Device Contacts against registered phone numbers
        for (ContactItem contact : rawDeviceContacts) {
            String clean = contact.getCleanPhone();
            User matchedUser = registeredPhoneToUserMap.get(clean);

            if (matchedUser == null && clean.length() > 10) {
                matchedUser = registeredPhoneToUserMap.get(clean.substring(clean.length() - 10));
            }

            if (matchedUser != null) {
                contact.setOnApp(true);
                contact.setUser(matchedUser);
                onAppContacts.add(contact);
            } else {
                contact.setOnApp(false);
                inviteContacts.add(contact);
            }
        }

        // 2. Build All Nexa Users list as ContactItems
        for (User u : allRegisteredNexaUsers) {
            String displayPhone = !TextUtils.isEmpty(u.getPhoneNumber()) ? u.getPhoneNumber() : "";
            allNexaUserContacts.add(new ContactItem(u.getFullName(), displayPhone, FirebaseManager.cleanPhoneNumber(displayPhone), true, u));
        }

        // Update Tab pill badges
        binding.pillOnApp.setText("On NexaChat (" + onAppContacts.size() + ")");
        binding.pillInvite.setText("Invite Friends (" + inviteContacts.size() + ")");
        binding.pillAllUsers.setText("All Users (" + allNexaUserContacts.size() + ")");

        binding.pbSearchLoading.setVisibility(View.GONE);

        // If no contacts on app, default to all users or invite tab
        if (onAppContacts.isEmpty() && !allNexaUserContacts.isEmpty() && currentTab == TabFilter.ON_APP) {
            switchTab(TabFilter.ALL_USERS);
        } else {
            applyFilterAndDisplay();
        }
    }

    private void applyFilterAndDisplay() {
        String query = binding.etSearchUsers.getText() != null ?
                binding.etSearchUsers.getText().toString().trim().toLowerCase() : "";

        List<ContactItem> sourceList;
        switch (currentTab) {
            case INVITE:
                sourceList = inviteContacts;
                break;
            case ALL_USERS:
                sourceList = allNexaUserContacts;
                break;
            case ON_APP:
            default:
                sourceList = onAppContacts;
                break;
        }

        List<ContactItem> filtered = new ArrayList<>();
        for (ContactItem item : sourceList) {
            boolean matches = TextUtils.isEmpty(query)
                    || item.getName().toLowerCase().contains(query)
                    || item.getPhoneNumber().toLowerCase().contains(query);

            if (!matches && item.getUser() != null) {
                if (!TextUtils.isEmpty(item.getUser().getUsername()) && item.getUser().getUsername().toLowerCase().contains(query)) {
                    matches = true;
                }
            }

            if (matches) {
                filtered.add(item);
            }
        }

        adapter.updateList(filtered);

        if (filtered.isEmpty()) {
            binding.tvEmptySearch.setVisibility(View.VISIBLE);
            if (currentTab == TabFilter.INVITE) {
                binding.tvEmptySearch.setText("No contacts to invite");
            } else if (currentTab == TabFilter.ON_APP) {
                binding.tvEmptySearch.setText("No friends from your contacts are on NexaChat yet. Tap 'Invite Friends' to invite them!");
            } else {
                binding.tvEmptySearch.setText("No users found");
            }
        } else {
            binding.tvEmptySearch.setVisibility(View.GONE);
        }
    }

    @Override
    public void onMessage(ContactItem contact) {
        if (contact.getUser() == null || currentUserId == null) return;
        User otherUser = contact.getUser();

        String conversationId = FirebaseManager.getOneToOneConversationId(currentUserId, otherUser.getUid());
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("conversationId", conversationId);
        intent.putExtra("otherUserId", otherUser.getUid());
        intent.putExtra("title", otherUser.getFullName());
        intent.putExtra("avatarUrl", otherUser.getProfileImageUrl());
        startActivity(intent);
        finish();
    }

    @Override
    public void onAudioCall(ContactItem contact) {
        startCall(contact, CallSession.TYPE_AUDIO);
    }

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

    @Override
    public void onInvite(ContactItem contact) {
        String phone = contact.getPhoneNumber();
        String inviteMsg = getString(R.string.invite_sms_text);

        try {
            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
            smsIntent.setData(Uri.parse("smsto:" + Uri.encode(phone)));
            smsIntent.putExtra("sms_body", inviteMsg);
            if (smsIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(smsIntent);
                return;
            }
        } catch (Exception ignored) {}

        // Fallback to general share intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, inviteMsg);
        startActivity(Intent.createChooser(shareIntent, "Invite to NexaChat via"));
    }
}
