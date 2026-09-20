package com.nexachat.app.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nexachat.app.R;
import com.nexachat.app.databinding.ItemMessageIncomingBinding;
import com.nexachat.app.databinding.ItemMessageOutgoingBinding;
import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.models.Message;
import com.nexachat.app.utils.AudioPlayerHelper;
import com.nexachat.app.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_OUTGOING = 1;
    private static final int VIEW_TYPE_INCOMING = 2;

    private final Context context;
    private final List<Message> messageList = new ArrayList<>();
    private final String currentUserId;
    private final boolean isGroup;

    public MessageAdapter(Context context, boolean isGroup) {
        this.context = context;
        this.currentUserId = FirebaseManager.getInstance().getCurrentUserId();
        this.isGroup = isGroup;
    }

    public void setMessages(List<Message> list) {
        messageList.clear();
        if (list != null) {
            messageList.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        if (message != null) {
            messageList.add(message);
            notifyItemInserted(messageList.size() - 1);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messageList.get(position);
        if (currentUserId != null && currentUserId.equals(msg.getSenderId())) {
            return VIEW_TYPE_OUTGOING;
        } else {
            return VIEW_TYPE_INCOMING;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_OUTGOING) {
            ItemMessageOutgoingBinding binding = ItemMessageOutgoingBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new OutgoingViewHolder(binding);
        } else {
            ItemMessageIncomingBinding binding = ItemMessageIncomingBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new IncomingViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messageList.get(position);
        if (holder instanceof OutgoingViewHolder) {
            ((OutgoingViewHolder) holder).bind(msg);
        } else if (holder instanceof IncomingViewHolder) {
            ((IncomingViewHolder) holder).bind(msg);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // Outgoing Message ViewHolder
    class OutgoingViewHolder extends RecyclerView.ViewHolder {
        private final ItemMessageOutgoingBinding binding;

        OutgoingViewHolder(ItemMessageOutgoingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Message message) {
            binding.tvOutgoingTimestamp.setText(DateTimeUtils.formatMessageTime(message.getTimestamp()));

            // Status checkmarks
            if (Message.STATUS_READ.equals(message.getStatus())) {
                binding.ivOutgoingStatus.setImageResource(R.drawable.ic_double_check);
                binding.ivOutgoingStatus.setColorFilter(ContextCompat.getColor(context, R.color.cyan_accent));
            } else if (Message.STATUS_DELIVERED.equals(message.getStatus())) {
                binding.ivOutgoingStatus.setImageResource(R.drawable.ic_double_check);
                binding.ivOutgoingStatus.setColorFilter(ContextCompat.getColor(context, R.color.status_delivered));
            } else {
                binding.ivOutgoingStatus.setImageResource(R.drawable.ic_check);
                binding.ivOutgoingStatus.setColorFilter(ContextCompat.getColor(context, R.color.status_sent));
            }

            // Message Type Handling
            String type = message.getMessageType();
            if (Message.TYPE_IMAGE.equals(type)) {
                binding.cardOutgoingImage.setVisibility(View.VISIBLE);
                binding.layoutOutgoingAudio.setVisibility(View.GONE);
                binding.layoutOutgoingFile.setVisibility(View.GONE);
                Glide.with(context)
                        .load(message.getMediaUrl())
                        .placeholder(R.drawable.bg_glass_input)
                        .into(binding.ivOutgoingImage);

                binding.cardOutgoingImage.setOnClickListener(v -> openMediaUrl(message.getMediaUrl()));
            } else if (Message.TYPE_AUDIO.equals(type)) {
                binding.cardOutgoingImage.setVisibility(View.GONE);
                binding.layoutOutgoingAudio.setVisibility(View.VISIBLE);
                binding.layoutOutgoingFile.setVisibility(View.GONE);

                String durStr = message.getAudioDurationSeconds() > 0
                        ? DateTimeUtils.formatDuration(message.getAudioDurationSeconds()) : "Voice Note";
                binding.tvOutgoingAudioDuration.setText(durStr);

                setupAudioPlayButton(binding.btnPlayOutgoingAudio, message.getMediaUrl());
            } else if (Message.TYPE_FILE.equals(type)) {
                binding.cardOutgoingImage.setVisibility(View.GONE);
                binding.layoutOutgoingAudio.setVisibility(View.GONE);
                binding.layoutOutgoingFile.setVisibility(View.VISIBLE);
                binding.tvOutgoingFileName.setText(
                        !TextUtils.isEmpty(message.getFileName()) ? message.getFileName() : "Attachment");

                binding.layoutOutgoingFile.setOnClickListener(v -> openMediaUrl(message.getMediaUrl()));
            } else {
                binding.cardOutgoingImage.setVisibility(View.GONE);
                binding.layoutOutgoingAudio.setVisibility(View.GONE);
                binding.layoutOutgoingFile.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(message.getText()) && !Message.TYPE_IMAGE.equals(type)
                    && !Message.TYPE_AUDIO.equals(type) && !Message.TYPE_FILE.equals(type)) {
                binding.tvOutgoingMessageText.setVisibility(View.VISIBLE);
                binding.tvOutgoingMessageText.setText(message.getText());
            } else if (Message.TYPE_IMAGE.equals(type) && !TextUtils.isEmpty(message.getText())
                    && !"Photo".equals(message.getText())) {
                binding.tvOutgoingMessageText.setVisibility(View.VISIBLE);
                binding.tvOutgoingMessageText.setText(message.getText());
            } else {
                binding.tvOutgoingMessageText.setVisibility(View.GONE);
            }
        }
    }

    // Incoming Message ViewHolder
    class IncomingViewHolder extends RecyclerView.ViewHolder {
        private final ItemMessageIncomingBinding binding;

        IncomingViewHolder(ItemMessageIncomingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Message message) {
            binding.tvIncomingTimestamp.setText(DateTimeUtils.formatMessageTime(message.getTimestamp()));

            if (isGroup && !TextUtils.isEmpty(message.getSenderName())) {
                binding.tvIncomingSenderName.setVisibility(View.VISIBLE);
                binding.tvIncomingSenderName.setText(message.getSenderName());
            } else {
                binding.tvIncomingSenderName.setVisibility(View.GONE);
            }

            // Message Type Handling
            String type = message.getMessageType();
            if (Message.TYPE_IMAGE.equals(type)) {
                binding.cardIncomingImage.setVisibility(View.VISIBLE);
                binding.layoutIncomingAudio.setVisibility(View.GONE);
                binding.layoutIncomingFile.setVisibility(View.GONE);
                Glide.with(context)
                        .load(message.getMediaUrl())
                        .placeholder(R.drawable.bg_glass_input)
                        .into(binding.ivIncomingImage);

                binding.cardIncomingImage.setOnClickListener(v -> openMediaUrl(message.getMediaUrl()));
            } else if (Message.TYPE_AUDIO.equals(type)) {
                binding.cardIncomingImage.setVisibility(View.GONE);
                binding.layoutIncomingAudio.setVisibility(View.VISIBLE);
                binding.layoutIncomingFile.setVisibility(View.GONE);

                String durStr = message.getAudioDurationSeconds() > 0
                        ? DateTimeUtils.formatDuration(message.getAudioDurationSeconds()) : "Voice Note";
                binding.tvIncomingAudioDuration.setText(durStr);

                setupAudioPlayButton(binding.btnPlayIncomingAudio, message.getMediaUrl());
            } else if (Message.TYPE_FILE.equals(type)) {
                binding.cardIncomingImage.setVisibility(View.GONE);
                binding.layoutIncomingAudio.setVisibility(View.GONE);
                binding.layoutIncomingFile.setVisibility(View.VISIBLE);
                binding.tvIncomingFileName.setText(
                        !TextUtils.isEmpty(message.getFileName()) ? message.getFileName() : "Attachment");

                binding.layoutIncomingFile.setOnClickListener(v -> openMediaUrl(message.getMediaUrl()));
            } else {
                binding.cardIncomingImage.setVisibility(View.GONE);
                binding.layoutIncomingAudio.setVisibility(View.GONE);
                binding.layoutIncomingFile.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(message.getText()) && !Message.TYPE_IMAGE.equals(type)
                    && !Message.TYPE_AUDIO.equals(type) && !Message.TYPE_FILE.equals(type)) {
                binding.tvIncomingMessageText.setVisibility(View.VISIBLE);
                binding.tvIncomingMessageText.setText(message.getText());
            } else if (Message.TYPE_IMAGE.equals(type) && !TextUtils.isEmpty(message.getText())
                    && !"Photo".equals(message.getText())) {
                binding.tvIncomingMessageText.setVisibility(View.VISIBLE);
                binding.tvIncomingMessageText.setText(message.getText());
            } else {
                binding.tvIncomingMessageText.setVisibility(View.GONE);
            }
        }
    }

    private void setupAudioPlayButton(View btnPlay, String audioUrl) {
        if (TextUtils.isEmpty(audioUrl)) return;

        AudioPlayerHelper player = AudioPlayerHelper.getInstance();
        boolean isCurrent = audioUrl.equals(player.getCurrentlyPlayingUrl()) && player.isPlaying();
        if (btnPlay instanceof android.widget.ImageView) {
            ((android.widget.ImageView) btnPlay).setImageResource(
                    isCurrent ? R.drawable.ic_pause : R.drawable.ic_play);
        }

        btnPlay.setOnClickListener(v -> {
            if (audioUrl.equals(player.getCurrentlyPlayingUrl()) && player.isPlaying()) {
                player.pause();
                if (btnPlay instanceof android.widget.ImageView) {
                    ((android.widget.ImageView) btnPlay).setImageResource(R.drawable.ic_play);
                }
            } else {
                player.play(audioUrl, new AudioPlayerHelper.OnPlaybackListener() {
                    @Override
                    public void onStart() {
                        if (btnPlay instanceof android.widget.ImageView) {
                            ((android.widget.ImageView) btnPlay).setImageResource(R.drawable.ic_pause);
                        }
                    }

                    @Override
                    public void onStop() {
                        if (btnPlay instanceof android.widget.ImageView) {
                            ((android.widget.ImageView) btnPlay).setImageResource(R.drawable.ic_play);
                        }
                    }

                    @Override
                    public void onError() {
                        if (btnPlay instanceof android.widget.ImageView) {
                            ((android.widget.ImageView) btnPlay).setImageResource(R.drawable.ic_play);
                        }
                    }
                });
            }
        });
    }

    private void openMediaUrl(String url) {
        if (TextUtils.isEmpty(url)) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception ignored) {
        }
    }
}
