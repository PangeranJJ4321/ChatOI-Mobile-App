package com.example.mychat;

import static android.text.format.DateUtils.isToday;

import android.content.Context; // Ditambahkan
import android.content.Intent;
import android.net.ConnectivityManager; // Ditambahkan
import android.net.NetworkInfo; // Ditambahkan
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast; // Ditambahkan

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mychat.models_conversation.ConversationListResponse;

import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private List<ConversationListResponse> conversationList;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

    public ConversationAdapter(List<ConversationListResponse> conversationList) {
        this.conversationList = conversationList;
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationAdapter.ConversationViewHolder holder, int position) {
        ConversationListResponse conversation = conversationList.get(position);

        String displayName;
        String displayAvatarUrl;

        if (conversation.isGroup()) {
            // Ini adalah grup chat, gunakan nama dan avatar dari objek conversation
            displayName = conversation.getName();
            displayAvatarUrl = conversation.getAvatar();
        } else {
            // Ini adalah chat pribadi, gunakan username dan avatar dari other_participant
            displayName = conversation.getOtherParticipantUsername();
            displayAvatarUrl = conversation.getOtherParticipantAvatar();
        }

        // Set nama percakapan
        if (displayName != null && !displayName.isEmpty()) {
            holder.tvConversationName.setText(displayName);
        } else {
            holder.tvConversationName.setText("Unnamed Chat");
        }

        // Set avatar percakapan
        if (displayAvatarUrl != null && !displayAvatarUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(displayAvatarUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(holder.ivProfilePicture);
        } else {
            holder.ivProfilePicture.setImageResource(R.drawable.default_avatar);
        }
        // --- Akhir Logika Penentuan Nama dan Avatar ---

        String lastMessageText = "";
        if (conversation.isGroup()) {
            if (conversation.getLastMessageSender() != null && !conversation.getLastMessageSender().isEmpty()) {
                lastMessageText += conversation.getLastMessageSender() + ": ";
            }
        }
        if (conversation.getLastMessage() != null) {
            lastMessageText += conversation.getLastMessage();
        }
        holder.tvLastMessage.setText(lastMessageText);


        Date lastMessageDate = conversation.getLastMessageAt();
        if (lastMessageDate != null) {
            if (isToday(lastMessageDate.getTime())) {
                holder.tvTimestamp.setText(timeFormat.format(lastMessageDate));
            } else if (isYesterday(lastMessageDate.getTime())) {
                holder.tvTimestamp.setText("Yesterday");
            }
            else {
                holder.tvTimestamp.setText(dateFormat.format(lastMessageDate));
            }
        } else {
            holder.tvTimestamp.setText("");
        }


        int unreadCount = conversation.getUnreadCount();
        if (unreadCount > 0) {
            holder.unreadBadgeFrame.setVisibility(View.VISIBLE);
            holder.tvUnreadCount.setText(String.valueOf(unreadCount));
        } else {
            holder.unreadBadgeFrame.setVisibility(View.GONE);
        }

        holder.ivMuted.setVisibility(conversation.isMuted() ? View.VISIBLE : View.GONE);
        holder.ivPinned.setVisibility(conversation.isPinned() ? View.VISIBLE : View.GONE);

        // Elements usually not present in ConversationList item, ensure they are hidden
        holder.viewOnlineStatus.setVisibility(View.GONE);
        holder.ivMessageStatus.setVisibility(View.GONE);
        holder.layoutTypingIndicator.setVisibility(View.GONE);
        holder.priorityIndicator.setVisibility(View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (!isNetworkAvailable(v.getContext())) {
                // Jika tidak ada koneksi, tampilkan Toast dan batalkan aksi
                Toast.makeText(v.getContext(), "Cannot open chat: No internet connection.", Toast.LENGTH_SHORT).show();
                return; // Hentikan eksekusi lebih lanjut
            }

            Intent intent = new Intent(v.getContext(), ChatActivity.class);
            intent.putExtra("conversation_id", conversation.getId());
            intent.putExtra("is_group_chat", conversation.isGroup());
            // Hanya kirim detail other_participant jika ini chat pribadi
            if (!conversation.isGroup()) {
                intent.putExtra("other_participant_id", conversation.getOtherParticipantId());
                intent.putExtra("other_participant_username", conversation.getOtherParticipantUsername());
                intent.putExtra("other_participant_avatar", conversation.getOtherParticipantAvatar());
            }
            v.getContext().startActivity(intent);
        });
    }

    // Helper method for yesterday check
    private boolean isYesterday(long when) {
        Calendar now = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(when);

        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        now.add(Calendar.DAY_OF_YEAR, -1);

        return now.get(Calendar.YEAR) == c.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    // Metode untuk memeriksa ketersediaan jaringan
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    public static class ConversationViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivProfilePicture;
        View viewOnlineStatus;
        TextView tvConversationName;
        TextView tvLastMessage;
        TextView tvTimestamp;
        ImageView ivMessageStatus;
        FrameLayout unreadBadgeFrame;
        TextView tvUnreadCount;
        ImageView ivMuted;
        ImageView ivPinned;
        LinearLayout layoutTypingIndicator;
        View priorityIndicator;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);

            ivProfilePicture = itemView.findViewById(R.id.iv_profile_picture);
            viewOnlineStatus = itemView.findViewById(R.id.view_online_status);
            tvConversationName = itemView.findViewById(R.id.tv_conversation_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            ivMessageStatus = itemView.findViewById(R.id.iv_message_status);
            unreadBadgeFrame = itemView.findViewById(R.id.unread_badge_frame);
            tvUnreadCount = itemView.findViewById(R.id.tv_unread_count);
            ivMuted = itemView.findViewById(R.id.iv_muted);
            ivPinned = itemView.findViewById(R.id.iv_pinned);
            layoutTypingIndicator = itemView.findViewById(R.id.layout_typing_indicator);
            priorityIndicator = itemView.findViewById(R.id.priority_indicator);
        }
    }
}