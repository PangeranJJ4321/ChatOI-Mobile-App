package com.example.mychat.models_conversation;

import com.google.gson.annotations.SerializedName;

public class ParticipantMuteUpdate {
    @SerializedName("is_muted")
    private boolean isMuted;

    public ParticipantMuteUpdate(boolean isMuted) {
        this.isMuted = isMuted;
    }

    public boolean isMuted() {
        return isMuted;
    }

    public void setMuted(boolean muted) {
        isMuted = muted;
    }
}