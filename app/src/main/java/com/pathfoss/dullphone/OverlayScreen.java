package com.pathfoss.dullphone;

import android.annotation.SuppressLint;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import static android.content.Context.WINDOW_SERVICE;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OverlayScreen {

    // Create global objects and variables
    private final Context context;
    private final View view;
    private final WindowManager.LayoutParams layoutParameters;
    private final WindowManager windowManager;
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor sharedPreferencesEditor;
    private final LinearLayout whitelistAppsLinearLayout;
    private final ProgressBar progressBar, appsLoadingProgressBar, phoneLoadingProgressBar;
    private final ImageView skullImageView;
    private final TextView tapsLeftTextView;
    private final ConstraintLayout timerLayout;
    private final PackageManager packageManager;
    private final ImageButton appsImageButton, phoneImageButton;
    private final SystemNavigationTool systemNavigationTool;
    private final Handler homeButtonHandler, foregroundMonitorHandler, mainThreadHandler, workerThreadHandler;
    private boolean tapsEnabled = false;
    private boolean whitelistToggled = false;
    private long timeRemaining;
    
    @SuppressLint("InflateParams")
    public OverlayScreen(@NonNull Context context) {
        
        //Initialize service context and related tools
        this.context = context;
        systemNavigationTool = new SystemNavigationTool(context);
        homeButtonHandler = new Handler(Looper.getMainLooper());
        mainThreadHandler = new Handler(Looper.getMainLooper());
        foregroundMonitorHandler = new Handler();
        workerThreadHandler = new Handler();
        packageManager = context.getPackageManager();

        // Initialize SharedPreferences
        sharedPreferences = context.getSharedPreferences("DullPhone", Context.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();
        timeRemaining = sharedPreferences.getLong("blockedUntil", 0) - System.currentTimeMillis();

        // Set layout parameters and create the window
        layoutParameters = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.blocker_overlay_screen,null);
        windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        
        // Initialize layout elements
        whitelistAppsLinearLayout = view.findViewById(R.id.whitelistAppsLinearLayout);
        tapsLeftTextView = view.findViewById(R.id.tapsLeftTextView);
        skullImageView = view.findViewById(R.id.skullImageView);
        progressBar = view.findViewById(R.id.progress_bar);
        timerLayout = view.findViewById(R.id.timerLayout);
        
        appsLoadingProgressBar = view.findViewById(R.id.appsLoadingProgressBar);
        phoneLoadingProgressBar = view.findViewById(R.id.phoneLoadingProgressBar);

        appsImageButton = view.findViewById(R.id.appsImageButton);
        phoneImageButton = view.findViewById(R.id.phoneImageButton);
        
        // Fully disable epp exit temporarily
        sharedPreferencesEditor.putLong("homeClicked", System.currentTimeMillis()).apply();
        hideImageButtons();

        // Initialize all timers and whitelist applications' layout
        startEndTimer();
        startBlockTimer(view.findViewById(R.id.hourTextView), view.findViewById(R.id.minuteTextView), view.findViewById(R.id.secondTextView));
        startForegroundMonitorTimer(sharedPreferences.getStringSet("allowedApplications", new HashSet<>()), sharedPreferences.getString("defaultDialer", "com.android.dialer"), (UsageStatsManager) (context.getSystemService(Context.USAGE_STATS_SERVICE)));
        createWhitelistLayout();

        // Create OnClickListeners to prevent user exit
        createAppButtonListener();
        createPhoneButtonListener(sharedPreferences.getString("defaultDialer", "com.android.dialer"));
        createTapModeToggleListener(view.findViewById(R.id.tapToggleImageButton));
        createTapCounterListener();
        startSystemNavigationListener();
    }

    // Create method for counting taps in tap to leave mode
    private void createTapCounterListener () {
        progressBar.setOnClickListener( v -> {
            if (tapsEnabled) {
                int tapsLeft = sharedPreferences.getInt("tapsToUnlock", 5000) - 1;
                sharedPreferencesEditor.putInt("tapsToUnlock", tapsLeft).apply();

                // End the block if user taps sufficiently
                if (tapsLeft > 0) {
                    String tapsText = "Taps to unlock: " + tapsLeft;
                    tapsLeftTextView.setText(tapsText);
                } else {
                    endBlock();
                }
            }
        });
    }

    // Create method for toggling the whitelist application list
    private void createAppButtonListener () {
        new Handler().post(() -> appsImageButton.setOnClickListener(v -> toggleWhitelistLayout()));
    }

    // Create method for launching the default phone application
    private void createPhoneButtonListener (String packageName) {
        new Handler().post(() -> phoneImageButton.setOnClickListener(v -> {
            if (System.currentTimeMillis() - sharedPreferences.getLong("homeClicked", 0) > 5500) {
                launchWhitelistApp(packageName);
            }
        }));
    }

    // Create method for toggling tap to leave mode
    private void createTapModeToggleListener (@NonNull ImageButton tapToggleImageButton) {
        new Handler(Looper.getMainLooper()).post(() -> tapToggleImageButton.setOnClickListener(v -> {
            if (tapsEnabled) {
                toggleVisibility(new View[]{timerLayout, progressBar, skullImageView}, new View[]{tapsLeftTextView, whitelistAppsLinearLayout});
                tapToggleImageButton.setBackground(AppCompatResources.getDrawable(context, R.drawable.fingerprint_button_background));
            } else {
                toggleVisibility(new View[]{tapsLeftTextView, progressBar, skullImageView}, new View[]{timerLayout, whitelistAppsLinearLayout});
                String tapsText = "Taps to unlock: " + sharedPreferences.getInt("tapsToUnlock", 5000);
                tapsLeftTextView.setText(tapsText);
                tapToggleImageButton.setBackground(AppCompatResources.getDrawable(context, R.drawable.clock_button_background));
            }
            tapsEnabled = !tapsEnabled;
            whitelistToggled = false;
        }));
    }

    // Create method for toggling View elements' visibility
    private void toggleVisibility(@NonNull View[] visibleView, View[] goneView) {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (View view : visibleView) {
                view.setVisibility(View.VISIBLE);
            }
            for (View view : goneView) {
                view.setVisibility(View.GONE);
            }
        });
    }

    // Create method to draw whitelist apps on screen
    private void createWhitelistLayout () {
        for (String packageName : sharedPreferences.getStringSet("allowedApplications", new HashSet<>())) {
            new Handler(Looper.getMainLooper()).post(() -> {

                // Initialize Views
                LinearLayout linearLayout = new LinearLayout(context);
                ImageView appIcon = new ImageView(context);
                TextView appName = new TextView(context);

                // Set style and handle PackageManager exceptions
                try {
                    appIcon.setBackground(packageManager.getApplicationIcon(packageName));
                    appName.setText(packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)));
                    appName.setTextSize(20);
                    appName.setTextColor(context.getResources().getColor(R.color.natural_white, context.getTheme()));
                } catch (Exception ignored) {}

                // Set layout parameters for each view
                linearLayout.setLayoutParams(createLayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT, 0, 20));
                appIcon.setLayoutParams(createLayoutParams(80, 80, 0, 0));
                appName.setLayoutParams(createLayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 80, 20, 0));

                // Add views to create an app container and initialize listener
                addViewsToViews(linearLayout, new View[]{appIcon, appName});
                addViewsToViews(whitelistAppsLinearLayout, new View[]{linearLayout});
                createWhitelistAppClickListener(linearLayout, packageName);
            });
        }
    }

    // Create method for adding views
    private void addViewsToViews (@NonNull LinearLayout adder, @NonNull View[] addable) {
        for (View view : addable) {
            adder.addView(view);
        }
    }

    // Create method for creating LayoutParams
    @NonNull
    private RelativeLayout.LayoutParams createLayoutParams (int width, int height, int marginLeft, int marginBottom) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
        layoutParams.setMargins(marginLeft, 0, 0, marginBottom);
        layoutParams.addRule(Gravity.CENTER_VERTICAL);
        return layoutParams;
    }

    // Create method for generating OnClickListeners for whitelisted apps
    private void createWhitelistAppClickListener (LinearLayout linearLayout, String packageName) {
        new Handler().post(() -> linearLayout.setOnClickListener(v -> {
            if (System.currentTimeMillis() - sharedPreferences.getLong("homeClicked", 0) > 5000) {
                launchWhitelistApp(packageName);
            }
        }));
    }

    // Create method to start a whitelisted application
    private void launchWhitelistApp (String packageName) {
        context.startActivity(packageManager.getLaunchIntentForPackage(packageName));
        sharedPreferencesEditor.putBoolean("usingWhitelistApplication", true).apply();
        sharedPreferencesEditor.putLong("whitelistApplicationAccessTime", System.currentTimeMillis()).apply();
        removeOverlay();
    }

    // Create method to toggle whitelist visibility
    private void toggleWhitelistLayout () {
        if (whitelistToggled) {
            toggleVisibility(new View[]{progressBar, skullImageView, timerLayout}, new View[]{whitelistAppsLinearLayout, tapsLeftTextView});
        } else {
            toggleVisibility(new View[]{whitelistAppsLinearLayout, timerLayout}, new View[]{progressBar, skullImageView, tapsLeftTextView});
        }
        whitelistToggled = !whitelistToggled;
    }

    // Create method to end the block
    private void startEndTimer () {
        new Handler().postDelayed(this::endBlock, timeRemaining);
    }

    // Create method to display remaining time and prevent user escape
    private void startBlockTimer (TextView hourText, TextView minuteText, TextView secondText) {

        // Initialize time values and handlers
        long goalTime = sharedPreferences.getLong("blockedUntil", 0);
        long fixedTimeLeft = goalTime - System.currentTimeMillis();

        // Create timer to change TextViews every second until the end of block
        workerThreadHandler.post(new Runnable() {
            @Override
            public void run() {

                long timeLeft = goalTime - System.currentTimeMillis();
                int hours = (int) timeLeft / 3600000;
                int minutes = (int) (timeLeft - hours * 3600000) / 60000;
                int second = (int) (timeLeft - hours * 3600000 - minutes * 60000) / 1000;
                int progress = Math.round(((float) (fixedTimeLeft - goalTime + System.currentTimeMillis()) / (float) fixedTimeLeft) * (10000));

                mainThreadHandler.post(() -> {
                    hourText.setText(String.valueOf(hours));
                    minuteText.setText(String.valueOf(minutes));
                    secondText.setText(String.valueOf(second));
                    progressBar.setProgress(progress);
                });

                workerThreadHandler.postDelayed(this, 1000 - timeLeft + goalTime - System.currentTimeMillis());
            }
        });
    }

    // Create a method to prevent disallowed apps to run in the foreground
    private void startForegroundMonitorTimer (Set<String> allowedApps, String defaultDialer, UsageStatsManager usageStatsManager) {
        foregroundMonitorHandler.post(new Runnable() {
            @Override
            public void run() {

                // Initialize variables
                long currentTime = System.currentTimeMillis();
                UsageEvents events = usageStatsManager.queryEvents(currentTime - 100, currentTime);
                UsageEvents.Event event = new UsageEvents.Event();

                // Monitor events for applications and close them accordingly
                List<UsageStats> appList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,currentTime - 100, currentTime);

                // Check if user has opened a non-whitelist application
                if (sharedPreferences.getLong("timeUserLeft", 0) > currentTime - 100
                        && !sharedPreferences.getBoolean("usingWhitelistApplication", false)
                        && sharedPreferences.getLong("timeRestarted", 0) < currentTime - 1000) {

                    restartActivity();
                } else {
                    for (UsageStats app : appList) {
                        if (app.getPackageName().contains("com.android.settings") && app.getLastTimeUsed() >= currentTime - 100) {
                            sharedPreferencesEditor.putBoolean("usingWhitelistApplication", false).apply();

                            restartActivity();
                            break;
                        }
                    } while (events.hasNextEvent()) {
                        events.getNextEvent(event);

                        if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                            if (!allowedApps.contains(event.getPackageName()) && !event.getPackageName().equals(defaultDialer)) {

                                sharedPreferencesEditor.putBoolean("usingWhitelistApplication", false).apply();
                                createOverlay();
                                break;
                            }
                        }
                    }
                }

                if (timeRemaining > 100) {
                    timeRemaining -= 100;
                    foregroundMonitorHandler.postDelayed(this, 100);
                }
            }
        });
    }

    // Create method to temporarily hide buttons when home button is clicked
    private void hideImageButtons () {
        toggleVisibility(new View[] {progressBar, skullImageView, appsLoadingProgressBar, phoneLoadingProgressBar}, new View[]{appsImageButton, phoneImageButton, whitelistAppsLinearLayout});
        homeButtonHandler.removeCallbacksAndMessages(null);
        homeButtonHandler.postDelayed(() -> {
            if (System.currentTimeMillis() - sharedPreferences.getLong("homeClicked", 0) > 5000) {
                toggleVisibility(new View[]{appsImageButton, phoneImageButton}, new View[]{appsLoadingProgressBar, phoneLoadingProgressBar});
            }
        }, 5500);
    }

    // Create method to identify system navigation clicks and prevent user exit
    private void startSystemNavigationListener() {
        systemNavigationTool.setNavigationListener(new SystemNavigationListener() {

            @Override
            public void onHomePressed() {
                sharedPreferencesEditor.putLong("homeClicked", System.currentTimeMillis()).apply();
                createOverlay();
                hideImageButtons();
            }

            @Override
            public void onRecentAppsPressed() {
                createOverlay();
            }
        });
        systemNavigationTool.startNavigationListener();
    }

    // Create method to reset SharedPreferences and close overlay
    private void endBlock () {
        systemNavigationTool.stopNavigationListener();
        foregroundMonitorHandler.removeCallbacksAndMessages(null);
        homeButtonHandler.removeCallbacksAndMessages(null);
        mainThreadHandler.removeCallbacksAndMessages(null);
        workerThreadHandler.removeCallbacksAndMessages(null);
        sharedPreferencesEditor.putBoolean("usingWhitelistApplication", false).apply();
        removeOverlay();
        Intent intent = new Intent(context, OverlayService.class);
        intent.setAction(OverlayService.ACTION_STOP_FOREGROUND_SERVICE);
        context.startService(intent);
        restartActivity();
    }

    // Create method to restart StartMenuActivity
    private void restartActivity () {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        assert intent != null;
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    // Create method to remove the overlay screen
    private void removeOverlay () {
        try {
            ((WindowManager) context.getSystemService(WINDOW_SERVICE)).removeView(view);
            view.invalidate();
            ((ViewGroup) view.getParent()).removeAllViews();
        } catch (Exception ignored) {}
    }

    // Create method to start the overlay screen
    public void createOverlay() {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (view.getWindowToken() == null && view.getParent() == null) {
                windowManager.addView(view, layoutParameters);
            }
        });
    }
}