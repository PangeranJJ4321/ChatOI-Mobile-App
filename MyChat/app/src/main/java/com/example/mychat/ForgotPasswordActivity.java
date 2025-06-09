package com.example.mychat;

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

import com.example.mychat.auth_models.MessageResponse;
import com.example.mychat.auth_models.PasswordResetRequest;
import com.example.mychat.retrofit_client.RetrofitClient;
import com.example.mychat.service_api.AuthApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";

    private EditText etEmailForgotPassword;
    private Button btnSendResetLink;
    private TextView tvBackToLoginForgot;

    private AuthApiService authApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.initializeTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Inisialisasi View
        etEmailForgotPassword = findViewById(R.id.etForgotEmail);
        btnSendResetLink = findViewById(R.id.btnSendResetLink);
        tvBackToLoginForgot = findViewById(R.id.btnBackToLogin);

        // Inisialisasi Retrofit Service
        authApiService = RetrofitClient.getAuthApiService();

        // Set Listener untuk tombol Send Reset Link
        btnSendResetLink.setOnClickListener(v -> sendResetLink());

        // Set Listener untuk teks Back to Login
        tvBackToLoginForgot.setOnClickListener(v -> finish());
    }

    private void sendResetLink() {
        String email = etEmailForgotPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmailForgotPassword.setError("Email is required");
            etEmailForgotPassword.requestFocus();
            return;
        }

        PasswordResetRequest resetRequest = new PasswordResetRequest(email);

        authApiService.forgotPassword(resetRequest).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Forgot password successful: " + response.body().getMessage());
                    Toast.makeText(ForgotPasswordActivity.this, response.body().getMessage(), Toast.LENGTH_LONG).show();
                    finish(); // Kembali ke LoginActivity
                } else {
                    String errorMessage = "Failed to send reset link: Unknown error";
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
                    Log.e(TAG, "Forgot password failed: " + response.code() + " - " + errorMessage);
                    Toast.makeText(ForgotPasswordActivity.this, "Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable t) {
                Log.e(TAG, "Forgot password error: " + t.getMessage(), t);
                Toast.makeText(ForgotPasswordActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}