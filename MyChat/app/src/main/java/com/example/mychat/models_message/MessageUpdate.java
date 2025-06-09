package com.example.mychat.models_message;

import com.google.gson.annotations.SerializedName;

public class MessageUpdate {
    @SerializedName("content")
    private String content;

    public MessageUpdate(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}