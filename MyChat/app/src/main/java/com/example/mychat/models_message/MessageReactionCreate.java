package com.example.mychat.models_message;

import com.google.gson.annotations.SerializedName;

public class MessageReactionCreate {
    @SerializedName("emoji")
    private String emoji;

    public MessageReactionCreate(String emoji) {
        this.emoji = emoji;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }
}