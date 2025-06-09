package com.example.mychat.models_conversation;

import com.google.gson.annotations.SerializedName;

public class ParticipantUpdate {
    @SerializedName("role")
    private String role; // Corresponds to ParticipantRole enum in Python

    public ParticipantUpdate(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}