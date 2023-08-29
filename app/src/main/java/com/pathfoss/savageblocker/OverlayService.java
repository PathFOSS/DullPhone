package com.pathfoss.savageblocker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class OverlayService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize overlay from the OverlayScreen class
        (new OverlayScreen(this)).initializeOverlay();
        launchOverlayAsForeground();
    }

    // Create notification channel
    private void launchOverlayAsForeground() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.createNotificationChannel(new NotificationChannel("Savage Blocker", "Background Service", NotificationManager.IMPORTANCE_HIGH));

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "Savage Blocker");
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Device screen is blocked")
                .setContentText("You are unable to use other apps")
                .setSmallIcon(R.drawable.ic_stat_name)
                .setPriority(NotificationManager.IMPORTANCE_MAX)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }
}