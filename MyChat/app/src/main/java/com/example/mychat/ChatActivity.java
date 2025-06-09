package com.example.mychat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable; // Import ini
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat; // Pastikan ini diimpor
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mychat.conversation_service.ConversationApiService;
import com.example.mychat.conversation_service.MessageApiService;
import com.example.mychat.models_conversation.ConversationResponse;
import com.example.mychat.models_message.MessageCreate;
import com.example.mychat.models_message.MessageResponse;
import com.example.mychat.models_message.MessagesResponse;
import com.example.mychat.models_message.MessageReadReceiptUpdate;
import com.example.mychat.models_message.RealTimeMessage;
import com.example.mychat.models_message.TypingEvent;
import com.example.mychat.retrofit_client.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

// Pusher Imports
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.AuthorizationFailureException;
import com.pusher.client.Authorizer;
import com.pusher.client.channel.PrivateChannel;
import com.pusher.client.channel.PrivateChannelEventListener;
import com.pusher.client.channel.ChannelEventListener;
import com.pusher.client.channel.PusherEvent;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;

// OkHttp Imports
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

// Retrofit2 Imports
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ChatActivity extends AppCompatActivity {

    private Toolbar toolbarChat;
    private ImageView ivChatProfilePicture;
    private TextView tvChatName;
    private TextView tvChatStatus;
    private RecyclerView rvMessages;
    private EditText etMessageInput;
    private ImageButton btnAttachment;
    private ImageButton fabSend;
    private LinearLayout layoutTypingIndicator;
    private LottieAnimationView lottieTypingDots;

    private String conversationId;
    private String currentUserId;
    private String accessToken;
    private boolean isGroupChat;
    private String otherParticipantId;
    private String otherParticipantUsername;
    private String otherParticipantAvatar;

    private List<MessageResponse> messageList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private LinearLayoutManager layoutManager;

    private int currentPage = 1;
    private int perPage = 20;
    private boolean isLoading = false;
    private boolean hasMoreMessages = true;

    private static final String TAG = "ChatActivity";

    // Pusher related variables
    private Pusher pusher;
    private PrivateChannel privateChannel;
    private Gson gson;

    private PrivateChannelEventListener newMessageEventListener;
    private PrivateChannelEventListener messageStatusEventListener;
    private PrivateChannelEventListener typingEventListener;

    private boolean isSubscriptionInProgress = false;
    private String currentChannelName = null;

    private boolean isPusherConnected = false;
    private boolean isDestroyed = false;

    private enum PusherState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        SUBSCRIBED,
        RECONNECTING,
        ERROR
    }

    private PusherState currentPusherState = PusherState.DISCONNECTED;
    private final Object pusherLock = new Object();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Handler typingHandler = new Handler(Looper.getMainLooper());
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long TYPING_TIMEOUT = 3000; // 3 seconds for typing indicator
    private int retryCount = 0;

    // --- Caching Variables ---
    private SharedPreferences sharedPreferences;
    private static final String CACHE_KEY_MESSAGES_PREFIX = "cached_messages_";
    // --- End Caching Variables ---


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Intent intent = getIntent();
        conversationId = intent.getStringExtra("conversation_id");
        isGroupChat = intent.getBooleanExtra("is_group_chat", false);
        otherParticipantId = intent.getStringExtra("other_participant_id");
        otherParticipantUsername = intent.getStringExtra("other_participant_username");
        otherParticipantAvatar = intent.getStringExtra("other_participant_avatar");

        if (conversationId == null) {
            Toast.makeText(this, "Conversation ID is missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        accessToken = getAccessToken(this);
        currentUserId = getCurrentUserId(this);

        if (accessToken == null || currentUserId == null) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish();
            return;
        }

        // --- Caching: Inisialisasi SharedPreferences dan Gson ---
        sharedPreferences = getSharedPreferences("ChatCache", Context.MODE_PRIVATE);
        gson = new Gson();
        // --- End Caching Initialization ---

        toolbarChat = findViewById(R.id.toolbar_chat);
        ivChatProfilePicture = findViewById(R.id.iv_chat_profile_picture);
        tvChatName = findViewById(R.id.tv_chat_name);
        tvChatStatus = findViewById(R.id.tv_chat_status);
        rvMessages = findViewById(R.id.rv_messages);
        etMessageInput = findViewById(R.id.et_message_input);
        btnAttachment = findViewById(R.id.btn_attachment);
        fabSend = findViewById(R.id.fab_send);
        layoutTypingIndicator = findViewById(R.id.layout_typing_indicator);
        lottieTypingDots = findViewById(R.id.lottie_typing_dots);


        setSupportActionBar(toolbarChat);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbarChat.setNavigationOnClickListener(v -> onBackPressed());

        setupChatHeader();

        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Pesan terbaru di bawah
        rvMessages.setLayoutManager(layoutManager);

        messageAdapter = new MessageAdapter(this, messageList, currentUserId, isGroupChat);
        rvMessages.setAdapter(messageAdapter);

        // --- Caching: Panggil metode untuk memuat pesan (dahulukan dari lokal) ---
        loadMessagesLocally();
        // --- End Caching Load Local ---

        // Load pesan awal dari API
        loadMessages(currentPage, perPage, null);

        rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // Jika scroll ke atas dan mencapai item pertama, muat pesan lama
                if (dy < 0) { // Scrolling upwards
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    if (firstVisibleItemPosition == 0 && !isLoading && hasMoreMessages) {
                        Log.d(TAG, "Loading more old messages...");
                        String beforeMessageId = null;
                        if (!messageList.isEmpty()) {
                            beforeMessageId = messageList.get(0).getId();
                        }
                        if (beforeMessageId != null) {
                            loadMessages(currentPage + 1, perPage, beforeMessageId);
                        } else {
                            Log.w(TAG, "beforeMessageId is null but hasMoreMessages is true. Cannot load more.");
                        }
                    }
                }

                int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                int totalItemCount = layoutManager.getItemCount();
                // Tandai pesan sebagai terbaca saat pengguna scroll ke akhir atau saat activity di resume
                if (totalItemCount > 0 && lastVisibleItemPosition == totalItemCount - 1) {
                    Log.d(TAG, "User scrolled to the end of messages. Marking ALL loaded messages as read.");
                    markMessagesAsRead(messageList);
                }
            }
        });

        setupMessageInputListener();

        fabSend.setOnClickListener(v -> {
            String messageContent = etMessageInput.getText().toString().trim();
            if (!messageContent.isEmpty()) {
                sendMessage(messageContent);
                etMessageInput.setText("");
                sendTypingEvent(false); // Stop typing after sending message
            } else {
                Toast.makeText(this, "Voice message (not implemented yet)", Toast.LENGTH_SHORT).show();
            }
        });

        btnAttachment.setOnClickListener(v -> {
            Toast.makeText(this, "Attachment (not implemented yet)", Toast.LENGTH_SHORT).show();
        });

        setupPusher();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDestroyed = false;

        // Pastikan Pusher tersambung dan langganan saluran
        ensurePusherConnection();

        // Tandai pesan yang terlihat sebagai sudah dibaca saat onResume
        // --- PERBAIKAN DI SINI ---
        if (!messageList.isEmpty() && layoutManager != null) { // Tambahkan cek layoutManager
            int firstVisible = layoutManager.findFirstVisibleItemPosition();
            int lastVisible = layoutManager.findLastVisibleItemPosition();

            // Penting: Pastikan indeks-indeks ini valid (tidak -1)
            if (firstVisible != RecyclerView.NO_POSITION && lastVisible != RecyclerView.NO_POSITION) {
                List<MessageResponse> visibleMessages = new ArrayList<>();
                // Loop harus dimulai dari 0 jika firstVisible adalah 0, atau dari firstVisible jika bukan.
                // Loop juga harus berakhir sebelum messageList.size()
                for (int i = firstVisible; i <= lastVisible && i < messageList.size(); i++) {
                    if (i >= 0) { // Pastikan indeks tidak negatif
                        visibleMessages.add(messageList.get(i));
                    }
                }
                if (!visibleMessages.isEmpty()) { // Hanya tandai jika ada pesan yang terlihat
                    markMessagesAsRead(visibleMessages);
                }
            } else {
                Log.d(TAG, "RecyclerView not fully laid out or no visible items on onResume.");
                // Opsi: Anda bisa menandai SEMUA pesan sebagai terbaca jika tidak ada yang terlihat saat resume
                // markMessagesAsRead(messageList); // Pertimbangkan ini jika Anda ingin semua pesan di-mark read saat membuka chat
            }
        }
        // --- AKHIR PERBAIKAN ---

        // Pastikan UI status koneksi overlay diperbarui saat onResume
        if (pusher != null) {
            updateConnectionStatusUI(PusherState.valueOf(pusher.getConnection().getState().name()));
        } else {
            updateConnectionStatusUI(PusherState.DISCONNECTED);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mainHandler.removeCallbacksAndMessages(null);
        typingHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDestroyed = true;

        synchronized (pusherLock) {
            currentPusherState = PusherState.DISCONNECTED;
        }

        mainHandler.removeCallbacksAndMessages(null);
        typingHandler.removeCallbacksAndMessages(null);
        cleanupPusherResources();
    }

    private void ensurePusherConnection() {
        if (isDestroyed) return;

        synchronized (pusherLock) {
            if (currentPusherState == PusherState.SUBSCRIBED) {
                Log.d(TAG, "Already properly connected and subscribed");
                return;
            }
        }

        if (pusher == null) {
            setupPusher();
        } else {
            checkAndReconnect();
        }
    }

    private void checkAndReconnect() {
        ConnectionState state = pusher.getConnection().getState();
        Log.d(TAG, "Current Pusher state: " + state);

        // --- New: Update UI immediately when re-attempting connection ---
        if (state == ConnectionState.DISCONNECTED) {
            updateConnectionStatusUI(PusherState.DISCONNECTED);
        } else if (state == ConnectionState.CONNECTING) {
            updateConnectionStatusUI(PusherState.CONNECTING);
        }
        // --- End New ---

        switch (state) {
            case CONNECTED:
                ensureChannelSubscription();
                break;

            case DISCONNECTED:
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    retryCount++;
                    Log.d(TAG, "Attempting reconnection, retry: " + retryCount);
                    connectPusher();
                } else {
                    Log.e(TAG, "Max retry attempts reached");
                    synchronized (pusherLock) {
                        currentPusherState = PusherState.ERROR;
                    }
                    // --- New: Jika max retry tercapai, pastikan UI menunjukkan error ---
                    updateConnectionStatusUI(PusherState.ERROR);
                    // --- End New ---
                }
                break;

            case CONNECTING:
                Log.d(TAG, "Pusher is connecting, waiting...");
                break;

            default:
                Log.w(TAG, "Unexpected Pusher state: " + state);
                break;
        }
    }


    private void setupPusher() {
        if (pusher != null) {
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
        connectPusher();
    }


    private void connectPusher() {
        synchronized (pusherLock) {
            currentPusherState = PusherState.CONNECTING;
        }

        pusher.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange change) {
                Log.d(TAG, "Pusher state: " + change.getPreviousState() + " -> " + change.getCurrentState());

                if (isDestroyed) return;

                // --- New: Update UI based on Pusher connection state ---
                updateConnectionStatusUI(PusherState.valueOf(change.getCurrentState().name()));
                // --- End New ---

                switch (change.getCurrentState()) {
                    case CONNECTED:
                        synchronized (pusherLock) {
                            currentPusherState = PusherState.CONNECTED;
                        }
                        retryCount = 0; // Reset retry count on success
                        mainHandler.post(() -> ensureChannelSubscription());
                        break;

                    case DISCONNECTED:
                        synchronized (pusherLock) {
                            if (currentPusherState != PusherState.ERROR) {
                                currentPusherState = PusherState.DISCONNECTED;
                            }
                        }
                        scheduleReconnection();
                        break;
                }
            }

            @Override
            public void onError(String message, String code, Exception e) {
                Log.e(TAG, "Pusher error: " + message + " code: " + code, e);

                synchronized (pusherLock) {
                    currentPusherState = PusherState.ERROR;
                }

                // --- New: Update UI for error state ---
                updateConnectionStatusUI(PusherState.ERROR);
                // --- End New ---

                if (!isDestroyed) {
                    mainHandler.post(() ->
                            Toast.makeText(ChatActivity.this, "Connection error: " + message, Toast.LENGTH_SHORT).show()
                    );
                    scheduleReconnection();
                }
            }
        }, ConnectionState.ALL);
    }

    private void scheduleReconnection() {
        if (isDestroyed || retryCount >= MAX_RETRY_ATTEMPTS) {
            Log.e(TAG, "Max retry attempts reached or activity destroyed");
            return;
        }

        long delayMs = Math.min((long) Math.pow(2, retryCount) * 1000, 30000);

        Log.d(TAG, "Scheduling reconnection attempt " + (retryCount + 1) + " in " + delayMs + "ms");

        mainHandler.postDelayed(() -> {
            if (!isDestroyed) {
                Log.d(TAG, "Attempting reconnection...");
                checkAndReconnect();
            }
        }, delayMs);
    }

    private void ensureChannelSubscription() {
        if (isDestroyed) return;

        if (pusher == null) {
            Log.w(TAG, "Pusher is null, setting up new connection");
            setupPusher();
            return;
        }

        ConnectionState actualState = pusher.getConnection().getState();
        Log.d(TAG, "Actual Pusher connection state: " + actualState);

        String channelName = "private-chat-" + conversationId;

        synchronized (pusherLock) {
            Log.d(TAG, "Internal Pusher state: " + currentPusherState);

            if (actualState == ConnectionState.CONNECTED && currentPusherState != PusherState.SUBSCRIBED && currentPusherState != PusherState.CONNECTED) {
                currentPusherState = PusherState.CONNECTED;
                Log.d(TAG, "Adjusting internal state to CONNECTED based on actual state.");
            } else if (actualState == ConnectionState.DISCONNECTED && currentPusherState != PusherState.DISCONNECTED) {
                currentPusherState = PusherState.DISCONNECTED;
                Log.d(TAG, "Adjusting internal state to DISCONNECTED based on actual state.");
            }
            if (currentPusherState == PusherState.SUBSCRIBED && privateChannel != null && privateChannel.isSubscribed() && Objects.equals(privateChannel.getName(), channelName)) {
                Log.d(TAG, "Already subscribed to the correct channel and internal state is SUBSCRIBED. Ensuring events are bound.");
                bindRealTimeEvents();
                return;
            }

            if (actualState != ConnectionState.CONNECTED) {
                Log.w(TAG, "Cannot subscribe, Pusher not connected. Actual state: " + actualState);
                return;
            }
        }

        if (privateChannel != null) {
            if (!Objects.equals(privateChannel.getName(), channelName) || currentPusherState == PusherState.ERROR) {
                try {
                    if (newMessageEventListener != null) {
                        privateChannel.unbind("new-message", newMessageEventListener);
                        newMessageEventListener = null;
                        Log.d(TAG, "Unbound existing new-message listener.");
                    }
                    if (messageStatusEventListener != null) {
                        privateChannel.unbind("message-status-updated", messageStatusEventListener);
                        messageStatusEventListener = null;
                        Log.d(TAG, "Unbound existing message-status-updated listener.");
                    }
                    if (typingEventListener != null) {
                        privateChannel.unbind("client-typing-event", typingEventListener);
                        typingEventListener = null;
                        Log.d(TAG, "Unbound existing client-typing-event listener.");
                    }

                    pusher.unsubscribe(privateChannel.getName());
                    Log.d(TAG, "Unsubscribed from previous or potentially problematic channel: " + privateChannel.getName());
                    privateChannel = null;
                } catch (Exception e) {
                    Log.w(TAG, "Error unsubscribing existing channel: " + e.getMessage());
                }
            } else if (Objects.equals(privateChannel.getName(), channelName)) {
                Log.d(TAG, "Channel is already subscribed. Re-binding events.");
                bindRealTimeEvents();
                synchronized (pusherLock) {
                    currentPusherState = PusherState.SUBSCRIBED;
                }
                return;
            }
        }

        if (privateChannel == null) {
            subscribeToChannel(channelName);
        }
    }

    private void subscribeToChannel(String channelName) {
        try {
            Log.d(TAG, "Attempting to subscribe to channel: " + channelName);

            privateChannel = pusher.subscribePrivate(channelName, new PrivateChannelEventListener() {
                @Override
                public void onSubscriptionSucceeded(String channelName) {
                    Log.d(TAG, "Successfully subscribed to: " + channelName);

                    synchronized (pusherLock) {
                        currentPusherState = PusherState.SUBSCRIBED;
                    }

                    // --- New: Update UI for subscribed state ---
                    updateConnectionStatusUI(PusherState.SUBSCRIBED);
                    // --- End New ---

                    mainHandler.post(() -> {
                        if (!isDestroyed) {
                            Toast.makeText(ChatActivity.this, "Connected to real-time chat", Toast.LENGTH_SHORT).show();
                            bindRealTimeEvents();
                        }
                    });
                }

                @Override
                public void onAuthenticationFailure(String message, Exception e) {
                    Log.e(TAG, "Auth failed: " + message, e);

                    synchronized (pusherLock) {
                        currentPusherState = PusherState.ERROR;
                    }

                    // --- New: Update UI for auth failure (treated as error) ---
                    updateConnectionStatusUI(PusherState.ERROR);
                    // --- End New ---

                    mainHandler.post(() -> {
                        if (!isDestroyed) {
                            Toast.makeText(ChatActivity.this, "Authentication failed", Toast.LENGTH_LONG).show();
                            if (message.contains("401")) {
                                redirectToLogin();
                            }
                        }
                    });
                }

                @Override
                public void onEvent(PusherEvent event) {
                    Log.d(TAG, "Generic channel event: " + event.getEventName());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error subscribing to channel: " + e.getMessage(), e);
            synchronized (pusherLock) {
                currentPusherState = PusherState.ERROR;
            }
        }
    }


    private void bindRealTimeEvents() {
        if (privateChannel == null || !privateChannel.isSubscribed()) {
            Log.w(TAG, "Cannot bind events: channel not ready");
            return;
        }

        if (newMessageEventListener == null) {
            newMessageEventListener = new PrivateChannelEventListener() {
                @Override
                public void onSubscriptionSucceeded(String channelName) {
                    Log.d(TAG, "Successfully bound new-message event to channel: " + channelName);
                }

                @Override
                public void onAuthenticationFailure(String message, Exception e) {
                    Log.e(TAG, "Authentication failure in new-message event listener: " + message, e);
                }

                @Override
                public void onEvent(PusherEvent event) {
                    if ("new-message".equals(event.getEventName())) {
                        Log.d(TAG, "Received new message: " + event.getData());
                        mainHandler.post(() -> {
                            if (!isDestroyed) {
                                handleRealTimeMessage(event.getData());
                            }
                        });
                    }
                }
            };
            privateChannel.bind("new-message", newMessageEventListener);
            Log.d(TAG, "Bound new-message event");
        }


        if (messageStatusEventListener == null) {
            messageStatusEventListener = new PrivateChannelEventListener() {
                @Override
                public void onSubscriptionSucceeded(String channelName) {
                    Log.d(TAG, "Successfully bound message-status-updated event to channel: " + channelName);
                }

                @Override
                public void onAuthenticationFailure(String message, Exception e) {
                    Log.e(TAG, "Authentication failure in message-status-updated event listener: " + message, e);
                }

                @Override
                public void onEvent(PusherEvent event) {
                    if ("message-status-updated".equals(event.getEventName())) {
                        Log.d(TAG, "Received message status update: " + event.getData());
                        mainHandler.post(() -> {
                            if (!isDestroyed) {
                                handleMessageStatusUpdate(event.getData());
                            }
                        });
                    }
                }
            };
            privateChannel.bind("message-status-updated", messageStatusEventListener);
            Log.d(TAG, "Bound message-status-updated event");
        }

        if (typingEventListener == null) {
            typingEventListener = new PrivateChannelEventListener() {
                @Override
                public void onSubscriptionSucceeded(String channelName) {
                    Log.d(TAG, "Successfully bound client-typing-event to channel: " + channelName);
                }

                @Override
                public void onAuthenticationFailure(String message, Exception e) {
                    Log.e(TAG, "Authentication failure in typing event listener: " + message, e);
                }

                @Override
                public void onEvent(PusherEvent event) {
                    if ("client-typing-event".equals(event.getEventName())) {
                        Log.d(TAG, "Received typing event: " + event.getData());
                        mainHandler.post(() -> {
                            if (!isDestroyed) {
                                handleTypingEvent(event.getData());
                            }
                        });
                    }
                }
            };
            privateChannel.bind("client-typing-event", typingEventListener);
            Log.d(TAG, "Bound client-typing-event");
        }
    }

    private void cleanupPusherResources() {
        try {
            mainHandler.removeCallbacksAndMessages(null);
            typingHandler.removeCallbacksAndMessages(null);

            if (privateChannel != null) {
                try {
                    if (newMessageEventListener != null) {
                        privateChannel.unbind("new-message", newMessageEventListener);
                        newMessageEventListener = null;
                        Log.d(TAG, "Unbound new-message event listener.");
                    }
                    if (messageStatusEventListener != null) {
                        privateChannel.unbind("message-status-updated", messageStatusEventListener);
                        messageStatusEventListener = null;
                        Log.d(TAG, "Unbound message-status-updated event listener.");
                    }
                    if (typingEventListener != null) {
                        privateChannel.unbind("client-typing-event", typingEventListener);
                        typingEventListener = null;
                        Log.d(TAG, "Unbound client-typing-event listener.");
                    }
                    pusher.unsubscribe(privateChannel.getName());
                    Log.d(TAG, "Unsubscribed from channel: " + privateChannel.getName());
                } catch (Exception e) {
                    Log.w(TAG, "Error unsubscribing or unbinding: " + e.getMessage());
                }
                privateChannel = null;
            }

            if (pusher != null) {
                ConnectionState state = pusher.getConnection().getState();
                if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) {
                    pusher.disconnect();
                }
                pusher = null;
            }

            synchronized (pusherLock) {
                currentPusherState = PusherState.DISCONNECTED;
            }

            // --- New: Update UI to disconnected state after cleanup ---
            updateConnectionStatusUI(PusherState.DISCONNECTED);
            // --- End New ---

            Log.d(TAG, "Pusher resources cleaned up");

        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup: " + e.getMessage(), e);
        }
    }

    private static class RealTimeMessageStatusUpdateEvent {
        @SerializedName("id")
        String id;
        @SerializedName("client_message_id")
        String clientMessageId;
        @SerializedName("status")
        String status;
        @SerializedName("read_by_count")
        int readByCount;
    }

    private void handleMessageStatusUpdate(String jsonData) {
        if (isDestroyed) {
            Log.d(TAG, "Activity destroyed, ignoring real-time status update");
            return;
        }

        try {
            RealTimeMessageStatusUpdateEvent statusUpdate = gson.fromJson(jsonData, RealTimeMessageStatusUpdateEvent.class);

            MessageResponse messageToUpdate = null;
            for (MessageResponse msg : messageList) {
                boolean isOwnMessage = msg.getSenderId().equals(currentUserId);
                boolean idMatches = (statusUpdate.id != null && Objects.equals(msg.getId(), statusUpdate.id));
                boolean clientidMatches = (statusUpdate.clientMessageId != null && Objects.equals(msg.getClientMessageId(), statusUpdate.clientMessageId));

                if (isOwnMessage && (idMatches || clientidMatches)) {
                    messageToUpdate = msg;
                    break;
                }
            }

            if (messageToUpdate != null) {
                messageAdapter.updateMessageStatus(
                        statusUpdate.id,
                        statusUpdate.clientMessageId,
                        statusUpdate.status,
                        statusUpdate.readByCount
                );
                saveMessagesToLocalCache();
                Log.d(TAG, "Handled status update for message. ID: " + statusUpdate.id + ", ClientID: " + statusUpdate.clientMessageId + ", New Status: " + statusUpdate.status);
            } else {
                Log.d(TAG, "Message for status update not found or not own message: ID=" + statusUpdate.id + ", ClientID=" + statusUpdate.clientMessageId);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling real-time message status update: " + e.getMessage(), e);
            if (!isDestroyed) {
                runOnUiThread(() -> Toast.makeText(this, "Error processing real-time status update", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void handleRealTimeMessage(String jsonData) {
        if (isDestroyed) {
            Log.d(TAG, "Activity destroyed, ignoring real-time message");
            return;
        }

        try {
            RealTimeMessage realTimeMessage = gson.fromJson(jsonData, RealTimeMessage.class);

            if (realTimeMessage.getSenderId().equals(currentUserId)) {
                Log.d(TAG, "Ignoring own message from real-time (it should be updated via API response or status update event)");
                return;
            }

            boolean messageExists = false;
            for (MessageResponse existingMessage : messageList) {
                if (realTimeMessage.getId() != null && Objects.equals(existingMessage.getId(), realTimeMessage.getId())) {
                    messageExists = true;
                    Log.d(TAG, "Message with ID " + realTimeMessage.getId() + " already exists (from Pusher). Skipping.");
                    break;
                }
            }

            if (!messageExists) {
                addMessageToList(realTimeMessage);
                saveMessagesToLocalCache();

                rvMessages.post(() -> {
                    int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                    if (lastVisibleItemPosition == messageList.size() - 1) {
                        markMessagesAsRead(Collections.singletonList(convertToMessageResponse(realTimeMessage)));
                    }
                });
            } else {
                Log.d(TAG, "Message already exists, skipping duplicate from Pusher.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling real-time message: " + e.getMessage(), e);
            if (!isDestroyed) {
                runOnUiThread(() -> Toast.makeText(this, "Error processing real-time message", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void handleTypingEvent(String jsonData) {
        try {
            TypingEvent typingEvent = gson.fromJson(jsonData, TypingEvent.class);

            if (typingEvent.getUserId().equals(currentUserId)) {
                return;
            }

            if (typingEvent.isTyping()) {
                rvMessages.post(() -> {
                    if (layoutManager.findLastCompletelyVisibleItemPosition() < messageList.size() - 1) {
                        rvMessages.smoothScrollToPosition(messageList.size() - 1);
                    }
                    showTypingIndicator();
                });
            } else {
                hideTypingIndicator();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling typing event: " + e.getMessage());
        }
    }

    private void showTypingIndicator() {
        layoutTypingIndicator.setVisibility(View.VISIBLE);
        lottieTypingDots.playAnimation();
        tvChatStatus.setText("mengetik..."); // Tetap ubah status di header untuk typing

        typingHandler.removeCallbacksAndMessages(null);

        typingHandler.postDelayed(() -> {
            if (!isDestroyed) {
                hideTypingIndicator();
            }
        }, TYPING_TIMEOUT);
    }

    private void hideTypingIndicator() {
        layoutTypingIndicator.setVisibility(View.GONE);
        lottieTypingDots.pauseAnimation();
        // --- Penting: Setelah typing berhenti, kembalikan status header ke normal, tapi periksa juga status Pusher ---
        if (currentPusherState == PusherState.CONNECTED || currentPusherState == PusherState.SUBSCRIBED) {
            setupChatHeader(); // Hanya panggil ini jika Pusher terhubung
        } else {
            // Jika Pusher tidak terhubung, biarkan updateConnectionStatusUI yang menentukan teks status
            updateConnectionStatusUI(currentPusherState);
        }
        typingHandler.removeCallbacksAndMessages(null);
    }

    private void setupMessageInputListener() {
        etMessageInput.addTextChangedListener(new TextWatcher() {
            private Runnable typingStoppedRunnable;
            private boolean wasTyping = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean isCurrentlyTyping = !s.toString().trim().isEmpty();

                if (isCurrentlyTyping) {
                    fabSend.setImageResource(R.drawable.ic_send);
                } else {
                    fabSend.setImageResource(R.drawable.ic_mic);
                }

                if (isCurrentlyTyping && !wasTyping) {
                    sendTypingEvent(true);
                    wasTyping = true;
                } else if (!isCurrentlyTyping && wasTyping) {
                    sendTypingEvent(false);
                    wasTyping = false;
                }

                if (isCurrentlyTyping) {
                    if (typingStoppedRunnable != null) {
                        typingHandler.removeCallbacks(typingStoppedRunnable);
                    }
                    typingStoppedRunnable = () -> {
                        sendTypingEvent(false);
                        wasTyping = false;
                    };
                    typingHandler.postDelayed(typingStoppedRunnable, TYPING_TIMEOUT);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void sendTypingEvent(boolean isTyping) {
        if (privateChannel == null) {
            Log.w(TAG, "Cannot send typing event: privateChannel is null");
            return;
        }

        if (!privateChannel.isSubscribed()) {
            Log.w(TAG, "Cannot send typing event: channel not subscribed");
            return;
        }

        String eventName = "client-typing-event";
        TypingEvent typingEvent = new TypingEvent(currentUserId, isTyping);
        String eventData = gson.toJson(typingEvent);

        try {
            privateChannel.trigger(eventName, eventData);
            Log.d(TAG, "Sent typing event: " + eventData);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send typing event: " + e.getMessage());
        }
    }

    private Authorizer createAuthorizer(String authEndpoint) {
        return new Authorizer() {
            @Override
            public String authorize(String channelName, String socketId) throws AuthorizationFailureException {
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
                            throw new AuthorizationFailureException("Auth failed: " + response.code() + " " + (response.body() != null ? response.body().string() : ""));
                        }
                    }
                } catch (IOException e) {
                    throw new AuthorizationFailureException("Network error: " + e.getMessage());
                }
            }
        };
    }

    private void addMessageToList(RealTimeMessage realTimeMessage) {
        MessageResponse newMessage = convertToMessageResponse(realTimeMessage);
        messageList.add(newMessage);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        rvMessages.scrollToPosition(messageList.size() - 1);
        Log.d(TAG, "Added new message via Pusher: " + newMessage.getContent());
    }

    private MessageResponse convertToMessageResponse(RealTimeMessage realTimeMessage) {
        return new MessageResponse(
                realTimeMessage.getId(),
                realTimeMessage.getConversationId(),
                realTimeMessage.getSenderId(),
                realTimeMessage.getSenderUsername(),
                realTimeMessage.getSenderAvatar(),
                realTimeMessage.getContent(),
                realTimeMessage.getMessageType(),
                realTimeMessage.getStatus(),
                realTimeMessage.isEdited(),
                realTimeMessage.isDeleted(),
                realTimeMessage.getSentAt(),
                null,
                null,
                null,
                0,
                realTimeMessage.getClientMessageId()
        );
    }


    private void setupChatHeader() {
        if (isGroupChat) {
            fetchConversationDetails(conversationId);
        } else {
            if (otherParticipantUsername != null && !otherParticipantUsername.isEmpty()) {
                tvChatName.setText(otherParticipantUsername);
            } else {
                tvChatName.setText("Private Chat");
            }

            if (otherParticipantAvatar != null && !otherParticipantAvatar.isEmpty()) {
                Glide.with(this)
                        .load(otherParticipantAvatar)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .into(ivChatProfilePicture);
            } else {
                ivChatProfilePicture.setImageResource(R.drawable.default_avatar);
            }
            tvChatStatus.setText("Offline"); // Default status for private chat until fetched
        }

        LinearLayout chatHeaderInfo = findViewById(R.id.chat_header_info);
        if (chatHeaderInfo != null) {
            chatHeaderInfo.setOnClickListener(v -> showConversationDetails());
        }
    }

    private void fetchConversationDetails(String convId) {
        ConversationApiService apiService = RetrofitClient.getConversationApiService();
        retrofit2.Call<ConversationResponse> call = apiService.getConversation("Bearer " + accessToken, convId);

        call.enqueue(new Callback<ConversationResponse>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<ConversationResponse> call, @NonNull Response<ConversationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ConversationResponse conversation = response.body();
                    tvChatName.setText(conversation.getName());
                    if (conversation.getAvatar() != null && !conversation.getAvatar().isEmpty()) {
                        Glide.with(ChatActivity.this)
                                .load(conversation.getAvatar())
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .into(ivChatProfilePicture);
                    } else {
                        ivChatProfilePicture.setImageResource(R.drawable.default_avatar);
                    }
                    if (conversation.getParticipants() != null) {
                        tvChatStatus.setText(conversation.getParticipants().size() + " members");
                    } else {
                        tvChatStatus.setText("Group Chat");
                    }
                } else {
                    Log.e(TAG, "Failed to fetch conversation details: " + response.code());
                    Toast.makeText(ChatActivity.this, "Failed to load conversation details.", Toast.LENGTH_SHORT).show();
                    tvChatName.setText(isGroupChat ? "Group Chat" : otherParticipantUsername);
                    tvChatStatus.setText("");
                }
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<ConversationResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching conversation details: " + t.getMessage());
                Toast.makeText(ChatActivity.this, "Network error fetching conversation details.", Toast.LENGTH_SHORT).show();
                tvChatName.setText(isGroupChat ? "Group Chat" : otherParticipantUsername);
                tvChatStatus.setText("");
            }
        });
    }

    private void showConversationDetails() {
        if (!isGroupChat) {
            Intent intent = new Intent(ChatActivity.this, DetailChatPribadiActivity.class);
            intent.putExtra("user_id", otherParticipantId);
            intent.putExtra("username", otherParticipantUsername);
            intent.putExtra("avatar_url", otherParticipantAvatar);
            intent.putExtra("conversation_id", conversationId);
            startActivity(intent);
        } else {
            Intent intent = new Intent(ChatActivity.this, GroupDetailsActivity.class);
            intent.putExtra("conversation_id", conversationId);
            startActivity(intent);
        }
    }

    private void loadMessages(int page, int perPage, String beforeMessageId) {
        isLoading = true;

        MessageApiService apiService = RetrofitClient.getMessageApiService();
        Call<MessagesResponse> call = apiService.getMessagesInConversation(
                "Bearer " + accessToken,
                conversationId,
                page,
                perPage,
                beforeMessageId
        );

        call.enqueue(new Callback<MessagesResponse>() {
            @Override
            public void onResponse(@NonNull Call<MessagesResponse> call, @NonNull Response<MessagesResponse> response) {
                isLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    List<MessageResponse> fetchedMessages = response.body().getMessages();

                    Collections.reverse(fetchedMessages);

                    if (beforeMessageId == null) {
                        messageList.clear();
                        messageList.addAll(fetchedMessages);
                        messageAdapter.notifyDataSetChanged();
                        if (page == 1) {
                            rvMessages.scrollToPosition(messageList.size() - 1);
                        }
                    } else {
                        messageList.addAll(0, fetchedMessages);
                        messageAdapter.notifyItemRangeInserted(0, fetchedMessages.size());
                        layoutManager.scrollToPositionWithOffset(fetchedMessages.size(), 0);
                    }
                    currentPage = response.body().getPage();
                    hasMoreMessages = response.body().hasMore();

                    markMessagesAsRead(fetchedMessages);

                    saveMessagesToLocalCache();

                } else {
                    String errorMessage = "Failed to load messages: Unknown error";
                    if (response.errorBody() != null) {
                        try {
                            errorMessage += " " + response.errorBody().string();
                        } catch (IOException e) {
                            Log.e(TAG, "Error parsing error body: " + e.getMessage());
                        }
                    }
                    Log.e(TAG, "Failed to load messages: " + response.code() + " - " + errorMessage);
                    Toast.makeText(ChatActivity.this, "Error loading messages: " + errorMessage, Toast.LENGTH_SHORT).show();
                    if (response.code() == 401) {
                        redirectToLogin();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<MessagesResponse> call, @NonNull Throwable t) {
                isLoading = false;
                Log.e(TAG, "Error fetching messages: " + t.getMessage(), t);
                Toast.makeText(ChatActivity.this, "Network error loading messages. Displaying cached data if available.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String content) {
        if (isDestroyed) {
            Log.w(TAG, "Cannot send message: Activity is destroyed");
            return;
        }

        String clientMessageId = UUID.randomUUID().toString();
        MessageCreate messageCreate = new MessageCreate(
                content,
                "text",
                null,
                conversationId,
                clientMessageId
        );

        String senderUsernameForTemp = "You";

        MessageResponse tempMessage = new MessageResponse(
                null,
                conversationId,
                currentUserId,
                senderUsernameForTemp,
                null,
                content,
                "text",
                "sending",
                false,
                false,
                new Date(),
                null,
                null,
                null,
                0,
                clientMessageId
        );

        messageList.add(tempMessage);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        rvMessages.scrollToPosition(messageList.size() - 1);
        messageAdapter.logAllMessageStatuses();

        saveMessagesToLocalCache();


        MessageApiService apiService = RetrofitClient.getMessageApiService();
        retrofit2.Call<MessageResponse> call = apiService.createMessage(
                "Bearer " + accessToken,
                messageCreate
        );

        call.enqueue(new retrofit2.Callback<MessageResponse>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<MessageResponse> call, @NonNull retrofit2.Response<MessageResponse> response) {
                if (isDestroyed) {
                    Log.d(TAG, "Activity destroyed, ignoring send message response");
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    MessageResponse serverMessage = response.body();
                    for (int i = 0; i < messageList.size(); i++) {
                        MessageResponse msg = messageList.get(i);
                        if (Objects.equals(msg.getClientMessageId(), clientMessageId)) {
                            msg.setId(serverMessage.getId());
                            msg.setStatus(serverMessage.getStatus() != null ? serverMessage.getStatus() : "sent");
                            msg.setSentAt(serverMessage.getSentAt());
                            msg.setReadByCount(serverMessage.getReadByCount());
                            messageAdapter.notifyItemChanged(i);
                            Log.d(TAG, "Message sent successfully. Server ID: " + msg.getId() + ", Client ID: " + msg.getClientMessageId() + ", New Status: " + msg.getStatus());
                            break;
                        }
                    }
                    saveMessagesToLocalCache();
                } else {
                    String errorMsg = "Failed to send message.";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing error body: " + e.getMessage());
                    }
                    Log.e(TAG, "Failed to send message: " + response.code() + " - " + errorMsg);

                    String finalErrorMsg = errorMsg;
                    runOnUiThread(() -> {
                        if (!isDestroyed) {
                            Toast.makeText(ChatActivity.this, "Failed to send message: " + finalErrorMsg, Toast.LENGTH_SHORT).show();
                        }
                    });

                    updateMessageStatusLocally(null, clientMessageId, "failed", 0);
                    saveMessagesToLocalCache();
                    if (response.code() == 401) {
                        redirectToLogin();
                    }
                }
                messageAdapter.logAllMessageStatuses();
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<MessageResponse> call, @NonNull Throwable t) {
                if (isDestroyed) {
                    Log.d(TAG, "Activity destroyed, ignoring send message failure");
                    return;
                }

                Log.e(TAG, "Error sending message: " + t.getMessage(), t);
                runOnUiThread(() -> {
                    if (!isDestroyed) {
                        Toast.makeText(ChatActivity.this, "Network error sending message: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

                updateMessageStatusLocally(null, clientMessageId, "failed", 0);
                saveMessagesToLocalCache();
                messageAdapter.logAllMessageStatuses();
            }
        });
    }

    private void updateMessageStatusLocally(String messageId, String clientMessageId, String status, int readByCount) {
        messageAdapter.updateMessageStatus(messageId, clientMessageId, status, readByCount);
        saveMessagesToLocalCache();
    }


    private void markMessagesAsRead(List<MessageResponse> messagesToMark) {
        List<String> unreadMessageIds = new ArrayList<>();
        for (MessageResponse msg : messagesToMark) {
            if (!msg.getSenderId().equals(currentUserId) && !"read".equalsIgnoreCase(msg.getStatus())) {
                unreadMessageIds.add(msg.getId());
                Log.d(TAG, "Candidate for marking as read: " + msg.getId() + " from sender: " + msg.getSenderId());
            }
        }

        if (!unreadMessageIds.isEmpty()) {
            markMessagesAsReadInternal(unreadMessageIds);
        } else {
            Log.d(TAG, "No unread messages from others to mark.");
        }
    }

    private void markMessagesAsReadInternal(List<String> messageIdsToMark) {
        if (messageIdsToMark.isEmpty()) {
            Log.d(TAG, "No messages to mark as read.");
            return;
        }

        Log.d(TAG, "Marking " + messageIdsToMark.size() + " messages as read on server.");

        MessageReadReceiptUpdate readReceipt = new MessageReadReceiptUpdate(messageIdsToMark);
        MessageApiService apiService = RetrofitClient.getMessageApiService();
        Call<Void> call = apiService.markMessagesAsRead(
                "Bearer " + accessToken,
                conversationId,
                readReceipt
        );

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Messages marked as read successfully on server.");

                    for (int i = 0; i < messageList.size(); i++) {
                        MessageResponse msg = messageList.get(i);
                        if (messageIdsToMark.contains(msg.getId()) && !msg.getSenderId().equals(currentUserId)) {
                            updateMessageStatusLocally(msg.getId(), msg.getClientMessageId(), "read", msg.getReadByCount());
                        }
                    }
                    saveMessagesToLocalCache();

                    Intent readReceiptIntent = new Intent("com.example.mychat.CONVERSATION_READ");
                    readReceiptIntent.putExtra("conversation_id", conversationId);
                    LocalBroadcastManager.getInstance(ChatActivity.this).sendBroadcast(readReceiptIntent);
                } else {
                    Log.e(TAG, "Failed to mark messages as read on server: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error reading error body: " + errorBody);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e(TAG, "Error marking messages as read on server: " + t.getMessage(), t);
            }
        });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        SharedPreferences chatCachePrefs = getSharedPreferences("ChatCache", Context.MODE_PRIVATE);
        chatCachePrefs.edit().clear().apply();
    }

    private String getAccessToken(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("access_token", null);
    }

    private String getCurrentUserId(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE);
        return sharedPreferences.getString("user_id", null);
    }

    private void startConnectionMonitoring() {
        Handler connectionMonitor = new Handler(Looper.getMainLooper());
        Runnable connectionCheck = new Runnable() {
            @Override
            public void run() {
                if (!isDestroyed && pusher != null) {
                    ConnectionState actualState = pusher.getConnection().getState();

                    synchronized (pusherLock) {
                        if ((actualState == ConnectionState.CONNECTED && currentPusherState == PusherState.DISCONNECTED) ||
                                (actualState == ConnectionState.DISCONNECTED && currentPusherState == PusherState.CONNECTED)) {
                            Log.w(TAG, "State mismatch detected! Actual: " + actualState + ", Internal: " + currentPusherState);

                            if (actualState == ConnectionState.CONNECTED) {
                                ensureChannelSubscription();
                            }
                        }
                    }

                    connectionMonitor.postDelayed(this, 30000);
                }
            }
        };
    }

    private void loadMessagesLocally() {
        String cacheKey = CACHE_KEY_MESSAGES_PREFIX + conversationId;
        String messagesJson = sharedPreferences.getString(cacheKey, null);
        if (messagesJson != null) {
            try {
                Type type = new TypeToken<List<MessageResponse>>() {}.getType();
                List<MessageResponse> cachedMessages = gson.fromJson(messagesJson, type);
                if (cachedMessages != null && !cachedMessages.isEmpty()) {
                    messageList.clear();
                    messageList.addAll(cachedMessages);
                    messageAdapter.notifyDataSetChanged();
                    rvMessages.scrollToPosition(messageList.size() - 1);
                    Log.d(TAG, "Loaded " + cachedMessages.size() + " messages from local cache for conversation: " + conversationId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing cached messages for conversation " + conversationId + ": " + e.getMessage());
                sharedPreferences.edit().remove(cacheKey).apply();
            }
        } else {
            Log.d(TAG, "No cached messages found for conversation: " + conversationId);
        }
    }

    private void saveMessagesToLocalCache() {
        String cacheKey = CACHE_KEY_MESSAGES_PREFIX + conversationId;
        String messagesJson = gson.toJson(messageList);
        sharedPreferences.edit().putString(cacheKey, messagesJson).apply();
        Log.d(TAG, "Messages saved to local cache for conversation: " + conversationId + ". Total: " + messageList.size());
    }

    private void updateConnectionStatusUI(PusherState state) {
        if (isDestroyed) return;

        runOnUiThread(() -> {
            switch (state) {
                case CONNECTED:
                case SUBSCRIBED:
                    // Jika sudah terhubung penuh, kembalikan tvChatStatus ke status aslinya
                    // Panggil setupChatHeader untuk me-reset ke status default (online/offline/member count)
                    setupChatHeader();
                    // Pastikan warnanya kembali ke warna teks utama dari layout
                    tvChatStatus.setTextColor(ContextCompat.getColor(this, R.color.primary_text));
                    break;
                case CONNECTING:
                case RECONNECTING:
                    tvChatStatus.setText("Connecting...");
                    // Gunakan warna yang mencolok untuk connecting, sesuai design Anda
                    tvChatStatus.setTextColor(ContextCompat.getColor(this, R.color.primary_color));
                    break;
                case DISCONNECTED:
                    tvChatStatus.setText("Disconnected");
                    // Gunakan warna yang mencolok untuk disconnected
                    tvChatStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_color));
                    break;
                case ERROR:
                    tvChatStatus.setText("Connection error");
                    // Gunakan warna yang mencolok untuk error
                    tvChatStatus.setTextColor(ContextCompat.getColor(this, R.color.red_500));
                    break;
                default:
                    // Dalam kasus default atau state yang tidak dikenal, kembalikan ke header chat normal
                    setupChatHeader();
                    tvChatStatus.setTextColor(ContextCompat.getColor(this, R.color.primary_text));
                    break;
            }
        });
    }

}