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
import com.nexachat.app.databinding.ItemStatusBinding;
import com.nexachat.app.models.StatusItem;
import com.nexachat.app.models.UserStatusGroup;
import com.nexachat.app.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

public class StatusAdapter extends RecyclerView.Adapter<StatusAdapter.StatusViewHolder> {

    public interface OnStatusClickListener {
        void onStatusClick(UserStatusGroup group);
    }

    private final Context context;
    private final List<UserStatusGroup> statusGroups = new ArrayList<>();
    private final OnStatusClickListener listener;

    public StatusAdapter(Context context, OnStatusClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateList(List<UserStatusGroup> newList) {
        statusGroups.clear();
        if (newList != null) {
            statusGroups.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public StatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemStatusBinding binding = ItemStatusBinding.inflate(LayoutInflater.from(context), parent, false);
        return new StatusViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull StatusViewHolder holder, int position) {
        UserStatusGroup group = statusGroups.get(position);
        holder.bind(group);
    }

    @Override
    public int getItemCount() {
        return statusGroups.size();
    }

    class StatusViewHolder extends RecyclerView.ViewHolder {
        private final ItemStatusBinding binding;

        public StatusViewHolder(@NonNull ItemStatusBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(UserStatusGroup group) {
            binding.tvStatusUserName.setText(group.getUserName());

            StatusItem latest = group.getLatestStatus();
            if (latest != null) {
                binding.tvStatusTimestamp.setText(DateTimeUtils.formatConversationTime(latest.getTimestamp()));
            } else {
                binding.tvStatusTimestamp.setText("Recent update");
            }

            if (!TextUtils.isEmpty(group.getUserAvatarUrl())) {
                binding.tvStatusInitial.setVisibility(View.GONE);
                binding.ivStatusAvatar.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(group.getUserAvatarUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .into(binding.ivStatusAvatar);
            } else {
                binding.ivStatusAvatar.setVisibility(View.GONE);
                binding.tvStatusInitial.setVisibility(View.VISIBLE);
                if (!TextUtils.isEmpty(group.getUserName())) {
                    binding.tvStatusInitial.setText(group.getUserName().substring(0, 1).toUpperCase());
                } else {
                    binding.tvStatusInitial.setText("?");
                }
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onStatusClick(group);
                }
            });
        }
    }
}
