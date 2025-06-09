package com.example.mychat.models_conversation;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class ParticipantResponse {
    @SerializedName("user_id")
    private String userId;
    @SerializedName("username")
    private String username;
    @SerializedName("email")
    private String email;
    @SerializedName("role")
    private String role;
    @SerializedName("joined_at")
    private Date joinedAt;
    @SerializedName("last_seen_at")
    private Date lastSeenAt;
    @SerializedName("is_muted")
    private boolean isMuted;
    @SerializedName("is_pinned")
    private boolean isPinned;
    @SerializedName("is_online")
    private boolean isOnline;
    @SerializedName("profile_picture")
    private String profilePicture;

    public ParticipantResponse(String userId, String username, String email, String role, Date joinedAt, Date lastSeenAt, boolean isMuted, boolean isPinned, boolean isOnline, String profilePicture) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.joinedAt = joinedAt;
        this.lastSeenAt = lastSeenAt;
        this.isMuted = isMuted;
        this.isPinned = isPinned;
        this.isOnline = isOnline;
        this.profilePicture = profilePicture;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
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

    public Date getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Date lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Date getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Date joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
