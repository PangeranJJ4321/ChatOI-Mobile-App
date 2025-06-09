package com.example.mychat.models_user;

import com.google.gson.annotations.SerializedName;

public class UserSettingsUpdate {
    @SerializedName("notification_enabled")
    private Boolean notificationEnabled;
    @SerializedName("sound_enabled")
    private Boolean soundEnabled;
    @SerializedName("vibration_enabled")
    private Boolean vibrationEnabled;
    @SerializedName("theme")
    private String theme;
    @SerializedName("language")
    private String language;

    public UserSettingsUpdate(Boolean notificationEnabled, Boolean soundEnabled, Boolean vibrationEnabled, String theme, String language) {
        this.notificationEnabled = notificationEnabled;
        this.soundEnabled = soundEnabled;
        this.vibrationEnabled = vibrationEnabled;
        this.theme = theme;
        this.language = language;
    }

    public Boolean getNotificationEnabled() {
        return notificationEnabled;
    }

    public void setNotificationEnabled(Boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    public Boolean getSoundEnabled() {
        return soundEnabled;
    }

    public void setSoundEnabled(Boolean soundEnabled) {
        this.soundEnabled = soundEnabled;
    }

    public Boolean getVibrationEnabled() {
        return vibrationEnabled;
    }

    public void setVibrationEnabled(Boolean vibrationEnabled) {
        this.vibrationEnabled = vibrationEnabled;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}