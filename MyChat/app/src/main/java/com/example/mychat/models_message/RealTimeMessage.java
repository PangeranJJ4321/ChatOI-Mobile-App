package com.example.mychat.models_message;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class RealTimeMessage {
    @SerializedName("id")
    private String id;
    @SerializedName("client_message_id")
    private String clientMessageId;
    @SerializedName("conversation_id")
    private String conversationId;
    @SerializedName("sender_id")
    private String senderId;
    @SerializedName("sender_username")
    private String senderUsername;
    @SerializedName("sender_avatar")
    private String senderAvatar;
    @SerializedName("content")
    private String content;
    @SerializedName("message_type")
    private String messageType;
    @SerializedName("sent_at")
    private Date sentAt;
    @SerializedName("is_edited")
    private boolean isEdited;
    @SerializedName("is_deleted")
    private boolean isDeleted;
    @SerializedName("reply_to_message_id")
    private String replyToMessageId;
    @SerializedName("status")
    private String status;

    // Constructor
    public RealTimeMessage(String id, String clientMessageId, String conversationId, String senderId, String senderUsername, String senderAvatar, String content, String messageType, Date sentAt, boolean isEdited, boolean isDeleted, String replyToMessageId, String status) {
        this.id = id;
        this.clientMessageId = clientMessageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderAvatar = senderAvatar;
        this.content = content;
        this.messageType = messageType;
        this.sentAt = sentAt;
        this.isEdited = isEdited;
        this.isDeleted = isDeleted;
        this.replyToMessageId = replyToMessageId;
        this.status = status;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getClientMessageId() {
        return clientMessageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getSenderAvatar() {
        return senderAvatar;
    }

    public String getContent() {
        return content;
    }

    public String getMessageType() {
        return messageType;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public boolean isEdited() {
        return isEdited;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public String getReplyToMessageId() {
        return replyToMessageId;
    }

    public String getStatus() {
        return status;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setClientMessageId(String clientMessageId) {
        this.clientMessageId = clientMessageId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public void setSenderAvatar(String senderAvatar) {
        this.senderAvatar = senderAvatar;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    public void setEdited(boolean edited) {
        isEdited = edited;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}