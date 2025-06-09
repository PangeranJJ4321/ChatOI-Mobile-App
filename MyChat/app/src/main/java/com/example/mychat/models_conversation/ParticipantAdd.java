package com.example.mychat.models_conversation;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ParticipantAdd {
    @SerializedName("user_ids")
    private List<String> userIds;

    public ParticipantAdd(List<String> userIds) {
        this.userIds = userIds;
    }

    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }
}