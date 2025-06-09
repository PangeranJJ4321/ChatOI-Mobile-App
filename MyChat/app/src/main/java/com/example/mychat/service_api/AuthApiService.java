package com.example.mychat.service_api;

import com.example.mychat.auth_models.MessageResponse;
import com.example.mychat.auth_models.PasswordChangeRequest;
import com.example.mychat.auth_models.PasswordResetConfirmRequest;
import com.example.mychat.auth_models.PasswordResetRequest;
import com.example.mychat.auth_models.RefreshTokenRequest;
import com.example.mychat.auth_models.Token;
import com.example.mychat.auth_models.UserLoginRequest;
import com.example.mychat.auth_models.UserProfile;
import com.example.mychat.auth_models.UserRegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface AuthApiService {

    @POST("auth/register")
    Call<UserProfile> registerUser(@Body UserRegisterRequest userRegisterRequest);

    @POST("auth/login")
    Call<Token> loginUser(@Body UserLoginRequest userLoginRequest);

    @POST("auth/refresh")
    Call<Token> refreshToken(
            @Body RefreshTokenRequest refreshTokenRequest
    );

    @POST("auth/logout")
    Call<MessageResponse> logoutUser(@Header("Authorization") String authToken); // Perlu token untuk logout

    @GET("auth/me")
    Call<UserProfile> getCurrentUserProfile(@Header("Authorization") String authToken);

    @PUT("auth/change-password")
    Call<MessageResponse> changePassword(
            @Header("Authorization") String authToken,
            @Body PasswordChangeRequest passwordChangeRequest
    );

    @POST("auth/forgot-password")
    Call<MessageResponse> forgotPassword(@Body PasswordResetRequest passwordResetRequest);

    @POST("auth/reset-password")
    Call<MessageResponse> resetPassword(@Body PasswordResetConfirmRequest resetData);

    @GET("auth/verify-token")
    Call<MessageResponse> verifyToken(@Header("Authorization") String authToken); // Respons bisa disesuaikan jika perlu data selain "valid: true"
}