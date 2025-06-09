// EditProfileActivity.java
package com.example.mychat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.mychat.conversation_service.FileApiService;
import com.example.mychat.conversation_service.UserApiService;
import com.example.mychat.models_file.FileUploadResponse;
import com.example.mychat.models_user.UserResponse;
import com.example.mychat.models_user.UserUpdate;
import com.example.mychat.retrofit_client.RetrofitClient;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";

    ImageButton btnBack;
    private ImageView ivEditUserAvatar;
    private CardView ivEditCameraOverlay;

    private TextInputEditText etEditUsername, etEditEmail, etEditPassword;

    private Button btnSaveChanges;

    private UserApiService userApiService;
    private FileApiService fileApiService;
    private String currentUserId;
    private String accessToken;
    private SharedPreferences sharedPreferences;
    private Gson gson;

    private Uri selectedImageUri;
    private String currentAvatarUrl; // Untuk melacak avatar yang sedang ditampilkan (bisa lokal atau dari server)

    private boolean isSaving = false;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(selectedImageUri).into(ivEditUserAvatar);
                    Toast.makeText(this, "Image selected. Click 'Save Changes' to update.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "No image selected.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    pickImageLauncher.launch("image/*");
                } else {
                    Toast.makeText(this, "Permission denied to access media files.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        initializeServices();
        gson = new Gson(); // Inisialisasi Gson

        if (currentUserId == null || accessToken == null) {
            Log.e(TAG, "User ID or Access Token not found. Redirecting to login.");
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        loadUserProfileLocally(); // Muat dari lokal terlebih dahulu
        loadUserProfileFromApi(); // Kemudian dari API untuk update

        setupClickListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        ivEditUserAvatar = findViewById(R.id.iv_edit_user_avatar);
        ivEditCameraOverlay = findViewById(R.id.iv_edit_camera_overlay);
        etEditUsername = findViewById(R.id.etEditUsername);
        etEditEmail = findViewById(R.id.etEditEmail);
        etEditPassword = findViewById(R.id.etEditPassword);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
    }

    private void initializeServices() {
        userApiService = RetrofitClient.getUserApiService();
        fileApiService = RetrofitClient.getFileApiService();
        currentUserId = LoginActivity.getUserId(this);
        accessToken = getAccessToken(this);
        sharedPreferences = getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE); // Inisialisasi SharedPreferences
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSaveChanges.setOnClickListener(v -> saveUserProfileChanges());
        ivEditCameraOverlay.setOnClickListener(v -> checkPermissionAndPickImage());
    }

    private void checkPermissionAndPickImage() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            pickImageLauncher.launch("image/*");
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    // Metode baru untuk memuat profil dari SharedPreferences
    private void loadUserProfileLocally() {
        String userProfileJson = sharedPreferences.getString("cached_user_profile", null);
        if (userProfileJson != null) {
            try {
                UserResponse user = gson.fromJson(userProfileJson, UserResponse.class);
                if (user != null) {
                    updateProfileUI(user);
                    Log.d(TAG, "User profile loaded from local cache.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing cached user profile: " + e.getMessage());
            }
        }
    }

    // Metode untuk memperbarui UI dengan data profil pengguna
    private void updateProfileUI(UserResponse user) {
        etEditUsername.setText(user.getUsername());
        etEditEmail.setText(user.getEmail());
        currentAvatarUrl = user.getProfilePicture(); // Simpan URL avatar saat ini

        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(currentAvatarUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(ivEditUserAvatar);
        } else {
            ivEditUserAvatar.setImageResource(R.drawable.default_avatar);
        }
    }

    // Ubah nama metode dari loadUserProfileForEditing menjadi loadUserProfileFromApi
    private void loadUserProfileFromApi() {
        userApiService.getUser("Bearer " + accessToken, currentUserId).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body();
                    updateProfileUI(user); // Perbarui UI dengan data terbaru dari API
                    // Simpan ke cache lokal
                    sharedPreferences.edit().putString("cached_user_profile", gson.toJson(user)).apply();
                    Log.d(TAG, "User profile loaded from API and cached.");
                } else {
                    String errorMessage = parseErrorResponse(response);
                    Log.e(TAG, "Failed to load user profile from API: " + response.code() + " - " + errorMessage);
                    Toast.makeText(EditProfileActivity.this, "Failed to load profile (API). Displaying cached data.", Toast.LENGTH_LONG).show();

                    if (response.code() == 401) {
                        redirectToLogin();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading user profile from API: " + t.getMessage(), t);
                Toast.makeText(EditProfileActivity.this, "Network error loading profile (API). Displaying cached data.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveUserProfileChanges() {
        if (isSaving) {
            Toast.makeText(this, "Please wait, saving in progress...", Toast.LENGTH_SHORT).show();
            return;
        }

        String newUsername = etEditUsername.getText().toString().trim();
        String newEmailInput = etEditEmail.getText().toString().trim();
        String newPasswordInput = etEditPassword.getText().toString().trim();

        if (!validateInputs(newUsername, newEmailInput, newPasswordInput)) {
            return;
        }

        isSaving = true;
        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("Saving...");
        Toast.makeText(this, "Saving changes locally...", Toast.LENGTH_SHORT).show();

        // Simpan perubahan ke SharedPreferences secara langsung
        UserResponse updatedUserLocally = new UserResponse(
                currentUserId,
                newUsername,
                currentAvatarUrl,
                true
        );
        sharedPreferences.edit().putString("cached_user_profile", gson.toJson(updatedUserLocally)).apply();
        // Update UI secara langsung setelah disimpan lokal
        updateProfileUI(updatedUserLocally);


        // Sekarang coba sinkronkan ke server
        String newEmail = newEmailInput.isEmpty() ? null : newEmailInput;
        String newPassword = newPasswordInput.isEmpty() ? null : newPasswordInput;

        if (selectedImageUri != null) {
            uploadAvatarAndSyncUserData(newUsername, newEmail, newPassword);
        } else {
            syncUserDataOnly(newUsername, newEmail, newPassword);
        }
    }

    private boolean validateInputs(String username, String email, String password) {
        if (username.isEmpty()) {
            etEditUsername.setError("Username cannot be empty");
            etEditUsername.requestFocus();
            return false;
        }

        if (!email.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEditEmail.setError("Enter a valid email address");
            etEditEmail.requestFocus();
            return false;
        }

        if (!password.isEmpty() && password.length() < 6) {
            etEditPassword.setError("Password must be at least 6 characters");
            etEditPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void uploadAvatarAndSyncUserData(String username, String email, String password) {
        // Counter untuk melacak completion dari kedua operasi
        final boolean[] avatarCompleted = {false};
        final boolean[] userDataCompleted = {false};
        final boolean[] hasError = {false};

        // Upload avatar
        uploadAvatarOnly(new AvatarUploadCallback() {
            @Override
            public void onSuccess(String newAvatarUrl) {
                currentAvatarUrl = newAvatarUrl; // Update currentAvatarUrl dengan yang dari server
                // Perbarui juga cache profil lokal dengan URL avatar baru
                String userProfileJson = sharedPreferences.getString("cached_user_profile", null);
                if (userProfileJson != null) {
                    UserResponse user = gson.fromJson(userProfileJson, UserResponse.class);
                    if (user != null) {
                        user.setProfilePicture(newAvatarUrl);
                        sharedPreferences.edit().putString("cached_user_profile", gson.toJson(user)).apply();
                    }
                }
                avatarCompleted[0] = true;
                checkBothOperationsCompleted(avatarCompleted[0], userDataCompleted[0], hasError[0]);
            }

            @Override
            public void onError(String error) {
                hasError[0] = true;
                Log.e(TAG, "Avatar upload failed: " + error);
                Toast.makeText(EditProfileActivity.this, "Avatar upload failed: " + error + ". Will retry later.", Toast.LENGTH_LONG).show();
                // Tetap lanjut update user data meskipun avatar gagal
                avatarCompleted[0] = true;
                checkBothOperationsCompleted(avatarCompleted[0], userDataCompleted[0], hasError[0]);
            }
        });

        // Update user data (tanpa avatar, karena avatar diunggah terpisah)
        syncUserDataOnly(username, email, password, new UserUpdateCallback() {
            @Override
            public void onSuccess() {
                userDataCompleted[0] = true;
                checkBothOperationsCompleted(avatarCompleted[0], userDataCompleted[0], hasError[0]);
            }

            @Override
            public void onError(String error) {
                hasError[0] = true;
                Toast.makeText(EditProfileActivity.this, "Profile data update failed: " + error + ". Will retry later.", Toast.LENGTH_LONG).show();
                userDataCompleted[0] = true;
                checkBothOperationsCompleted(avatarCompleted[0], userDataCompleted[0], hasError[0]);
            }
        });
    }

    private void checkBothOperationsCompleted(boolean avatarDone, boolean userDataDone, boolean hasError) {
        if (avatarDone && userDataDone) {
            isSaving = false;
            btnSaveChanges.setEnabled(true);
            btnSaveChanges.setText("Save Changes");

            if (!hasError) {
                Toast.makeText(this, "Profile synced successfully!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Profile partially synced or will retry. Check internet connection.", Toast.LENGTH_LONG).show();
                // Di sini, Anda bisa memicu WorkManager untuk sinkronisasi ulang jika ada error
            }
        }
    }

    private void syncUserDataOnly(String username, String email, String password) {
        syncUserDataOnly(username, email, password, new UserUpdateCallback() {
            @Override
            public void onSuccess() {
                isSaving = false;
                btnSaveChanges.setEnabled(true);
                btnSaveChanges.setText("Save Changes");
                Toast.makeText(EditProfileActivity.this, "Profile synced successfully!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onError(String error) {
                isSaving = false;
                btnSaveChanges.setEnabled(true);
                btnSaveChanges.setText("Save Changes");
                Toast.makeText(EditProfileActivity.this, "Failed to sync profile: " + error + ". Will retry later.", Toast.LENGTH_LONG).show();
                // Memicu WorkManager untuk sinkronisasi ulang jika ada error
            }
        });
    }

    private void syncUserDataOnly(String username, String email, String password, UserUpdateCallback callback) {
        UserUpdate userUpdate = new UserUpdate(
                username,
                email,
                password,
                null, // Avatar selalu null untuk endpoint user update, karena diunggah terpisah
                null
        );

        userApiService.updateUser(currentUserId, userUpdate).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "User data synced: " + response.body().getUsername());
                    // Perbarui cache profil lokal dengan data terbaru dari server
                    sharedPreferences.edit().putString("cached_user_profile", gson.toJson(response.body())).apply();
                    callback.onSuccess();
                } else {
                    String errorMessage = parseErrorResponse(response);
                    Log.e(TAG, "Failed to sync user data: " + response.code() + " - " + errorMessage);

                    if (response.code() == 401) {
                        redirectToLogin();
                        return;
                    }
                    callback.onError(errorMessage);
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Network error syncing user data: " + t.getMessage(), t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    private void uploadAvatarOnly(AvatarUploadCallback callback) {
        if (selectedImageUri == null) {
            callback.onError("No image selected");
            return;
        }

        File file = getFileFromUri(selectedImageUri);
        if (file == null) {
            callback.onError("Could not process selected image");
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse(getContentResolver().getType(selectedImageUri)), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

        fileApiService.uploadAvatar("Bearer " + accessToken, body)
                .enqueue(new Callback<FileUploadResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<FileUploadResponse> call, @NonNull Response<FileUploadResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String newAvatarUrl = response.body().getFileUrl();
                            Log.d(TAG, "Avatar uploaded successfully: " + newAvatarUrl);
                            callback.onSuccess(newAvatarUrl);
                        } else {
                            String errorMessage = parseErrorResponse(response);
                            Log.e(TAG, "Failed to upload avatar: " + response.code() + " - " + errorMessage);
                            callback.onError(errorMessage);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<FileUploadResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "Network error during avatar upload: " + t.getMessage(), t);
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
    }

    private String parseErrorResponse(Response<?> response) {
        String errorMessage = "Unknown error";
        try {
            if (response.errorBody() != null) {
                errorMessage = response.errorBody().string();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing error body: " + e.getMessage());
        }
        return errorMessage;
    }

    private interface AvatarUploadCallback {
        void onSuccess(String newAvatarUrl);
        void onError(String error);
    }

    private interface UserUpdateCallback {
        void onSuccess();
        void onError(String error);
    }

    private File getFileFromUri(Uri uri) {
        String fileName = getFileName(uri);
        if (fileName == null) {
            fileName = "temp_image_upload.jpg";
        }

        File tempFile = new File(getCacheDir(), fileName);
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            if (inputStream != null) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                return tempFile;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting URI to file: " + e.getMessage(), e);
        }
        return null;
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void redirectToLogin() {
        Intent intent = new Intent(EditProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getAccessToken(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("access_token", null);
    }
}