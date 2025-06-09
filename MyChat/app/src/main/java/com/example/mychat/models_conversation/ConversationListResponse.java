package com.example.mychat.models_conversation;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class ConversationListResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("is_group")
    private boolean isGroup;
    @SerializedName("avatar")
    private String avatar;
    @SerializedName("created_at")
    private Date createdAt;
    @SerializedName("last_message_at")
    private Date lastMessageAt;
    @SerializedName("unread_count")
    private int unreadCount;
    @SerializedName("last_message")
    private String lastMessage;
    @SerializedName("last_message_sender")
    private String lastMessageSender;
    @SerializedName("participants_count")
    private int participantsCount;
    @SerializedName("is_muted")
    private boolean isMuted;
    @SerializedName("is_pinned")
    private boolean isPinned;

    // --- Tambahan untuk chat pribadi ---
    @SerializedName("other_participant_id")
    private String otherParticipantId;
    @SerializedName("other_participant_username")
    private String otherParticipantUsername;
    @SerializedName("other_participant_avatar")
    private String otherParticipantAvatar;
    // -----------------------------------

    public ConversationListResponse(String id, String name, boolean isGroup, String avatar, Date createdAt, Date lastMessageAt, int unreadCount, String lastMessage, String lastMessageSender, int participantsCount, boolean isMuted, boolean isPinned, String otherParticipantId, String otherParticipantUsername, String otherParticipantAvatar) {
        this.id = id;
        this.name = name;
        this.isGroup = isGroup;
        this.avatar = avatar;
        this.createdAt = createdAt;
        this.lastMessageAt = lastMessageAt;
        this.unreadCount = unreadCount;
        this.lastMessage = lastMessage;
        this.lastMessageSender = lastMessageSender;
        this.participantsCount = participantsCount;
        this.isMuted = isMuted;
        this.isPinned = isPinned;
        this.otherParticipantId = otherParticipantId;
        this.otherParticipantUsername = otherParticipantUsername;
        this.otherParticipantAvatar = otherParticipantAvatar;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void setMuted(boolean muted) {
        isMuted = muted;
    }

    public int getParticipantsCount() {
        return participantsCount;
    }

    public void setParticipantsCount(int participantsCount) {
        this.participantsCount = participantsCount;
    }

    public String getLastMessageSender() {
        return lastMessageSender;
    }

    public void setLastMessageSender(String lastMessageSender) {
        this.lastMessageSender = lastMessageSender;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Date lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public String getOtherParticipantId() {
        return otherParticipantId;
    }

    public void setOtherParticipantId(String otherParticipantId) {
        this.otherParticipantId = otherParticipantId;
    }

    public String getOtherParticipantAvatar() {
        return otherParticipantAvatar;
    }

    public void setOtherParticipantAvatar(String otherParticipantAvatar) {
        this.otherParticipantAvatar = otherParticipantAvatar;
    }

    public String getOtherParticipantUsername() {
        return otherParticipantUsername;
    }

    public void setOtherParticipantUsername(String otherParticipantUsername) {
        this.otherParticipantUsername = otherParticipantUsername;
    }
}
