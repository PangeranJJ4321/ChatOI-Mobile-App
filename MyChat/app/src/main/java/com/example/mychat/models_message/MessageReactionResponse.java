package com.example.mychat.models_message;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class MessageReactionResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("username")
    private String username;
    @SerializedName("emoji")
    private String emoji;
    @SerializedName("created_at")
    private Date createdAt;

    // Constructor
    public MessageReactionResponse(String id, String userId, String username, String emoji, Date createdAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.emoji = emoji;
        this.createdAt = createdAt;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmoji() {
        return emoji;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}