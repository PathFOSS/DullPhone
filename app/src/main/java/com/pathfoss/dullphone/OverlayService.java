package com.pathfoss.dullphone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class OverlayService extends Service {
    
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_START_FOREGROUND_SERVICE:
                    startForegroundService();
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopForegroundService();
                    break;
            }
        }
        return START_STICKY;
    }

    // Create notification channel
    private void startForegroundService() {
        (new OverlayScreen(this)).createOverlay();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.createNotificationChannel(new NotificationChannel("Dull Phone", "Background Service", NotificationManager.IMPORTANCE_HIGH));

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "Dull Phone");
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Device screen is blocked")
                .setContentText("Only whitelisted apps allowed")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setPriority(NotificationManager.IMPORTANCE_MAX)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setSilent(true)
                .build();
        startForeground(2, notification);
    }

    // Create a method to kill the service on request
    private void stopForegroundService() {
        stopForeground(true);
        stopSelf();
    }
}