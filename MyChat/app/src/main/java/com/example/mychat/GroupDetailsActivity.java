package com.example.mychat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mychat.conversation_service.ConversationApiService;
import com.example.mychat.conversation_service.FileApiService;
import com.example.mychat.models_conversation.ConversationResponse;
import com.example.mychat.models_conversation.ConversationUpdate;
import com.example.mychat.models_conversation.ParticipantAdd;
import com.example.mychat.models_conversation.ParticipantMuteUpdate;
import com.example.mychat.models_conversation.ParticipantResponse;
import com.example.mychat.models_conversation.ParticipantUpdate;
import com.example.mychat.models_file.FileUploadResponse;
import com.example.mychat.retrofit_client.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson; // Import Gson
import com.google.gson.reflect.TypeToken; // Import TypeToken

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type; // Import Type
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroupDetailsActivity extends AppCompatActivity implements ParticipantAdapter.OnParticipantClickListener, AddMembersDialogFragment.AddMembersListener {

    private static final String TAG = "GroupDetailsActivity";

    // UI elements
    private ImageView ivGroupAvatar;
    private ImageView image_view_notification;
    private TextView tvGroupNameDisplay;
    private TextView tvGroupMembersCount;
    private TextView tvGroupDescription;
    private ImageView ivEditGroup;
    private RecyclerView recyclerViewParticipants;
    private LinearLayout layoutAddMember;
    private LinearLayout actionSearch;
    private LinearLayout actionNotifications;
    private LinearLayout actionMedia;
    private LinearLayout actionFiles;
    private LinearLayout layoutLeaveGroup;
    private LinearLayout layoutDeleteGroup;
    private ImageButton btnBack;
    private ImageButton btnMoreOptions;

    // Data
    private String conversationId;
    private String currentUserId;
    private String accessToken;
    private ConversationResponse currentConversation; // Untuk menyimpan data grup yang sedang dilihat
    private ParticipantResponse currentUserParticipant; // Data partisipan user saat ini
    private List<ParticipantResponse> participantsList = new ArrayList<>();
    private ParticipantAdapter participantAdapter;

    // Variables for image picking/uploading
    private static final int PICK_IMAGE_REQUEST = 101;
    private static final int CAPTURE_IMAGE_REQUEST = 102;
    private Uri tempImageUri; // Untuk menyimpan URI sementara jika menggunakan kamera
    private String selectedAvatarUrl; // Untuk menyimpan URL avatar yang berhasil diupload atau URL lama

    // References to dialog views (to update them after image selection)
    private ImageView dialogGroupAvatarImageView;
    private AlertDialog editGroupDialog; // Reference to the dialog itself

    // --- Caching Variables ---
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private static final String CACHE_KEY_GROUP_DETAILS_PREFIX = "cached_group_details_"; // Cache key untuk detail grup
    // --- End Caching Variables ---

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        // Get data from Intent
        Intent intent = getIntent();
        conversationId = intent.getStringExtra("conversation_id");

        if (conversationId == null) {
            Toast.makeText(this, "ID percakapan tidak ditemukan.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        accessToken = getAccessToken(this);
        currentUserId = getCurrentUserId(this);

        if (accessToken == null || currentUserId == null) {
            Toast.makeText(this, "Autentikasi gagal. Silakan masuk lagi.", Toast.LENGTH_LONG).show();
            redirectToLogin();
            return;
        }

        // --- Caching: Inisialisasi SharedPreferences dan Gson ---
        sharedPreferences = getSharedPreferences("GroupDetailsCache", Context.MODE_PRIVATE);
        gson = new Gson();
        // --- End Caching Initialization ---

        // Initialize UI components
        ivGroupAvatar = findViewById(R.id.iv_group_avatar);
        tvGroupNameDisplay = findViewById(R.id.tv_group_name_display);
        tvGroupMembersCount = findViewById(R.id.tv_group_members_count);
        tvGroupDescription = findViewById(R.id.tv_group_description);
        ivEditGroup = findViewById(R.id.iv_edit_group);
        recyclerViewParticipants = findViewById(R.id.recycler_view_participants);
        layoutAddMember = findViewById(R.id.layout_add_member);
        actionSearch = findViewById(R.id.action_search);
        actionNotifications = findViewById(R.id.action_notifications);
        actionMedia = findViewById(R.id.action_media);
        actionFiles = findViewById(R.id.action_files);
        layoutLeaveGroup = findViewById(R.id.layout_leave_group);
        layoutDeleteGroup = findViewById(R.id.layout_delete_group);
        btnBack = findViewById(R.id.btn_back_settings);
        btnMoreOptions = findViewById(R.id.btn_more_options);
        image_view_notification = findViewById(R.id.image_view_notification_icon);

        // Setup Participants RecyclerView
        participantAdapter = new ParticipantAdapter(this, participantsList, currentUserId, this);
        recyclerViewParticipants.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewParticipants.setAdapter(participantAdapter);

        setupListeners();

        // --- Caching: Load data from local cache first ---
        loadDataLocally();
        // --- End Caching Load Local ---

        // Fetch group details from API
        fetchGroupDetails();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
        btnMoreOptions.setOnClickListener(v -> Toast.makeText(this, "Opsi lainnya belum diimplementasikan", Toast.LENGTH_SHORT).show());

        ivEditGroup.setOnClickListener(v -> showEditGroupDialog());
        layoutAddMember.setOnClickListener(v -> showAddMembersDialog());
        layoutLeaveGroup.setOnClickListener(v -> showLeaveGroupConfirmation());
        layoutDeleteGroup.setOnClickListener(v -> showDeleteGroupConfirmation());

        // Quick Actions
        actionSearch.setOnClickListener(v -> Toast.makeText(this, "Cari media & pesan belum diimplementasikan", Toast.LENGTH_SHORT).show());
        actionNotifications.setOnClickListener(v -> toggleGroupNotifications());
        actionMedia.setOnClickListener(v -> Toast.makeText(this, "Galeri media grup belum diimplementasikan", Toast.LENGTH_SHORT).show());
        actionFiles.setOnClickListener(v -> Toast.makeText(this, "File bersama grup belum diimplementasikan", Toast.LENGTH_SHORT).show());
    }

    // --- Caching: New method to load data from SharedPreferences ---
    private void loadDataLocally() {
        String groupDetailsJson = sharedPreferences.getString(CACHE_KEY_GROUP_DETAILS_PREFIX + conversationId, null);
        if (groupDetailsJson != null) {
            try {
                ConversationResponse cachedConversation = gson.fromJson(groupDetailsJson, ConversationResponse.class);
                if (cachedConversation != null) {
                    currentConversation = cachedConversation;
                    updateUIWithGroupDetails(); // Update UI with cached data
                    Log.d(TAG, "Group details loaded from local cache: " + currentConversation.getName());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing cached group details: " + e.getMessage());
                sharedPreferences.edit().remove(CACHE_KEY_GROUP_DETAILS_PREFIX + conversationId).apply(); // Clear corrupted cache
            }
        }
    }
    // --- End Caching: loadDataLocally ---


    private void fetchGroupDetails() {
        ConversationApiService apiService = RetrofitClient.getConversationApiService();
        Call<ConversationResponse> call = apiService.getConversation("Bearer " + accessToken, conversationId);

        call.enqueue(new Callback<ConversationResponse>() {
            @Override
            public void onResponse(@NonNull Call<ConversationResponse> call, @NonNull Response<ConversationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentConversation = response.body();
                    updateUIWithGroupDetails();
                    // --- Caching: Save fetched group details to local cache ---
                    sharedPreferences.edit().putString(CACHE_KEY_GROUP_DETAILS_PREFIX + conversationId, gson.toJson(currentConversation)).apply();
                    Log.d(TAG, "Group details fetched from API and cached.");
                    // --- End Caching Save ---
                } else {
                    Log.e(TAG, "Failed to fetch group details: " + response.code());
                    Toast.makeText(GroupDetailsActivity.this, "Gagal memuat detail grup.", Toast.LENGTH_SHORT).show();
                    if (response.code() == 401) {
                        redirectToLogin();
                    } else {
                        // Jika gagal dari API, tetapi ada cache, tetap tampilkan cache.
                        // Jika tidak ada cache, atau cache rusak, activity mungkin kosong.
                        // finish(); // Jangan langsung finish jika ada kemungkinan data cache ditampilkan
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ConversationResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Error fetching group details: " + t.getMessage());
                Toast.makeText(GroupDetailsActivity.this, "Kesalahan jaringan saat memuat detail grup.", Toast.LENGTH_SHORT).show();
                // Data akan tetap menampilkan dari cache jika ada
                // finish(); // Jangan langsung finish jika ada kemungkinan data cache ditampilkan
            }
        });
    }

    private void updateUIWithGroupDetails() {
        if (currentConversation == null) return;

        // Group Header
        tvGroupNameDisplay.setText(currentConversation.getName());
        tvGroupDescription.setText(currentConversation.getDescription());
        tvGroupMembersCount.setText(currentConversation.getParticipants().size() + " anggota");

        if (currentConversation.getAvatar() != null && !currentConversation.getAvatar().isEmpty()) {
            Glide.with(this)
                    .load(currentConversation.getAvatar())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(ivGroupAvatar);
        } else {
            ivGroupAvatar.setImageResource(R.drawable.default_avatar);
        }

        // Determine current user's role and visibility of edit/add member/delete group
        currentUserParticipant = null;
        for (ParticipantResponse p : currentConversation.getParticipants()) {
            if (p.getUserId().equals(currentUserId)) {
                currentUserParticipant = p;
                break;
            }
        }

        if (currentUserParticipant != null) {
            updateNotificationIcon(currentUserParticipant.isMuted());
        }

        if (currentUserParticipant != null) {
            boolean isAdmin = "ADMIN".equalsIgnoreCase(currentUserParticipant.getRole());
            boolean isModerator = "MODERATOR".equalsIgnoreCase(currentUserParticipant.getRole());

            ivEditGroup.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
            layoutAddMember.setVisibility(isAdmin || isModerator ? View.VISIBLE : View.GONE);
            layoutDeleteGroup.setVisibility(isAdmin && currentConversation.getCreatedBy().equals(currentUserId) ? View.VISIBLE : View.GONE);

            // Hide leave group if current user is the only admin AND the creator
            boolean canLeave = true;
            if (isAdmin) {
                long adminCount = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    adminCount = currentConversation.getParticipants().stream()
                            .filter(p -> "ADMIN".equalsIgnoreCase(p.getRole()))
                            .count();
                }
                if (adminCount == 1 && currentConversation.getCreatedBy().equals(currentUserId)) {
                    canLeave = false; // Cannot leave if you are the sole admin and creator
                }
            }
            layoutLeaveGroup.setVisibility(canLeave ? View.VISIBLE : View.GONE);

            // Update participant list
            participantsList.clear();
            // Sort participants: Admins first, then online users, then by username
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                currentConversation.getParticipants().sort(new Comparator<ParticipantResponse>() {
                    @Override
                    public int compare(ParticipantResponse p1, ParticipantResponse p2) {
                        // Admins first
                        boolean p1IsAdmin = "ADMIN".equalsIgnoreCase(p1.getRole());
                        boolean p2IsAdmin = "ADMIN".equalsIgnoreCase(p2.getRole());
                        if (p1IsAdmin && !p2IsAdmin) return -1;
                        if (!p1IsAdmin && p2IsAdmin) return 1;

                        // Then Moderators
                        boolean p1IsModerator = "MODERATOR".equalsIgnoreCase(p1.getRole());
                        boolean p2IsModerator = "MODERATOR".equalsIgnoreCase(p2.getRole());
                        if (p1IsModerator && !p2IsModerator) return -1;
                        if (!p1IsModerator && p2IsModerator) return 1;

                        // Then Online users
                        if (p1.isOnline() && !p2.isOnline()) return -1;
                        if (!p1.isOnline() && p2.isOnline()) return 1;

                        // Finally, by username
                        return p1.getUsername().compareToIgnoreCase(p2.getUsername());
                    }
                });
            }
            participantsList.addAll(currentConversation.getParticipants());
            participantAdapter.notifyDataSetChanged();
        } else {
            // Should not happen if user is authenticated and is a participant, but for safety:
            ivEditGroup.setVisibility(View.GONE);
            layoutAddMember.setVisibility(View.GONE);
            layoutDeleteGroup.setVisibility(View.GONE);
            layoutLeaveGroup.setVisibility(View.GONE); // Cannot leave if not a participant
            participantsList.clear();
            participantAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Anda bukan anggota grup ini.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateNotificationIcon(boolean isMuted) {
        if (image_view_notification != null) {
            if (isMuted) {
                image_view_notification.setImageResource(R.drawable.ic_notifications_off);
            } else {
                image_view_notification.setImageResource(R.drawable.ic_notifications);
            }
            image_view_notification.setColorFilter(ContextCompat.getColor(this, isMuted ? R.color.red_500 : R.color.blue_500));
        }
    }

    private void showEditGroupDialog() {
        if (currentConversation == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_group, null);
        builder.setView(dialogView);

        EditText etGroupName = dialogView.findViewById(R.id.et_group_name);
        EditText etGroupDescription = dialogView.findViewById(R.id.et_group_description);
        dialogGroupAvatarImageView = dialogView.findViewById(R.id.iv_dialog_group_avatar);
        MaterialButton btnSave = dialogView.findViewById(R.id.btn_save_group_changes);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel_group_changes);

        etGroupName.setText(currentConversation.getName());
        etGroupDescription.setText(currentConversation.getDescription());
        if (currentConversation.getAvatar() != null && !currentConversation.getAvatar().isEmpty()) {
            Glide.with(this).load(currentConversation.getAvatar()).into(dialogGroupAvatarImageView);
        } else {
            dialogGroupAvatarImageView.setImageResource(R.drawable.default_avatar);
        }

        selectedAvatarUrl = currentConversation.getAvatar();

        dialogGroupAvatarImageView.setOnClickListener(v -> showImagePickerDialog());

        editGroupDialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String newName = etGroupName.getText().toString().trim();
            String newDescription = etGroupDescription.getText().toString().trim();

            if (newName.isEmpty()) {
                Toast.makeText(this, "Nama grup tidak boleh kosong.", Toast.LENGTH_SHORT).show();
                return;
            }
            updateGroupDetails(newName, newDescription, selectedAvatarUrl);
            editGroupDialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> editGroupDialog.dismiss());
        editGroupDialog.show();
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this);
        pictureDialog.setTitle("Pilih Aksi");
        String[] pictureDialogItems = {"Pilih dari Galeri", "Ambil Foto dari Kamera"};
        pictureDialog.setItems(pictureDialogItems,
                (dialog, which) -> {
                    switch (which) {
                        case 0:
                            chooseImageFromGallery();
                            break;
                        case 1:
                            takePhotoFromCamera();
                            break;
                    }
                });
        pictureDialog.show();
    }

    private void chooseImageFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    private void takePhotoFromCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file: " + ex.getMessage());
                Toast.makeText(this, "Gagal membuat file gambar sementara", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                tempImageUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempImageUri);
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri imageUri = null;

            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                imageUri = data.getData();
                Log.d(TAG, "Image selected from gallery: " + imageUri.toString());
            } else if (requestCode == CAPTURE_IMAGE_REQUEST && tempImageUri != null) {
                imageUri = tempImageUri;
                Log.d(TAG, "Image captured from camera: " + imageUri.toString());
            }

            if (imageUri != null) {
                try {
                    if (dialogGroupAvatarImageView != null) {
                        Glide.with(this).load(imageUri).into(dialogGroupAvatarImageView);
                    }

                    uploadGroupAvatar(imageUri);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing selected image: " + e.getMessage(), e);
                    Toast.makeText(this, "Gagal memproses gambar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Image URI is null");
                Toast.makeText(this, "Gagal mendapatkan gambar", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void uploadGroupAvatar(Uri imageUri) {
        Toast.makeText(this, "Mengupload avatar grup...", Toast.LENGTH_LONG).show();

        try {
            File file = getFileFromUri(imageUri);
            if (file == null || !file.exists()) {
                Toast.makeText(this, "File gambar tidak ditemukan.", Toast.LENGTH_SHORT).show();
                return;
            }

            String mimeType = getMimeType(imageUri);
            if (mimeType == null) {
                mimeType = "image/jpeg";
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            FileApiService apiService = RetrofitClient.getFileApiService();
            Call<FileUploadResponse> call = apiService.uploadGroupAvatar("Bearer " + accessToken, body);

            call.enqueue(new Callback<FileUploadResponse>() {
                @Override
                public void onResponse(@NonNull Call<FileUploadResponse> call, @NonNull Response<FileUploadResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        selectedAvatarUrl = response.body().getFileUrl();
                        Toast.makeText(GroupDetailsActivity.this, "Avatar grup berhasil diupload!", Toast.LENGTH_SHORT).show();
                    } else {
                        String errorMsg = "Gagal mengupload avatar grup. Code: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                errorMsg += " - " + response.errorBody().string();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error parsing error body: " + e.getMessage());
                        }
                        Toast.makeText(GroupDetailsActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Upload avatar grup gagal: " + response.code() + " - " + errorMsg);
                        if (response.code() == 401) redirectToLogin();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<FileUploadResponse> call, @NonNull Throwable t) {
                    String errorMsg = "Kesalahan jaringan saat mengupload avatar grup: " + t.getMessage();
                    Toast.makeText(GroupDetailsActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error uploading group avatar", t);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error preparing file for upload: " + e.getMessage(), e);
            Toast.makeText(this, "Gagal memproses file untuk upload: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private File getFileFromUri(Uri uri) {
        try {
            if ("file".equals(uri.getScheme())) {
                return new File(uri.getPath());
            }

            if ("content".equals(uri.getScheme())) {
                return createTempFileFromUri(uri);
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting file from URI: " + e.getMessage(), e);
            return null;
        }
    }

    private File createTempFileFromUri(Uri uri) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "temp_avatar_" + timeStamp + ".jpg";
            File tempFile = new File(getCacheDir(), fileName);

            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }

            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "Error creating temp file from URI: " + e.getMessage(), e);
            return null;
        }
    }

    private String getMimeType(Uri uri) {
        try {
            return getContentResolver().getType(uri);
        } catch (Exception e) {
            Log.e(TAG, "Error getting MIME type: " + e.getMessage(), e);
            return null;
        }
    }


    private void updateGroupDetails(String name, String description, String avatar) {
        ConversationApiService apiService = RetrofitClient.getConversationApiService();
        ConversationUpdate updateData = new ConversationUpdate(name, description, avatar);

        Call<ConversationResponse> call = apiService.updateConversation(
                "Bearer " + accessToken, conversationId, updateData);

        call.enqueue(new Callback<ConversationResponse>() {
            @Override
            public void onResponse(@NonNull Call<ConversationResponse> call, @NonNull Response<ConversationResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentConversation = response.body();
                    updateUIWithGroupDetails();
                    // --- Caching: Save updated group details to local cache ---
                    sharedPreferences.edit().putString(CACHE_KEY_GROUP_DETAILS_PREFIX + conversationId, gson.toJson(currentConversation)).apply();
                    Log.d(TAG, "Group details updated via API and cached.");
                    // --- End Caching Save ---
                    Toast.makeText(GroupDetailsActivity.this, "Detail grup berhasil diperbarui.", Toast.LENGTH_SHORT).show();
                } else {
                    String errorMsg = "Gagal memperbarui detail grup.";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing error body: " + e.getMessage());
                    }
                    Toast.makeText(GroupDetailsActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to update group: " + response.code() + " - " + errorMsg);
                    if (response.code() == 401) redirectToLogin();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ConversationResponse> call, @NonNull Throwable t) {
                Toast.makeText(GroupDetailsActivity.this, "Kesalahan jaringan saat memperbarui grup: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error updating group details", t);
            }
        });
    }

    private void showAddMembersDialog() {
        AddMembersDialogFragment dialog = AddMembersDialogFragment.newInstance(conversationId, accessToken, currentUserId);
        dialog.show(getSupportFragmentManager(), "AddMembersDialogFragment");
    }

    @Override
    public void onMembersAdded(List<String> newMemberIds) {
        if (newMemberIds.isEmpty()) {
            Toast.makeText(this, "Tidak ada anggota yang dipilih.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Menambahkan " + newMemberIds.size() + " anggota...", Toast.LENGTH_SHORT).show();

        ConversationApiService apiService = RetrofitClient.getConversationApiService();
        ParticipantAdd participantAdd = new ParticipantAdd(newMemberIds);

        Call<Void> call = apiService.addParticipants("Bearer " + accessToken, conversationId, participantAdd);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(GroupDetailsActivity.this, "Anggota berhasil ditambahkan!", Toast.LENGTH_SHORT).show();
                    fetchGroupDetails(); // Muat ulang detail grup untuk memperbarui daftar anggota dan cache
                } else {
                    String errorMsg = "Gagal menambahkan anggota.";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing add members error body: " + e.getMessage());
                    }
                    Toast.makeText(GroupDetailsActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to add members: " + response.code() + " - " + errorMsg);
                    if (response.code() == 401) redirectToLogin();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(GroupDetailsActivity.this, "Kesalahan jaringan saat menambahkan anggota: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error adding members", t);
            }
        });
    }

    private void showLeaveGroupConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Keluar Grup")
                .setMessage("Apakah Anda yakin ingin keluar dari grup " + currentConversation.getName() + "?")
                .setPositiveButton("Keluar", (dialog, which) -> leaveGroup())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void leaveGroup() {
        ConversationApiService apiService = RetrofitClient.getConversationApiService();
        Call<Void> call = apiService.leaveConversation("Bearer " + accessToken, conversationId);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(GroupDetailsActivity.this, "Berhasil keluar dari grup.", Toast.LENGTH_SHORT).show();
                    // --- Caching: Clear cache for this group after leaving ---
                    sharedPreferences.edit().remove(CACHE_KEY_GROUP_DETAILS_PREFIX + conversationId).apply();
                    // --- End Caching Clear ---
                    Intent intent = new Intent(GroupDetailsActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMsg = "Gagal keluar dari grup.";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing error body: " + e.getMessage());
                    }
                    Toast.makeText(GroupDetailsActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to leave group: " + response.code() + " - " + errorMsg);
                    if (response.code() == 401) redirectToLogin();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(GroupDetailsActivity.this, "Kesalahan jaringan saat keluar grup: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error leaving group", t);
            }
        });
    }

    private void showDeleteGroupConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Grup")
                .setMessage("Apakah Anda yakin ingin menghapus grup " + currentConversation.getName() + " secara permanen? Aksi ini tidak bisa dibatalkan.")
                .setPositiveButton("Hapus", (dialog, which) -> deleteGroup())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteGroup() {
        ConversationApiService apiService = RetrofitClient.getConversationApiService();
        Call<Void> call = apiService.deleteConversation("Bearer " + accessToken, conversationId);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(GroupDetailsActivity.this, "Grup berhasil dihapus.", Toast.LENGTH_SHORT).show();
                    // --- Caching: Clear cache for this group after deleting ---
                    sharedPreferences.edit().remove(CACHE_KEY_GROUP_DETAILS_PREFIX + conversationId).apply();
                    // --- End Caching Clear ---
                    Intent intent = new Intent(GroupDetailsActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMsg = "Gagal menghapus grup.";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing error body: " + e.getMessage());
                    }
                    Toast.makeText(GroupDetailsActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to delete group: " + response.code() + " - " + errorMsg);
                    if (response.code() == 401) redirectToLogin();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(GroupDetailsActivity.this, "Kesalahan jaringan saat menghapus grup: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error deleting group", t);
            }
        });
    }

    private void toggleGroupNotifications() {
        Toast.makeText(this, "Mengupdate notifikasi grup...", Toast.LENGTH_SHORT).show();

        boolean currentMuteStatus = false;
        if (currentUserParticipant != null) {
            currentMuteStatus = currentUserParticipant.isMuted();
        } else {
            Log.e(TAG, "currentUserParticipant is null! Cannot toggle notifications.");
            Toast.makeText(this, "Gagal mengupdate notifikasi: Data pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean willBeMuted = !currentMuteStatus;

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
                    Toast.makeText(GroupDetailsActivity.this, "Notifikasi grup " + (willBeMuted ? "dibisukan" : "diaktifkan"), Toast.LENGTH_SHORT).show();
                    if (currentUserParticipant != null) {
                        currentUserParticipant.setMuted(willBeMuted);
                    }
                    updateNotificationIcon(willBeMuted);
                    // --- Caching: Update cache after toggling mute status ---
                    // Re-save entire conversation to reflect updated participant's mute status
                    // Note: This assumes currentConversation is up-to-date locally or will be refreshed by fetchGroupDetails()
                    if (currentConversation != null) {
                        // Find and update the current user's participant object in currentConversation
                        for (ParticipantResponse p : currentConversation.getParticipants()) {
                            if (p.getUserId().equals(currentUserId)) {
                                p.setMuted(willBeMuted);
                                break;
                            }
                        }
                        sharedPreferences.edit().putString(CACHE_KEY_GROUP_DETAILS_PREFIX + conversationId, gson.toJson(currentConversation)).apply();
                        Log.d(TAG, "Group mute status updated via API and cached.");
                    }
                    // --- End Caching Update ---
                } else {
                    String error = "Gagal memperbarui notifikasi grup.";
                    try {
                        if (response.errorBody() != null) {
                            error += " " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing error body for toggle group notifications", e);
                    }
                    Toast.makeText(GroupDetailsActivity.this, error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to update group notifications: " + response.code() + " - " + error);
                    if (response.code() == 401) redirectToLogin();
                }
            }


            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(GroupDetailsActivity.this, "Kesalahan jaringan saat memperbarui notifikasi grup: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error updating group notifications", t);
            }
        });
    }

    @Override
    public void onParticipantClick(ParticipantResponse participant) {
        if (currentUserParticipant != null && ("ADMIN".equalsIgnoreCase(currentUserParticipant.getRole()) || "MODERATOR".equalsIgnoreCase(currentUserParticipant.getRole()))) {
            if (participant.getUserId().equals(currentUserId)) {
                Toast.makeText(this, "Ini Anda", Toast.LENGTH_SHORT).show();
            } else {
                showParticipantOptionsDialog(participant);
            }
        } else {
            Toast.makeText(this, participant.getUsername(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showParticipantOptionsDialog(ParticipantResponse targetParticipant) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(targetParticipant.getUsername());

        List<String> options = new ArrayList<>();
        if ("ADMIN".equalsIgnoreCase(currentUserParticipant.getRole())) {
            if (!"ADMIN".equalsIgnoreCase(targetParticipant.getRole())) {
                options.add("Promosikan ke Admin");
            } else {
                options.add("Demote dari Admin");
            }
            if (!"MODERATOR".equalsIgnoreCase(targetParticipant.getRole()) && !"ADMIN".equalsIgnoreCase(targetParticipant.getRole())) {
                options.add("Promosikan ke Moderator");
            } else if ("MODERATOR".equalsIgnoreCase(targetParticipant.getRole())) {
                options.add("Demote dari Moderator");
            }
        }

        if ( ("ADMIN".equalsIgnoreCase(currentUserParticipant.getRole()) && !targetParticipant.getUserId().equals(currentUserId)) ||
                ("MODERATOR".equalsIgnoreCase(currentUserParticipant.getRole()) && !"ADMIN".equalsIgnoreCase(targetParticipant.getRole()) && !"MODERATOR".equalsIgnoreCase(targetParticipant.getRole())) ) {
            options.add("Hapus dari Grup");
        }

        String[] optionsArray = options.toArray(new String[0]);

        if (optionsArray.length == 0) {
            Toast.makeText(this, "Tidak ada opsi yang tersedia.", Toast.LENGTH_SHORT).show();
            return;
        }

        builder.setItems(optionsArray, (dialog, which) -> {
            String selectedOption = optionsArray[which];
            switch (selectedOption) {
                case "Promosikan ke Admin":
                    updateParticipantRole(targetParticipant.getUserId(), "ADMIN");
                    break;
                case "Promosikan ke Moderator":
                    updateParticipantRole(targetParticipant.getUserId(), "MODERATOR");
                    break;
                case "Demote dari Admin":
                case "Demote dari Moderator":
                    updateParticipantRole(targetParticipant.getUserId(), "MEMBER");
                    break;
                case "Hapus dari Grup":
                    showRemoveParticipantConfirmation(targetParticipant);
                    break;
            }
        });
        builder.show();
    }

    private void updateParticipantRole(String participantUserId, String newRole) {
        ConversationApiService apiService = RetrofitClient.getConversationApiService();
        ParticipantUpdate roleUpdate = new ParticipantUpdate(newRole);

        Call<Void> call = apiService.updateParticipantRole(
                "Bearer " + accessToken, conversationId, participantUserId, roleUpdate);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(GroupDetailsActivity.this, "Role " + participantUserId + " diubah menjadi " + newRole, Toast.LENGTH_SHORT).show();
                    fetchGroupDetails(); // Refresh data untuk update UI dan cache
                } else {
                    String errorMsg = "Gagal mengubah role partisipan.";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing error body: " + e.getMessage());
                    }
                    Toast.makeText(GroupDetailsActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to update participant role: " + response.code() + " - " + errorMsg);
                    if (response.code() == 401) redirectToLogin();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(GroupDetailsActivity.this, "Kesalahan jaringan saat mengubah role: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error updating participant role", t);
            }
        });
    }

    private void showRemoveParticipantConfirmation(ParticipantResponse targetParticipant) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Anggota")
                .setMessage("Apakah Anda yakin ingin menghapus " + targetParticipant.getUsername() + " dari grup?")
                .setPositiveButton("Hapus", (dialog, which) -> removeParticipant(targetParticipant.getUserId()))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void removeParticipant(String participantUserId) {
        ConversationApiService apiService = RetrofitClient.getConversationApiService();
        Call<Void> call = apiService.removeParticipant(
                "Bearer " + accessToken, conversationId, participantUserId);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(GroupDetailsActivity.this, "Anggota berhasil dihapus.", Toast.LENGTH_SHORT).show();
                    fetchGroupDetails(); // Refresh data untuk update UI dan cache
                } else {
                    String errorMsg = "Gagal menghapus anggota.";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " " + response.errorBody().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing error body: " + e.getMessage());
                    }
                    Toast.makeText(GroupDetailsActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to remove participant: " + response.code() + " - " + errorMsg);
                    if (response.code() == 401) redirectToLogin();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(GroupDetailsActivity.this, "Kesalahan jaringan saat menghapus anggota: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error removing participant", t);
            }
        });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        // --- Caching: Clear GroupDetailsCache on logout ---
        SharedPreferences groupCachePrefs = getSharedPreferences("GroupDetailsCache", Context.MODE_PRIVATE);
        groupCachePrefs.edit().clear().apply();
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

    private void cleanupTempFiles() {
        try {
            File cacheDir = getCacheDir();
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith("temp_avatar_")) {
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning temp files: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupTempFiles();
    }
}