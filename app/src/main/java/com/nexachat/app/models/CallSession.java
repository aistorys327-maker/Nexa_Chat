package com.nexachat.app.models;

import java.io.Serializable;

public class CallSession implements Serializable {

    public static final String TYPE_AUDIO = "AUDIO";
    public static final String TYPE_VIDEO = "VIDEO";

    public static final String STATUS_CALLING = "CALLING";
    public static final String STATUS_RINGING = "RINGING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_ENDED = "ENDED";

    private String callId;
    private String callerId;
    private String callerName;
    private String callerAvatarUrl;
    private String receiverId;
    private String receiverName;
    private String callType; // AUDIO or VIDEO
    private String status;   // CALLING, ACCEPTED, ENDED, etc.
    private long timestamp;
    private int durationSeconds;

    public CallSession() {
        // Required for Firebase
    }

    public CallSession(String callId, String callerId, String callerName, String callerAvatarUrl,
                       String receiverId, String receiverName, String callType) {
        this.callId = callId;
        this.callerId = callerId;
        this.callerName = callerName;
        this.callerAvatarUrl = callerAvatarUrl;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.callType = callType;
        this.status = STATUS_CALLING;
        this.timestamp = System.currentTimeMillis();
        this.durationSeconds = 0;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getCallerId() {
        return callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }

    public String getCallerName() {
        return callerName;
    }

    public void setCallerName(String callerName) {
        this.callerName = callerName;
    }

    public String getCallerAvatarUrl() {
        return callerAvatarUrl;
    }

    public void setCallerAvatarUrl(String callerAvatarUrl) {
        this.callerAvatarUrl = callerAvatarUrl;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getCallType() {
        return callType != null ? callType : TYPE_AUDIO;
    }

    public void setCallType(String callType) {
        this.callType = callType;
    }

    public String getStatus() {
        return status != null ? status : STATUS_CALLING;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
