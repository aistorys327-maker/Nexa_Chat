package com.nexachat.app.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nexachat.app.R;
import com.nexachat.app.databinding.ItemUserSearchBinding;
import com.nexachat.app.models.User;

import java.util.ArrayList;
import java.util.List;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.ViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private final Context context;
    private final List<User> userList = new ArrayList<>();
    private final OnUserClickListener listener;

    public UserSearchAdapter(Context context, OnUserClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setUsers(List<User> users) {
        userList.clear();
        if (users != null) {
            userList.addAll(users);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserSearchBinding binding = ItemUserSearchBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemUserSearchBinding binding;

        ViewHolder(ItemUserSearchBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User user) {
            binding.tvUserFullName.setText(user.getFullName() != null ? user.getFullName() : "User");
            binding.tvUserUsername.setText("@" + (user.getUsername() != null ? user.getUsername() : "user"));

            if (!TextUtils.isEmpty(user.getProfileImageUrl())) {
                Glide.with(context)
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.circle_avatar_placeholder)
                        .circleCrop()
                        .into(binding.ivUserAvatar);
            } else {
                binding.ivUserAvatar.setImageResource(R.drawable.ic_person);
            }

            binding.btnMessageUser.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUserClick(user);
                }
            });
        }
    }
}
