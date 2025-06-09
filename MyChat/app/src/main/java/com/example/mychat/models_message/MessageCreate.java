package com.example.mychat.models_message;

import com.google.gson.annotations.SerializedName;

public class MessageCreate {
    @SerializedName("content")
    private String content;
    @SerializedName("message_type")
    private String messageType;
    @SerializedName("reply_to_message_id")
    private String replyToMessageId;
    @SerializedName("conversation_id")
    private String conversationId;
    @SerializedName("client_message_id")
    private String clientMessageId;

    public MessageCreate(String content, String messageType, String replyToMessageId, String conversationId, String clientMessageId) {
        this.content = content;
        this.messageType = messageType;
        this.replyToMessageId = replyToMessageId;
        this.conversationId = conversationId;
        this.clientMessageId = clientMessageId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getClientMessageId() {
        return clientMessageId;
    }

    public void setClientMessageId(String clientMessageId) {
        this.clientMessageId = clientMessageId;
    }
}