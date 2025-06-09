package com.example.mychat.models_message;

import com.example.mychat.models_file.AttachmentResponse;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;
import java.util.Objects; // Perlu import Objects untuk equals dan hashCode

public class MessageResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("conversation_id")
    private String conversationId;
    @SerializedName("sender_id")
    private String senderId;
    @SerializedName("sender_username")
    private String senderUsername;
    @SerializedName("sender_avatar")
    private String senderAvatar;
    @SerializedName("content")
    private String content;
    @SerializedName("message_type")
    private String messageType;
    @SerializedName("status")
    private String status;
    @SerializedName("is_deleted")
    private boolean isDeleted;
    @SerializedName("is_edited")
    private boolean isEdited;
    @SerializedName("sent_at")
    private Date sentAt;
    @SerializedName("edited_at")
    private Date editedAt;
    @SerializedName("deleted_at")
    private Date deletedAt;
    @SerializedName("reply_to")
    private MessageResponse replyTo; // Self-referencing
    @SerializedName("attachments")
    private List<AttachmentResponse> attachments;
    @SerializedName("reactions")
    private List<MessageReactionResponse> reactions;
    @SerializedName("read_by_count")
    private int readByCount;

    @SerializedName("client_message_id") // <-- TAMBAHKAN FIELD INI
    private String clientMessageId;


    // Constructor Lengkap (jika semua field akan selalu diisi)
    public MessageResponse(String id, String conversationId, String senderId, String senderUsername, String senderAvatar, String content, String messageType, String status, boolean isDeleted, boolean isEdited, Date sentAt, Date editedAt, Date deletedAt, MessageResponse replyTo, List<AttachmentResponse> attachments, List<MessageReactionResponse> reactions, int readByCount, String clientMessageId) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderAvatar = senderAvatar;
        this.content = content;
        this.messageType = messageType;
        this.status = status;
        this.isDeleted = isDeleted;
        this.isEdited = isEdited;
        this.sentAt = sentAt;
        this.editedAt = editedAt;
        this.deletedAt = deletedAt;
        this.replyTo = replyTo;
        this.attachments = attachments;
        this.reactions = reactions;
        this.readByCount = readByCount;
        this.clientMessageId = clientMessageId; // <-- Inisialisasi di constructor
    }

    // Constructor Disederhanakan (sering digunakan untuk Pusher atau temp message)
    // Sesuaikan parameter jika ada field lain yang tidak selalu ada
    public MessageResponse(String id, String conversationId, String senderId, String senderUsername, String senderAvatar, String content, String messageType, String status, boolean isEdited, boolean isDeleted, Date sentAt, MessageResponse replyTo, List<AttachmentResponse> attachments, List<MessageReactionResponse> reactions, int readByCount, String clientMessageId) {
        this.id = id;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderUsername = senderUsername;
        this.senderAvatar = senderAvatar;
        this.content = content;
        this.messageType = messageType;
        this.status = status;
        this.isEdited = isEdited;
        this.isDeleted = isDeleted;
        this.sentAt = sentAt;
        this.replyTo = replyTo;
        this.attachments = attachments;
        this.reactions = reactions;
        this.readByCount = readByCount;
        this.clientMessageId = clientMessageId; // <-- Inisialisasi di constructor
    }

    // Getters
    public String getId() { return id; }
    public String getConversationId() { return conversationId; }
    public String getSenderId() { return senderId; }
    public String getSenderUsername() { return senderUsername; }
    public String getSenderAvatar() { return senderAvatar; }
    public String getContent() { return content; }
    public String getMessageType() { return messageType; }
    public String getStatus() { return status; }
    public boolean isDeleted() { return isDeleted; }
    public boolean isEdited() { return isEdited; }
    public Date getSentAt() { return sentAt; }
    public Date getEditedAt() { return editedAt; }
    public Date getDeletedAt() { return deletedAt; }
    public MessageResponse getReplyTo() { return replyTo; }
    public List<AttachmentResponse> getAttachments() { return attachments; }
    public List<MessageReactionResponse> getReactions() { return reactions; }
    public int getReadByCount() { return readByCount; }
    public String getClientMessageId() { return clientMessageId; } // <-- TAMBAHKAN GETTER INI

    // Setters
    public void setId(String id) { this.id = id; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    public void setSenderAvatar(String senderAvatar) { this.senderAvatar = senderAvatar; }
    public void setContent(String content) { this.content = content; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public void setStatus(String status) { this.status = status; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    public void setEdited(boolean edited) { isEdited = edited; }
    public void setSentAt(Date sentAt) { this.sentAt = sentAt; }
    public void setEditedAt(Date editedAt) { this.editedAt = editedAt; }
    public void setDeletedAt(Date deletedAt) { this.deletedAt = deletedAt; }
    public void setReplyTo(MessageResponse replyTo) { this.replyTo = replyTo; }
    public void setAttachments(List<AttachmentResponse> attachments) { this.attachments = attachments; }
    public void setReactions(List<MessageReactionResponse> reactions) { this.reactions = reactions; }
    public void setReadByCount(int readByCount) { this.readByCount = readByCount; }
    public void setClientMessageId(String clientMessageId) { this.clientMessageId = clientMessageId; } // <-- TAMBAHKAN SETTER INI

    // Override equals and hashCode untuk memastikan perbandingan yang benar
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageResponse that = (MessageResponse) o;
        // Prioritaskan ID unik dari server, lalu client_message_id
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(clientMessageId, that.clientMessageId);
    }

    @Override
    public int hashCode() {
        // Hashing berdasarkan ID atau client_message_id
        return Objects.hash(id, clientMessageId);
    }
}