package com.example.mychat.models_conversation;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ConversationCreate {
    @SerializedName("name")
    private String name;
    @SerializedName("description")
    private String description;
    @SerializedName("is_group")
    private boolean isGroup;
    @SerializedName("avatar")
    private String avatar;
    @SerializedName("participant_ids")
    private List<String> participantIds;

    public ConversationCreate(String name, String description, boolean isGroup, String avatar, List<String> participantIds) {
        this.name = name;
        this.description = description;
        this.isGroup = isGroup;
        this.avatar = avatar;
        this.participantIds = participantIds;
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

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public List<String> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds;
    }
}