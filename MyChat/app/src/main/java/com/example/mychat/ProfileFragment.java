// ProfileFragment.java
package com.example.mychat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.mychat.auth_models.MessageResponse;
import com.example.mychat.conversation_service.UserApiService;
import com.example.mychat.models_user.BlockUserRequest;
import com.example.mychat.models_user.UserResponse;
import com.example.mychat.service_api.AuthApiService;
import com.example.mychat.retrofit_client.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class ProfileFragment extends Fragment implements BlockedUserAdapter.OnUnblockClickListener {

    private static  final String TAG = "ProfileFragment";

    private ImageButton btnEditProfile;

    // User Info Card elements
    private ImageView ivUserAvatar;
    private ImageView ivCameraOverlayProfile;
    private TextView tvUserName, tvUserEmail, tvUserId, tvUserStatus;
    private ImageButton btnCopyUserId;


    // Logout Button
    private ImageButton btnLogout;

    private TextView tvBlockedUsersCount;
    private RecyclerView rvBlockedUsers;
    private BlockedUserAdapter blockedUserAdapter;
    private List<UserResponse> blockedUsersList;

    private UserApiService userApiService;
    private AuthApiService authApiService;
    private String currentUserId;
    private String accessToken;
    private SharedPreferences sharedPreferences; // Tambahkan SharedPreferences

    private Gson gson; // Untuk JSON serialisasi/deserialisasi

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inisialisasi SharedPreferences dan Gson
        sharedPreferences = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        gson = new Gson();

        btnEditProfile = view.findViewById(R.id.btn_edit_profile);

        // User info
        ivUserAvatar = view.findViewById(R.id.iv_user_avatar);
        ivCameraOverlayProfile = view.findViewById(R.id.iv_camera_overlay_profile);
        tvUserName = view.findViewById(R.id.tv_user_name);
        tvUserEmail = view.findViewById(R.id.tv_user_email);
        tvUserId = view.findViewById(R.id.tv_user_id);
        tvUserStatus = view.findViewById(R.id.tv_user_status);
        btnCopyUserId = view.findViewById(R.id.btn_copy_user_id);


        // Logout Button
        btnLogout = view.findViewById(R.id.btn_logout);

        tvBlockedUsersCount = view.findViewById(R.id.tv_blocked_users_count);
        rvBlockedUsers = view.findViewById(R.id.rv_blocked_users);

        // Inisialisasi API Services
        userApiService = RetrofitClient.getUserApiService();
        authApiService = RetrofitClient.getAuthApiService();

        // Ambil User ID dan Access Token dari SharedPreferences
        currentUserId = LoginActivity.getUserId(getContext());
        accessToken = getAccessToken(getContext());

        if (currentUserId == null && accessToken == null) {
            Log.d(TAG, "User ID or Access Token not found. Redirecting to login.");
            Toast.makeText(getContext(), "User ID or Access Token not found. Redirecting to login.", Toast.LENGTH_SHORT).show();
            redirectToLogin();
            return;
        }

        // Konfigurasi RecyclerView
        rvBlockedUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        blockedUsersList = new ArrayList<>();
        blockedUserAdapter = new BlockedUserAdapter(getContext(), this);
        rvBlockedUsers.setAdapter(blockedUserAdapter);

        // Panggil metode untuk memuat data (dahulukan dari lokal)
        loadUserProfileLocally(); // Baru
        loadBlockedUsersLocally(); // Baru

        // Kemudian panggil API untuk mendapatkan data terbaru
        loadUserProfileFromApi(); // Ubah nama metode
        loadBlockedUsersFromApi(); // Ubah nama metode

        btnEditProfile.setOnClickListener(v -> handleEditProfile());
        ivCameraOverlayProfile.setOnClickListener(v -> handleChangeProfilePicture());
        btnCopyUserId.setOnClickListener(v -> copyUserIdToClipboard());
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    // Metode baru untuk memuat profil dari SharedPreferences
    private void loadUserProfileLocally() {
        String userProfileJson = sharedPreferences.getString("cached_user_profile", null);
        if (userProfileJson != null) {
            try {
                UserResponse user = gson.fromJson(userProfileJson, UserResponse.class);
                if (user != null) {
                    updateUserProfileUI(user);
                    Log.d(TAG, "User profile loaded from local cache: " + user.getUsername());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing cached user profile: " + e.getMessage());
            }
        }
    }

    // Metode baru untuk memuat daftar pengguna diblokir dari SharedPreferences
    private void loadBlockedUsersLocally() {
        String blockedUsersJson = sharedPreferences.getString("cached_blocked_users", null);
        if (blockedUsersJson != null) {
            try {
                Type type = new TypeToken<List<UserResponse>>(){}.getType();
                List<UserResponse> cachedBlockedUsers = gson.fromJson(blockedUsersJson, type);
                if (cachedBlockedUsers != null) {
                    blockedUsersList.clear();
                    blockedUsersList.addAll(cachedBlockedUsers);
                    blockedUserAdapter.setBlockedUsers(blockedUsersList);
                    tvBlockedUsersCount.setText("Pengguna Diblokir (" + blockedUsersList.size() + ")");
                    Log.d(TAG, "Blocked users loaded from local cache: " + blockedUsersList.size());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing cached blocked users: " + e.getMessage());
            }
        }
    }

    // Metode untuk memperbarui UI dengan data profil pengguna
    private void updateUserProfileUI(UserResponse user) {
        tvUserName.setText(user.getUsername());
        tvUserEmail.setText(user.getEmail());

        String fullId = user.getId();
        String displayId;
        if (fullId.length() > 4) {
            displayId = "..." + fullId.substring(fullId.length() - 4);
        } else {
            displayId = fullId;
        }
        tvUserId.setText("ID : " + displayId);

        if (user.isOnline()) {
            tvUserStatus.setText("Online");
            tvUserStatus.setSelected(true);
        } else {
            tvUserStatus.setText("Offline");
            tvUserStatus.setSelected(false);
        }

        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            Glide.with(ProfileFragment.this)
                    .load(user.getProfilePicture())
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(ivUserAvatar);
        } else {
            ivUserAvatar.setImageResource(R.drawable.default_avatar);
        }
    }

    // Ubah nama metode dari loadBlockedUsers menjadi loadBlockedUsersFromApi
    private void loadBlockedUsersFromApi() {
        if (currentUserId == null || accessToken == null) {
            Log.w(TAG, "Skipping loadBlockedUsersFromApi: User ID or Access Token is null.");
            return;
        }

        userApiService.getBlockedUsers("Bearer " + accessToken, currentUserId).enqueue(new Callback<List<UserResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserResponse>> call, @NonNull Response<List<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    blockedUsersList.clear();
                    blockedUsersList.addAll(response.body());
                    blockedUserAdapter.setBlockedUsers(blockedUsersList);
                    tvBlockedUsersCount.setText("Pengguna Diblokir (" + blockedUsersList.size() + ")");
                    Log.d(TAG, "Blocked users loaded from API: " + blockedUsersList.size());
                    // Simpan ke cache lokal
                    sharedPreferences.edit().putString("cached_blocked_users", gson.toJson(response.body())).apply();
                } else {
                    String errorMessage = "Failed to load blocked users from API: Unknown error";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage += " " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error body: " + e.getMessage());
                    }
                    Log.e(TAG, "Failed to load blocked users from API: " + response.code() + " - " + errorMessage);
                    Toast.makeText(getContext(), "Failed to load blocked users: " + errorMessage, Toast.LENGTH_LONG).show();
                    if (response.code() == 401) {
                        redirectToLogin();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading blocked users from API: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Network error loading blocked users. Displaying cached data.", Toast.LENGTH_LONG).show();
                // Tidak ada aksi lain, karena sudah menampilkan data cache
            }
        });
    }


    private void logoutUser() {
        if (accessToken == null) {
            redirectToLogin();
            return;
        }

        authApiService.logoutUser("Bearer " + accessToken).enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(@NonNull Call<MessageResponse> call, @NonNull Response<MessageResponse> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Logout API failed: " + response.code() + " - " + response.message());
                    Toast.makeText(getContext(), "Logout failed on server, but clearing local data.", Toast.LENGTH_SHORT).show();
                }
                // Selalu bersihkan data lokal dan redirect ke login
                LoginActivity.clearAuthData(getContext());
                // Hapus juga cache profil dan blocked users
                sharedPreferences.edit().remove("cached_user_profile").apply();
                sharedPreferences.edit().remove("cached_blocked_users").apply();
                redirectToLogin();
            }

            @Override
            public void onFailure(@NonNull Call<MessageResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Logout network error: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Network error during logout, clearing local data.", Toast.LENGTH_SHORT).show();
                // Tetap bersihkan data lokal dan redirect ke login
                LoginActivity.clearAuthData(getContext());
                // Hapus juga cache profil dan blocked users
                sharedPreferences.edit().remove("cached_user_profile").apply();
                sharedPreferences.edit().remove("cached_blocked_users").apply();
                redirectToLogin();
            }
        });
    }

    private void copyUserIdToClipboard() {
        String userId = tvUserId.getText().toString().replace("ID: ", "").trim();
        if (!userId.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("User ID", userId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "User ID copied to clipboard!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "User ID is not available.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleChangeProfilePicture() {
        Toast.makeText(getContext(), "Ke Profile Edit aja Anjir, bukan WA🙏", Toast.LENGTH_SHORT).show();
    }


    private void reinitializeUserData() {
        accessToken = getAccessToken(requireContext());
        currentUserId = getCurrentUserId(requireContext());

        Log.d(TAG, "Re-initialized - Access Token: " + (accessToken != null ? "Present" : "Null") +
                ", User ID: " + (currentUserId != null ? currentUserId : "Null"));

        if (accessToken == null || currentUserId == null) {
            Log.e(TAG, "Critical: Missing authentication data after re-initialization");
            redirectToLogin();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        // Re-initialize user data setiap kali fragment resume
        reinitializeUserData();

        // Hanya lanjut jika user data valid
        if (currentUserId != null && accessToken != null) {
            loadUserProfileLocally(); // Muat dari lokal (opsional, jika ingin refresh dari lokal setiap resume)
            loadUserProfileFromApi(); // Kemudian dari API
            loadBlockedUsersLocally(); // Muat dari lokal
            loadBlockedUsersFromApi(); // Kemudian dari API
        } else {
            Log.e(TAG, "Cannot proceed: Missing user authentication data");
            redirectToLogin();
        }
    }

    private void handleEditProfile() {
        Intent intent = new Intent(getActivity(), EditProfileActivity.class);
        startActivity(intent);
    }

    // Ubah nama metode dari loadUserProfile menjadi loadUserProfileFromApi
    private void loadUserProfileFromApi() {
        // Cek apakah user_id null sebelum memanggil API
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "Cannot load user profile: User ID is null or empty");
            Toast.makeText(getContext(), "User ID not found. Please log in again.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "Cannot load user profile: Access token is null or empty");
            Toast.makeText(getContext(), "Authentication failed. Please log in again.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        UserApiService userApiService = RetrofitClient.getUserApiService();
        Call<UserResponse> call = userApiService.getUser("Bearer " + accessToken, currentUserId);

        call.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body();
                    updateUserProfileUI(user);
                    Log.d(TAG, "User profile loaded from API: " + user.getUsername());
                    // Save to local cache
                    sharedPreferences.edit().putString("cached_user_profile", gson.toJson(user)).apply();
                } else {
                    String errorMessage = "Failed to load user profile from API: Unknown error";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage += " " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error body: " + e.getMessage());
                    }
                    Log.e(TAG, "Failed to load user profile from API: " + response.code() + " - " + errorMessage);
                    Toast.makeText(getContext(), "Failed to load profile: " + errorMessage, Toast.LENGTH_LONG).show();
                    if (response.code() == 401) {
                        redirectToLogin();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error loading user profile from API: " + t.getMessage(), t);

                // 🚨 Cek apakah fragment masih attached
                if (!isAdded()) return;

                Context context = getContext();
                if (context == null) return;

                // Cek apakah error karena user_id null
                if (t.getMessage() != null && t.getMessage().contains("user_id\" value must not be null")) {
                    Toast.makeText(context, "User session expired. Please log in again.", Toast.LENGTH_LONG).show();
                    redirectToLogin();
                    return;
                }

                // Handle other network errors
                String errorMessage;
                if (isNetworkAvailable()) {
                    errorMessage = "Failed to load profile from server. Using cached data.";
                } else {
                    errorMessage = "No internet connection. Using cached profile data.";
                }
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void redirectToLogin() {
        if (getActivity() != null) {
            // Clear SharedPreferences
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
            sharedPreferences.edit().clear().apply();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }

    // 6. Method untuk cek network (jika belum ada)
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private String getCurrentUserId(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("user_id", null);

        // Debug log untuk melihat nilai yang disimpan
        Log.d(TAG, "Retrieved user_id from SharedPreferences: " + userId);

        // Jika null, coba cek semua keys yang tersimpan
        if (userId == null) {
            Log.w(TAG, "User ID is null. Checking all saved keys in SharedPreferences:");
            for (String key : sharedPreferences.getAll().keySet()) {
                Log.d(TAG, "Key: " + key + ", Value: " + sharedPreferences.getAll().get(key));
            }
        }

        return userId;
    }
    // Helper method to get access token from SharedPreferences
    private String getAccessToken(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("access_token", null);
    }

    @Override
    public void onUnblockClick(UserResponse userToUnblock) {
        if (currentUserId == null || accessToken == null) {
            Toast.makeText(getContext(), "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        if (userToUnblock.getId() == null || userToUnblock.getId().isEmpty()) {
            Toast.makeText(getContext(), "Invalid user ID to unblock.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Optimistic UI update: hapus dari daftar lokal dulu
        int position = -1;
        for (int i = 0; i < blockedUsersList.size(); i++) {
            if (blockedUsersList.get(i).getId().equals(userToUnblock.getId())) {
                position = i;
                break;
            }
        }
        if (position != -1) {
            blockedUsersList.remove(position);
            blockedUserAdapter.notifyItemRemoved(position);
            tvBlockedUsersCount.setText("Pengguna Diblokir (" + blockedUsersList.size() + ")");
            // Update cache setelah perubahan lokal
            sharedPreferences.edit().putString("cached_blocked_users", gson.toJson(blockedUsersList)).apply();
        }


        BlockUserRequest unblockRequest = new BlockUserRequest(userToUnblock.getId());

        int finalPosition = position;
        userApiService.unblockUser("Bearer " + accessToken, currentUserId, unblockRequest).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), userToUnblock.getUsername() + " unblocked successfully!", Toast.LENGTH_SHORT).show();
                    // loadBlockedUsersFromApi(); // Tidak perlu muat ulang penuh, sudah diupdate lokal
                } else {
                    String errorMessage = "Failed to unblock user: Unknown error";
                    try {
                        if (response.errorBody() != null) {
                            errorMessage += " " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error body: " + e.getMessage());
                    }
                    Log.e(TAG, "Failed to unblock user: " + response.code() + " - " + errorMessage);
                    Toast.makeText(getContext(), "Failed to unblock: " + errorMessage, Toast.LENGTH_LONG).show();

                    // Rollback UI jika API gagal
                    if (finalPosition != -1) {
                        blockedUsersList.add(finalPosition, userToUnblock);
                        blockedUserAdapter.notifyItemInserted(finalPosition);
                        tvBlockedUsersCount.setText("Pengguna Diblokir (" + blockedUsersList.size() + ")");
                        sharedPreferences.edit().putString("cached_blocked_users", gson.toJson(blockedUsersList)).apply();
                    }

                    if (response.code() == 401) {
                        redirectToLogin();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Error unblocking user: " + t.getMessage(), t);
                Toast.makeText(getContext(), "Network error unblocking: " + t.getMessage(), Toast.LENGTH_LONG).show();

                // Rollback UI jika API gagal
                if (finalPosition != -1) {
                    blockedUsersList.add(finalPosition, userToUnblock);
                    blockedUserAdapter.notifyItemInserted(finalPosition);
                    tvBlockedUsersCount.setText("Pengguna Diblokir (" + blockedUsersList.size() + ")");
                    sharedPreferences.edit().putString("cached_blocked_users", gson.toJson(blockedUsersList)).apply();
                }
            }
        });
    }
}