package com.example.mychat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mychat.conversation_service.UserApiService;
import com.example.mychat.models_user.UserResponse;
import com.example.mychat.retrofit_client.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson; // Import Gson
import com.google.gson.reflect.TypeToken; // Import TypeToken

import java.io.IOException;
import java.lang.reflect.Type; // Import Type
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddMembersDialogFragment extends DialogFragment implements UserSelectionAdapter.OnUserSelectedListener {

    private static final String TAG = "AddMembersDialog";
    private static final String ARG_CONVERSATION_ID = "conversation_id";
    private static final String ARG_ACCESS_TOKEN = "access_token";
    private static final String ARG_CURRENT_USER_ID = "current_user_id";

    private String conversationId;
    private String accessToken;
    private String currentUserId;

    private EditText etSearchUsers;
    private ProgressBar progressBarLoadingUsers;
    private TextView tvEmptyStateUsers;
    private RecyclerView rvAvailableUsers;
    private MaterialButton btnCancel;
    private MaterialButton btnAddMembers;

    private UserSelectionAdapter userSelectionAdapter;
    private final List<UserResponse> availableUserList = new ArrayList<>(); // Semua user yang tersedia dari API (atau cache)
    private final List<UserResponse> filteredUserList = new ArrayList<>(); // User yang ditampilkan setelah filter/search
    private final List<UserResponse> selectedUsers = new ArrayList<>(); // User yang dipilih saat ini

    private Timer searchTimer; // Untuk debounce pencarian

    // Callback ke Activity host
    public interface AddMembersListener {
        void onMembersAdded(List<String> newMemberIds);
    }
    private AddMembersListener listener;

    // --- Caching Variables ---
    private SharedPreferences sharedPreferences;
    private Gson gson;
    // Cache key akan spesifik untuk percakapan ini
    private static final String CACHE_KEY_AVAILABLE_USERS_PREFIX = "cached_available_users_";
    // --- End Caching Variables ---


    public static AddMembersDialogFragment newInstance(String conversationId, String accessToken, String currentUserId) {
        AddMembersDialogFragment fragment = new AddMembersDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONVERSATION_ID, conversationId);
        args.putString(ARG_ACCESS_TOKEN, accessToken);
        args.putString(ARG_CURRENT_USER_ID, currentUserId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof AddMembersListener) {
            listener = (AddMembersListener) context;
        } else {
            Log.w(TAG, "Host Activity does not implement AddMembersListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            conversationId = getArguments().getString(ARG_CONVERSATION_ID);
            accessToken = getArguments().getString(ARG_ACCESS_TOKEN);
            currentUserId = getArguments().getString(ARG_CURRENT_USER_ID);
        }
        // Set style for dialog (optional, for full screen dialogs etc.)
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);

        // --- Caching: Inisialisasi SharedPreferences dan Gson ---
        sharedPreferences = requireContext().getSharedPreferences("AddMembersCache", Context.MODE_PRIVATE);
        gson = new Gson();
        // --- End Caching Initialization ---
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_members, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearchUsers = view.findViewById(R.id.et_search_users);
        progressBarLoadingUsers = view.findViewById(R.id.progress_bar_loading_users);
        tvEmptyStateUsers = view.findViewById(R.id.tv_empty_state_users);
        rvAvailableUsers = view.findViewById(R.id.rv_available_users);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnAddMembers = view.findViewById(R.id.btn_add_members);

        userSelectionAdapter = new UserSelectionAdapter(requireContext(), filteredUserList, this);
        rvAvailableUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAvailableUsers.setAdapter(userSelectionAdapter);

        // Initial state for Add button
        updateAddButtonState();

        setupListeners();

        // --- Caching: Load initial list from local cache first ---
        loadAvailableUsersLocally();
        // --- End Caching Load Local ---

        fetchAvailableUsers(""); // Load initial list from API (empty search query)
    }

    private void setupListeners() {
        btnCancel.setOnClickListener(v -> dismiss());

        btnAddMembers.setOnClickListener(v -> {
            if (!selectedUsers.isEmpty()) {
                List<String> userIdsToAdd = new ArrayList<>();
                for (UserResponse user : selectedUsers) {
                    userIdsToAdd.add(user.getId());
                }
                if (listener != null) {
                    listener.onMembersAdded(userIdsToAdd); // Beri tahu Activity
                }
                // --- Caching: Clear cache after adding members, as the list of available users might change significantly ---
                sharedPreferences.edit().remove(CACHE_KEY_AVAILABLE_USERS_PREFIX + conversationId).apply();
                // --- End Caching Clear ---
                dismiss(); // Tutup dialog
            } else {
                Toast.makeText(requireContext(), "Pilih setidaknya satu anggota.", Toast.LENGTH_SHORT).show();
            }
        });

        etSearchUsers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Debounce search input
                if (searchTimer != null) {
                    searchTimer.cancel();
                }
                searchTimer = new Timer();
                searchTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                // --- Caching: When searching, first try to filter the cached data if possible ---
                                // For simplicity, we directly call API, but for large local caches,
                                // you might want to filter availableUserList directly first.
                                fetchAvailableUsers(s.toString()); // Panggil API dengan query baru
                            });
                        }
                    }
                }, 500); // 500ms delay
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // --- Caching: New method to load available users from local cache ---
    private void loadAvailableUsersLocally() {
        String cacheKey = CACHE_KEY_AVAILABLE_USERS_PREFIX + conversationId;
        String usersJson = sharedPreferences.getString(cacheKey, null);
        if (usersJson != null) {
            try {
                Type type = new TypeToken<List<UserResponse>>() {}.getType();
                List<UserResponse> cachedUsers = gson.fromJson(usersJson, type);
                if (cachedUsers != null && !cachedUsers.isEmpty()) {
                    availableUserList.clear();
                    availableUserList.addAll(cachedUsers);
                    filterAndDisplayUsers(""); // Display all cached users initially (or with empty query)
                    Log.d(TAG, "Available users loaded from local cache: " + cachedUsers.size());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing cached available users: " + e.getMessage());
                sharedPreferences.edit().remove(cacheKey).apply(); // Clear corrupted cache
            }
        }
    }
    // --- End Caching: loadAvailableUsersLocally ---

    // Helper method to filter the local list (used for both cache and API results)
    private void filterAndDisplayUsers(String query) {
        filteredUserList.clear();
        String lowerCaseQuery = query.toLowerCase(Locale.getDefault());

        for (UserResponse user : availableUserList) {
            // Check if user is already selected (from previous sessions or within current selection)
            boolean isUserSelectedInCurrentList = false;
            for (UserResponse selected : selectedUsers) {
                if (selected.getId().equals(user.getId())) {
                    isUserSelectedInCurrentList = true;
                    break;
                }
            }
            user.setSelected(isUserSelectedInCurrentList); // Ensure selection status is correct

            // Filter by query and exclude current user
            if ((user.getUsername().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery))) &&
                    !user.getId().equals(currentUserId)) { // Exclude current user from list
                filteredUserList.add(user);
            }
        }

        if (filteredUserList.isEmpty()) {
            tvEmptyStateUsers.setVisibility(View.VISIBLE);
            rvAvailableUsers.setVisibility(View.GONE);
            tvEmptyStateUsers.setText("Tidak ada pengguna yang ditemukan.");
        } else {
            tvEmptyStateUsers.setVisibility(View.GONE);
            rvAvailableUsers.setVisibility(View.VISIBLE);
        }
        userSelectionAdapter.notifyDataSetChanged();
    }


    private void fetchAvailableUsers(String query) {
        progressBarLoadingUsers.setVisibility(View.VISIBLE);
        tvEmptyStateUsers.setVisibility(View.GONE);
        rvAvailableUsers.setVisibility(View.GONE);

        UserApiService apiService = RetrofitClient.getUserApiService();
        Call<List<UserResponse>> call = apiService.getAvailableUsersForConversation(
                "Bearer " + accessToken, conversationId, query, 1, 100 // Page 1, up to 100 users
        );

        call.enqueue(new Callback<List<UserResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<UserResponse>> call, @NonNull Response<List<UserResponse>> response) {
                progressBarLoadingUsers.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    availableUserList.clear(); // Clear all original data
                    availableUserList.addAll(response.body());

                    // Filter out current user and apply search query
                    filterAndDisplayUsers(query); // Call helper to filter and display

                    // --- Caching: Save fetched data to local cache ---
                    // Save the raw availableUserList, then filterAndDisplayUsers() can handle filtering from it.
                    sharedPreferences.edit().putString(CACHE_KEY_AVAILABLE_USERS_PREFIX + conversationId, gson.toJson(availableUserList)).apply();
                    Log.d(TAG, "Available users fetched from API and cached.");
                    // --- End Caching Save ---

                } else {
                    String errorMsg = "Gagal memuat pengguna. Code: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing error body: " + e.getMessage());
                    }
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to fetch available users: " + response.code() + " - " + errorMsg);
                    tvEmptyStateUsers.setText("Gagal memuat pengguna.");
                    tvEmptyStateUsers.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<UserResponse>> call, @NonNull Throwable t) {
                progressBarLoadingUsers.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Kesalahan jaringan: " + t.getMessage() + ". Menampilkan data cache.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error fetching available users: " + t.getMessage(), t);
                // Jika gagal dari API, biarkan data dari cache yang sudah dimuat tetap tampil.
                // Jika tidak ada cache, maka tvEmptyStateUsers akan menampilkan "Kesalahan jaringan."
                if (filteredUserList.isEmpty()) { // Only update empty state if no data from cache
                    tvEmptyStateUsers.setText("Kesalahan jaringan.");
                    tvEmptyStateUsers.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void updateAddButtonState() {
        if (btnAddMembers != null) {
            btnAddMembers.setEnabled(!selectedUsers.isEmpty());
        }
    }

    @Override
    public void onUserSelected(UserResponse user, boolean isSelected) {
        // Callback dari adapter ketika user diklik
        // selectedUsers sudah diupdate di dalam adapter
        updateAddButtonState();
    }

    @Override
    public void onSelectionChanged(List<UserResponse> currentSelectedUsers) {
        // Callback ini memastikan `selectedUsers` di Fragment selalu sinkron
        selectedUsers.clear();
        selectedUsers.addAll(currentSelectedUsers);
        updateAddButtonState();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null; // Hindari memory leak
        if (searchTimer != null) {
            searchTimer.cancel();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // --- Caching: Clear cache for this dialog when it's destroyed,
        // as the list of available users might change frequently or become stale.
        // Or, you might choose to keep it if you expect frequent re-opening with similar results.
        // For simplicity and to ensure fresh data, clearing it is often good.
        sharedPreferences.edit().remove(CACHE_KEY_AVAILABLE_USERS_PREFIX + conversationId).apply();
        // --- End Caching Clear ---
    }
}