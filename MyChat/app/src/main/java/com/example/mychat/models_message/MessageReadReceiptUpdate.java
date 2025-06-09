package com.example.mychat.models_message;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MessageReadReceiptUpdate {
    @SerializedName("message_ids")
    private List<String> messageIds;

    public MessageReadReceiptUpdate(List<String> messageIds) {
        this.messageIds = messageIds;
    }

    public List<String> getMessageIds() {
        return messageIds;
    }

    public void setMessageIds(List<String> messageIds) {
        this.messageIds = messageIds;
    }
}