package com.example.mychat.models_file; // Keep this package

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class AttachmentResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("file_url")
    private String fileUrl;
    @SerializedName("file_type")
    private String fileType;
    @SerializedName("mime_type")
    private String mimeType;
    @SerializedName("filename")
    private String filename;
    @SerializedName("file_size")
    private Integer fileSize; // Optional in Python, so Integer in Java
    @SerializedName("thumbnail_url")
    private String thumbnailUrl;
    @SerializedName("duration")
    private Integer duration; // Optional in Python, so Integer in Java
    @SerializedName("uploaded_at")
    private Date uploadedAt;

    // Constructor
    public AttachmentResponse(String id, String fileUrl, String fileType, String mimeType, String filename, Integer fileSize, String thumbnailUrl, Integer duration, Date uploadedAt) {
        this.id = id;
        this.fileUrl = fileUrl;
        this.fileType = fileType;
        this.mimeType = mimeType;
        this.filename = filename;
        this.fileSize = fileSize;
        this.thumbnailUrl = thumbnailUrl;
        this.duration = duration;
        this.uploadedAt = uploadedAt;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getFileType() {
        return fileType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFilename() {
        return filename;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public Integer getDuration() {
        return duration;
    }

    public Date getUploadedAt() {
        return uploadedAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setFileSize(Integer fileSize) {
        this.fileSize = fileSize;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public void setUploadedAt(Date uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}