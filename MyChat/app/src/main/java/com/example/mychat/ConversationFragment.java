package com.example.mychat;

import static android.text.format.DateUtils.isToday;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.ConnectivityManager; // Ditambahkan
import android.net.NetworkInfo; // Ditambahkan
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mychat.conversation_service.ConversationApiService;
import com.example.mychat.models_conversation.ConversationListResponse;
import com.example.mychat.retrofit_client.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken; // Ditambahkan

// Pusher Imports
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.AuthorizationFailureException;
import com.pusher.client.Authorizer;
import com.pusher.client.channel.PrivateChannel;
import com.pusher.client.channel.PrivateChannelEventListener;
import com.pusher.client.channel.PusherEvent;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;

// OkHttp Imports (for Pusher Authorizer)
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.IOException;
import java.lang.reflect.Type; // Ditambahkan
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

// Retrofit2 Imports for Call and Response
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConversationFragment extends Fragment {
    private RecyclerView rvConversations;
    private ConversationAdapter conversationAdapter;
    private List<ConversationListResponse> conversationList;
    private LinearLayout layoutEmptyState;
    private LinearLayout layoutLoading;
    private ProgressBar progressLoading;
    private TextView tvEmptyStateMessage;
    private Button btnStartChat; // Menggunakan Button karena di layout MaterialButton
    private Button btnRefreshConversations; // Tombol Refresh

    private EditText etSearch;
    private ImageView icFilter;

    // Filter states
    private boolean isGroupFilter = false;
    private boolean isPrivateFilter = false;
    private boolean isUnreadFilter = false;

    private String accessToken;
    private String currentUserId;
    private SharedPreferences sharedPreferences; // Ditambahkan

    private static final String TAG = "ConversationFragment";
    private static final String CACHED_CONVERSATIONS_KEY = "cached_conversations"; // Kunci cache

    private static final int REQUEST_CODE_NEW_CONVERSATION = 1;

    // Pusher related variables for ConversationFragment
    private Pusher pusher;
    private PrivateChannel userPrivateChannel;
    private Gson gson;
    private boolean isDestroyed = false;
    private final Object pusherLock = new Object();
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private int retryCount = 0;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // Store EventListener instances to unbind them correctly
    private PrivateChannelEventListener conversationUpdateEventListener;
    private PrivateChannelEventListener conversationDeletedEventListener;

    private enum PusherState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        SUBSCRIBED,
        ERROR
    }
    private PusherState currentPusherState = PusherState.DISCONNECTED;


    // BroadcastReceiver for handling 'conversation-read' updates (from ChatActivity)
    private BroadcastReceiver conversationReadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String readConversationId = intent.getStringExtra("conversation_id");
            if (readConversationId != null) {
                Log.d(TAG, "Received CONVERSATION_READ broadcast for ID: " + readConversationId);
                // Trigger a full fetch to ensure accurate unread counts after read receipt
                fetchUserConversations();
            }
        }
    };

    public ConversationFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_conversation, container, false);

        rvConversations = view.findViewById(R.id.rv_conversations);
        layoutEmptyState = view.findViewById(R.id.layout_empty_state);
        layoutLoading = view.findViewById(R.id.layout_loading);
        progressLoading = view.findViewById(R.id.progress_loading);
        tvEmptyStateMessage = view.findViewById(R.id.tv_empty_state_message);
        btnStartChat = view.findViewById(R.id.btn_start_chat);
        btnRefreshConversations = view.findViewById(R.id.btn_refresh_conversations); // Inisialisasi tombol refresh
        etSearch = view.findViewById(R.id.et_search);
        icFilter = view.findViewById(R.id.ic_filter);

        conversationList = new ArrayList<>();

        sharedPreferences = requireContext().getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE); // Inisialisasi SharedPreferences
        accessToken = getAccessToken(requireContext());
        currentUserId = getCurrentUserId(requireContext());

        if (currentUserId == null) {
            Log.e(TAG, "Current User ID is null. Cannot initialize adapter.");
            showEmptyStateWithError("User ID not found. Please log in again.", false); // false agar tidak tampil tombol refresh
            redirectToLogin();
            return view;
        }

        conversationAdapter = new ConversationAdapter(conversationList);
        rvConversations.setLayoutManager(new LinearLayoutManager(getContext()));
        rvConversations.setAdapter(conversationAdapter);

        // Awalnya tampilkan loading atau data cache
        showLoadingState();
        loadConversationsFromCache(); // Coba muat dari cache terlebih dahulu

        etSearch.addTextChangedListener(new TextWatcher() {
            private Runnable searchRunnable;
            private final long SEARCH_DELAY = 500;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mainHandler.removeCallbacks(searchRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchRunnable = () -> fetchUserConversations(); // Panggil API untuk hasil pencarian terbaru
                mainHandler.postDelayed(searchRunnable, SEARCH_DELAY);
            }
        });

        icFilter.setOnClickListener(v -> showFilterDialog());

        btnStartChat.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), NewConversationActivity.class);
            startActivityForResult(intent, REQUEST_CODE_NEW_CONVERSATION);
        });

        btnRefreshConversations.setOnClickListener(v -> { // Listener untuk tombol refresh
            fetchUserConversations();
        });

        updateFilterIconAppearance();

        gson = new Gson();

        setupPusher();

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_NEW_CONVERSATION && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "New conversation created, fetching conversations...");
            fetchUserConversations();
        }
    }

    // Metode untuk memuat percakapan dari SharedPreferences (cache lokal)
    private void loadConversationsFromCache() {
        String cachedConversationsJson = sharedPreferences.getString(CACHED_CONVERSATIONS_KEY, null);
        if (cachedConversationsJson != null) {
            try {
                Type type = new TypeToken<List<ConversationListResponse>>(){}.getType();
                List<ConversationListResponse> cachedList = gson.fromJson(cachedConversationsJson, type);
                if (cachedList != null && !cachedList.isEmpty()) {
                    conversationList.clear();
                    conversationList.addAll(cachedList);
                    sortConversationList();
                    conversationAdapter.notifyDataSetChanged();
                    showConversations();
                    sendTotalUnreadCountToMainActivity(); // Perbarui badge di MainActivity
                    Log.d(TAG, "Conversations loaded from local cache: " + conversationList.size());
                } else {
                    Log.d(TAG, "Cached conversations list is empty or null.");
                    showEmptyState(); // Tampilkan empty state jika cache kosong
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing cached conversations: " + e.getMessage());
                // Jika error parsing, anggap cache rusak dan coba fetch dari API
                showEmptyStateWithError("Failed to load cached conversations.", true); // Tampilkan tombol refresh
            }
        } else {
            Log.d(TAG, "No cached conversations found.");
            // Tidak ada cache, akan tetap di loading state sampai API respons
        }
    }

    // --- Pusher Setup for ConversationFragment ---
    private void setupPusher() {
        // Use pusherLock to synchronize setup
        synchronized (pusherLock) {
            if (pusher != null) {
                // Ensure existing resources are cleaned up before creating a new Pusher instance
                cleanupPusherResources();
            }

            String PUSHER_APP_KEY = "461431472b0c2e952aa5";
            String PUSHER_APP_CLUSTER = "ap1";
            String PUSHER_AUTH_ENDPOINT = RetrofitClient.BASE_URL + "messages/pusher/auth";

            PusherOptions options = new PusherOptions()
                    .setCluster(PUSHER_APP_CLUSTER)
                    .setUseTLS(false)
                    .setAuthorizer(createAuthorizer(PUSHER_AUTH_ENDPOINT));

            pusher = new Pusher(PUSHER_APP_KEY, options);
            connectPusher(); // Attempt to connect
        }
    }

    private void connectPusher() {
        synchronized (pusherLock) {
            if (pusher == null) {
                Log.w(TAG, "Pusher instance is null during connectPusher. Aborting.");
                currentPusherState = PusherState.ERROR;
                return;
            }

            ConnectionState currentState = pusher.getConnection().getState();
            if (currentState == ConnectionState.CONNECTED || currentState == ConnectionState.CONNECTING) {
                Log.d(TAG, "Pusher is already connecting or connected. State: " + currentState);
                if (currentState == ConnectionState.CONNECTED) {
                    currentPusherState = PusherState.CONNECTED;
                } else {
                    currentPusherState = PusherState.CONNECTING;
                }
                return;
            }

            currentPusherState = PusherState.CONNECTING;

            pusher.connect(new ConnectionEventListener() {

                @Override
                public void onConnectionStateChange(ConnectionStateChange change) {
                    Log.d(TAG, "Pusher (ConversationFragment) state: " + change.getPreviousState() + " -> " + change.getCurrentState());
                    if (isDestroyed) return;
                    ConnectionState newState = change.getCurrentState();
                    synchronized (pusherLock) {
                        switch (newState) {
                            case CONNECTED:
                                currentPusherState = PusherState.CONNECTED;
                                break;
                            case DISCONNECTED:
                                currentPusherState = PusherState.DISCONNECTED;
                                break;
                            case CONNECTING:
                                currentPusherState = PusherState.CONNECTING;
                                break;
                            case DISCONNECTING:
                                Log.d(TAG, "Pusher is disconnecting. Will update state once fully DISCONNECTED.");
                                break;
                            case ALL:
                                Log.w(TAG, "Received ALL state in onConnectionStateChange. Ignoring.");
                                break;
                            default:
                                currentPusherState = PusherState.ERROR;
                                Log.w(TAG, "Unexpected Pusher state: " + newState + ". Setting to ERROR.");
                                break;
                        }
                    }

                    switch (newState) {
                        case CONNECTED:
                            retryCount = 0;
                            mainHandler.post(() -> ensureChannelSubscription());
                            break;

                        case DISCONNECTED:
                            if (isDestroyed) {
                                Log.d(TAG, "Fragment destroyed, clean disconnect. No reconnection needed.");
                            } else {
                                scheduleReconnection();
                            }
                            break;

                        case CONNECTING:
                            break;

                        case DISCONNECTING:
                            Log.d(TAG, "Pusher is disconnecting. Will handle once fully DISCONNECTED.");
                            break;

                        default:
                            Log.w(TAG, "Unexpected Pusher state (outside switch for internal state update): " + newState);
                            break;
                    }
                }

                @Override
                public void onError(String message, String code, Exception e) {
                    Log.e(TAG, "Pusher (ConversationFragment) error: " + message + " code: " + code, e);
                    synchronized (pusherLock) {
                        currentPusherState = PusherState.ERROR;
                    }
                    if (!isDestroyed) {
                        mainHandler.post(() -> Toast.makeText(getContext(), "Real-time error: " + message, Toast.LENGTH_SHORT).show());
                        scheduleReconnection();
                    }
                }
            }, ConnectionState.ALL);
        }
    }

    private void scheduleReconnection() {
        if (isDestroyed || retryCount >= MAX_RETRY_ATTEMPTS) {
            Log.d(TAG, "Max retry attempts reached or fragment destroyed. Not scheduling reconnection.");
            return;
        }

        long delayMs = (long) Math.pow(2, retryCount) * 1000;
        retryCount++;

        mainHandler.postDelayed(() -> {
            if (!isDestroyed && pusher != null && pusher.getConnection().getState() == ConnectionState.DISCONNECTED) {
                Log.d(TAG, "Attempting Pusher reconnection (ConversationFragment), retry: " + retryCount);
                connectPusher();
            } else if (pusher != null && pusher.getConnection().getState() == ConnectionState.DISCONNECTING) {
                Log.d(TAG, "Still disconnecting, reschedule reconnection check.");
                scheduleReconnection();
            } else {
                Log.d(TAG, "Pusher is already connected or in another state. No reconnection needed now.");
            }
        }, delayMs);
    }

    private Authorizer createAuthorizer(String authEndpoint) {
        return (channelName, socketId) -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build();

                RequestBody body = new FormBody.Builder()
                        .add("socket_id", socketId)
                        .add("channel_name", channelName)
                        .build();

                Request request = new Request.Builder()
                        .url(authEndpoint)
                        .post(body)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .build();

                try (okhttp3.Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        return response.body().string();
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "No error body";
                        throw new AuthorizationFailureException("Auth failed: " + response.code() + " - " + errorBody);
                    }
                }
            } catch (IOException e) {
                throw new AuthorizationFailureException("Network error during authorization: " + e.getMessage());
            }
        };
    }

    private void ensureChannelSubscription() {
        if (isDestroyed) return;

        synchronized (pusherLock) {
            if (pusher == null || pusher.getConnection().getState() != ConnectionState.CONNECTED) {
                Log.w(TAG, "Cannot subscribe: Pusher is not connected. Current state: " + (pusher != null ? pusher.getConnection().getState() : "null"));
                if (pusher == null || pusher.getConnection().getState() == ConnectionState.DISCONNECTED) {
                    connectPusher();
                }
                return;
            }

            if (currentPusherState == PusherState.SUBSCRIBED && userPrivateChannel != null && userPrivateChannel.isSubscribed()) {
                String expectedChannelName = "private-user-" + currentUserId;
                if (Objects.equals(userPrivateChannel.getName(), expectedChannelName)) {
                    Log.d(TAG, "Already subscribed to the correct user channel: " + expectedChannelName + ". Ensuring events are bound.");
                    bindRealTimeEvents();
                    return;
                } else {
                    Log.w(TAG, "Subscribed to wrong channel: " + userPrivateChannel.getName() + ", expected: " + expectedChannelName + ". Will re-subscribe.");
                }
            }
        }

        String userChannelName = "private-user-" + currentUserId;

        if (userPrivateChannel != null) {
            boolean shouldUnsubscribe = false;
            if (!Objects.equals(userPrivateChannel.getName(), userChannelName)) {
                Log.d(TAG, "Unsubscribing from old channel: " + userPrivateChannel.getName() + " to subscribe to: " + userChannelName);
                shouldUnsubscribe = true;
            } else if (!userPrivateChannel.isSubscribed() && pusher.getConnection().getState() == ConnectionState.CONNECTED) {
                Log.w(TAG, "Existing userPrivateChannel is not subscribed but Pusher is connected. Forcing re-subscription.");
                shouldUnsubscribe = true;
            }

            if (shouldUnsubscribe) {
                try {
                    if (conversationUpdateEventListener != null) {
                        userPrivateChannel.unbind("conversation-updated", conversationUpdateEventListener);
                        conversationUpdateEventListener = null;
                    }
                    if (conversationDeletedEventListener != null) {
                        userPrivateChannel.unbind("conversation-deleted", conversationDeletedEventListener);
                        conversationDeletedEventListener = null;
                    }
                    pusher.unsubscribe(userPrivateChannel.getName());
                    Log.d(TAG, "Unsubscribed from previous user channel: " + userPrivateChannel.getName());
                } catch (Exception e) {
                    Log.w(TAG, "Error unsubscribing previous user channel: " + e.getMessage());
                } finally {
                    userPrivateChannel = null;
                }
            }
        }

        if (userPrivateChannel == null) {
            subscribeToUserChannelInternal(userChannelName);
        } else {
            Log.d(TAG, "User channel still exists and matches name. Ensuring events are bound.");
            bindRealTimeEvents();
            synchronized (pusherLock) {
                currentPusherState = PusherState.SUBSCRIBED;
            }
        }
    }

    private void subscribeToUserChannelInternal(String channelName) {
        try {
            userPrivateChannel = pusher.subscribePrivate(channelName, new PrivateChannelEventListener() {
                @Override
                public void onSubscriptionSucceeded(String channelName) {
                    Log.d(TAG, "Successfully subscribed to user channel: " + channelName);

                    synchronized (pusherLock) {
                        currentPusherState = PusherState.SUBSCRIBED;
                    }
                    mainHandler.post(() -> bindRealTimeEvents());
                }

                @Override
                public void onAuthenticationFailure(String message, Exception e) {
                    Log.e(TAG, "User channel auth failed: " + message, e);
                    synchronized (pusherLock) {
                        currentPusherState = PusherState.ERROR;
                    }
                    if (!isDestroyed) {
                        mainHandler.post(() -> {
                            Toast.makeText(getContext(), "Real-time authentication failed for conversations.", Toast.LENGTH_LONG).show();
                            if (message.contains("401")) {
                                redirectToLogin();
                            }
                        });
                    }
                }

                @Override
                public void onEvent(PusherEvent event) {
                }
            });
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error subscribing to user channel: " + e.getMessage());
            if (userPrivateChannel != null && Objects.equals(userPrivateChannel.getName(), channelName)) {
                bindRealTimeEvents();
                synchronized (pusherLock) {
                    currentPusherState = PusherState.SUBSCRIBED;
                }
            } else {
                Log.e(TAG, "Unexpected subscription error, trying to clean up and reconnect.");
                cleanupPusherResources();
                scheduleReconnection();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error subscribing to user channel: " + e.getMessage(), e);
            synchronized (pusherLock) {
                currentPusherState = PusherState.ERROR;
            }
        }
    }

    private void bindRealTimeEvents() {
        if (userPrivateChannel == null || !userPrivateChannel.isSubscribed()) {
            Log.w(TAG, "Cannot bind real-time events: user channel not ready.");
            return;
        }

        if (conversationUpdateEventListener != null) {
            userPrivateChannel.unbind("conversation-updated", conversationUpdateEventListener);
            conversationUpdateEventListener = null;
            Log.d(TAG, "Unbound previous conversation-updated listener.");
        }
        if (conversationDeletedEventListener != null) {
            userPrivateChannel.unbind("conversation-deleted", conversationDeletedEventListener);
            conversationDeletedEventListener = null;
            Log.d(TAG, "Unbound previous conversation-deleted listener.");
        }

        conversationUpdateEventListener = new PrivateChannelEventListener() {
            @Override
            public void onSubscriptionSucceeded(String channelName) {}
            @Override
            public void onAuthenticationFailure(String message, Exception e) {
                Log.e(TAG, "Auth failed for conversation-updated event: " + message, e);
            }
            @Override
            public void onEvent(PusherEvent event) {
                if ("conversation-updated".equals(event.getEventName())) {
                    Log.d(TAG, "Received conversation update: " + event.getData());
                    mainHandler.post(() -> handleConversationUpdate(event.getData()));
                }
            }
        };
        userPrivateChannel.bind("conversation-updated", conversationUpdateEventListener);
        Log.d(TAG, "Bound conversation-updated event listener.");

        conversationDeletedEventListener = new PrivateChannelEventListener() {
            @Override
            public void onSubscriptionSucceeded(String channelName) {}
            @Override
            public void onAuthenticationFailure(String message, Exception e) {
                Log.e(TAG, "Auth failed for conversation-deleted event: " + message, e);
            }
            @Override
            public void onEvent(PusherEvent event) {
                if ("conversation-deleted".equals(event.getEventName())) {
                    Log.d(TAG, "Received conversation deleted event: " + event.getData());
                    mainHandler.post(() -> handleConversationDeleted(event.getData()));
                }
            }
        };
        userPrivateChannel.bind("conversation-deleted", conversationDeletedEventListener);
        Log.d(TAG, "Bound conversation-deleted event listener.");
    }

    private static class DeletedConversationPayload {
        @SerializedName("conversation_id")
        String conversation_id;
        @SerializedName("deleted_by")
        String deleted_by;
        @SerializedName("left_by")
        String left_by;
    }

    private void handleConversationUpdate(String jsonData) {
        if (isDestroyed) {
            Log.d(TAG, "Fragment destroyed, ignoring conversation update.");
            return;
        }

        try {
            ConversationListResponse updatedConversation = gson.fromJson(jsonData, ConversationListResponse.class);

            if (updatedConversation == null || updatedConversation.getId() == null) {
                Log.w(TAG, "Received invalid conversation update data.");
                return;
            }

            boolean found = false;
            for (int i = 0; i < conversationList.size(); i++) {
                if (conversationList.get(i).getId().equals(updatedConversation.getId())) {
                    conversationList.set(i, updatedConversation);
                    found = true;
                    break;
                }
            }

            if (found) {
                Log.d(TAG, "Updating existing conversation locally: " + updatedConversation.getName());
                sortConversationList();
                conversationAdapter.notifyDataSetChanged();
                showConversations();
                sendTotalUnreadCountToMainActivity();
                // Simpan ke cache setelah update real-time
                sharedPreferences.edit().putString(CACHED_CONVERSATIONS_KEY, gson.toJson(conversationList)).apply();
            } else {
                Log.d(TAG, "New conversation received or existing conversation not found in current filtered/paginated list. Re-fetching all conversations.");
                fetchUserConversations(); // Re-fetch to get the new conversation or update state
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling conversation update: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error processing real-time conversation update.", Toast.LENGTH_SHORT).show();
            fetchUserConversations(); // Re-fetch on error to ensure consistency
        }
    }

    private void handleConversationDeleted(String jsonData) {
        if (isDestroyed) {
            Log.d(TAG, "Fragment destroyed, ignoring conversation deleted event.");
            return;
        }
        try {
            DeletedConversationPayload payload = gson.fromJson(jsonData, DeletedConversationPayload.class);

            if (payload == null || payload.conversation_id == null) {
                Log.w(TAG, "Received invalid conversation deleted data.");
                return;
            }

            int removedPosition = -1;
            for (int i = 0; i < conversationList.size(); i++) {
                if (conversationList.get(i).getId().equals(payload.conversation_id)) {
                    conversationList.remove(i);
                    removedPosition = i;
                    break;
                }
            }

            if (removedPosition != -1) {
                Log.d(TAG, "Removed conversation locally: " + payload.conversation_id);
                conversationAdapter.notifyItemRemoved(removedPosition);
                if (conversationList.isEmpty()) {
                    showEmptyState();
                }
                sendTotalUnreadCountToMainActivity();
                // Simpan ke cache setelah update real-time
                sharedPreferences.edit().putString(CACHED_CONVERSATIONS_KEY, gson.toJson(conversationList)).apply();
            } else {
                Log.d(TAG, "Conversation to delete not found in current list: " + payload.conversation_id);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling conversation deleted: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error processing real-time conversation deletion.", Toast.LENGTH_SHORT).show();
            fetchUserConversations();
        }
    }


    private void cleanupPusherResources() {
        synchronized (pusherLock) {
            mainHandler.removeCallbacksAndMessages(null);

            if (userPrivateChannel != null) {
                try {
                    if (conversationUpdateEventListener != null) {
                        userPrivateChannel.unbind("conversation-updated", conversationUpdateEventListener);
                        conversationUpdateEventListener = null;
                    }
                    if (conversationDeletedEventListener != null) {
                        userPrivateChannel.unbind("conversation-deleted", conversationDeletedEventListener);
                        conversationDeletedEventListener = null;
                    }
                    pusher.unsubscribe(userPrivateChannel.getName());
                    Log.d(TAG, "Unsubscribed from user channel: " + userPrivateChannel.getName());
                } catch (Exception e) {
                    Log.w(TAG, "Error unsubscribing user channel: " + e.getMessage());
                }
                userPrivateChannel = null;
            }

            if (pusher != null && (pusher.getConnection().getState() == ConnectionState.CONNECTED || pusher.getConnection().getState() == ConnectionState.CONNECTING)) {
                pusher.disconnect();
                Log.d(TAG, "Pusher (ConversationFragment) disconnected.");
            } else if (pusher != null) {
                Log.d(TAG, "Pusher (ConversationFragment) not in CONNECTED/CONNECTING state, no explicit disconnect needed. Current state: " + pusher.getConnection().getState());
            }
            pusher = null;
            currentPusherState = PusherState.DISCONNECTED;
            Log.d(TAG, "Pusher resources cleaned up.");
        }
    }

    private void sortConversationList() {
        Collections.sort(conversationList, (c1, c2) -> {
            boolean c1HasUnread = c1.getUnreadCount() > 0;
            boolean c2HasUnread = c2.getUnreadCount() > 0;

            if (c1HasUnread && !c2HasUnread) {
                return -1;
            }
            if (!c1HasUnread && c2HasUnread) {
                return 1;
            }

            Date d1 = c1.getLastMessageAt();
            Date d2 = c2.getLastMessageAt();

            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;

            return d2.compareTo(d1); // Newest date first
        });
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_filter, null);

        CheckBox cbGroupDialog = dialogView.findViewById(R.id.cb_group_dialog);
        CheckBox cbPrivateDialog = dialogView.findViewById(R.id.cb_private_dialog);
        CheckBox cbUnreadDialog = dialogView.findViewById(R.id.cb_unread_dialog);
        Button btnApplyFilter = dialogView.findViewById(R.id.btn_apply_filter);
        Button btnClearFilter = dialogView.findViewById(R.id.btn_clear_filter);

        cbGroupDialog.setChecked(isGroupFilter);
        cbPrivateDialog.setChecked(isPrivateFilter);
        cbUnreadDialog.setChecked(isUnreadFilter);

        cbGroupDialog.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cbPrivateDialog.setChecked(false);
            }
        });

        cbPrivateDialog.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                cbGroupDialog.setChecked(false);
            }
        });

        AlertDialog dialog = builder.setView(dialogView).create();

        btnApplyFilter.setOnClickListener(v -> {
            isGroupFilter = cbGroupDialog.isChecked();
            isPrivateFilter = cbPrivateDialog.isChecked();
            isUnreadFilter = cbUnreadDialog.isChecked();

            updateFilterIconAppearance();
            fetchUserConversations();
            dialog.dismiss();
            Toast.makeText(getContext(), "Filter applied", Toast.LENGTH_SHORT).show();
        });

        btnClearFilter.setOnClickListener(v -> {
            isGroupFilter = false;
            isPrivateFilter = false;
            isUnreadFilter = false;

            cbGroupDialog.setChecked(false);
            cbPrivateDialog.setChecked(false);
            cbUnreadDialog.setChecked(false);

            updateFilterIconAppearance();
            fetchUserConversations();
            dialog.dismiss();
            Toast.makeText(getContext(), "Filter cleared", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void updateFilterIconAppearance() {
        boolean hasActiveFilter = isGroupFilter || isPrivateFilter || isUnreadFilter;

        if (hasActiveFilter) {
            icFilter.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_color)));
            icFilter.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.surface_color)));
        } else {
            icFilter.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.surface_color)));
            icFilter.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.primary_color)));
        }
    }

    // Cek ketersediaan jaringan
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void fetchUserConversations() {
        // Tampilkan loading hanya jika tidak ada data di RecyclerView atau jika sedang refresh
        if (conversationList.isEmpty()) {
            showLoadingState();
        } else {
            // Jika ada data, tetap tampilkan data lama, tapi sembunyikan tombol refresh
            // dan mungkin tampilkan indikator "refreshing" yang lebih kecil
            btnRefreshConversations.setVisibility(View.GONE);
            tvEmptyStateMessage.setVisibility(View.GONE); // Sembunyikan pesan error jika ada data
            layoutEmptyState.setVisibility(View.GONE); // Sembunyikan empty state
        }


        if (accessToken == null) {
            Log.e(TAG, "Cannot fetch conversations: Access Token is null.");
            showEmptyStateWithError("Authentication failed. Please log in again.", false); // False: tidak tampil refresh
            redirectToLogin();
            return;
        }

        if (!isNetworkAvailable()) {
            Log.d(TAG, "No network connection. Displaying cached conversations.");
            if (conversationList.isEmpty()) { // Jika tidak ada cache, tampilkan pesan offline
                showEmptyStateWithError("No internet connection. Displaying offline data if available.", true); // True: tampil refresh
            } else {
                // Jika ada cache, biarkan data cache tampil dan informasikan bahwa itu data offline
                Toast.makeText(getContext(), "No internet connection. Displaying cached data.", Toast.LENGTH_LONG).show();
                showConversations(); // Pastikan RecyclerView terlihat
            }
            return; // Berhenti di sini jika tidak ada jaringan
        }

        int page = 1;
        int perPage = 20;

        String searchQuery = etSearch.getText().toString();
        if (searchQuery.isEmpty()) {
            searchQuery = null;
        }

        Boolean isGroupFilterParam = null;
        if (isGroupFilter) {
            isGroupFilterParam = true;
        } else if (isPrivateFilter) {
            isGroupFilterParam = false;
        }

        Boolean unreadOnlyFilter = isUnreadFilter ? true : null;

        ConversationApiService apiService = RetrofitClient.getConversationApiService();
        Call<List<ConversationListResponse>> call = apiService.getUserConversations(
                "Bearer " + accessToken,
                page,
                perPage,
                searchQuery,
                isGroupFilterParam,
                unreadOnlyFilter
        );

        call.enqueue(new Callback<List<ConversationListResponse>>() {
            @Override
            public void onResponse(@NonNull Call<List<ConversationListResponse>> call, @NonNull Response<List<ConversationListResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ConversationListResponse> fetchedConversations = response.body();

                    if (fetchedConversations.isEmpty()) {
                        conversationList.clear();
                        conversationAdapter.notifyDataSetChanged();
                        showEmptyState(); // Menampilkan empty state tanpa tombol refresh
                    } else {
                        conversationList.clear();
                        conversationList.addAll(fetchedConversations);
                        sortConversationList();
                        conversationAdapter.notifyDataSetChanged();
                        showConversations();
                        Log.d(TAG, "Conversations loaded and sorted: " + fetchedConversations.size());
                        // Simpan ke cache setelah berhasil fetch dari API
                        sharedPreferences.edit().putString(CACHED_CONVERSATIONS_KEY, gson.toJson(fetchedConversations)).apply();
                    }
                    sendTotalUnreadCountToMainActivity();
                } else {
                    String errorMessage = "Failed to load conversations: Unknown error";
                    if (response.errorBody() != null) {
                        try {
                            errorMessage += " " + response.errorBody().string();
                        } catch (IOException e) {
                            Log.e(TAG, "Error parsing error body: " + e.getMessage());
                        }
                    }
                    Log.e(TAG, "Failed to load conversations: " + response.code() + " - " + errorMessage);
                    Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_LONG).show();

                    if (response.code() == 401) {
                        redirectToLogin();
                    } else {
                        // Jika ada data di cache, tetap tampilkan
                        if (conversationList.isEmpty()) {
                            showEmptyStateWithError("Failed to load conversations. Please try again.", true); // True: tampil refresh
                        } else {
                            Toast.makeText(getContext(), "Failed to load latest conversations. Displaying cached data.", Toast.LENGTH_LONG).show();
                            showConversations();
                            // Tombol refresh sudah tersembunyi jika ada data, biarkan saja
                        }
                    }
                    sendTotalUnreadCountToMainActivity();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<ConversationListResponse>> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching conversations: " + t.getMessage(), t);

                String toastMessage;
                if (isNetworkAvailable()) {
                    toastMessage = "Server error: " + (t.getMessage() != null ? t.getMessage() : "Unknown error") + ". Displaying cached data.";
                } else {
                    toastMessage = "No internet connection. Displaying cached data.";
                }

                try {
                    Context context = getContext();
                    if (context != null) {
                        Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error showing Toast: " + e.getMessage(), e);
                }

                // Fallback logic kalau gagal load dari server
                if (conversationList != null && !conversationList.isEmpty()) {
                    // Masih ada data cache, tampilkan aja
                    showConversations();
                } else {
                    // Kosong, tampilkan empty state dengan tombol refresh
                    if (isNetworkAvailable()) {
                        showEmptyStateWithError("Server error. Please try again.", true);
                    } else {
                        showEmptyStateWithError("No internet connection. Please check your network.", true);
                    }
                }

                // Tetap kirim unread count, walaupun gagal fetch
                try {
                    sendTotalUnreadCountToMainActivity();
                } catch (Exception e) {
                    Log.e(TAG, "Error sending unread count: " + e.getMessage(), e);
                }
            }

        });
    }

    private void showConversations() {
        rvConversations.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        layoutEmptyState.setVisibility(View.VISIBLE);
        rvConversations.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
        tvEmptyStateMessage.setText("No conversations found. Start a new chat!");
        btnStartChat.setVisibility(View.VISIBLE);
        btnRefreshConversations.setVisibility(View.GONE); // Sembunyikan tombol refresh
    }

    // Overload showEmptyStateWithError untuk mengontrol visibilitas tombol refresh
    private void showEmptyStateWithError(String errorMessage, boolean showRefreshButton) {
        layoutEmptyState.setVisibility(View.VISIBLE);
        rvConversations.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);

        if (tvEmptyStateMessage != null) {
            tvEmptyStateMessage.setText(errorMessage);
        }
        btnStartChat.setVisibility(View.GONE); // Tombol "Start Chat" disembunyikan saat ada error

        if (showRefreshButton) {
            btnRefreshConversations.setVisibility(View.VISIBLE);
        } else {
            btnRefreshConversations.setVisibility(View.GONE);
        }
    }

    private void showLoadingState() {
        layoutLoading.setVisibility(View.VISIBLE);
        rvConversations.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();
        isDestroyed = false;
        Log.d(TAG, "onResume: Fetching conversations and ensuring Pusher connection.");

        // Memuat dari cache dan kemudian mencoba dari API
        loadConversationsFromCache();
        fetchUserConversations(); // Ini akan mencoba memuat dari API dan memperbarui cache

        // Ensure Pusher connection and subscription on resume
        synchronized (pusherLock) {
            if (pusher == null) {
                setupPusher();
            } else if (pusher.getConnection().getState() != ConnectionState.CONNECTED) {
                connectPusher();
            } else {
                ensureChannelSubscription();
            }
        }

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(requireContext());
        lbm.registerReceiver(conversationReadReceiver, new IntentFilter("com.example.mychat.CONVERSATION_READ"));
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: No longer receiving real-time updates for conversations directly from Pusher.");
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(requireContext());
        try {
            lbm.unregisterReceiver(conversationReadReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver not registered on pause: " + e.getMessage());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isDestroyed = true;
        cleanupPusherResources();
        rvConversations = null;
        layoutEmptyState = null;
        layoutLoading = null;
        progressLoading = null;
        tvEmptyStateMessage = null;
        btnStartChat = null;
        btnRefreshConversations = null; // Set null juga
        etSearch = null;
        icFilter = null;
    }

    private void sendTotalUnreadCountToMainActivity() {
        int totalUnread = 0;
        for (ConversationListResponse conv : conversationList) {
            totalUnread += conv.getUnreadCount();
        }

        Intent intent = new Intent("com.example.mychat.UPDATE_TOTAL_UNREAD_COUNT");
        intent.putExtra("total_unread_count", totalUnread);
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent);
        Log.d(TAG, "Sent total unread count to MainActivity: " + totalUnread);
    }

    private void redirectToLogin() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
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
}