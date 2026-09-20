package com.nexachat.app;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.nexachat.app.firebase.FirebaseManager;
import com.nexachat.app.firebase.NexaFirebaseMessagingService;

public class NexaChatApp extends Application {

    private int activeActivities = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Critical Safeguard: Always ensure MainActivity component is ENABLED so app never crashes on launch
        try {
            getPackageManager().setComponentEnabledSetting(
                    new ComponentName(this, MainActivity.class),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
            );
        } catch (Throwable ignored) {}

        // 2. Safely initialize Firebase
        try {
            FirebaseApp.initializeApp(this);
        } catch (Throwable ignored) {}

        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Throwable ignored) {}

        try {
            createNotificationChannel();
        } catch (Throwable ignored) {}

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                activeActivities++;
                if (activeActivities == 1 && FirebaseManager.getInstance().isUserLoggedIn()) {
                    FirebaseManager.getInstance().setupPresence();
                    FirebaseManager.getInstance().setOnlineStatus(true);
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {}

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                activeActivities--;
                if (activeActivities <= 0) {
                    activeActivities = 0;
                    if (FirebaseManager.getInstance().isUserLoggedIn()) {
                        FirebaseManager.getInstance().setOnlineStatus(false);
                    }
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NexaFirebaseMessagingService.CHANNEL_ID,
                    NexaFirebaseMessagingService.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("NexaChat incoming message alerts");
            channel.enableVibration(true);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
