package com.example.mychat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.mychat.models_user.UserResponse;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class SelectableUserAdapter extends RecyclerView.Adapter<SelectableUserAdapter.UserViewHolder> {

    private List<UserResponse> userList;
    private OnUserSelectedListener listener;
    private boolean isGroupChatMode = false;

    public interface OnUserSelectedListener {
        void onUserSelected(UserResponse user, boolean isChecked);
    }

    public SelectableUserAdapter(List<UserResponse> userList, OnUserSelectedListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    public void setGroupChatMode(boolean isGroupChatMode) {
        this.isGroupChatMode = isGroupChatMode;
        notifyDataSetChanged();
    }


    public void updateList(List<UserResponse> newList) {
        Log.d("SelectableUserAdapter", "updateList called with " + newList.size() + " items");

        // Debug: log beberapa item dari newList
        for (int i = 0; i < Math.min(3, newList.size()); i++) {
            UserResponse user = newList.get(i);
            Log.d("SelectableUserAdapter", "Item " + i + ": " + user.getUsername() + " (ID: " + user.getId() + ")");
        }

        // Coba approach yang lebih safe
        if (newList != null) {
            this.userList.clear();
            this.userList.addAll(newList);
        }

        Log.d("SelectableUserAdapter", "userList size after update: " + this.userList.size());

        // Debug: cek apakah items di userList valid
        for (int i = 0; i < Math.min(3, this.userList.size()); i++) {
            UserResponse user = this.userList.get(i);
            Log.d("SelectableUserAdapter", "UserList item " + i + ": " + (user != null ? user.getUsername() : "null"));
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_selectable, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserResponse user = userList.get(position);

        holder.tvUserName.setText(user.getUsername() != null && !user.getUsername().isEmpty() ? user.getUsername() : user.getId());
        holder.tvUserStatus.setText(user.isOnline() ? "Online" : "Offline");
        holder.tvUserStatus.setTextColor(holder.itemView.getContext().getResources().getColor(user.isOnline() ? R.color.green_500 : R.color.red_500));

        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfilePicture())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(holder.imgUserAvatar);
        } else {
            holder.imgUserAvatar.setImageResource(R.drawable.default_avatar);
        }

        holder.cbSelectUser.setOnCheckedChangeListener(null);
        holder.cbSelectUser.setChecked(user.isSelected());

        holder.cbSelectUser.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isGroupChatMode && isChecked) {
                for (int i = 0; i < userList.size(); i++) {
                    UserResponse u = userList.get(i);
                    if (u.isSelected() && !u.getId().equals(user.getId())) {
                        u.setSelected(false);
                        notifyItemChanged(i);
                    }
                }
            }
            user.setSelected(isChecked);
            listener.onUserSelected(user, isChecked);
        });

        holder.itemView.setOnClickListener(v -> holder.cbSelectUser.performClick());
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }


    public static class UserViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelectUser;
        ShapeableImageView imgUserAvatar;
        TextView tvUserName;
        TextView tvUserStatus;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelectUser = itemView.findViewById(R.id.cb_select_user);
            imgUserAvatar = itemView.findViewById(R.id.img_user_avatar);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvUserStatus = itemView.findViewById(R.id.tv_user_status);
        }
    }
}