package com.example.mychat.models_user;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class UserSettingsResponse {
    @SerializedName("user_id")
    private String userId;
    @SerializedName("notification_enabled")
    private boolean notificationEnabled;
    @SerializedName("sound_enabled")
    private boolean soundEnabled;
    @SerializedName("vibration_enabled")
    private boolean vibrationEnabled;
    @SerializedName("theme")
    private String theme;
    @SerializedName("language")
    private String language;
    @SerializedName("updated_at")
    private Date updatedAt;

    // Constructor
    public UserSettingsResponse(String userId, boolean notificationEnabled, boolean soundEnabled, boolean vibrationEnabled, String theme, String language, Date updatedAt) {
        this.userId = userId;
        this.notificationEnabled = notificationEnabled;
        this.soundEnabled = soundEnabled;
        this.vibrationEnabled = vibrationEnabled;
        this.theme = theme;
        this.language = language;
        this.updatedAt = updatedAt;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public boolean isVibrationEnabled() {
        return vibrationEnabled;
    }

    public String getTheme() {
        return theme;
    }

    public String getLanguage() {
        return language;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    public void setSoundEnabled(boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public void setVibrationEnabled(boolean vibrationEnabled) {
        this.vibrationEnabled = vibrationEnabled;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}