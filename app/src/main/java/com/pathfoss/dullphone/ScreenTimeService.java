package com.pathfoss.dullphone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;
import java.util.Map;
import java.util.Objects;

public class ScreenTimeService extends Service {

    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";

    private NotificationCompat.Builder notification;

    private Context context;
    private Calendar midnightToday;
    private Map<String, UsageStats> lUsageStatsMap;
    private String timeText = "0h 0m";

    private long midnightTomorrow;
    private long screenTime = 0;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = this;
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
        return START_NOT_STICKY;
    }

    // Create notification channel
    private void startForegroundService() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.createNotificationChannel(new NotificationChannel("Dull Phone", "ScreenTime", NotificationManager.IMPORTANCE_LOW));
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "Dull Phone");

        setTimeBounds();
        notification =  notificationBuilder
                .setContentTitle("Screen time")
                .setContentText(timeText)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setSilent(true);

        lUsageStatsMap = Controller.getUsageStatsManager().queryAndAggregateUsageStats(midnightToday.getTimeInMillis(), midnightTomorrow);
        startForeground(1, notification.build());

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Please enable notifications to view screen time", Toast.LENGTH_SHORT).show();
                } else {
                    if (midnightTomorrow<= System.currentTimeMillis()) {
                        setTimeBounds();
                    }

                    lUsageStatsMap = Controller.getUsageStatsManager().queryAndAggregateUsageStats(midnightToday.getTimeInMillis(), midnightTomorrow);
                    screenTime = 0;

                    for (String packageName : lUsageStatsMap.keySet()) {
                        screenTime += Objects.requireNonNull(lUsageStatsMap.get(packageName)).getTotalTimeInForeground();
                    }

                    int hours = (int) (screenTime / 3600000);
                    int minutes = (int) ((screenTime - hours * 3600000) / 60000);
                    timeText =  hours+ "h " + minutes + "m";

                    notification.setContentText(timeText);
                    notificationManagerCompat.notify(1, notification.build());
                    handler.postDelayed(this, 60000);
                }
            }
        };
        handler.post(runnable);
    }

    // Create a method to set a midnight-to-midnight tracking period
    private void setTimeBounds() {
        midnightToday = Calendar.getInstance();
        midnightToday.set(Calendar.HOUR_OF_DAY, 0);
        midnightToday.set(Calendar.MINUTE, 0);
        midnightToday.set(Calendar.SECOND, 0);
        midnightToday.set(Calendar.MILLISECOND, 0);
        midnightTomorrow = midnightToday.getTimeInMillis() + 24 * 60 * 60 * 1000;
    }

    // Create a method to kill the service on request
    private void stopForegroundService() {
        stopForeground(true);
        stopSelf();
    }
}