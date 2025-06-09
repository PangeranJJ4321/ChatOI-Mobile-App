package com.example.mychat.models_user;

import com.google.gson.annotations.SerializedName;

public class BlockUserRequest {
    @SerializedName("blocked_id")
    private String blockedId;

    public BlockUserRequest(String blockedId) {
        this.blockedId = blockedId;
    }

    public String getBlockedId() {
        return blockedId;
    }

    public void setBlockedId(String blockedId) {
        this.blockedId = blockedId;
    }
}