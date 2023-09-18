package com.pathfoss.savageblocker;

import android.app.AppOpsManager;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class StartMenuActivity extends AppCompatActivity {

    // Create global objects and variables
    private final ArrayList<String> allowedApps = new ArrayList<>();
    private PackageManager packageManager;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;
    private ConstraintLayout mainContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize global items
        sharedPreferences = getSharedPreferences("SavageBlocker", MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();
        packageManager = getPackageManager();

        // Check if user opens app first time or returns with an active block
        if (sharedPreferences.getLong("blockedUntil", 0) <= System.currentTimeMillis()) {
            if (sharedPreferences.getBoolean("firstLaunch", true)) {
                showDisclaimer();
            } else {
                initializeLayout();
            }
        }
    }

    // Create method to display the main menu
    private void initializeLayout() {
        new Handler(Looper.getMainLooper()).post(() -> {

            // Initialize entire layout to toggle
            setContentView(R.layout.start_menu_screen);
            mainContainer = findViewById(R.id.mainContainer);

            // Initialize NumberPickers for time selection
            NumberPicker dayPicker = findViewById(R.id.dayNumberPicker);
            NumberPicker hourPicker = findViewById(R.id.hourNumberPicker);
            NumberPicker minutePicker = findViewById(R.id.minuteNumberPicker);

            // Set NumberPicker values for time selection
            setNumberPickerValues(dayPicker, 365, "d");
            setNumberPickerValues(hourPicker, 23, "h");
            setNumberPickerValues(minutePicker, 59, "m");

            // Create a button listener to see when user wants to start block
            findViewById(R.id.confirmBlockButton).setOnClickListener(v -> {
                if (Settings.canDrawOverlays(getApplicationContext()) && isAccessGranted()) {
                    sharedPreferencesEditor.putInt("tapsToUnlock", 5000);
                    createConfirmationDialog(createDialog(R.layout.block_confirmation_dialog), modifyCalendarInstance(Calendar.getInstance(), dayPicker.getValue(), hourPicker.getValue(), minutePicker.getValue()));
                } else {
                    checkUsagePermission();
                    checkOverlayPermission();
                }
            });

            // Determine default dialer
            String defaultDialer = Objects.requireNonNull(packageManager.resolveActivity(new Intent(Intent.ACTION_DIAL), PackageManager.MATCH_DEFAULT_ONLY)).activityInfo.packageName;
            sharedPreferencesEditor.putString("defaultDialer", defaultDialer).apply();

            findViewById(R.id.appsImageButton).setOnClickListener(v -> createWhitelistDialog());
        });
    }

    // Create method to set NumberPicker values
    private void setNumberPickerValues(NumberPicker numberPicker, int maxValue, String timeUnit) {
        String[] timePickerArray = new String[maxValue + 1];
        for (int i = 0; i <= maxValue; i++) {
            timePickerArray[i] = i + " " + timeUnit;
        }

        numberPicker.setValue(0);
        numberPicker.setMaxValue(maxValue);
        numberPicker.setDisplayedValues(timePickerArray);
    }

    // Create method to show and return dialogs
    @NonNull
    private Dialog createDialog(int layoutFile) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(layoutFile);
        Objects.requireNonNull(dialog.getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }

    // Create method to show disclaimer dialog on first boot
    private void showDisclaimer() {
        Dialog dialog1 = createDialog(R.layout.disclaimer_dialog);

        dialog1.findViewById(R.id.button).setOnClickListener(v -> {
            dialog1.dismiss();
            initializeLayout();
            sharedPreferencesEditor.putBoolean("firstLaunch", false).apply();
        });
    }

    // Create method to allow to select whitelisted applications
    private void createWhitelistDialog() {

        // Initialize required whitelist elements
        Dialog dialog = createDialog(R.layout.whitelist_dialog);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> appList = packageManager.queryIntentActivities(intent, 0);
        LinearLayout whitelistLinearLayout = dialog.findViewById(R.id.whitelistLinearLayout);

        // Create layout for each enabled app
        for (ResolveInfo resolveInfo : appList) {

            // Define package name
            String packageName = resolveInfo.activityInfo.packageName;

            if (!packageName.equals(sharedPreferences.getString("defaultDialer", "com.android.dialer"))
                    && !packageName.equals("com.pathfoss.savageblocker")
                    && !packageName.equals("com.android.settings")) {

                // Initialize layout elements
                RelativeLayout appContainer = new RelativeLayout(StartMenuActivity.this);
                ImageView appIcon = new ImageView(StartMenuActivity.this);
                TextView appName = new TextView(StartMenuActivity.this);

                // Stylize app containers with identifiers
                try {
                    appContainer.setBackground(AppCompatResources.getDrawable(StartMenuActivity.this, R.drawable.button_background));
                    appContainer.setTag(packageName);
                    appIcon.setBackground(packageManager.getApplicationIcon(packageName));
                    appName.setTextSize(20);
                    appName.setTextColor(getResources().getColor(R.color.black, getTheme()));
                    appName.setText((String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)));
                } catch (Exception ignored) {}

                // Add icons and names in containers
                appContainer.addView(appName);
                appContainer.addView(appIcon);
                whitelistLinearLayout.addView(appContainer);

                // Set layout parameters for layouts
                appContainer.setLayoutParams(createLayoutParams(false, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0, 0, 0, 0));
                appIcon.setLayoutParams(createLayoutParams(true, 100, 100, RelativeLayout.ALIGN_PARENT_LEFT, 20, 20, 20));
                appName.setLayoutParams(createLayoutParams(true, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.ALIGN_PARENT_RIGHT, 20, 20, 20));

                // Highlight old whitelist applications
                if (sharedPreferences.getStringSet("allowedApplications", new HashSet<>()).contains(packageName)) {
                    allowedApps.add((String) packageName);
                    appContainer.setBackgroundTintList(getResources().getColorStateList(R.color.gray, getTheme()));
                    appName.setTextColor(AppCompatResources.getColorStateList(StartMenuActivity.this, R.color.natural_white));
                }

                // Initialize listener for the application selector
                createWhitelistAppClickListener(appContainer, appName);
            }
        }

        // Listeners for whitelist confirmation or cancellation
        createWhitelistConfirmationListener(dialog);
        createWhitelistCancelListener(dialog);
    }

    // Create method for starting whitelist application list dialog confirmation listener
    private void createWhitelistConfirmationListener (Dialog dialog) {
        new Handler().post(() -> {

            // Add whitelisted applications to SharedPreferences
            dialog.findViewById(R.id.confirmImageButton).setOnClickListener(v -> {
                sharedPreferencesEditor.putStringSet("allowedApplications", new HashSet<>(allowedApps)).apply();
                dialog.dismiss();
            });
        });
    }

    // Create method for starting whitelist application list dialog termination listener
    private void createWhitelistCancelListener (Dialog dialog) {
        new Handler().post(() -> {

            // Remove selected items from temporary list
            dialog.findViewById(R.id.cancelImageButton).setOnClickListener(v -> {
                allowedApps.subList(0, allowedApps.size()).clear();
                dialog.dismiss();
            });
        });
    }

    // Create method for creating LayoutParams
    @NonNull
    private ViewGroup.LayoutParams createLayoutParams(boolean relative, int width, int height, int alignMode, int marginLeft, int marginTop, int marginRight) {
        if (relative) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(width, height);
            layoutParams.setMargins(marginLeft, marginTop, marginRight, 20);
            layoutParams.addRule(Gravity.CENTER_VERTICAL);
            layoutParams.addRule(alignMode);
            return layoutParams;
        } else {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width, height);
            layoutParams.setMargins(marginLeft, marginTop, marginRight, 20);
            return layoutParams;
        }
    }

    // Create method to generate OnClickListeners for whitelist apps in a list
    private void createWhitelistAppClickListener(RelativeLayout relativeLayout, TextView textView) {
        new Handler(Looper.getMainLooper()).post(() -> relativeLayout.setOnClickListener(v -> {
            if (!allowedApps.contains(relativeLayout.getTag().toString())) {
                allowedApps.add((String) relativeLayout.getTag());
                relativeLayout.setBackgroundTintList(getResources().getColorStateList(R.color.gray, getTheme()));
                textView.setTextColor(AppCompatResources.getColorStateList(StartMenuActivity.this, R.color.natural_white));
            } else {
                allowedApps.remove(relativeLayout.getTag().toString());
                relativeLayout.setBackgroundTintList(getResources().getColorStateList(R.color.natural_white, getTheme()));
                textView.setTextColor(AppCompatResources.getColorStateList(StartMenuActivity.this, R.color.black));
            }
        }));
    }

    // Create method to show confirmation dialog before the phone block starts
    private void createConfirmationDialog(@NonNull Dialog dialog, long goalTime) {

        // Initialize variables requires for countdown
        TextView timeLeftTextView = dialog.findViewById(R.id.timeTextView);
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());
        Handler workerThreadHandler = new Handler();

        // Show decrease in blocking time every second and close dialog if time runs out
        workerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                long timeLeft = goalTime - System.currentTimeMillis();
                int hours = (int) timeLeft / 3600000;
                int minutes = (int) (timeLeft - hours * 3600000) / 60000;
                int seconds = (int) (timeLeft - hours * 3600000 - minutes * 60000) / 1000;

                String timeText = hours + "h " + minutes + "m " + seconds + "s";
                mainThreadHandler.post(() -> timeLeftTextView.setText(timeText));

                if (timeLeft > 0) {
                    workerThreadHandler.postDelayed(this, 1000 - timeLeft + goalTime - System.currentTimeMillis());
                } else {
                    dialog.dismiss();
                    workerThreadHandler.removeCallbacksAndMessages(null);
                }
            }
        });

        // Listen to user confirm or cancel
        createBlockConfirmationListener(dialog, goalTime);
        createBlockCancelListener(dialog);
    }

    // Create method for listening to block confirmation
    private void createBlockConfirmationListener (@NonNull Dialog dialog, long goalTime) {
        dialog.findViewById(R.id.confirmImageButton).setOnClickListener(v -> {
            dialog.dismiss();
            sharedPreferencesEditor.putBoolean("usingWhitelistApplication", false).apply();
            sharedPreferencesEditor.putLong("blockedUntil", goalTime).apply();
            runPreliminaryChecks();
        });
    }

    // Create method for listening to block cancellation
    private void createBlockCancelListener (@NonNull Dialog dialog) {
        dialog.findViewById(R.id.cancelImageButton).setOnClickListener(v -> dialog.dismiss());
    }

    // Create method to return the time requested in milliseconds with the Calendar utility
    private long modifyCalendarInstance(@NonNull Calendar calendar, int daysToAdd, int hourOfDay, int minuteOfHour) {
        calendar.add(Calendar.DATE, daysToAdd);
        calendar.add(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.add(Calendar.MINUTE, minuteOfHour);
        return calendar.getTimeInMillis();
    }

    // Create method to check if recorded time exists and is not after tomorrow
    private void runPreliminaryChecks() {
        if (sharedPreferences.getLong("blockedUntil", 0) > System.currentTimeMillis()) {
            startService();
            if (mainContainer != null) {
                mainContainer.setVisibility(View.GONE);
            }
        }
    }

    // Create method for launching the screen overlay service
    private void startService() {

        // Check for overlay permission and restart overlay screen service
        if (Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(this, OverlayService.class);
            intent.setAction(OverlayService.ACTION_START_FOREGROUND_SERVICE);
            startForegroundService(intent);
            sharedPreferencesEditor.putLong("timeRestarted", System.currentTimeMillis()).apply();
        }

        // Create a fullscreen interface
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    // Create method for creating a dialog to enable "Display over other apps" permission
    private void checkOverlayPermission() {

        // Check if app can draw overlays and create OnClickListener for "Open Settings" link
        if (!Settings.canDrawOverlays(this)) {
            Dialog dialog = createDialog(R.layout.screen_overlay_permission_dialog);

            dialog.findViewById(R.id.linkTextView).setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
                dialog.dismiss();
            });
        }
    }

    // Create method for creating a dialog to enable "Permit usage access" permission
    private void checkUsagePermission() {

        // Check if app permits usage access and create OnClickListener for "Open Settings" link
        if (!isAccessGranted()) {
            Dialog dialog = createDialog(R.layout.usage_access_permission_dialog);

            dialog.findViewById(R.id.linkTextView).setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
                dialog.dismiss();
            });
        }
    }

    // Create boolean method for checking if the "Permit usage access" permission is granted
    private boolean isAccessGranted() {
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
            return (appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName) == AppOpsManager.MODE_ALLOWED);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // Check for user exit signals
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        sharedPreferencesEditor.putLong("timeUserLeft", System.currentTimeMillis()).apply();
    }

    // Check for user exit signals
    @Override
    protected void onStop() {
        super.onStop();
        sharedPreferencesEditor.putLong("timeUserLeft", System.currentTimeMillis()).apply();
    }

    // Check for user exit signals
    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferencesEditor.putLong("timeUserLeft", System.currentTimeMillis()).apply();
    }
}