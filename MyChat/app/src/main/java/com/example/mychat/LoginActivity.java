package com.example.mychat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mychat.auth_models.Token;
import com.example.mychat.auth_models.UserLoginRequest;
import com.example.mychat.auth_models.UserProfile;
import com.example.mychat.retrofit_client.RetrofitClient;
import com.example.mychat.service_api.AuthApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView btnRegister;
    private TextView tvForgotPassword;

    private AuthApiService authApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.initializeTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        authApiService = RetrofitClient.getAuthApiService();

        btnLogin.setOnClickListener(v -> loginUser());

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            // Tidak perlu finish() di sini jika Anda ingin pengguna bisa kembali ke LoginActivity dari RegisterActivity
            // finish();
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        UserLoginRequest loginRequest = new UserLoginRequest(email, password);

        authApiService.loginUser(loginRequest).enqueue(new Callback<Token>() {
            @Override
            public void onResponse(Call<Token> call, Response<Token> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Token token = response.body();
                    Log.d(TAG, "Login successful: Access Token - " + token.getAccessToken());
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                    // --- LANGSUNG SIMPAN TOKEN DI SINI ---
                    saveAuthTokens(token.getAccessToken(), token.getRefreshToken());

                    // Kemudian dapatkan profil pengguna
                    getCurrentUserProfile("Bearer " + token.getAccessToken());

                } else {
                    String errorMessage = "Login failed: Unknown error";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorMessage);
                            if (errorMessage.contains("detail")) {
                                int startIndex = errorMessage.indexOf("detail\":\"") + "detail\":\"".length();
                                int endIndex = errorMessage.indexOf("\"", startIndex);
                                if (startIndex != -1 && endIndex != -1) {
                                    errorMessage = errorMessage.substring(startIndex, endIndex);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error body: " + e.getMessage());
                    }
                    Log.e(TAG, "Login failed: " + response.code() + " - " + errorMessage);
                    Toast.makeText(LoginActivity.this, "Login failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Token> call, Throwable t) {
                Log.e(TAG, "Login error: " + t.getMessage(), t);
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Metode baru untuk mendapatkan profil pengguna setelah login
    private void getCurrentUserProfile(String accessToken) {
        authApiService.getCurrentUserProfile(accessToken).enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserProfile userProfile = response.body();
                    Log.d(TAG, "User Profile fetched: " + userProfile.getUsername() + ", ID: " + userProfile.getId());

                    // --- HANYA SIMPAN ID PENGGUNA DI SINI ---
                    saveUserId(userProfile.getId());

                    // Navigasi ke Main Activity
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = "Failed to fetch user profile after login.";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage += " Error: " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error body for profile: " + e.getMessage());
                    }
                    Log.e(TAG, errorMessage);
                    Toast.makeText(LoginActivity.this, "Failed to retrieve user profile.", Toast.LENGTH_LONG).show();
                    // Mungkin paksa logout atau minta login ulang jika profil tidak bisa didapatkan
                }
            }

            @Override
            public void onFailure(Call<UserProfile> call, Throwable t) {
                Log.e(TAG, "Error fetching user profile after login: " + t.getMessage(), t);
                Toast.makeText(LoginActivity.this, "Network error fetching profile: " + t.getMessage(), Toast.LENGTH_LONG).show();
                // Mungkin paksa logout atau minta login ulang
            }
        });
    }

    // Metode untuk menyimpan access dan refresh token
    private void saveAuthTokens(String accessToken, String refreshToken) {
        SharedPreferences sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("access_token", accessToken);
        editor.putString("refresh_token", refreshToken);
        editor.apply();
        Log.d(TAG, "Access and Refresh Tokens saved to SharedPreferences.");
    }

    // Metode untuk menyimpan ID pengguna
    private void saveUserId(String userId) {
        SharedPreferences sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_id", userId);
        editor.apply();
        Log.d(TAG, "User ID saved to SharedPreferences: " + userId);
    }


    // Metode untuk mendapatkan user ID di mana saja
    public static String getUserId(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("user_id", null);
    }

    // Metode untuk menghapus semua data autentikasi saat logout
    public static void clearAuthData(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        Log.d(TAG, "All auth data cleared from SharedPreferences.");
    }
}