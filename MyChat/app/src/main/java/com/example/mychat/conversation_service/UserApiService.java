package com.example.mychat.conversation_service;


import com.example.mychat.models_user.BlockUserRequest;
import com.example.mychat.models_user.UserResponse;
import com.example.mychat.models_user.UserSettingsResponse;
import com.example.mychat.models_user.UserSettingsUpdate;
import com.example.mychat.models_user.UserUpdate;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UserApiService {

    @GET("users/{user_id}")
    Call<UserResponse> getUser(
            @Header("Authorization") String authToken, // Uncomment if authentication is needed
            @Path("user_id") String userId
    );

    @PUT("users/{user_id}")
    Call<UserResponse> updateUser(
            @Path("user_id") String userId,
            @Body UserUpdate userUpdate // Tetap gunakan @Body untuk data JSON
    );


    @GET("users/search/")
    Call<List<UserResponse>> searchUsers(
            @Header("Authorization") String authToken,
            @Query("query") String query
    );

    @GET("users/{user_id}/settings")
    Call<UserSettingsResponse> getUserSettings(
            // @Header("Authorization") String authToken, // Uncomment if authentication is needed
            @Path("user_id") String userId
    );

    @PUT("users/{user_id}/settings")
    Call<UserSettingsResponse> updateUserSettings(
            // @Header("Authorization") String authToken, // Uncomment if authentication is needed
            @Path("user_id") String userId,
            @Body UserSettingsUpdate settingsUpdate
    );

    @POST("users/{user_id}/block")
    Call<Void> blockUser(
            @Header("Authorization") String authToken, // Uncomment if authentication is needed
            @Path("user_id") String userId,
            @Body BlockUserRequest blockRequest
    );

    @POST("users/{user_id}/unblock")
    Call<Void> unblockUser(
            @Header("Authorization") String authToken, // Uncomment if authentication is needed
            @Path("user_id") String userId,
            @Body BlockUserRequest unblockRequest
    );

    @GET("users/{user_id}/blocked")
    Call<List<UserResponse>> getBlockedUsers(
            @Header("Authorization") String authToken, // Uncomment if authentication is needed
            @Path("user_id") String userId
    );

    @GET("users")
    Call<List<UserResponse>> getAllUsers(
            @Header("Authorization") String authToken
    );

    @GET("users/available-for-conversation/{conversation_id}")
    Call<List<UserResponse>> getAvailableUsersForConversation(
            @Header("Authorization") String authToken,
            @Path("conversation_id") String conversationId,
            @Query("search_query") String searchQuery, // Optional search
            @Query("page") int page,
            @Query("per_page") int perPage
    );
}