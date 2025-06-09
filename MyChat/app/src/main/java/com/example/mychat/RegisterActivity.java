package com.example.mychat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText; // This import is not strictly necessary if you only use TextInputEditText
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mychat.auth_models.UserProfile;
import com.example.mychat.auth_models.UserRegisterRequest;
import com.example.mychat.retrofit_client.RetrofitClient;
import com.example.mychat.service_api.AuthApiService;
import com.google.android.material.textfield.TextInputEditText; // This is the correct import for your usage
import com.google.android.material.textfield.TextInputLayout; // You might want to keep this if you need to manipulate the TextInputLayout itself

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private TextInputEditText etRegisterUsername, etRegisterEmail, etRegisterPassword;
    private Button btnSubmitRegister;
    private TextView tvBackToLogin;

    private AuthApiService authApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.initializeTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etRegisterUsername = findViewById(R.id.etRegisterUsername);
        // CORRECTED LINE: Get the TextInputEditText for email
        etRegisterEmail = findViewById(R.id.etRegisterEmail);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        btnSubmitRegister = findViewById(R.id.btnRegister);
        tvBackToLogin = findViewById(R.id.btnGoToLogin);

        authApiService = RetrofitClient.getAuthApiService();

        btnSubmitRegister.setOnClickListener(v -> registerUser());

        tvBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String username = etRegisterUsername.getText().toString().trim();
        String email = etRegisterEmail.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();

        if (username.isEmpty()) {
            etRegisterUsername.setError("Username is required");
            etRegisterUsername.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            etRegisterEmail.setError("Email is required"); // This will now correctly set error on the TextInputEditText
            etRegisterEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etRegisterPassword.setError("Password is required");
            etRegisterPassword.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etRegisterPassword.setError("Password must be at least 6 characters");
            etRegisterPassword.requestFocus();
            return;
        }

        UserRegisterRequest registerRequest = new UserRegisterRequest(username, email, password);

        authApiService.registerUser(registerRequest).enqueue(new Callback<UserProfile>() {
            @Override
            public void onResponse(Call<UserProfile> call, Response<UserProfile> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserProfile userProfile = response.body();
                    Log.d(TAG, "Registration successful for user: " + userProfile.getUsername() + ", ID: " + userProfile.getId());
                    Toast.makeText(RegisterActivity.this, "Registration successful! Please login.", Toast.LENGTH_LONG).show();


                    saveUserId(userProfile.getId());

                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMessage = "Registration failed: Unknown error";
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
                    Log.e(TAG, "Registration failed: " + response.code() + " - " + errorMessage);
                    Toast.makeText(RegisterActivity.this, "Registration failed: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<UserProfile> call, Throwable t) {
                Log.e(TAG, "Registration error: " + t.getMessage(), t);
                Toast.makeText(RegisterActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveUserId(String userId) {
        SharedPreferences sharedPreferences = getSharedPreferences("AuthPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_id", userId);
        editor.apply();
        Log.d(TAG, "User ID saved after registration: " + userId);
    }
}