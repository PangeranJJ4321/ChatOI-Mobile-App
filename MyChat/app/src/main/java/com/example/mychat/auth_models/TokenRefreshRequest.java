package com.example.mychat.auth_models;

import com.google.gson.annotations.SerializedName;

public class TokenRefreshRequest {
    @SerializedName("refresh_token")
    private String refreshToken;

    public TokenRefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}