package com.nexachat.app.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

public class HiddenChatManager {
    private static final String PREF_NAME = "nexachat_hidden_chats";
    private static final String KEY_HIDDEN_SET = "hidden_conversation_ids";
    private static final String KEY_PASSWORD_HASH = "hidden_chats_password_hash";

    private static HiddenChatManager instance;
    private final SharedPreferences prefs;

    private HiddenChatManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized HiddenChatManager getInstance(Context context) {
        if (instance == null) {
            instance = new HiddenChatManager(context);
        }
        return instance;
    }

    public boolean hasPassword() {
        return !TextUtils.isEmpty(prefs.getString(KEY_PASSWORD_HASH, null));
    }

    public void setPassword(String password) {
        if (password == null) return;
        String hash = hashString(password.trim());
        prefs.edit().putString(KEY_PASSWORD_HASH, hash).apply();
    }

    public boolean verifyPassword(String password) {
        if (password == null) return false;
        String storedHash = prefs.getString(KEY_PASSWORD_HASH, null);
        if (storedHash == null) {
            return false;
        }
        return storedHash.equals(hashString(password.trim()));
    }

    public boolean isChatHidden(String conversationId) {
        if (conversationId == null) return false;
        Set<String> set = prefs.getStringSet(KEY_HIDDEN_SET, null);
        return set != null && set.contains(conversationId);
    }

    public void setChatHidden(String conversationId, boolean hidden) {
        if (conversationId == null) return;
        Set<String> existing = prefs.getStringSet(KEY_HIDDEN_SET, new HashSet<>());
        Set<String> updated = new HashSet<>(existing);
        if (hidden) {
            updated.add(conversationId);
        } else {
            updated.remove(conversationId);
        }
        prefs.edit().putStringSet(KEY_HIDDEN_SET, updated).apply();
    }

    public Set<String> getHiddenChatIds() {
        Set<String> set = prefs.getStringSet(KEY_HIDDEN_SET, null);
        return set != null ? new HashSet<>(set) : new HashSet<>();
    }

    public int getHiddenChatCount() {
        return getHiddenChatIds().size();
    }

    public void unhideAll() {
        prefs.edit().putStringSet(KEY_HIDDEN_SET, new HashSet<>()).apply();
    }

    public static String hashString(String input) {
        if (input == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(input.hashCode());
        }
    }
}
