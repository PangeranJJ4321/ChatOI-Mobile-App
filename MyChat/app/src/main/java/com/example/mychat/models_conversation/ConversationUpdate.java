package com.example.mychat.models_conversation;

import com.google.gson.annotations.SerializedName;

public class ConversationUpdate {
    @SerializedName("name")
    private String name;
    @SerializedName("description")
    private String description;
    @SerializedName("avatar")
    private String avatar;

    public ConversationUpdate(String name, String description, String avatar) {
        this.name = name;
        this.description = description;
        this.avatar = avatar;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}