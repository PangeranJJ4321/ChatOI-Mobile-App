package com.example.mychat.conversation_service;

import com.example.mychat.models_conversation.ConversationListResponse;
import com.example.mychat.models_conversation.ConversationResponse;
import com.example.mychat.models_conversation.ConversationCreate;
import com.example.mychat.models_conversation.ConversationUpdate;
import com.example.mychat.models_conversation.ParticipantAdd;
import com.example.mychat.models_conversation.ParticipantMuteUpdate;
import com.example.mychat.models_conversation.ParticipantUpdate;


import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ConversationApiService {

    @POST("conversations/")
    Call<ConversationResponse> createConversation(
            @Header("Authorization") String authorization,
            @Body ConversationCreate conversationCreate
    );

    @GET("conversations/{conversation_id}")
    Call<ConversationResponse> getConversation(
            @Header("Authorization") String authorization,
            @Path("conversation_id") String conversationId
    );

    @GET("conversations/")
    Call<List<ConversationListResponse>> getUserConversations(
            @Header("Authorization") String authorization,
            @Query("page") int page,
            @Query("per_page") int perPage,
            @Query("search_query") String searchQuery, // Tambahan
            @Query("is_group_filter") Boolean isGroupFilter, // Tambahan
            @Query("unread_only_filter") Boolean unreadOnlyFilter // Tambahan
    );

    @PUT("conversations/{conversation_id}")
    Call<ConversationResponse> updateConversation(
            @Header("Authorization") String authorization,
            @Path("conversation_id") String conversationId,
            @Body ConversationUpdate conversationUpdate
    );

    @DELETE("conversations/{conversation_id}")
    Call<Void> deleteConversation(
            @Header("Authorization") String authorization,
            @Path("conversation_id") String conversationId
    );

    @POST("conversations/{conversation_id}/participants")
    Call<Void> addParticipants(
            @Header("Authorization") String authorization,
            @Path("conversation_id") String conversationId,
            @Body ParticipantAdd participantAdd
    );

    @DELETE("conversations/{conversation_id}/participants/{participant_user_id}")
    Call<Void> removeParticipant(
            @Header("Authorization") String authorization,
            @Path("conversation_id") String conversationId,
            @Path("participant_user_id") String participantUserId
    );

    @PUT("conversations/{conversation_id}/participants/{participant_user_id}/role")
    Call<Void> updateParticipantRole(
            @Header("Authorization") String authorization,
            @Path("conversation_id") String conversationId,
            @Path("participant_user_id") String participantUserId,
            @Body ParticipantUpdate participantUpdate
    );

    @POST("conversations/{conversation_id}/leave")
    Call<Void> leaveConversation(
            @Header("Authorization") String authorization,
            @Path("conversation_id") String conversationId
    );

    @PUT("conversations/{conversation_id}/participants/{participant_user_id}/mute")
    Call<Void> updateParticipantMuteStatus(
            @Header("Authorization") String authorization,
            @Path("conversation_id") String conversationId,
            @Path("participant_user_id") String participantUserId,
            @Body ParticipantMuteUpdate muteUpdate
    );
}