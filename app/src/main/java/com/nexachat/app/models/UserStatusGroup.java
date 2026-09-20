package com.nexachat.app.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserStatusGroup implements Serializable {
    private String userId;
    private String userName;
    private String userAvatarUrl;
    private List<StatusItem> statusList = new ArrayList<>();
    private long latestTimestamp;
    private boolean seen;

    public UserStatusGroup() {
    }

    public UserStatusGroup(String userId, String userName, String userAvatarUrl) {
        this.userId = userId;
        this.userName = userName;
        this.userAvatarUrl = userAvatarUrl;
    }

    public void addStatus(StatusItem item) {
        if (item != null && !item.isExpired()) {
            statusList.add(item);
            if (item.getTimestamp() > latestTimestamp) {
                latestTimestamp = item.getTimestamp();
            }
        }
    }

    public StatusItem getLatestStatus() {
        if (statusList.isEmpty()) return null;
        return statusList.get(statusList.size() - 1);
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

    public List<StatusItem> getStatusList() {
        return statusList;
    }

    public void setStatusList(List<StatusItem> statusList) {
        this.statusList = statusList;
    }

    public long getLatestTimestamp() {
        return latestTimestamp;
    }

    public void setLatestTimestamp(long latestTimestamp) {
        this.latestTimestamp = latestTimestamp;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }
}
