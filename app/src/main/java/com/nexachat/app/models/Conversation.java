package com.nexachat.app.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Conversation implements Serializable {
    private String conversationId;
    private String title;
    private String otherUserId;
    private String otherUserAvatarUrl;
    private String otherUsername;
    private String lastMessage;
    private long lastMessageTimestamp;
    private String lastSenderId;
    private int unreadCount;
    private boolean isGroup;
    private boolean isLocked;
    private boolean isOnline;
    private long lastSeen;
    private Map<String, Boolean> members = new HashMap<>();

    public Conversation() {
    }

    public Conversation(String conversationId, String title, String otherUserId, String otherUserAvatarUrl, String lastMessage, long lastMessageTimestamp) {
        this.conversationId = conversationId;
        this.title = title;
        this.otherUserId = otherUserId;
        this.otherUserAvatarUrl = otherUserAvatarUrl;
        this.lastMessage = lastMessage;
        this.lastMessageTimestamp = lastMessageTimestamp;
        this.isGroup = false;
        this.isLocked = false;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getOtherUserAvatarUrl() {
        return otherUserAvatarUrl;
    }

    public void setOtherUserAvatarUrl(String otherUserAvatarUrl) {
        this.otherUserAvatarUrl = otherUserAvatarUrl;
    }

    public String getOtherUsername() {
        return otherUsername;
    }

    public void setOtherUsername(String otherUsername) {
        this.otherUsername = otherUsername;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public String getLastSenderId() {
        return lastSenderId;
    }

    public void setLastSenderId(String lastSenderId) {
        this.lastSenderId = lastSenderId;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Map<String, Boolean> getMembers() {
        return members;
    }

    public void setMembers(Map<String, Boolean> members) {
        this.members = members;
    }
}
