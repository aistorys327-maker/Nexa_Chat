package com.nexachat.app.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class StatusItem implements Serializable {
    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_IMAGE = "IMAGE";

    private String statusId;
    private String userId;
    private String userName;
    private String userAvatarUrl;
    private long timestamp;
    private String type; // TEXT or IMAGE
    private String text;
    private String imageUrl;
    private String backgroundColor;
    private String caption;
    private int viewersCount;
    private Map<String, Long> viewers = new HashMap<>();

    public StatusItem() {
    }

    public StatusItem(String statusId, String userId, String userName, String userAvatarUrl, long timestamp, String type, String text, String imageUrl, String backgroundColor, String caption) {
        this.statusId = statusId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatarUrl = userAvatarUrl;
        this.timestamp = timestamp;
        this.type = type;
        this.text = text;
        this.imageUrl = imageUrl;
        this.backgroundColor = backgroundColor != null ? backgroundColor : "#0284C7";
        this.caption = caption;
        this.viewersCount = 0;
    }

    public boolean isExpired() {
        // 24 hours in milliseconds = 24 * 60 * 60 * 1000 = 86,400,000 ms
        return (System.currentTimeMillis() - timestamp) > (24L * 60 * 60 * 1000L);
    }

    public String getStatusId() {
        return statusId;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public int getViewersCount() {
        return viewers != null ? viewers.size() : viewersCount;
    }

    public void setViewersCount(int viewersCount) {
        this.viewersCount = viewersCount;
    }

    public Map<String, Long> getViewers() {
        return viewers;
    }

    public void setViewers(Map<String, Long> viewers) {
        this.viewers = viewers;
    }
}
