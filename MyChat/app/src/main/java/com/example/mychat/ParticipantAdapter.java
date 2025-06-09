package com.example.mychat;

import android.content.Context;
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
import com.example.mychat.models_conversation.ParticipantResponse;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ParticipantAdapter extends RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder> {

    private static final String TAG = "ParticipantAdapter";
    private Context context;
    private List<ParticipantResponse> participants;
    private String currentUserId;
    private OnParticipantClickListener listener;

    public interface OnParticipantClickListener {
        void onMembersAdded(List<String> newMemberIds);

        void onParticipantClick(ParticipantResponse participant);
    }

    public ParticipantAdapter(Context context, List<ParticipantResponse> participants, String currentUserId, OnParticipantClickListener listener) {
        this.context = context;
        this.participants = participants;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_participant, parent, false);
        return new ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        ParticipantResponse participant = participants.get(position);
        holder.bind(participant);
    }

    @Override
    public int getItemCount() {
        return participants.size();
    }

    class ParticipantViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivProfilePicture;
        TextView tvParticipantName;
        TextView tvAdminBadge;
        TextView tvEmail; // Changed from tvUsername to tvEmail
        TextView tvStatus;
        ImageView ivMoreOptions;

        ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProfilePicture = itemView.findViewById(R.id.iv_profile_picture);
            tvParticipantName = itemView.findViewById(R.id.tv_participant_name);
            tvAdminBadge = itemView.findViewById(R.id.tv_admin_badge);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvStatus = itemView.findViewById(R.id.tv_status);
            ivMoreOptions = itemView.findViewById(R.id.iv_more_options);
        }

        void bind(ParticipantResponse participant) {
            // Set profile picture
            if (participant.getProfilePicture() != null && !participant.getProfilePicture().isEmpty()) {
                Glide.with(context)
                        .load(participant.getProfilePicture())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .into(ivProfilePicture);
            } else {
                ivProfilePicture.setImageResource(R.drawable.default_avatar);
            }

            // Set name (add "Anda" if it's the current user)
            String displayName = participant.getUsername(); //
            if (participant.getUserId().equals(currentUserId)) { //
                displayName += " (Anda)";
            }
            tvParticipantName.setText(displayName); //

            // Set role badge
            if ("ADMIN".equalsIgnoreCase(participant.getRole()) || "MODERATOR".equalsIgnoreCase(participant.getRole())) { //
                tvAdminBadge.setVisibility(View.VISIBLE);
                tvAdminBadge.setText(participant.getRole().toLowerCase(Locale.ROOT)); //
            } else {
                tvAdminBadge.setVisibility(View.GONE);
            }

            if (participant.getEmail() != null && !participant.getEmail().isEmpty()) {
                tvEmail.setText(participant.getEmail());
            } else if (participant.getUsername() != null && !participant.getUsername().isEmpty()) {
                tvEmail.setText("@" + participant.getUsername());
            } else {
                tvEmail.setText("");
            }


            // Set online status
            if (participant.isOnline()) { //
                tvStatus.setText("Online"); //
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green_500));
            } else if (participant.getLastSeenAt() != null) { //
                tvStatus.setText("Terakhir terlihat " + formatTimeAgo(participant.getLastSeenAt()));
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.secondary_text));
            } else {
                tvStatus.setText("Offline"); //
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.secondary_text));
            }

            // Set click listener for the item
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onParticipantClick(participant);
                }
            });

            // Handle more options button visibility if needed (e.g., only for admin to manage other members)
            ivMoreOptions.setVisibility(View.GONE); // Default: hidden
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
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault()); // "dd MMM YYYY" if very old
                return sdf.format(date);
            }
        }
    }
}