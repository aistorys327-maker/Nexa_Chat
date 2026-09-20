package com.nexachat.app.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Group implements Serializable {
    private String groupId;
    private String groupName;
    private String groupPhotoUrl;
    private String createdBy;
    private long createdAt;
    private Map<String, Boolean> members = new HashMap<>();
    private Map<String, Boolean> adminIds = new HashMap<>();

    public Group() {
    }

    public Group(String groupId, String groupName, String groupPhotoUrl, String createdBy, long createdAt) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.groupPhotoUrl = groupPhotoUrl;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupPhotoUrl() {
        return groupPhotoUrl;
    }

    public void setGroupPhotoUrl(String groupPhotoUrl) {
        this.groupPhotoUrl = groupPhotoUrl;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Boolean> getMembers() {
        return members;
    }

    public void setMembers(Map<String, Boolean> members) {
        this.members = members;
    }

    public Map<String, Boolean> getAdminIds() {
        return adminIds;
    }

    public void setAdminIds(Map<String, Boolean> adminIds) {
        this.adminIds = adminIds;
    }
}
