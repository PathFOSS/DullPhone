package com.pathfoss.dullphone;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;

public class OverlayNotification extends Application {
    public static final String CHANNEL_ID = "DullPhone";
    public static final String CHANNEL_NAME = "DullPhone";

    // Create a new notification channel
    @Override
    public void onCreate() {
        super.onCreate();
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT));
    }
}