package com.nexachat.app.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nexachat.app.R;
import com.nexachat.app.databinding.ItemSelectMemberBinding;
import com.nexachat.app.models.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectMemberAdapter extends RecyclerView.Adapter<SelectMemberAdapter.ViewHolder> {

    private final Context context;
    private final List<User> userList = new ArrayList<>();
    private final Set<String> selectedUserIds = new HashSet<>();

    public SelectMemberAdapter(Context context) {
        this.context = context;
    }

    public void setUsers(List<User> users) {
        userList.clear();
        if (users != null) {
            userList.addAll(users);
        }
        notifyDataSetChanged();
    }

    public Set<String> getSelectedUserIds() {
        return selectedUserIds;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSelectMemberBinding binding = ItemSelectMemberBinding.inflate(
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
        private final ItemSelectMemberBinding binding;

        ViewHolder(ItemSelectMemberBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User user) {
            binding.tvMemberName.setText(user.getFullName() != null ? user.getFullName() : "User");
            binding.tvMemberUsername.setText("@" + (user.getUsername() != null ? user.getUsername() : "user"));

            if (!TextUtils.isEmpty(user.getProfileImageUrl())) {
                Glide.with(context)
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.circle_avatar_placeholder)
                        .circleCrop()
                        .into(binding.ivMemberAvatar);
            } else {
                binding.ivMemberAvatar.setImageResource(R.drawable.ic_person);
            }

            boolean isChecked = selectedUserIds.contains(user.getUid());
            binding.cbSelectMember.setChecked(isChecked);

            binding.cbSelectMember.setOnCheckedChangeListener((buttonView, isChecked1) -> {
                if (isChecked1) {
                    selectedUserIds.add(user.getUid());
                } else {
                    selectedUserIds.remove(user.getUid());
                }
            });

            binding.getRoot().setOnClickListener(v -> {
                boolean newState = !binding.cbSelectMember.isChecked();
                binding.cbSelectMember.setChecked(newState);
            });
        }
    }
}
