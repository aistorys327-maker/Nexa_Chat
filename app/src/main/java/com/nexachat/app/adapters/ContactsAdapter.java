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
import com.nexachat.app.databinding.ItemContactBinding;
import com.nexachat.app.models.ContactItem;
import com.nexachat.app.models.User;

import java.util.ArrayList;
import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    public interface OnContactClickListener {
        void onMessage(ContactItem contact);
        void onAudioCall(ContactItem contact);
        void onVideoCall(ContactItem contact);
        void onInvite(ContactItem contact);
    }

    private final Context context;
    private final List<ContactItem> contactList = new ArrayList<>();
    private final OnContactClickListener listener;

    public ContactsAdapter(Context context, OnContactClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateList(List<ContactItem> newList) {
        contactList.clear();
        if (newList != null) {
            contactList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContactBinding binding = ItemContactBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ContactViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ContactItem item = contactList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    class ContactViewHolder extends RecyclerView.ViewHolder {
        private final ItemContactBinding binding;

        public ContactViewHolder(@NonNull ItemContactBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ContactItem item) {
            binding.tvContactName.setText(item.getName());

            if (item.isOnApp() && item.getUser() != null) {
                User user = item.getUser();
                String username = !TextUtils.isEmpty(user.getUsername()) ? "@" + user.getUsername() : "";
                String phone = "";
                if (user.isPhoneNumberHidden()) {
                    phone = "Private";
                } else if (!TextUtils.isEmpty(item.getPhoneNumber())) {
                    phone = item.getPhoneNumber();
                }
                
                if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(phone)) {
                    binding.tvContactSubtext.setText(username + " • " + phone);
                } else if (!TextUtils.isEmpty(username)) {
                    binding.tvContactSubtext.setText(username);
                } else {
                    binding.tvContactSubtext.setText(phone);
                }

                binding.layoutOnAppActions.setVisibility(View.VISIBLE);
                binding.btnContactInvite.setVisibility(View.GONE);

                if (!TextUtils.isEmpty(user.getProfileImageUrl())) {
                    binding.tvContactInitial.setVisibility(View.GONE);
                    binding.ivContactAvatar.setVisibility(View.VISIBLE);
                    Glide.with(context)
                            .load(user.getProfileImageUrl())
                            .circleCrop()
                            .placeholder(R.drawable.ic_person)
                            .into(binding.ivContactAvatar);
                } else {
                    displayInitials(item.getName());
                }

                binding.btnContactMessage.setOnClickListener(v -> {
                    if (listener != null) listener.onMessage(item);
                });

                binding.btnContactCall.setOnClickListener(v -> {
                    if (listener != null) listener.onAudioCall(item);
                });

                binding.btnContactVideoCall.setOnClickListener(v -> {
                    if (listener != null) listener.onVideoCall(item);
                });

                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onMessage(item);
                });

            } else {
                // Not on app
                binding.tvContactSubtext.setText(!TextUtils.isEmpty(item.getPhoneNumber()) ? item.getPhoneNumber() : "Not on NexaChat");
                binding.layoutOnAppActions.setVisibility(View.GONE);
                binding.btnContactInvite.setVisibility(View.VISIBLE);

                displayInitials(item.getName());

                binding.btnContactInvite.setOnClickListener(v -> {
                    if (listener != null) listener.onInvite(item);
                });

                itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onInvite(item);
                });
            }
        }

        private void displayInitials(String name) {
            binding.ivContactAvatar.setVisibility(View.GONE);
            binding.tvContactInitial.setVisibility(View.VISIBLE);
            if (!TextUtils.isEmpty(name)) {
                binding.tvContactInitial.setText(name.substring(0, 1).toUpperCase());
            } else {
                binding.tvContactInitial.setText("?");
            }
        }
    }
}
