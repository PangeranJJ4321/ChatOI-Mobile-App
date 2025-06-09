package com.example.mychat.auth_models;

import com.google.gson.annotations.SerializedName;

public class PasswordResetConfirmRequest {
    @SerializedName("token")
    private String token;
    @SerializedName("new_password")
    private String newPassword;

    public PasswordResetConfirmRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}