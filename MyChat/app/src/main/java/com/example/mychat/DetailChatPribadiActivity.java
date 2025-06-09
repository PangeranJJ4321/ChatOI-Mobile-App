package com.example.mychat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mychat.conversation_service.ConversationApiService;
import com.example.mychat.conversation_service.UserApiService;
import com.example.mychat.models_conversation.ConversationResponse;
import com.example.mychat.models_conversation.ParticipantMuteUpdate;
import com.example.mychat.models_conversation.ParticipantResponse;
import com.example.mychat.models_user.BlockUserRequest;
import com.example.mychat.models_user.UserResponse;
import com.example.mychat.retrofit_client.RetrofitClient;
import com.google.gson.Gson; // Import Gson
import com.google.gson.reflect.TypeToken; // Import TypeToken

import java.io.IOException;
import java.lang.reflect.Type; // Import Type
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailChatPribadiActivity extends AppCompatActivity {

    private static final String TAG = "DetailChatPribadi";

    // UI elements
    private ImageView ivContactAvatar;
    private TextView tvContactName;
    private TextView tvContactIdentifier;
    private TextView tvLastStatus;
    private LinearLayout btnMessage;
    private LinearLayout btnCall;
    private LinearLayout btnVideoCall;
    private LinearLayout layoutMuteNotifications;
    private Switch switchNotifications;
    private TextView tvNotificationStatus;
    private LinearLayout layoutMediaVisibility;
    private Switch switchMediaVisibility;
    private LinearLayout layoutWallpaper;
    private LinearLayout layoutDisappearingMessages;
    private TextView tvDisappearingStatus;
    private LinearLayout btnBlockContact;
    private TextView tvBlockContactText;
    private ImageView ivBlockContactIcon;
    private LinearLayout btnClearChat;
    private ImageButton btnBack;
    private ImageButton btnMoreOptions;

    // Data for the other user
    private String userId;
    private String username;
    private String avatarUrl;
    private String conversationId;

    private String currentUserId;
    private String accessToken;

    private boolean isContactBlocked = false;
    private boolean isNotificationsMuted = false;

    // --- Caching Variables ---
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private static final String CACHE_KEY_USER_PROFILE_PREFIX = "cached_user_profile_"; // Cache key untuk detail user kontak
    private static final String CACHE_KEY_BLOCKED_USERS = "cached_blocked_users_"; // Cache key untuk daftar user yang diblokir (dari current user)
    private static final String CACHE_KEY_CONVERSATION_MUTE_PREFIX = "cached_conv_mute_"; // Cache key untuk status mute percakapan
    // --- End Caching Variables ---


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_chat_pribadi);

        // Get data from Intent
        Intent intent = getIntent();
        conversationId = intent.getStringExtra("conversation_id");
        userId = intent.getStringExtra("user_id");
        username = intent.getStringExtra("username");
        avatarUrl = intent.getStringExtra("avatar_url");

        if (userId == null || conversationId == null) {
            Toast.makeText(this, "Informasi pengguna atau percakapan tidak lengkap.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // --- Caching: Inisialisasi SharedPreferences dan Gson ---
        sharedPreferences = getSharedPreferences("DetailChatCache", Context.MODE_PRIVATE);
        gson = new Gson();
        // --- End Caching Initialization ---

        // Initialize UI components
        ivContactAvatar = findViewById(R.id.iv_contact_avatar);
        tvContactName = findViewById(R.id.tv_contact_name);
        tvContactIdentifier = findViewById(R.id.tv_contact_identifier);
        tvLastStatus = findViewById(R.id.tv_last_status);
        btnMessage = findViewById(R.id.btn_message);
        btnCall = findViewById(R.id.btn_call);
        btnVideoCall = findViewById(R.id.btn_video_call);
        layoutMuteNotifications = findViewById(R.id.layout_mute_notifications);
        switchNotifications = findViewById(R.id.switch_notifications);
        tvNotificationStatus = findViewById(R.id.tv_notification_status);
        layoutMediaVisibility = findViewById(R.id.layout_media_visibility);
        switchMediaVisibility = findViewById(R.id.switch_media_visibility);
        layoutWallpaper = findViewById(R.id.layout_wallpaper);
        layoutDisappearingMessages = findViewById(R.id.layout_disappearing_messages);
        tvDisappearingStatus = findViewById(R.id.tv_disappearing_status);
        btnBlockContact = findViewById(R.id.btn_block_contact);
        tvBlockContactText = findViewById(R.id.tv_block_contact_text);
        ivBlockContactIcon = findViewById(R.id.iv_block_contact_icon);
        btnClearChat = findViewById(R.id.btn_clear_chat);
        btnBack = findViewById(R.id.btn_back_settings);
        btnMoreOptions = findViewById(R.id.btn_more_options);


        // Get current user auth info
        accessToken = getAccessToken(this);
        currentUserId = getCurrentUserId(this);

        if (accessToken == null || currentUserId == null) {
            Toast.makeText(this, "Autentikasi gagal. Silakan masuk lagi.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        // Setup UI with initial data (from Intent or placeholders)
        setupInitialUI();

        // --- Caching: Load data from local cache first ---
        loadDataLocally();
        // --- End Caching Load Local ---

        // Setup listeners
        setupListeners();

        // Fetch dynamic data from API
        fetchUserDetails();
        fetchContactBlockStatus();
        fetchConversationSettings();
    }

    private void setupInitialUI() {
        if (username != null && !username.isEmpty()) {
            tvContactName.setText(username);
            tvContactIdentifier.setText(username.toLowerCase(Locale.ROOT)); // Default to username lowercase
        } else {
            tvContactName.setText("Unknown User");
            tvContactIdentifier.setText("ID: " + userId.substring(0, 8) + "...");
        }

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(ivContactAvatar);
        } else {
            ivContactAvatar.setImageResource(R.drawable.default_avatar);
        }

        tvLastStatus.setText("Memuat status...");
        tvNotificationStatus.setText("Memuat...");
        // Default values before fetching actual settings
        switchNotifications.setChecked(true); // Default: not muted
        isNotificationsMuted = false;
        switchMediaVisibility.setChecked(false);
        tvDisappearingStatus.setText("Tidak aktif");
        updateBlockButtonUI(); // Initial state, will be updated by fetchContactBlockStatus()
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
        if (btnMoreOptions != null) {
            btnMoreOptions.setOnClickListener(v -> Toast.makeText(this, "Opsi lainnya diklik", Toast.LENGTH_SHORT).show());
        }

        btnMessage.setOnClickListener(v -> {
            Intent chatIntent = new Intent(this, ChatActivity.class);
            chatIntent.putExtra("conversation_id", conversationId);
            chatIntent.putExtra("other_participant_id", userId);
            chatIntent.putExtra("other_participant_username", username);
            chatIntent.putExtra("other_participant_avatar", avatarUrl);
            chatIntent.putExtra("is_group_chat", false);
            chatIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(chatIntent);
            finish();
        });

        layoutMuteNotifications.setOnClickListener(v -> {
            switchNotifications.toggle();
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleNotifications(isChecked);
        });

        btnBlockContact.setOnClickListener(v -> {
            if (isContactBlocked) {
                unblockContact();
            } else {
                showBlockConfirmationDialog();
            }
        });

        btnClearChat.setOnClickListener(v -> {
            Toast.makeText(this, "Hapus Riwayat Chat - Fitur ditunda", Toast.LENGTH_SHORT).show();
        });

        btnCall.setOnClickListener(v -> Toast.makeText(this, "Fitur Panggilan (Call) belum diimplementasikan", Toast.LENGTH_SHORT).show());
        btnVideoCall.setOnClickListener(v -> Toast.makeText(this, "Fitur Video Call belum diimplementasikan", Toast.LENGTH_SHORT).show());
        layoutMediaVisibility.setOnClickListener(v -> Toast.makeText(this, "Simpan ke Galeri belum diimplementasikan", Toast.LENGTH_SHORT).show());
        switchMediaVisibility.setOnCheckedChangeListener((buttonView, isChecked) -> Toast.makeText(this, "Switch Simpan ke Galeri " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show());
        layoutWallpaper.setOnClickListener(v -> Toast.makeText(this, "Wallpaper Chat belum diimplementasikan", Toast.LENGTH_SHORT).show());
        layoutDisappearingMessages.setOnClickListener(v -> Toast.makeText(this, "Pesan Otomatis Terhapus belum diimplementasikan", Toast.LENGTH_SHORT).show());
    }

    // --- Caching: New method to load data from SharedPreferences ---
    private void loadDataLocally() {
        // Load User Details
        String userProfileJson = sharedPreferences.getString(CACHE_KEY_USER_PROFILE_PREFIX + userId, null);
        if (userProfileJson != null) {
            try {
                UserResponse user = gson.fromJson(userProfileJson, UserResponse.class);
                if (user != null) {
                    updateUserDetailsUI(user);
                    Log.d(TAG, "User details loaded from local cache: " + user.getUsername());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing cached user profile: " + e.getMessage());
                sharedPreferences.edit().remove(CACHE_KEY_USER_PROFILE_PREFIX + userId).apply();
            }
        }

        // Load Block Status
        String blockedUsersJson = sharedPreferences.getString(CACHE_KEY_BLOCKED_USERS + currentUserId, null);
        if (blockedUsersJson != null) {
            try {
                Type type = new TypeToken<List<UserResponse>>(){}.getType();
                List<UserResponse> blockedUsers = gson.fromJson(blockedUsersJson, type);
                if (blockedUsers != null) {
                    isContactBlocked = false;
                    for (UserResponse blockedUser : blockedUsers) {
                        if (blockedUser.getId().equals(userId)) {
                            isContactBlocked = true;
                            break;
                        }
                    }
                    updateBlockButtonUI();
                    Log.d(TAG, "Block status loaded from local cache. Is blocked: " + isContactBlocked);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing cached blocked users: " + e.getMessage());
                sharedPreferences.edit().remove(CACHE_KEY_BLOCKED_USERS + currentUserId).apply();
            }
        }

        // Load Mute Status
        String muteStatusJson = sharedPreferences.getString(CACHE_KEY_CONVERSATION_MUTE_PREFIX + conversationId + "_" + currentUserId, null);
        if (muteStatusJson != null) {
            try {
                Boolean muted = gson.fromJson(muteStatusJson, Boolean.class);
                if (muted != null) {
                    isNotificationsMuted = muted;
                    switchNotifications.setChecked(!isNotificationsMuted);
                    tvNotificationStatus.setText(isNotificationsMuted ? "Dinonaktifkan" : "Diaktifkan");
                    Log.d(TAG, "Mute status loaded from local cache. Is muted: " + isNotificationsMuted);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing cached mute status: " + e.getMessage());
                sharedPreferences.edit().remove(CACHE_KEY_CONVERSATION_MUTE_PREFIX + conversationId + "_" + currentUserId).apply();
            }
        }
    }
    // --- End Caching: loadDataLocally ---

    // --- Caching: Helper method to update User Details UI from a UserResponse object ---
    private void updateUserDetailsUI(UserResponse user) {
        tvContactName.setText(user.getUsername());
        tvContactIdentifier.setText(user.getEmail() != null && !user.getEmail().isEmpty() ? user.getEmail() : "@" + user.getUsername());

        if (user.isOnline()) {
            tvLastStatus.setText("online");
            tvLastStatus.setTextColor(ContextCompat.getColor(DetailChatPribadiActivity.this, R.color.green_500));
        } else {
            if (user.getLastActive() != null) {
                String timeAgo = formatTimeAgo(user.getLastActive());
                tvLastStatus.setText("terlihat " + timeAgo);
            } else {
                tvLastStatus.setText("offline");
            }
            tvLastStatus.setTextColor(ContextCompat.getColor(DetailChatPribadiActivity.this, R.color.secondary_text));
        }
        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            Glide.with(DetailChatPribadiActivity.this)
                    .load(user.getProfilePicture())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(ivContactAvatar);
        } else {
            ivContactAvatar.setImageResource(R.drawable.default_avatar);
        }
    }
    // --- End Caching: updateUserDetailsUI ---


    private void fetchUserDetails() {
        UserApiService apiService = RetrofitClient.getUserApiService();
        Call<UserResponse> call = apiService.getUser("Bearer " + accessToken, userId);

        call.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserResponse> call, @NonNull Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UserResponse user = response.body();
                    updateUserDetailsUI(user); // Update UI
                    // --- Caching: Save user details to local cache ---
                    sharedPreferences.edit().putString(CACHE_KEY_USER_PROFILE_PREFIX + userId, gson.toJson(user)).apply();
                    Log.d(TAG, "User details fetched from API and cached.");
                    // --- End Caching Save ---
                } else {
                    Log.e(TAG, "Failed to fetch user details: " + response.code());
                    Toast.makeText(DetailChatPribadiActivity.this, "Gagal memuat detail pengguna.", Toast.LENGTH_SHORT).show();
                    if (response.code() == 401) {
                        redirectToLogin();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching user details: " + t.getMessage());
                Toast.makeText(DetailChatPribadiActivity.this, "Kesalahan jaringan: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                // Data akan tetap menampilkan dari cache jika ada
            }
        });
    }

    private void fetchContactBlockStatus() {
        UserApiService apiService = RetrofitClient.getUserApiService();
        Call<List<UserResponse>> call = apiService.getBlockedUsers("Bearer " + accessToken, currentUserId);

        call.enqueue(new Callback<List<UserResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserResponse>> call, @NonNull Response<List<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<UserResponse> blockedUsers = response.body();
                    isContactBlocked = false;
                    for (UserResponse blockedUser : blockedUsers) {
                        if (blockedUser.getId().equals(userId)) {
                            isContactBlocked = true;
                            break;
                        }
                    }
                    updateBlockButtonUI();
                    // --- Caching: Save blocked users list to local cache ---
                    sharedPreferences.edit().putString(CACHE_KEY_BLOCKED_USERS + currentUserId, gson.toJson(blockedUsers)).apply();
                    Log.d(TAG, "Blocked users status fetched from API and cached. Is blocked: " + isContactBlocked);
                    // --- End Caching Save ---
                } else {
                    Log.e(TAG, "Failed to fetch blocked users: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching blocked users: " + t.getMessage());
            }
        });
    }

    private void updateBlockButtonUI() {
        if (isContactBlocked) {
            tvBlockContactText.setText("Buka Blokir Kontak");
            tvBlockContactText.setTextColor(ContextCompat.getColor(this, R.color.blue_500));
            ivBlockContactIcon.setImageResource(R.drawable.ic_unblock);
        } else {
            tvBlockContactText.setText("Blokir Kontak");
            tvBlockContactText.setTextColor(ContextCompat.getColor(this, R.color.red_500));
            ivBlockContactIcon.setImageResource(R.drawable.ic_blockir);
        }
    }

    private void showBlockConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Blokir Kontak")
                .setMessage("Apakah Anda yakin ingin memblokir " + username + "? Anda tidak akan menerima pesan dari mereka lagi.")
                .setPositiveButton("Blokir", (dialog, which) -> blockContactAction())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void blockContactAction() {
        UserApiService apiService = RetrofitClient.getUserApiService();
        BlockUserRequest blockRequest = new BlockUserRequest(userId);
        Call<Void> call = apiService.blockUser("Bearer " + accessToken, currentUserId, blockRequest);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DetailChatPribadiActivity.this, username + " berhasil diblokir.", Toast.LENGTH_SHORT).show();
                    isContactBlocked = true;
                    updateBlockButtonUI();
                    // --- Caching: Update cache for blocked users ---
                    fetchContactBlockStatus(); // Re-fetch to update cache accurately
                    // --- End Caching Update ---
                } else {
                    String error = "Gagal memblokir kontak.";
                    try {
                        if (response.errorBody() != null) {
                            error += " " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing block error body", e);
                    }
                    Toast.makeText(DetailChatPribadiActivity.this, error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to block user: " + response.code() + " - " + error);
                    if (response.code() == 401) redirectToLogin();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(DetailChatPribadiActivity.this, "Kesalahan jaringan saat memblokir: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error blocking user", t);
            }
        });
    }

    private void unblockContact() {
        UserApiService apiService = RetrofitClient.getUserApiService();
        BlockUserRequest unblockRequest = new BlockUserRequest(userId);
        Call<Void> call = apiService.unblockUser("Bearer " + accessToken, currentUserId, unblockRequest);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DetailChatPribadiActivity.this, username + " berhasil dibuka blokirnya.", Toast.LENGTH_SHORT).show();
                    isContactBlocked = false;
                    updateBlockButtonUI();
                    // --- Caching: Update cache for blocked users ---
                    fetchContactBlockStatus(); // Re-fetch to update cache accurately
                    // --- End Caching Update ---
                } else {
                    String error = "Gagal membuka blokir kontak.";
                    try {
                        if (response.errorBody() != null) {
                            error += " " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing unblock error body", e);
                    }
                    Toast.makeText(DetailChatPribadiActivity.this, error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to unblock user: " + response.code() + " - " + error);
                    if (response.code() == 401) redirectToLogin();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(DetailChatPribadiActivity.this, "Kesalahan jaringan saat membuka blokir: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error unblocking user", t);
            }
        });
    }

    private void fetchConversationSettings() {
        ConversationApiService conversationApiService = RetrofitClient.getConversationApiService();
        Call<ConversationResponse> convCall = conversationApiService.getConversation("Bearer " + accessToken, conversationId);

        convCall.enqueue(new Callback<ConversationResponse>() {
            @Override
            public void onResponse(@NonNull Call<ConversationResponse> call, @NonNull Response<ConversationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ParticipantResponse> participants = response.body().getParticipants();
                    if (participants != null) {
                        for (ParticipantResponse participant : participants) {
                            if (participant.getUserId().equals(currentUserId)) {
                                isNotificationsMuted = participant.isMuted();
                                switchNotifications.setChecked(!isNotificationsMuted);
                                tvNotificationStatus.setText(isNotificationsMuted ? "Dinonaktifkan" : "Diaktifkan");
                                // --- Caching: Save mute status to local cache ---
                                sharedPreferences.edit().putString(CACHE_KEY_CONVERSATION_MUTE_PREFIX + conversationId + "_" + currentUserId, gson.toJson(isNotificationsMuted)).apply();
                                Log.d(TAG, "Mute status fetched from API and cached: " + isNotificationsMuted);
                                // --- End Caching Save ---
                                break;
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to fetch conversation for mute status: " + response.code());
                    Toast.makeText(DetailChatPribadiActivity.this, "Gagal memuat pengaturan percakapan.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ConversationResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching conversation for mute status: " + t.getMessage());
                Toast.makeText(DetailChatPribadiActivity.this, "Kesalahan jaringan saat memuat pengaturan percakapan.", Toast.LENGTH_SHORT).show();
                // Data akan tetap menampilkan dari cache jika ada
            }
        });
    }

    private void toggleNotifications(boolean enable) {
        Toast.makeText(this, "Mengupdate notifikasi...", Toast.LENGTH_SHORT).show();

        boolean willBeMuted = !enable;

        ConversationApiService conversationApiService = RetrofitClient.getConversationApiService();
        ParticipantMuteUpdate muteUpdate = new ParticipantMuteUpdate(willBeMuted);

        Call<Void> call = conversationApiService.updateParticipantMuteStatus(
                "Bearer " + accessToken,
                conversationId,
                currentUserId,
                muteUpdate
        );

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    isNotificationsMuted = willBeMuted;
                    tvNotificationStatus.setText(isNotificationsMuted ? "Dinonaktifkan" : "Diaktifkan");
                    Toast.makeText(DetailChatPribadiActivity.this, "Notifikasi " + (enable ? "diaktifkan" : "dinonaktifkan"), Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Notifikasi berhasil diperbarui menjadi: " + (enable ? "aktif" : "nonaktif"));
                    // --- Caching: Update mute status in cache ---
                    sharedPreferences.edit().putString(CACHE_KEY_CONVERSATION_MUTE_PREFIX + conversationId + "_" + currentUserId, gson.toJson(isNotificationsMuted)).apply();
                    // --- End Caching Update ---
                } else {
                    switchNotifications.setChecked(!enable); // Kembalikan switch jika gagal
                    String error = "Gagal memperbarui notifikasi.";
                    try {
                        if (response.errorBody() != null) {
                            error += " " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing error body for toggle notifications", e);
                    }
                    Toast.makeText(DetailChatPribadiActivity.this, error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to update notifications: " + response.code() + " - " + error);
                    if (response.code() == 401) redirectToLogin();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                switchNotifications.setChecked(!enable); // Kembalikan switch jika gagal
                Toast.makeText(DetailChatPribadiActivity.this, "Kesalahan jaringan saat memperbarui notifikasi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error updating notifications", t);
            }
        });
    }

    private String formatTimeAgo(Date date) {
        if (date == null) return "";
        long now = System.currentTimeMillis();
        long time = date.getTime();
        long diff = now - time;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "baru saja";
        } else if (minutes < 60) {
            return minutes + " menit yang lalu";
        } else if (hours < 24) {
            return hours + " jam yang lalu";
        } else if (days < 7) {
            return days + " hari yang lalu";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
            return sdf.format(date);
        }
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        // --- Caching: Clear DetailChatCache on logout ---
        SharedPreferences chatCachePrefs = getSharedPreferences("DetailChatCache", Context.MODE_PRIVATE);
        chatCachePrefs.edit().clear().apply();
        // --- End Caching Clear ---
    }

    private String getAccessToken(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("access_token", null);
    }

    private String getCurrentUserId(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("user_id", null);
    }
}