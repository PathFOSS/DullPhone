package com.pathfoss.dullphone;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static android.content.Context.WINDOW_SERVICE;

import android.annotation.SuppressLint;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class OverlayScreen {

    private ConstraintLayout clRow;

    private final View view;
    private final ConstraintLayout timerLayout;
    private final LinearLayout linearLayout;
    private final ProgressBar pbTime;
    private final ProgressBar pbLoadingWhitelist;
    private final ProgressBar pbLoadingDialer;
    private final ImageButton ibWhitelist;
    private final ImageButton ibDialer;
    private final ImageView ivIcon;
    private final TextView tvTapsLeft;

    private final Context context;

    private final WindowManager.LayoutParams layoutParameters;
    private final WindowManager windowManager;

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor sharedPreferencesEditor;

    private final PackageManager packageManager;
    private final NavigationTool systemNavigationTool;

    private final Handler homeButtonHandler;
    private final Handler foregroundMonitorHandler;
    private final Handler mainThreadHandler;
    private final Handler workerThreadHandler;
    private final Vibrator vibrator = Controller.vibrator;

    private final boolean vibrationEnabled;

    private boolean tapsEnabled = false;
    private boolean whitelistToggled = false;

    private long timeRemaining;
    private long midnightTomorrow;
    private long whiteListStart;
    private long whiteListStop;
    private int currentImage = 0;

    @SuppressLint("InflateParams")
    public OverlayScreen(@NonNull Context context) {

        //Initialize service context and related tools
        this.context = context;
        systemNavigationTool = new NavigationTool(context);
        homeButtonHandler = new Handler(Looper.getMainLooper());
        mainThreadHandler = new Handler(Looper.getMainLooper());
        foregroundMonitorHandler = new Handler();
        workerThreadHandler = new Handler();
        packageManager = context.getPackageManager();

        // Initialize SharedPreferences
        sharedPreferences = context.getSharedPreferences("DullPhone", Context.MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();
        timeRemaining = sharedPreferences.getLong("UnlockTime", 0) - System.currentTimeMillis();
        vibrationEnabled = sharedPreferences.getBoolean("TapVibration", false);

        // Set layout parameters and create the window
        layoutParameters = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.blocker_overlay_screen,null);
        windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);

        // Initialize layout elements
        linearLayout = view.findViewById(R.id.ll);
        tvTapsLeft = view.findViewById(R.id.tv_taps_left);
        ivIcon = view.findViewById(R.id.iv_icon);
        pbTime = view.findViewById(R.id.pb_time);
        timerLayout = view.findViewById(R.id.cl_timer);

        pbLoadingWhitelist = view.findViewById(R.id.pb_loading_whitelist);
        pbLoadingDialer = view.findViewById(R.id.pb_loading_dialer);

        ibWhitelist = view.findViewById(R.id.ib_whitelist);
        ibDialer = view.findViewById(R.id.ib_dialer);

        ivIcon.setBackgroundResource(sharedPreferences.getInt("DefaultIcon", R.drawable.icon_dullphone));

        // Fully disable epp exit temporarily
        sharedPreferencesEditor.putLong("HomePressed", System.currentTimeMillis()).apply();
        hideImageButtons();

        // Initialize all timers and whitelist applications' layout
        startBlockTimer(view.findViewById(R.id.tv_hour), view.findViewById(R.id.tv_minute), view.findViewById(R.id.tv_second));
        startForegroundMonitorTimer(sharedPreferences.getStringSet("WhitelistApps", new HashSet<>()), sharedPreferences.getString("DefaultDialer", "com.android.dialer"), (UsageStatsManager) (context.getSystemService(Context.USAGE_STATS_SERVICE)));

        if (sharedPreferences.getBoolean("WhitelistEnabled", true)) {
            createWhitelistLayout();
        }

        // Create OnClickListeners to prevent user exit
        createAppButtonListener();
        createPhoneButtonListener(sharedPreferences.getString("DefaultDialer", "com.android.dialer"));
        createTapModeToggleListener(view.findViewById(R.id.ib_tap_toggle));
        createTapCounterListener();
        startSystemNavigationListener();

        // Set day range boundaries to enable whitelist range checks
        setTimeBounds();
    }

    // Create method to set midnight today and tomorrow for whitelist day boundaries
    private void setTimeBounds () {
        Calendar midnightToday = Calendar.getInstance();
        midnightToday.set(Calendar.HOUR_OF_DAY, 0);
        midnightToday.set(Calendar.MINUTE, 0);
        midnightToday.set(Calendar.SECOND, 0);
        midnightToday.set(Calendar.MILLISECOND, 0);
        whiteListStart = midnightToday.getTimeInMillis() + sharedPreferences.getInt("WhitelistActiveStart", 0);
        whiteListStop = midnightToday.getTimeInMillis() + sharedPreferences.getInt("WhitelistActiveStop", 86400000);
        midnightTomorrow = midnightToday.getTimeInMillis() + 86400000;
    }

    // Create method for counting taps in tap to leave mode
    private void createTapCounterListener () {
        pbTime.setOnClickListener( v -> {
            if (tapsEnabled) {
                int tapsLeft = sharedPreferences.getInt("TapsToUnlock", 5000) - 1;
                sharedPreferencesEditor.putInt("TapsToUnlock", tapsLeft).apply();

                if (vibrationEnabled) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                }
                // End the block if user taps sufficiently
                if (tapsLeft > 0) {
                    String tapsText = "Taps to unlock: " + tapsLeft;
                    tvTapsLeft.setText(tapsText);
                } else {
                    endBlock();
                }
            }
        });
    }

    // Create method for toggling the whitelist application list
    private void createAppButtonListener () {
        new Handler().post(() -> ibWhitelist.setOnClickListener(v -> {
            if (sharedPreferences.getBoolean("WhitelistEnabled", true)) {
                toggleWhitelistLayout();
            }
        })
        );
    }

    // Create method for launching the default phone application
    private void createPhoneButtonListener (String packageName) {
        new Handler().post(() -> ibDialer.setOnClickListener(v -> {
            if (System.currentTimeMillis() - sharedPreferences.getLong("HomePressed", 0) > 5500) {
                launchWhitelistApp(packageName);
            }
        }));
    }

    // Create method for toggling tap to leave mode
    private void createTapModeToggleListener (@NonNull ImageButton tapToggleImageButton) {
        new Handler(Looper.getMainLooper()).post(() -> tapToggleImageButton.setOnClickListener(v -> {
            if (tapsEnabled) {
                toggleVisibility(new View[]{timerLayout, pbTime, ivIcon}, new View[]{tvTapsLeft, linearLayout});
                tapToggleImageButton.setBackground(AppCompatResources.getDrawable(context, R.drawable.fingerprint_button_background));
            } else {
                toggleVisibility(new View[]{tvTapsLeft, pbTime, ivIcon}, new View[]{timerLayout, linearLayout});
                String tapsText = "Taps to unlock: " + sharedPreferences.getInt("TapsToUnlock", 5000);
                tvTapsLeft.setText(tapsText);
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
                view.setVisibility(View.INVISIBLE);
            }
        });
    }

    // Create method to draw whitelist apps on screen
    private void createWhitelistLayout () {

        // Order whitelist apps alphabetically
        SortedMap<String, String> sortedAppList = new TreeMap<>();
        for (String packageName : sharedPreferences.getStringSet("WhitelistApps", new HashSet<>())) {
            try {
                sortedAppList.put((String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)), packageName);
            } catch (Exception ignored){}
        }

        // Create a clickable layout for each app
        new Handler(Looper.getMainLooper()).post(() -> {
            for (Map.Entry<String, String> stringStringEntry : sortedAppList.entrySet()) {

                // Define index and package name
                currentImage++;
                String packageName = stringStringEntry.getValue();

                switch (currentImage % 3) {
                    case 0:
                        clRow.findViewById(R.id.iv_3).setBackground(getIcon(packageName));
                        createWhitelistAppClickListener(clRow.findViewById(R.id.iv_3), packageName);
                        break;
                    case 1:
                        clRow = (ConstraintLayout) ((LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.layout_app_rows, (ViewGroup) view.getParent(), false);
                        clRow.findViewById(R.id.iv_1).setBackground(getIcon(packageName));
                        linearLayout.addView(clRow);
                        createWhitelistAppClickListener(clRow.findViewById(R.id.iv_1), packageName);
                        break;
                    case 2:
                        clRow.findViewById(R.id.iv_2).setBackground(getIcon(packageName));
                        createWhitelistAppClickListener(clRow.findViewById(R.id.iv_2), packageName);
                        break;
                }
            }
        });
    }

    // Create method to fetch icon without errors
    @Nullable
    private Drawable getIcon (String packageName) {
        try {
            return packageManager.getApplicationIcon(packageName);
        } catch (Exception ignored) {
            return null;
        }
    }

    // Create method for generating OnClickListeners for whitelisted apps
    private void createWhitelistAppClickListener (ImageView imageView, String packageName) {
        new Handler().post(() -> imageView.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - sharedPreferences.getLong("HomePressed", 0) > 5000 && whiteListStart < currentTime && whiteListStop > currentTime) {
                launchWhitelistApp(packageName);
            }
        }));
    }

    // Create method to start a whitelisted application
    private void launchWhitelistApp (String packageName) {
        context.startActivity(packageManager.getLaunchIntentForPackage(packageName));
        sharedPreferencesEditor.putBoolean("UsingWhitelistApp", true).apply();
        removeOverlay();
    }

    // Create method to toggle whitelist visibility
    private void toggleWhitelistLayout () {
        if (whitelistToggled) {
            toggleVisibility(new View[]{pbTime, ivIcon, timerLayout}, new View[]{linearLayout, tvTapsLeft});
        } else {
            toggleVisibility(new View[]{linearLayout, timerLayout}, new View[]{pbTime, ivIcon, tvTapsLeft});
        }
        whitelistToggled = !whitelistToggled;
    }

    // Create method to display remaining time and prevent user escape
    private void startBlockTimer (TextView hourText, TextView minuteText, TextView secondText) {

        // Initialize time values and handlers
        long goalTime = sharedPreferences.getLong("UnlockTime", 0);
        long fixedTimeLeft = goalTime - System.currentTimeMillis();

        // Create timer to change TextViews every second until the end of block
        workerThreadHandler.post(new Runnable() {
            @Override
            public void run() {

                long timeLeft = goalTime - System.currentTimeMillis();
                int hours = (int) (timeLeft / 3600000);
                int minutes = (int) (timeLeft - hours * 3600000) / 60000;
                int seconds = (int) (timeLeft - hours * 3600000 - minutes * 60000) / 1000;
                int progress = Math.round(((float) (fixedTimeLeft - goalTime + System.currentTimeMillis()) / (float) fixedTimeLeft) * (10000));

                mainThreadHandler.post(() -> {
                    hourText.setText(getTimeNumber(hours));
                    minuteText.setText(getTimeNumber(minutes));
                    secondText.setText(getTimeNumber(seconds));
                    pbTime.setProgress(progress);
                });

                if (midnightTomorrow < System.currentTimeMillis()) {
                    setTimeBounds();
                }

                if (timeLeft > 0) {
                    workerThreadHandler.postDelayed(this, 1000 - timeLeft + goalTime - System.currentTimeMillis());
                } else {
                    endBlock();
                }
            }
        });
    }

    // Create method to get consistent time numbers
    @NonNull
    private String getTimeNumber(int input) {
        if (input < 10) {
            return "0" + input;
        }
        return String.valueOf(input);
    }

    // Create a method to prevent disallowed apps to run in the foreground
    private void startForegroundMonitorTimer (Set<String> allowedApps, String DefaultDialer, UsageStatsManager usageStatsManager) {
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
                if (sharedPreferences.getLong("UserLeaveTime", 0) > currentTime - 100
                        && !sharedPreferences.getBoolean("UsingWhitelistApp", false)
                        && sharedPreferences.getLong("TimeRestarted", 0) < currentTime - 1000) {
                    restartActivity();
                } else {
                    for (UsageStats app : appList) {
                        if (app.getPackageName().contains("com.android.settings") && app.getLastTimeUsed() >= currentTime - 100) {
                            sharedPreferencesEditor.putBoolean("UsingWhitelistApp", false).apply();
                            restartActivity();
                            break;
                        }
                    } while (events.hasNextEvent()) {
                        events.getNextEvent(event);

                        if (event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                            if (!allowedApps.contains(event.getPackageName()) && !event.getPackageName().equals(DefaultDialer)) {

                                sharedPreferencesEditor.putBoolean("UsingWhitelistApp", false).apply();
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
        toggleVisibility(new View[] {pbTime, ivIcon, pbLoadingWhitelist, pbLoadingDialer}, new View[]{ibWhitelist, ibDialer, linearLayout});
        homeButtonHandler.removeCallbacksAndMessages(null);
        homeButtonHandler.postDelayed(() -> {
            if (System.currentTimeMillis() - sharedPreferences.getLong("HomePressed", 0) > 5000) {
                toggleVisibility(new View[]{ibWhitelist, ibDialer}, new View[]{pbLoadingWhitelist, pbLoadingDialer});
            }
        }, 5500);
    }

    // Create method to identify system navigation clicks and prevent user exit
    private void startSystemNavigationListener() {
        systemNavigationTool.setNavigationListener(new NavigationListener() {

            @Override
            public void onHomePressed() {
                sharedPreferencesEditor.putLong("HomePressed", System.currentTimeMillis()).apply();
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
        sharedPreferencesEditor.putBoolean("UsingWhitelistApp", false).apply();
        sharedPreferencesEditor.putLong("UnlockTime", 0).apply();
        removeOverlay();
        Intent intent = new Intent(context, OverlayService.class);
        intent.setAction(OverlayService.ACTION_STOP_FOREGROUND_SERVICE);
        context.startService(intent);
        restartActivity();
    }

    // Create method to restart Controller
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