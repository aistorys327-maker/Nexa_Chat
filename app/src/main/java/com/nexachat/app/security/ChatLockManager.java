package com.nexachat.app.security;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class ChatLockManager {
    private static final String PREF_NAME = "nexachat_chat_locks";
    private static final String KEY_LOCKED_CHATS = "locked_conversations";

    private static ChatLockManager instance;
    private final SharedPreferences prefs;
    private final Set<String> sessionUnlockedChats = new HashSet<>();

    private ChatLockManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized ChatLockManager getInstance(Context context) {
        if (instance == null) {
            instance = new ChatLockManager(context);
        }
        return instance;
    }

    public boolean isChatLocked(String conversationId) {
        if (conversationId == null) return false;
        Set<String> lockedSet = prefs.getStringSet(KEY_LOCKED_CHATS, new HashSet<>());
        return lockedSet != null && lockedSet.contains(conversationId);
    }

    public void setChatLocked(String conversationId, boolean locked) {
        if (conversationId == null) return;
        Set<String> existing = prefs.getStringSet(KEY_LOCKED_CHATS, new HashSet<>());
        Set<String> updated = new HashSet<>(existing);
        if (locked) {
            updated.add(conversationId);
        } else {
            updated.remove(conversationId);
            sessionUnlockedChats.remove(conversationId);
        }
        prefs.edit().putStringSet(KEY_LOCKED_CHATS, updated).apply();
    }

    public boolean isSessionUnlocked(String conversationId) {
        return sessionUnlockedChats.contains(conversationId);
    }

    public void markSessionUnlocked(String conversationId) {
        if (conversationId != null) {
            sessionUnlockedChats.add(conversationId);
        }
    }

    public void clearSessionUnlocks() {
        sessionUnlockedChats.clear();
    }
}
