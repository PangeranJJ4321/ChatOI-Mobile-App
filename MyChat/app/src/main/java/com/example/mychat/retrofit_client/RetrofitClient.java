package com.example.mychat.retrofit_client;

import com.example.mychat.conversation_service.ConversationApiService;
import com.example.mychat.conversation_service.FileApiService;
import com.example.mychat.conversation_service.MessageApiService;
import com.example.mychat.conversation_service.UserApiService;
import com.example.mychat.service_api.AuthApiService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    public static final String BASE_URL = "http://192.168.43.43:8000/api/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(logging);

            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS") // Sesuaikan jika format microsecond berbeda
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(httpClient.build())
                    .build();
        }
        return retrofit;
    }

    public static AuthApiService getAuthApiService() {
        return getClient().create(AuthApiService.class);
    }

    public static UserApiService getUserApiService() {
        return getClient().create(UserApiService.class);

    }

    public static FileApiService getFileApiService() {
        return getClient().create(FileApiService.class);
    }

    public static ConversationApiService getConversationApiService() {
        return getClient().create(ConversationApiService.class);

    }

    public static MessageApiService getMessageApiService() {
        return getClient().create(MessageApiService.class);
    }




}