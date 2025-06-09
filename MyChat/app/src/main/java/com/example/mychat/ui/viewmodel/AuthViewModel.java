package com.example.mychat.ui.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.mychat.auth_models.RefreshTokenRequest;
import com.example.mychat.auth_models.Token;
import com.example.mychat.auth_models.UserProfile;
import com.example.mychat.retrofit_client.RetrofitClient;
import com.example.mychat.service_api.AuthApiService;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends AndroidViewModel {

    private static final String TAG = "AuthViewModel";
    private static final String AUTH_PREFS_NAME = "AuthPrefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";

    private final AuthApiService authApiService;
    private final SharedPreferences sharedPreferences;

    // LiveData untuk mengamati status autentikasi
    public final MutableLiveData<Boolean> isAuthenticated = new MutableLiveData<>();
    // LiveData untuk pesan error (misalnya dari API)
    public final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        authApiService = RetrofitClient.getAuthApiService();
        sharedPreferences = application.getSharedPreferences(AUTH_PREFS_NAME, Context.MODE_PRIVATE);

        // Inisialisasi awal status autentikasi berdasarkan token yang ada
        isAuthenticated.setValue(hasValidTokens());
    }

    // Metode untuk memeriksa token di SharedPreferences
    private boolean hasValidTokens() {
        String accessToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, null);
        String refreshToken = sharedPreferences.getString(KEY_REFRESH_TOKEN, null);
        String userId = sharedPreferences.getString(KEY_USER_ID, null);

        // Kriteria sederhana: token tidak null dan tidak kosong, serta userId ada.
        // Untuk validasi yang lebih kuat, Anda mungkin perlu memeriksa kedaluwarsa token.
        return accessToken != null && !accessToken.isEmpty() &&
                refreshToken != null && !refreshToken.isEmpty() &&
                userId != null && !userId.isEmpty();
    }

    // Metode yang dipanggil dari SplashActivity untuk pengecekan awal
    public void checkInitialAuthState() {
        if (hasValidTokens()) {
            // Jika ada token, coba refresh token untuk memastikan validitasnya
            // atau langsung anggap terautentikasi jika Anda tidak ingin terlalu ketat
            Log.d(TAG, "Local tokens found. Attempting to refresh token...");
            refreshToken(); // Coba refresh token
        } else {
            Log.d(TAG, "No valid local tokens found. User is not authenticated.");
            isAuthenticated.postValue(false);
        }
    }

    // Metode untuk melakukan refresh token
    private void refreshToken() {
        String currentRefreshToken = sharedPreferences.getString(KEY_REFRESH_TOKEN, null);
        if (currentRefreshToken == null) {
            isAuthenticated.postValue(false);
            errorMessage.postValue("No refresh token available.");
            return;
        }

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(currentRefreshToken);

        authApiService.refreshToken(refreshTokenRequest).enqueue(new Callback<Token>() {
            @Override
            public void onResponse(Call<Token> call, Response<Token> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Token newToken = response.body();
                    saveAuthTokens(newToken.getAccessToken(), newToken.getRefreshToken());
                    Log.d(TAG, "Token refreshed successfully. Fetching user profile...");
                    // Setelah refresh, panggil API profil untuk memastikan ID pengguna ada
                    fetchUserProfile(newToken.getAccessToken());
                } else {
                    String error = "Failed to refresh token.";
                    try {
                        if (response.errorBody() != null) {
                            error += " " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing refresh error body: " + e.getMessage());
                    }
                    Log.e(TAG, "Refresh token failed: " + response.code() + " - " + error);
                    clearAuthData(); // Hapus token jika refresh gagal
                    isAuthenticated.postValue(false);
                    errorMessage.postValue("Session expired. Please log in again.");
                }
            }

            @Override
            public void onFailure(Call<Token> call, Throwable t) {
                Log.e(TAG, "Network error during token refresh: " + t.getMessage(), t);
                clearAuthData(); // Hapus token jika ada error jaringan
                isAuthenticated.postValue(false);
                errorMessage.postValue("Network error. Please try logging in again.");
            }
        });
    }

    // Metode untuk mengambil profil pengguna setelah login/refresh
    private void fetchUserProfile(String accessToken) {
        authApiService.getCurrentUserProfile("Bearer " + accessToken).enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserProfile userProfile = response.body();
                    saveUserId(userProfile.getId());
                    Log.d(TAG, "User profile fetched: " + userProfile.getUsername() + ", ID: " + userProfile.getId());
                    isAuthenticated.postValue(true); // Autentikasi berhasil
                } else {
                    String error = "Failed to fetch user profile after refresh/login.";
                    try {
                        if (response.errorBody() != null) {
                            error += " " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing profile error body: " + e.getMessage());
                    }
                    Log.e(TAG, error);
                    clearAuthData();
                    isAuthenticated.postValue(false);
                    errorMessage.postValue("Failed to get user profile. Please log in again.");
                }
            }

            @Override
            public void onFailure(Call<UserProfile> call, Throwable t) {
                Log.e(TAG, "Network error fetching user profile: " + t.getMessage(), t);
                clearAuthData();
                isAuthenticated.postValue(false);
                errorMessage.postValue("Network error. Please try logging in again.");
            }
        });
    }


    // Metode untuk menyimpan access dan refresh token ke SharedPreferences
    private void saveAuthTokens(String accessToken, String refreshToken) {
        sharedPreferences.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
        Log.d(TAG, "Access and Refresh Tokens saved.");
    }

    // Metode untuk menyimpan ID pengguna ke SharedPreferences
    private void saveUserId(String userId) {
        sharedPreferences.edit()
                .putString(KEY_USER_ID, userId)
                .apply();
        Log.d(TAG, "User ID saved: " + userId);
    }

    // Metode untuk menghapus semua data autentikasi
    private void clearAuthData() {
        sharedPreferences.edit().clear().apply();
        Log.d(TAG, "All auth data cleared from SharedPreferences.");
    }
}