package com.nexachat.app.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nexachat.app.R;
import com.nexachat.app.databinding.ItemHiddenChatBinding;
import com.nexachat.app.models.Conversation;

import java.util.ArrayList;
import java.util.List;

public class HiddenChatAdapter extends RecyclerView.Adapter<HiddenChatAdapter.ViewHolder> {

    public interface OnHiddenChatActionListener {
        void onChatClick(Conversation conversation);
        void onUnhideClick(Conversation conversation);
    }

    private final Context context;
    private final List<Conversation> hiddenList = new ArrayList<>();
    private final OnHiddenChatActionListener listener;

    public HiddenChatAdapter(Context context, OnHiddenChatActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setConversations(List<Conversation> list) {
        hiddenList.clear();
        if (list != null) {
            hiddenList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHiddenChatBinding binding = ItemHiddenChatBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(hiddenList.get(position));
    }

    @Override
    public int getItemCount() {
        return hiddenList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHiddenChatBinding binding;

        ViewHolder(ItemHiddenChatBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Conversation conv) {
            binding.tvHiddenTitle.setText(conv.getTitle() != null ? conv.getTitle() : "Protected Chat");
            binding.tvHiddenLastMessage.setText(
                    !TextUtils.isEmpty(conv.getLastMessage()) ? conv.getLastMessage() : "No messages yet");

            if (!TextUtils.isEmpty(conv.getOtherUserAvatarUrl())) {
                Glide.with(context)
                        .load(conv.getOtherUserAvatarUrl())
                        .placeholder(R.drawable.circle_avatar_placeholder)
                        .error(R.drawable.circle_avatar_placeholder)
                        .circleCrop()
                        .into(binding.ivHiddenAvatar);
            } else {
                binding.ivHiddenAvatar.setImageResource(conv.isGroup() ? R.drawable.ic_group : R.drawable.ic_person);
            }

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChatClick(conv);
                }
            });

            binding.btnItemUnhide.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onUnhideClick(conv);
                }
            });
        }
    }
}
