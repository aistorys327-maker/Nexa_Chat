package com.nexachat.app.models;

import java.io.Serializable;

public class User implements Serializable {
    private String uid;
    private String fullName;
    private String username;
    private String email;
    private String profileImageUrl;
    private String bio;
    private boolean online;
    private long lastSeen;
    private long createdAt;
    private String fcmToken;
    private String phoneNumber;
    private boolean phoneNumberHidden;

    // Default constructor required for calls to DataSnapshot.getValue(User.class)
    public User() {
    }

    public User(String uid, String fullName, String username, String email, String profileImageUrl, String bio, long createdAt) {
        this.uid = uid;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
        this.online = true;
        this.lastSeen = System.currentTimeMillis();
        this.createdAt = createdAt;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isPhoneNumberHidden() {
        return phoneNumberHidden;
    }

    public void setPhoneNumberHidden(boolean phoneNumberHidden) {
        this.phoneNumberHidden = phoneNumberHidden;
    }
}
