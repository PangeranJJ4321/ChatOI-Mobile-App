package com.example.mychat.conversation_service;


import com.example.mychat.models_message.MessageCreate;
import com.example.mychat.models_message.MessageReactionCreate;
import com.example.mychat.models_message.MessageReactionResponse;
import com.example.mychat.models_message.MessageReadReceiptUpdate;
import com.example.mychat.models_message.MessageResponse;
import com.example.mychat.models_message.MessageUpdate;
import com.example.mychat.models_message.MessagesResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MessageApiService {
    @POST("messages/")
    Call<MessageResponse> createMessage(
            @Header("Authorization") String authToken,
            @Body MessageCreate messageData
    );

    @GET("messages/{message_id}")
    Call<MessageResponse> getMessage(
            @Header("Authorization") String authToken,
            @Path("message_id") String messageId
    );

    @GET("messages/conversation/{conversation_id}")
    Call<MessagesResponse> getMessagesInConversation(
            @Header("Authorization") String authToken,
            @Path("conversation_id") String conversationId,
            @Query("page") int page,
            @Query("per_page") int perPage,
            @Query("before_message_id") String beforeMessageId
    );

    @PUT("messages/{message_id}")
    Call<MessageResponse> updateMessage(
            @Header("Authorization") String authToken,
            @Path("message_id") String messageId,
            @Body MessageUpdate messageData
    );

    @DELETE("messages/{message_id}")
    Call<Void> deleteMessage(
            @Header("Authorization") String authToken,
            @Path("message_id") String messageId
    );

    @POST("messages/{message_id}/reactions")
    Call<MessageReactionResponse> addMessageReaction(
            @Header("Authorization") String authToken,
            @Path("message_id") String messageId,
            @Body MessageReactionCreate reactionData
    );

    @DELETE("messages/{message_id}/reactions")
    Call<Void> removeMessageReaction(
            @Header("Authorization") String authToken,
            @Path("message_id") String messageId,
            @Query("emoji") String emoji
    );

    @POST("messages/conversation/{conversation_id}/read-receipts")
    Call<Void> markMessagesAsRead(
            @Header("Authorization") String authToken,
            @Path("conversation_id") String conversationId,
            @Body MessageReadReceiptUpdate readReceiptData
    );
}