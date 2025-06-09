package com.example.mychat.models_file;

import com.google.gson.annotations.SerializedName;

public class ThumbnailResponse {
    @SerializedName("original_url")
    private String originalUrl;
    @SerializedName("thumbnail_url")
    private String thumbnailUrl;
    @SerializedName("width")
    private int width;
    @SerializedName("height")
    private int height;

    public ThumbnailResponse(String originalUrl, String thumbnailUrl, int width, int height) {
        this.originalUrl = originalUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.width = width;
        this.height = height;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}