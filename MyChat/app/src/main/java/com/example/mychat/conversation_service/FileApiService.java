package com.example.mychat.conversation_service;


import com.example.mychat.models_file.AttachmentResponse;
import com.example.mychat.models_file.FileUploadResponse;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface FileApiService {
    @Multipart
    @POST("files/avatar")
    Call<FileUploadResponse> uploadAvatar(
            @Header("Authorization") String authToken,
            @Part MultipartBody.Part file
    );

    @Multipart
    @POST("files/upload-group-avatar")
    Call<FileUploadResponse> uploadGroupAvatar(
            @Header("Authorization") String authToken,
            @Part MultipartBody.Part file
    );

    @Multipart
    @POST("files/attachments/{message_id}")
    Call<AttachmentResponse> uploadAttachment(
            @Header("Authorization") String authToken,
            @Path("message_id") String messageId,
            @Part MultipartBody.Part file
    );

    @DELETE("files/attachments/{attachment_id}")
    Call<Void> deleteAttachment(
            @Header("Authorization") String authToken,
            @Path("attachment_id") String attachmentId
    );

}