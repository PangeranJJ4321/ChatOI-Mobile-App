package com.example.mychat;

import android.content.Context;
import android.text.TextUtils;
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
import com.example.mychat.R;
import com.example.mychat.models_user.UserResponse;

import java.util.ArrayList;
import java.util.List;

public class UserSelectionAdapter extends RecyclerView.Adapter<UserSelectionAdapter.UserViewHolder> {

    private final Context context;
    private final List<UserResponse> userList;
    private final List<UserResponse> selectedUsers; // List untuk menyimpan user yang dipilih
    private final OnUserSelectedListener listener;



    public interface OnUserSelectedListener {
        void onUserSelected(UserResponse user, boolean isSelected);
        void onSelectionChanged(List<UserResponse> currentSelectedUsers);
    }

    public UserSelectionAdapter(Context context, List<UserResponse> userList, OnUserSelectedListener listener) {
        this.context = context;
        this.userList = userList;
        this.selectedUsers = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_selection, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserResponse user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // Metode untuk mengupdate daftar user (misal setelah pencarian)
    public void updateList(List<UserResponse> newList) {
        userList.clear();
        userList.addAll(newList);
        notifyDataSetChanged();
    }

    // Metode untuk mendapatkan user yang sedang dipilih
    public List<UserResponse> getSelectedUsers() {
        return selectedUsers;
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
        TextView tvUserName;
        TextView tvUserEmail;
        CheckBox checkboxSelectUser;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvUserEmail = itemView.findViewById(R.id.tv_user_email);
            checkboxSelectUser = itemView.findViewById(R.id.checkbox_select_user);

            // Menangani klik pada seluruh item untuk toggle checkbox
            itemView.setOnClickListener(v -> {
                UserResponse user = userList.get(getAdapterPosition());
                boolean isSelected = !checkboxSelectUser.isChecked(); // Status baru
                checkboxSelectUser.setChecked(isSelected); // Perbarui tampilan checkbox

                // Perbarui daftar selectedUsers
                if (isSelected) {
                    selectedUsers.add(user);
                } else {
                    selectedUsers.remove(user);
                }

                // Beri tahu listener tentang perubahan
                if (listener != null) {
                    listener.onUserSelected(user, isSelected);
                    listener.onSelectionChanged(selectedUsers);
                }
                user.setSelected(isSelected); // Update status isSelected di model UserResponse
            });
        }

        void bind(UserResponse user) {
            tvUserName.setText(user.getUsername());
            tvUserEmail.setText(user.getEmail() != null && !user.getEmail().isEmpty() ? user.getEmail() : "@" + user.getUsername());

            Glide.with(context)
                    .load(user.getProfilePicture())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(ivUserAvatar);

            // Set status checkbox berdasarkan apakah user sudah terpilih sebelumnya
            checkboxSelectUser.setChecked(user.isSelected());
        }
    }
}