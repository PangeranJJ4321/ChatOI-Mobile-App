package com.example.mychat;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mychat.models_message.MessageResponse; //cite: 6

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED_PRIVATE = 2;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED_GROUP = 3;

    private List<MessageResponse> messageList; //cite: 6
    private String currentUserId;
    private boolean isGroupChat;
    private Context context;

    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

    private static final String TAG = "MessageAdapter";

    public MessageAdapter(Context context, List<MessageResponse> messageList, String currentUserId, boolean isGroupChat) {
        this.context = context;
        this.messageList = messageList;
        this.currentUserId = currentUserId;
        this.isGroupChat = isGroupChat;
    }

    @Override
    public int getItemViewType(int position) {
        MessageResponse message = messageList.get(position); //cite: 6

        if (message.getSenderId().equals(currentUserId)) { //cite: 6
            return VIEW_TYPE_MESSAGE_SENT;
        } else {
            if (isGroupChat) {
                return VIEW_TYPE_MESSAGE_RECEIVED_GROUP;
            } else {
                return VIEW_TYPE_MESSAGE_RECEIVED_PRIVATE;
            }
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_send, parent, false);
            return new SentMessageViewHolder(view);
        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED_PRIVATE) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received_private, parent, false);
            return new ReceivedMessageViewHolder(view);
        } else { // VIEW_TYPE_MESSAGE_RECEIVED_GROUP
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received_group, parent, false);
            return new ReceivedGroupMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageResponse message = messageList.get(position); //cite: 6

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED_PRIVATE:
                ((ReceivedMessageViewHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED_GROUP:
                ((ReceivedGroupMessageViewHolder) holder).bind(message);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // --- ViewHolder for Sent Messages ---
    private class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        TextView textTime;
        ImageView ivMessageStatus; // Deklarasi lokal untuk SentMessageViewHolder

        SentMessageViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
            textTime = itemView.findViewById(R.id.text_time);
            ivMessageStatus = itemView.findViewById(R.id.iv_message_status); // Inisialisasi
        }

        void bind(MessageResponse message) { //cite: 6
            textMessage.setText(message.getContent()); //cite: 6
            textTime.setText(formatDateTime(message.getSentAt())); //cite: 6

            if (ivMessageStatus != null) {
                String status = message.getStatus(); //cite: 6
                int readByCount = message.getReadByCount(); //cite: 6
                Log.d(TAG, "Binding sent message status: " + status + " ReadByCount: " + readByCount + " for message: " + message.getId() + " clientID: " + message.getClientMessageId()); //cite: 6

                if (status == null) {
                    status = "sent"; // Default to sent if status is null from server
                }

                // Normalize status to lowercase for consistent comparison
                status = status.toLowerCase(Locale.ROOT);

                // Logika untuk menentukan status ikon
                // Urutan prioritas: Failed > Read > Delivered > Sent > Sending
                if ("failed".equalsIgnoreCase(status)) {
                    ivMessageStatus.setImageResource(R.drawable.ic_message_status_failed);
                    ivMessageStatus.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.error_color)));
                    ivMessageStatus.setVisibility(View.VISIBLE);
                }
                else if ("read".equalsIgnoreCase(status) || (readByCount > 0 && !isGroupChat)) {
                    ivMessageStatus.setImageResource(R.drawable.ic_message_status_read); // Centang dua biru
                    ivMessageStatus.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.status_read_color))); // Warna biru untuk terbaca
                    ivMessageStatus.setVisibility(View.VISIBLE);
                }
                // --- KONDISI UNTUK STATUS "TERKIRIM/TERDELIVER" (CENTANG DUA ABU-ABU/BIRU MUDA) ---
                // Ini berarti pesan sudah sampai ke penerima, tetapi belum tentu dibaca.
                else if ("delivered".equalsIgnoreCase(status)) {
                    ivMessageStatus.setImageResource(R.drawable.ic_message_status_delivered); // Centang dua abu-abu/biru muda
                    ivMessageStatus.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.status_delivered_color))); // Warna biru muda
                    ivMessageStatus.setVisibility(View.VISIBLE);
                }
                // --- KONDISI UNTUK STATUS "TERKIRIM KE SERVER" (CENTANG SATU ABU-ABU) ---
                else if ("sent".equalsIgnoreCase(status)) {
                    ivMessageStatus.setImageResource(R.drawable.ic_message_status_sent); // Centang satu
                    ivMessageStatus.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.status_sent_color))); // Warna abu-abu
                    ivMessageStatus.setVisibility(View.VISIBLE);
                }
                // --- KONDISI UNTUK STATUS "SEDANG MENGIRIM" (JAM/LOADING ICON) ---
                else if ("sending".equalsIgnoreCase(status)) {
                    ivMessageStatus.setImageResource(R.drawable.ic_message_status_sending); // Ikon jam/loading
                    ivMessageStatus.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.status_sending_color))); // Warna abu-abu
                    ivMessageStatus.setVisibility(View.VISIBLE);
                } else {
                    // Default case jika status tidak dikenal atau tidak ada
                    Log.w(TAG, "Unknown or unsupported status for message: " + message.getId() + ". Status: " + status); //cite: 6
                    ivMessageStatus.setImageResource(R.drawable.ic_message_status_sent); // Fallback ke ikon sent
                    ivMessageStatus.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(itemView.getContext(), R.color.status_sent_color)));
                    ivMessageStatus.setVisibility(View.VISIBLE);
                }
            } else {
                Log.w(TAG, "ivMessageStatus is null for message: " + message.getId() + " in SentMessageViewHolder."); //cite: 6
            }
        }
    }

    // --- ViewHolder for Received Private Messages ---
    private class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        TextView textTime;

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
            textTime = itemView.findViewById(R.id.text_time);
        }

        void bind(MessageResponse message) { //cite: 6
            textMessage.setText(message.getContent()); //cite: 6
            textTime.setText(formatDateTime(message.getSentAt())); //cite: 6
            // Untuk pesan diterima, biasanya tidak ada status ikon
        }
    }

    // --- ViewHolder for Received Group Messages ---
    private class ReceivedGroupMessageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageAvatar;
        TextView textSenderName;
        TextView textMessage;
        TextView textTime;
        ImageView ivMessageStatus; // Deklarasi lokal untuk ReceivedGroupMessageViewHolder

        ReceivedGroupMessageViewHolder(View itemView) {
            super(itemView);
            imageAvatar = itemView.findViewById(R.id.image_avatar);
            textSenderName = itemView.findViewById(R.id.text_sender_name);
            textMessage = itemView.findViewById(R.id.text_message);
            textTime = itemView.findViewById(R.id.text_time);
            // Anda mungkin perlu menambahkan iv_message_status di layout item_message_received_group.xml
            // Jika ada, inisialisasi di sini:
            ivMessageStatus = itemView.findViewById(R.id.iv_message_status); // Pastikan ID ini ada di layout
        }

        void bind(MessageResponse message) {
            textMessage.setText(message.getContent());
            textTime.setText(formatDateTime(message.getSentAt()));
            textSenderName.setText(message.getSenderUsername()); // Tampilkan nama pengirim
            // Load avatar pengirim jika ada
            if (message.getSenderAvatar() != null && !message.getSenderAvatar().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(message.getSenderAvatar())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .into(imageAvatar);
            } else {
                imageAvatar.setImageResource(R.drawable.default_avatar);
            }

            // Jika Anda ingin menampilkan status ikon untuk pesan grup yang diterima
            if (ivMessageStatus != null) {
                String status = message.getStatus();
                int readByCount = message.getReadByCount();

                Log.d(TAG, "Binding received group message - ID: " + message.getId() +
                        ", ClientID: " + message.getClientMessageId() +
                        ", Status: " + status +
                        ", ReadByCount: " + readByCount);

                if (status == null || status.isEmpty()) {
                    status = "sending"; // Default to sending if status is null
                }

                status = status.toLowerCase(Locale.ROOT);

                // Logika status untuk pesan grup yang diterima (misalnya untuk dibaca oleh anggota lain)
                // Ini mungkin tidak sepenuhnya relevan untuk pesan yang DITERIMA, karena Anda hanya melacak status pesan yang DIKIRIM.
                // Biasanya, pesan yang diterima tidak memiliki status ikon (sent, delivered, read) di sisi penerima.
                // Status ini lebih relevan untuk pesan yang DIKIRIM oleh pengguna saat ini.
                // Namun, jika ada kasus penggunaan untuk menampilkan status pesan yang diterima (misal: "read by me"),
                // Anda bisa mengimplementasikannya di sini. Saat ini, logika ini mirip dengan SentMessageViewHolder.
                if ("failed".equalsIgnoreCase(status)) {
                    setStatusIconForViewHolder(ivMessageStatus, R.drawable.ic_message_status_failed, R.color.error_color);
                } else if ("read".equalsIgnoreCase(status) && isGroupChat && readByCount > 0) { // Contoh: "read" oleh setidaknya satu anggota lain
                    setStatusIconForViewHolder(ivMessageStatus, R.drawable.ic_message_status_read, R.color.status_read_color);
                } else if ("delivered".equalsIgnoreCase(status)) {
                    setStatusIconForViewHolder(ivMessageStatus, R.drawable.ic_message_status_delivered, R.color.status_delivered_color);
                } else if ("sent".equalsIgnoreCase(status)) {
                    setStatusIconForViewHolder(ivMessageStatus, R.drawable.ic_message_status_sent, R.color.status_sent_color);
                } else if ("sending".equalsIgnoreCase(status)) {
                    setStatusIconForViewHolder(ivMessageStatus, R.drawable.ic_message_status_sending, R.color.status_sending_color);
                } else {
                    Log.w(TAG, "Unknown or unsupported status for received group message: " + message.getId() + ". Status: " + status);
                    ivMessageStatus.setVisibility(View.GONE); // Sembunyikan jika tidak ada status yang relevan
                }
            } else {
                Log.w(TAG, "ivMessageStatus is null for message: " + message.getId() + " in ReceivedGroupMessageViewHolder. Make sure it's in the layout.");
            }
        }
    }

    // Metode helper untuk mengatur ikon status dalam ViewHolder
    private void setStatusIconForViewHolder(ImageView imageView, int iconResource, int colorResource) {
        if (imageView != null) {
            imageView.setImageResource(iconResource);
            imageView.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, colorResource))); // Gunakan context dari adapter
            imageView.setVisibility(View.VISIBLE);
        }
    }


    private String formatDateTime(Date date) {
        if (date == null) {
            return "";
        }

        try {
            if (isToday(date.getTime())) {
                return timeFormat.format(date);
            } else if (isYesterday(date.getTime())) {
                return "Yesterday";
            } else {
                return dateFormat.format(date);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date: " + e.getMessage());
            return "";
        }
    }

    // Helper method for today check
    private boolean isToday(long when) {
        Calendar now = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(when);

        return now.get(Calendar.YEAR) == c.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isYesterday(long when) {
        Calendar now = Calendar.getInstance();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(when);

        // Set both to start of their respective days
        now.set(Calendar.HOUR_OF_DAY, 0);
        now.set(Calendar.MINUTE, 0);
        now.set(Calendar.SECOND, 0);
        now.set(Calendar.MILLISECOND, 0);

        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        // Roll back 'now' by one day to get the start of yesterday
        now.add(Calendar.DAY_OF_YEAR, -1);

        // Compare if 'c' (the message date) is exactly 'yesterday'
        return now.get(Calendar.YEAR) == c.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR);
    }

    // Metode ini sekarang akan dipanggil oleh ChatActivity
    public void updateMessageStatus(String messageId, String clientMessageId, String newStatus, int newReadByCount) {
        boolean found = false;
        for (int i = 0; i < messageList.size(); i++) {
            MessageResponse message = messageList.get(i);

            // Check both server ID and client message ID
            boolean isMatch = (message.getId() != null && message.getId().equals(messageId)) ||
                    (message.getClientMessageId() != null && message.getClientMessageId().equals(clientMessageId));

            if (isMatch) {
                String oldStatus = message.getStatus();
                int oldReadByCount = message.getReadByCount();

                // Only update if the new status is "better" or different
                // Priority: failed > read > delivered > sent > sending
                int oldStatusPriority = getStatusPriority(oldStatus);
                int newStatusPriority = getStatusPriority(newStatus);

                if (newStatusPriority < oldStatusPriority) { // New status is higher priority (lower number)
                    message.setStatus(newStatus);
                    message.setReadByCount(newReadByCount);
                    notifyItemChanged(i);
                    Log.d(TAG, "Updated message status from " + oldStatus + " (" + oldStatusPriority + ") to " + newStatus + " (" + newStatusPriority + ")" +
                            " and readByCount from " + oldReadByCount + " to " + newReadByCount +
                            " for messageId: " + messageId + ", clientId: " + clientMessageId);
                    found = true;
                    break;
                } else if (newStatusPriority == oldStatusPriority && newReadByCount > oldReadByCount) {
                    // Update readByCount even if status priority is the same (for group chats)
                    message.setReadByCount(newReadByCount);
                    notifyItemChanged(i);
                    Log.d(TAG, "Updated readByCount from " + oldReadByCount + " to " + newReadByCount +
                            " for messageId: " + messageId + ", clientId: " + clientMessageId + " (status remained " + oldStatus + ")");
                    found = true;
                    break;
                } else {
                    Log.d(TAG, "Not updating status for messageId: " + messageId + " because new status (" + newStatus + ", priority " + newStatusPriority +
                            ") is not better than current (" + oldStatus + ", priority " + oldStatusPriority + ")");
                }
            }
        }
        if (!found) {
            Log.w(TAG, "Message with ID: " + messageId + " or Client ID: " + clientMessageId + " not found in list for status update.");
        }
    }

    // Helper to determine status priority for updates
    private int getStatusPriority(String status) {
        if (status == null) return 99; // Unknown status, lowest priority
        switch (status.toLowerCase(Locale.ROOT)) {
            case "failed": return 0;
            case "read": return 1;
            case "delivered": return 2;
            case "sent": return 3;
            case "sending": return 4;
            default: return 5;
        }
    }


    public void logAllMessageStatuses() {
        Log.d(TAG, "=== Current Message Statuses ===");
        for (int i = 0; i < messageList.size(); i++) {
            MessageResponse message = messageList.get(i);
            Log.d(TAG, "Position " + i + ": ID=" + message.getId() +
                    ", ClientID=" + message.getClientMessageId() +
                    ", Status=" + message.getStatus() +
                    ", Content=" + message.getContent() +
                    ", ReadByCount=" + message.getReadByCount());
        }
        Log.d(TAG, "=== End Message Statuses ===");
    }
}