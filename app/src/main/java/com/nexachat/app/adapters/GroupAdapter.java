package com.nexachat.app.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nexachat.app.R;
import com.nexachat.app.databinding.ItemGroupChatBinding;
import com.nexachat.app.models.Conversation;
import com.nexachat.app.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    public interface OnGroupClickListener {
        void onGroupClick(Conversation group);
    }

    private final Context context;
    private final List<Conversation> groups = new ArrayList<>();
    private final OnGroupClickListener listener;

    public GroupAdapter(Context context, OnGroupClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setGroups(List<Conversation> newGroups) {
        groups.clear();
        if (newGroups != null) {
            groups.addAll(newGroups);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGroupChatBinding binding = ItemGroupChatBinding.inflate(LayoutInflater.from(context), parent, false);
        return new GroupViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Conversation group = groups.get(position);
        holder.bind(group);
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {
        private final ItemGroupChatBinding binding;

        public GroupViewHolder(@NonNull ItemGroupChatBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Conversation group) {
            binding.tvGroupName.setText(group.getTitle());
            binding.tvGroupLastMessage.setText(!TextUtils.isEmpty(group.getLastMessage()) ? group.getLastMessage() : "No messages yet");
            binding.tvGroupTime.setText(DateTimeUtils.formatConversationTime(group.getLastMessageTimestamp()));

            if (group.getUnreadCount() > 0) {
                binding.tvGroupUnreadBadge.setVisibility(View.VISIBLE);
                binding.tvGroupUnreadBadge.setText(String.valueOf(group.getUnreadCount()));
            } else {
                binding.tvGroupUnreadBadge.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(group.getOtherUserAvatarUrl())) {
                binding.tvGroupInitial.setVisibility(View.GONE);
                binding.ivGroupAvatar.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(group.getOtherUserAvatarUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_group)
                        .into(binding.ivGroupAvatar);
            } else {
                binding.ivGroupAvatar.setVisibility(View.GONE);
                binding.tvGroupInitial.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(group.getTitle())) {
                    binding.tvGroupInitial.setText(group.getTitle().substring(0, 1).toUpperCase());
                } else {
                    binding.tvGroupInitial.setText("G");
                }
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onGroupClick(group);
                }
            });
        }
    }
}
