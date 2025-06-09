package com.example.mychat.models_message;

import com.google.gson.annotations.SerializedName;

public class TypingEvent {
    @SerializedName("user_id")
    private String userId;
    @SerializedName("is_typing")
    private boolean isTyping;

    // Constructor, getters, and setters
    public TypingEvent(String userId, boolean isTyping) {
        this.userId = userId;
        this.isTyping = isTyping;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isTyping() {
        return isTyping;
    }
}