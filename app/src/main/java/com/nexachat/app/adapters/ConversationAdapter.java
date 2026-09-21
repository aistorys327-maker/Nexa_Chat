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
import com.nexachat.app.databinding.ItemConversationBinding;
import com.nexachat.app.models.Conversation;
import com.nexachat.app.security.ChatLockManager;
import com.nexachat.app.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
        default void onConversationLongClick(Conversation conversation) {}
    }

    private final Context context;
    private final List<Conversation> conversationList = new ArrayList<>();
    private final OnConversationClickListener listener;
    private final ChatLockManager chatLockManager;

    public ConversationAdapter(Context context, OnConversationClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.chatLockManager = ChatLockManager.getInstance(context);
    }

    public void setConversations(List<Conversation> list) {
        conversationList.clear();
        if (list != null) {
            conversationList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemConversationBinding binding = ItemConversationBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Conversation conv = conversationList.get(position);
        holder.bind(conv);
    }

    @Override
    public int getItemCount() {
        return conversationList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemConversationBinding binding;

        ViewHolder(ItemConversationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Conversation conv) {
            binding.tvConvTitle.setText(conv.getTitle() != null ? conv.getTitle() : "Chat");
            binding.tvConvLastMessage.setText(
                    !TextUtils.isEmpty(conv.getLastMessage()) ? conv.getLastMessage() : "No messages yet");
            binding.tvConvTime.setText(DateTimeUtils.formatConversationTime(conv.getLastMessageTimestamp()));

            boolean isLocked = chatLockManager.isChatLocked(conv.getConversationId());
            binding.ivConvLock.setVisibility(isLocked ? View.VISIBLE : View.GONE);

            // Online status
            binding.viewConvOnlineDot.setVisibility(conv.isOnline() ? View.VISIBLE : View.GONE);

            // Unread badge
            if (conv.getUnreadCount() > 0) {
                binding.tvConvUnreadCount.setText(String.valueOf(conv.getUnreadCount()));
                binding.tvConvUnreadCount.setVisibility(View.VISIBLE);
            } else {
                binding.tvConvUnreadCount.setVisibility(View.GONE);
            }

            // Avatar loading
            if (!TextUtils.isEmpty(conv.getOtherUserAvatarUrl())) {
                Glide.with(context)
                        .load(conv.getOtherUserAvatarUrl())
                        .placeholder(R.drawable.circle_avatar_placeholder)
                        .error(R.drawable.circle_avatar_placeholder)
                        .circleCrop()
                        .into(binding.ivConvAvatar);
            } else {
                binding.ivConvAvatar.setImageResource(conv.isGroup() ? R.drawable.ic_group : R.drawable.ic_person);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConversationClick(conv);
                }
            });

            binding.getRoot().setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onConversationLongClick(conv);
                    return true;
                }
                return false;
            });
        }
    }
}
