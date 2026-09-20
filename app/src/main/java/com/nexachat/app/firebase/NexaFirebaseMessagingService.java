package com.nexachat.app.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.nexachat.app.ChatActivity;
import com.nexachat.app.MainActivity;
import com.nexachat.app.R;

public class NexaFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "NexaFCMService";
    public static final String CHANNEL_ID = "nexachat_messages_channel";
    public static final String CHANNEL_NAME = "NexaChat Messages";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM Token: " + token);
        FirebaseManager.getInstance().updateFcmToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "NexaChat";
        String body = "You have a new message";
        String conversationId = null;
        String senderId = null;

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        }

        if (remoteMessage.getData().size() > 0) {
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().containsKey("body")) {
                body = remoteMessage.getData().get("body");
            }
            if (remoteMessage.getData().containsKey("conversationId")) {
                conversationId = remoteMessage.getData().get("conversationId");
            }
            if (remoteMessage.getData().containsKey("senderId")) {
                senderId = remoteMessage.getData().get("senderId");
            }
        }

        // Privacy check: If user disabled preview, hide content
        SharedPreferences settingsPrefs = getSharedPreferences("nexachat_settings", Context.MODE_PRIVATE);
        boolean showPreview = settingsPrefs.getBoolean("show_notification_preview", true);
        if (!showPreview) {
            body = "New message received";
        }

        sendNotification(title, body, conversationId, senderId);
    }

    private void sendNotification(String title, String messageBody, String conversationId, String senderId) {
        Intent intent;
        if (conversationId != null) {
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("conversationId", conversationId);
            intent.putExtra("otherUserId", senderId);
            intent.putExtra("title", title);
        } else {
            intent = new Intent(this, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for incoming NexaChat messages");
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}
