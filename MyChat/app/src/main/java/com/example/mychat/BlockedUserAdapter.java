package com.example.mychat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mychat.models_user.UserResponse;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;
public class BlockedUserAdapter extends RecyclerView.Adapter<BlockedUserAdapter.BlockedUserViewHolder> {

    private List<UserResponse> blockedUsers;
    private Context context;
    private OnUnblockClickListener onUnblockClickListener;

    public interface OnUnblockClickListener {
        void onUnblockClick(UserResponse user);
    }

    public BlockedUserAdapter(Context context, OnUnblockClickListener listener) {
        this.context = context;
        this.onUnblockClickListener = listener;
    }

    public void setBlockedUsers(List<UserResponse> blockedUsers) {
        this.blockedUsers = blockedUsers;
        notifyDataSetChanged(); // Beri tahu adapter bahwa data telah berubah
    }

    @NonNull
    @Override
    public BlockedUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_blocked_user, parent, false);
        return new BlockedUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BlockedUserViewHolder holder, int position) {
        UserResponse user = blockedUsers.get(position);
        holder.tvBlockedUserName.setText(user.getUsername());

        // Muat gambar profil menggunakan Glide
        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            Glide.with(context)
                    .load(user.getProfilePicture())
                    .placeholder(R.drawable.default_avatar) // Placeholder default
                    .error(R.drawable.default_avatar) // Gambar error
                    .into(holder.ivBlockedUserAvatar);
        } else {
            holder.ivBlockedUserAvatar.setImageResource(R.drawable.default_avatar); // Gambar default jika tidak ada URL
        }

        holder.btnUnblockUser.setOnClickListener(v -> {
            if (onUnblockClickListener != null) {
                onUnblockClickListener.onUnblockClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return blockedUsers != null ? blockedUsers.size() : 0;
    }

    public static class BlockedUserViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivBlockedUserAvatar;
        TextView tvBlockedUserName;
        Button btnUnblockUser;

        public BlockedUserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBlockedUserAvatar = itemView.findViewById(R.id.iv_blocked_user_avatar);
            tvBlockedUserName = itemView.findViewById(R.id.tv_blocked_user_name);
            btnUnblockUser = itemView.findViewById(R.id.btn_unblock_user);
        }
    }
}