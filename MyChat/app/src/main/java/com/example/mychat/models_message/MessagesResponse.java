package com.example.mychat.models_message;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MessagesResponse {
    @SerializedName("messages")
    private List<MessageResponse> messages;
    @SerializedName("total")
    private int total;
    @SerializedName("page")
    private int page;
    @SerializedName("per_page")
    private int perPage;
    @SerializedName("has_more")
    private boolean hasMore;

    // Constructor
    public MessagesResponse(List<MessageResponse> messages, int total, int page, int perPage, boolean hasMore) {
        this.messages = messages;
        this.total = total;
        this.page = page;
        this.perPage = perPage;
        this.hasMore = hasMore;
    }

    // Getters
    public List<MessageResponse> getMessages() {
        return messages;
    }

    public int getTotal() {
        return total;
    }

    public int getPage() {
        return page;
    }

    public int getPerPage() {
        return perPage;
    }

    public boolean hasMore() {
        return hasMore;
    }

    // Setters
    public void setMessages(List<MessageResponse> messages) {
        this.messages = messages;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setPerPage(int perPage) {
        this.perPage = perPage;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}