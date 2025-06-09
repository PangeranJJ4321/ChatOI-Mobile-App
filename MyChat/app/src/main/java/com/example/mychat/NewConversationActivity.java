package com.example.mychat;

import static java.security.AccessController.getContext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mychat.conversation_service.ConversationApiService;
import com.example.mychat.conversation_service.UserApiService;
import com.example.mychat.models_conversation.ConversationCreate;
import com.example.mychat.models_conversation.ConversationResponse;
import com.example.mychat.retrofit_client.RetrofitClient;
import com.example.mychat.models_user.UserResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask; // Import untuk Timer dan TimerTask

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NewConversationActivity extends AppCompatActivity {

    private CheckBox cbIsGroup;
    private EditText etConversationName;

    private EditText svUserSearch;
    private RecyclerView rvUserList;
    private Button btnCreateConversation;
    private ImageButton btnBack;

    private List<UserResponse> allUsers = new ArrayList<>(); // Semua user yang pernah di-fetch
    private List<UserResponse> displayedUsers = new ArrayList<>(); // User yang ditampilkan di RecyclerView (setelah filter)
    private List<UserResponse> selectedUsersForCreation = new ArrayList<>(); // Untuk melacak pilihan

    private SelectableUserAdapter userAdapter; // Menggunakan SelectableUserAdapter
    private Timer searchDebounceTimer; // Timer untuk debounce pencarian

    private String accessToken;
    private String currentUserId;
    private SharedPreferences sharedPreferences;
    private Gson gson;

    private static final String TAG = "NewConversationActivity";
    private static final String CACHED_ALL_USERS_KEY = "cached_all_users";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_conversation);

        sharedPreferences = getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        gson = new Gson();

        // Initialize views
        cbIsGroup = findViewById(R.id.cb_is_group);
        etConversationName = findViewById(R.id.et_conversation_name);
        svUserSearch = findViewById(R.id.sv_user_search);
        rvUserList = findViewById(R.id.rv_user_list);
        btnCreateConversation = findViewById(R.id.btn_create_conversation);
        btnBack = findViewById(R.id.btnBack); // Pastikan ID ini benar di layout Anda

        accessToken = getAccessToken(this);
        currentUserId = getCurrentUserId(this);

        if (accessToken == null || currentUserId == null) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Setup RecyclerView
        userAdapter = new SelectableUserAdapter(displayedUsers, (user, isChecked) -> { // Menggunakan displayedUsers
            // Perbarui selectedUsersForCreation berdasarkan aksi user
            if (isChecked) {
                if (!selectedUsersForCreation.contains(user)) { // Hindari duplikasi
                    selectedUsersForCreation.add(user);
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    selectedUsersForCreation.removeIf(u -> u.getId().equals(user.getId()));
                }
            }
            // Jika dalam mode chat pribadi dan ada yang baru dipilih, pastikan hanya satu yang terpilih
            if (!cbIsGroup.isChecked() && isChecked) {
                // Hapus semua kecuali yang baru saja dipilih
                List<UserResponse> toRemove = new ArrayList<>();
                for (UserResponse selectedUser : selectedUsersForCreation) {
                    if (!selectedUser.getId().equals(user.getId())) {
                        toRemove.add(selectedUser);
                    }
                }
                selectedUsersForCreation.removeAll(toRemove);

                // Update UI untuk user yang di-uncheck secara otomatis
                for (UserResponse u : displayedUsers) {
                    if (!u.getId().equals(user.getId()) && u.isSelected()) {
                        u.setSelected(false);
                    }
                }
                userAdapter.notifyDataSetChanged(); // Refresh adapter untuk mencerminkan perubahan seleksi
            }

            updateCreateButtonState();
        });
        rvUserList.setLayoutManager(new LinearLayoutManager(this));
        rvUserList.setAdapter(userAdapter);

        // Load users from local cache first
        loadUsersFromCache();
        // Kemudian fetch dari API
        fetchUsers(""); // Memuat semua user saat ini

        // Checkbox listener for group chat
        cbIsGroup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etConversationName.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                etConversationName.setText(""); // Clear group name if switching to private
                userAdapter.setGroupChatMode(false); // Set adapter to single-select mode
                clearAllSelectionsExceptFirstIfAny(); // Clear all but one if multiple were selected
            } else {
                userAdapter.setGroupChatMode(true); // Set adapter to multi-select mode
            }
            updateCreateButtonState();
        });

        svUserSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Debounce search input
                if (searchDebounceTimer != null) {
                    searchDebounceTimer.cancel();
                }
                searchDebounceTimer = new Timer();
                searchDebounceTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> {
                            // Selalu filter dari 'allUsers' (data lengkap) berdasarkan query yang sedang diketik
                            filterAndDisplayUsers(s.toString());
                            // Kemudian, panggil API untuk hasil terbaru (jika ada perubahan server)
                            // Anda bisa membuat logika untuk tidak memanggil API jika query sangat pendek dan data sudah ada di cache
                            fetchUsers(s.toString());
                        });
                    }
                }, 500); // 500ms delay
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Create Conversation button (for selected users from list)
        btnCreateConversation.setOnClickListener(v -> createConversation());

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Initial button state update
        updateCreateButtonState();
        userAdapter.setGroupChatMode(cbIsGroup.isChecked());

        addTestData();
    }

    private void loadUsersFromCache() {
        String cachedUsersJson = sharedPreferences.getString(CACHED_ALL_USERS_KEY, null);
        if (cachedUsersJson != null) {
            Type type = new TypeToken<List<UserResponse>>() {}.getType();
            List<UserResponse> cachedUsers = gson.fromJson(cachedUsersJson, type);
            if (cachedUsers != null && !cachedUsers.isEmpty()) {
                allUsers.clear();
                // Filter out current user from cached list as well
                for (UserResponse user : cachedUsers) {
                    if (!user.getId().equals(currentUserId)) {
                        allUsers.add(user);
                    }
                }
                // Tampilkan data cache ini secara langsung
                filterAndDisplayUsers(svUserSearch.getText().toString());
                Log.d(TAG, "Users loaded from cache: " + allUsers.size());
            }
        }
    }

    // Metode untuk memfilter allUsers dan menampilkan ke RecyclerView
    private void filterAndDisplayUsers(String query) {
        Log.d(TAG, "filterAndDisplayUsers called with query: '" + query + "'");
        Log.d(TAG, "allUsers size before filter: " + allUsers.size());

        displayedUsers.clear();
        String lowerCaseQuery = query.toLowerCase().trim();

        for (UserResponse user : allUsers) {
            user.setSelected(selectedUsersForCreation.contains(user));

            if (lowerCaseQuery.isEmpty() ||
                    user.getUsername().toLowerCase().contains(lowerCaseQuery) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerCaseQuery))) {
                displayedUsers.add(user);
            }
        }

        Log.d(TAG, "displayedUsers size after filter: " + displayedUsers.size());

        // Ganti updateList dengan notifyDataSetChanged langsung
        // karena adapter sudah menggunakan reference ke displayedUsers
        userAdapter.notifyDataSetChanged();

        updateCreateButtonState();
    }
    private void clearAllSelections() {
        for (UserResponse user : allUsers) {
            user.setSelected(false);
        }
        selectedUsersForCreation.clear();
        userAdapter.notifyDataSetChanged(); // Refresh adapter untuk memastikan semua checkbox tidak terpilih
    }

    private void clearAllSelectionsExceptFirstIfAny() {
        if (!selectedUsersForCreation.isEmpty() && !cbIsGroup.isChecked()) { // Hanya berlaku untuk mode pribadi
            UserResponse firstSelected = selectedUsersForCreation.get(0);
            selectedUsersForCreation.clear();
            selectedUsersForCreation.add(firstSelected); // Pertahankan yang pertama saja

            // Update status isSelected pada objek UserResponse di allUsers dan displayedUsers
            for (UserResponse user : allUsers) {
                user.setSelected(user.equals(firstSelected));
            }
            // Setelah update allUsers, panggil filterAndDisplayUsers untuk refresh displayedUsers dan adapter
            filterAndDisplayUsers(svUserSearch.getText().toString());
        } else if (!cbIsGroup.isChecked()) {
            // Jika tidak ada yang terpilih dan mode pribadi, pastikan semuanya clear
            clearAllSelections();
        }
        // Jika mode grup, tidak perlu melakukan apa-apa pada seleksi otomatis
    }


    private void fetchUsers(String query) {
        UserApiService userApiService = RetrofitClient.getUserApiService();
        Call<List<UserResponse>> call;

        String trimmedQuery = query != null ? query.trim() : "";

        if (!trimmedQuery.isEmpty()) {
            call = userApiService.searchUsers(
                    "Bearer " + accessToken,
                    trimmedQuery
            );
        } else {
            call = userApiService.getAllUsers(
                    "Bearer " + accessToken
            );
        }

        call.enqueue(new Callback<List<UserResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserResponse>> call, @NonNull Response<List<UserResponse>> response) {
                // Cek apakah Activity masih aktif sebelum update UI
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    List<UserResponse> fetchedUsers = response.body();

                    // DEBUG: Log total users dari API
                    Log.d(TAG, "Total users from API: " + fetchedUsers.size());
                    Log.d(TAG, "Current user ID: " + currentUserId);

                    // Perbarui 'allUsers' dengan data terbaru dari API
                    allUsers.clear();
                    int filteredCount = 0;
                    for (UserResponse user : fetchedUsers) {
                        Log.d(TAG, "Processing user: " + user.getId() + " - " + user.getUsername());
                        if (!user.getId().equals(currentUserId)) {
                            allUsers.add(user);
                            filteredCount++;
                        } else {
                            Log.d(TAG, "Skipping current user: " + user.getId());
                        }
                    }

                    Log.d(TAG, "Users after filtering current user: " + filteredCount);
                    Log.d(TAG, "allUsers size: " + allUsers.size());

                    // Pertahankan status seleksi untuk pengguna yang masih ada di hasil pencarian
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        selectedUsersForCreation.removeIf(selectedUser -> !allUsers.contains(selectedUser));
                    }

                    // Kemudian filter dan tampilkan ulang berdasarkan query yang sedang aktif
                    filterAndDisplayUsers(svUserSearch.getText().toString());

                    // Simpan ke cache lokal
                    if (sharedPreferences != null) {
                        sharedPreferences.edit().putString(CACHED_ALL_USERS_KEY, gson.toJson(fetchedUsers)).apply();
                    }

                    updateCreateButtonState();
                } else {
                    String errorMessage = "Failed to load users: Unknown error";
                    if (response.errorBody() != null) {
                        try {
                            String error = response.errorBody().string();
                            Log.e(TAG, "Failed to load users: " + response.code() + " - " + error);
                            errorMessage = "Failed to load users: " + error;
                        } catch (IOException e) {
                            Log.e(TAG, "Error parsing error body: " + e.getMessage());
                        }
                    }
                    showToastSafely("Error fetching users: " + errorMessage + ". Displaying cached data.");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserResponse>> call, @NonNull Throwable t) {
                // Cek apakah Activity masih aktif sebelum show toast
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                Log.e(TAG, "Error fetching users: " + t.getMessage(), t);

                // Cek jenis error untuk offline handling
                if (t instanceof IOException) {
                    // Network error - berarti offline, gunakan cache
                    showToastSafely("Network offline - using cached data");
                } else {
                    showToastSafely("Network error fetching users: " + t.getMessage() + ". Displaying cached data.");
                }
            }
        });
    }

    private List<String> getSelectedParticipantIds() {
        List<String> participantIds = new ArrayList<>();
        // Add current user first
        participantIds.add(currentUserId);

        // Add selected users from the list
        for (UserResponse user : selectedUsersForCreation) {
            if (!user.getId().equals(currentUserId)) {
                participantIds.add(user.getId());
            }
        }
        return participantIds;
    }

    private void createConversation() {
        // Cek network dulu
        if (!isNetworkAvailable()) {
            showToastSafely("Cannot create conversation while offline. Please check your internet connection.", Toast.LENGTH_LONG);
            return;
        }

        List<String> participantIds = getSelectedParticipantIds();

        if (!cbIsGroup.isChecked() && participantIds.size() == 1 && participantIds.contains(currentUserId)) {
            showToastSafely("Please select another participant for a private chat.");
            return;
        }

        if (participantIds.size() < 2) {
            showToastSafely("Please select at least one other participant.");
            return;
        }

        String conversationName = etConversationName.getText().toString().trim();
        boolean isGroup = cbIsGroup.isChecked();

        if (isGroup && conversationName.isEmpty()) {
            showToastSafely("Please enter a group name.");
            return;
        }

        if (!isGroup && participantIds.size() > 2) {
            showToastSafely("For private chat, please select only one participant.");
            return;
        }

        showToastSafely("Creating conversation...");
        performCreateConversation(conversationName, isGroup, participantIds);
    }

    private void performCreateConversation(String name, boolean isGroup, List<String> participantIds) {
        ConversationCreate conversationCreate = new ConversationCreate(
                name,
                null, // description
                isGroup,
                null, // avatar
                participantIds
        );

        ConversationApiService apiService = RetrofitClient.getConversationApiService();
        Call<ConversationResponse> call = apiService.createConversation(
                "Bearer " + accessToken,
                conversationCreate
        );

        call.enqueue(new Callback<ConversationResponse>() {
            @Override
            public void onResponse(@NonNull Call<ConversationResponse> call, @NonNull Response<ConversationResponse> response) {
                // Cek apakah Activity masih aktif
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    ConversationResponse newConversation = response.body();
                    showToastSafely("Conversation created: " + newConversation.getName());
                    setResult(RESULT_OK);
                    finish();
                } else {
                    String errorMessage = "Failed to create conversation: Unknown error";
                    if (response.errorBody() != null) {
                        try {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error creating conversation: " + response.code() + " - " + errorBody);
                            errorMessage = "Failed to create conversation: " + errorBody;
                        } catch (IOException e) {
                            Log.e(TAG, "Error parsing error body: " + e.getMessage());
                        }
                    }
                    showToastSafely(errorMessage, Toast.LENGTH_LONG);

                    if (response.code() == 401) {
                        showToastSafely("Authentication error. Please log in again.", Toast.LENGTH_LONG);
                        Intent intent = new Intent(NewConversationActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ConversationResponse> call, @NonNull Throwable t) {
                // Cek apakah Activity masih aktif
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                Log.e(TAG, "Error creating conversation: " + t.getMessage(), t);

                if (t instanceof IOException) {
                    // Network error - offline
                    showToastSafely("Cannot create conversation while offline", Toast.LENGTH_LONG);
                } else {
                    showToastSafely("Network error: " + t.getMessage(), Toast.LENGTH_LONG);
                }
            }
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }

    private void showToastSafely(String message) {
        if (!isFinishing() && !isDestroyed()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showToastSafely(String message, int duration) {
        if (!isFinishing() && !isDestroyed()) {
            Toast.makeText(this, message, duration).show();
        }
    }

    private void updateCreateButtonState() {
        List<String> selectedParticipants = getSelectedParticipantIds();
        boolean isGroup = cbIsGroup.isChecked();
        boolean canCreate = false;

        if (selectedParticipants.size() >= 2) {
            if (isGroup) {
                canCreate = true;
            } else {
                canCreate = selectedParticipants.size() == 2;
            }
        }
        btnCreateConversation.setEnabled(canCreate);
        if (canCreate) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                btnCreateConversation.setBackgroundTintList(getResources().getColorStateList(R.color.primary_light, getTheme()));
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                btnCreateConversation.setBackgroundTintList(getResources().getColorStateList(R.color.secondary_text, getTheme()));
            }
        }
    }

    private String getAccessToken(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("access_token", null);
    }

    private String getCurrentUserId(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("user_id", null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchDebounceTimer != null) {
            searchDebounceTimer.cancel();
            searchDebounceTimer = null;
        }
    }

    private void addTestData() {
        UserResponse testUser = new UserResponse();
        testUser.setId("test-123");
        testUser.setUsername("Test User");
        testUser.setEmail("test@example.com");
        testUser.setOnline(true);

        displayedUsers.add(testUser);
        userAdapter.notifyDataSetChanged();

        Log.d(TAG, "Test data added. DisplayedUsers size: " + displayedUsers.size());
    }
}