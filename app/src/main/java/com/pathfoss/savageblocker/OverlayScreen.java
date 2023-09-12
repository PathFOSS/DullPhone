package com.pathfoss.savageblocker;

import android.annotation.SuppressLint;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import static android.content.Context.WINDOW_SERVICE;
import java.util.Calendar;
import java.util.List;

public class OverlayScreen {

    // Create global objects and variables
    private final Context context;
    private final View view;
    private final WindowManager.LayoutParams layoutParameters;
    private final WindowManager windowManager;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor sharedPreferencesEditor;
    private final ProgressBar progressBar;
    private final TextView hourText;
    private final TextView minuteText;
    private final TextView secondText;
    private final int tapsToUnlock = 2500;
    private boolean tapTextToggled = false;
    private int tapCount = 1;

    @SuppressLint("InflateParams")
    public OverlayScreen(Context context){
        this.context = context;

        // Initialize SharedPreferences
        sharedPreferences = context.getSharedPreferences("SavageBlocker", Context.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();
        
        // Set layout parameters and inflate the window
        layoutParameters = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = layoutInflater.inflate(R.layout.blocker_overlay_screen,null);
        windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        
        // Initialize layout elements
        hourText = view.findViewById(R.id.hourTextView);
        minuteText = view.findViewById(R.id.minuteTextView);
        secondText = view.findViewById(R.id.secondTextView);

        ImageButton imageButton = view.findViewById(R.id.helperImageButton);
        progressBar = view.findViewById(R.id.progress_bar);

        TextView toggleTapsTextView = view.findViewById(R.id.toggleTapsTextView);

        // Create OnClickListener to toggle help section
        imageButton.setOnClickListener(v -> {
            if (tapTextToggled) {
                toggleTapsTextView.setVisibility(View.GONE);
                tapTextToggled = false;
            } else {
                toggleTapsTextView.setVisibility(View.VISIBLE);
                tapTextToggled = true;
            }
        });

        // Create OnClickListener for early block exit
        view.setOnClickListener( v -> {
            tapCount += 1;
            String tapsLeftText = "Taps left to unblock: " + (tapsToUnlock - tapCount);
            toggleTapsTextView.setText(tapsLeftText);
        });

        // Start all timers
        startBlockTimer();
        startProgressTimer();
    }

    // Create method to display remaining time and to prevent user escape
    private void startBlockTimer() {
        long timeRemaining = sharedPreferences.getLong("blockedUntil", 0) - System.currentTimeMillis();
        UsageStatsManager usageStatsManager = (UsageStatsManager)context.getSystemService(Context.USAGE_STATS_SERVICE);

        // Create timer to change TextViews every second until the end of block
        new CountDownTimer(timeRemaining, 1000) {
            @Override
            public void onTick(long l) {
                int h = (int) l / 3600000;
                int m = (int) (l - h * 3600000) / 60000;
                int s = (int) (l - h * 3600000 - m * 60000) / 1000;

                hourText.setText(String.valueOf(h));
                minuteText.setText(String.valueOf(m));
                secondText.setText(String.valueOf(s));

                long currentTime = System.currentTimeMillis();
                List<UsageStats> appList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,currentTime - 950, currentTime);
                
                // Force restart overlay screen if user has opened other apps except home or tasks
                for (UsageStats app : appList) {
                    String packageName = app.getPackageName();

                    if (!packageName.equals("com.pathfoss.savageblocker") 
                            && !packageName.equals(sharedPreferences.getString("defaultHome", "com.android.launcher3"))
                            && !packageName.equals(sharedPreferences.getString("defaultTaskManager", "com.android.launcher3"))
                            && app.getLastTimeUsed() >= currentTime - 950) {

                        createRemoveOverlayThread();
                        createRestartAppThread();
                    }
                }

                // Check is user has enough taps to end block early
                if (tapCount >= tapsToUnlock) {
                    cancel();
                    endBlock();
                }
            }

            @Override
            public void onFinish() {
                endBlock();
            }
        }.start();
    }

    // Create method to set smoother progress in ProgressBar
    private void startProgressTimer() {
        long timeLeft = sharedPreferences.getLong("blockedUntil", 0) - Calendar.getInstance().getTimeInMillis();

        // Set timer to run every 10 milliseconds
        new CountDownTimer(timeLeft, 10) {
            @Override
            public void onTick(long l) {
                progressBar.setProgress(Math.round(((float) (timeLeft - l) / (float) timeLeft) * (10000)));
            }
            @Override
            public void onFinish() {
            }
        }.start();
    }

    // Create method to end block upon request
    private void endBlock () {

        // Reset SharedPreferences and close overlay
        sharedPreferencesEditor.putLong("blockedUntil", 0).apply();
        sharedPreferencesEditor.putBoolean("isBlocking", false).apply();
        context.stopService(new Intent(context, OverlayService.class));
        createRemoveOverlayThread();
        createRestartAppThread();
    }

    // Create method to remove and invalidate the overlay screen
    private void createRemoveOverlayThread () {
        new Thread(() -> {
            try {
                ((WindowManager) context.getSystemService(WINDOW_SERVICE)).removeView(view);
                view.invalidate();
                ((ViewGroup) view.getParent()).removeAllViews();
            } catch (Exception ignored) {}
        }).start();
    }

    // Create method to restart StartMenuActivity
    private void createRestartAppThread () {
        new Thread(() -> {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            assert intent != null;
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        }).start();
    }

    // Create method to start the overlay screen from service
    public void initializeOverlay() {
        try {
            if(view.getWindowToken() == null && view.getParent() == null) {
                windowManager.addView(view, layoutParameters);
            }
        } catch (Exception ignored) {}
    }
}