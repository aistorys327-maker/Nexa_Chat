package com.nexachat.app.models;

import java.io.Serializable;

public class Message implements Serializable {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_FILE = "file";
    public static final String TYPE_AUDIO = "audio";

    public static final String STATUS_SENT = "sent";
    public static final String STATUS_DELIVERED = "delivered";
    public static final String STATUS_READ = "read";

    private String messageId;
    private String conversationId;
    private String senderId;
    private String senderName;
    private String text;
    private String messageType;
    private String mediaUrl;
    private String fileName;
    private long fileSize;
    private int audioDurationSeconds;
    private long timestamp;
    private String status;

    public Message() {
        this.messageType = TYPE_TEXT;
        this.status = STATUS_SENT;
        this.timestamp = System.currentTimeMillis();
    }

    public Message(String messageId, String conversationId, String senderId, String senderName, String text) {
        this.messageId = messageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.messageType = TYPE_TEXT;
        this.timestamp = System.currentTimeMillis();
        this.status = STATUS_SENT;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getAudioDurationSeconds() {
        return audioDurationSeconds;
    }

    public void setAudioDurationSeconds(int audioDurationSeconds) {
        this.audioDurationSeconds = audioDurationSeconds;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
