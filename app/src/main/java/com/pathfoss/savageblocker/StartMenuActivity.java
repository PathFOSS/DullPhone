package com.pathfoss.savageblocker;

import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.ncorti.slidetoact.SlideToActView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StartMenuActivity extends AppCompatActivity {

    // Create global objects and variables
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;
    private ConstraintLayout mainContainer;
    private SlideToActView confirmBlockSlideToActView;
    private Thread showLayoutThread;
    private SystemNavigationTool systemNavigationTool;
    private long homePressedTime = 0;
    private long tasksPressedTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("SavageBlocker", MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();

        // Check if user opens app first time or returns with an active block
        if(sharedPreferences.getBoolean("isBlocking", false)) {
            runPreliminaryChecks();
        } else if (sharedPreferences.getBoolean("firstLaunch", true)) {
            showDisclaimer();
        } else {
            initializeLayout();
        }
    }

    private void initializeLayout () {
        showLayoutThread = new Thread(() -> runOnUiThread(() -> {
            setContentView(R.layout.start_menu_screen);

            // Initialize entire layout to toggle
            mainContainer = findViewById(R.id.mainContainer);

            // Initialize SlideToActView for blocking the phone
            confirmBlockSlideToActView = findViewById(R.id.confirmBlockSlideToActView);

            // Initialize NumberPickers for time selection
            NumberPicker dayPicker = findViewById(R.id.dayNumberPicker);
            NumberPicker hourPicker = findViewById(R.id.hourNumberPicker);
            NumberPicker minutePicker = findViewById(R.id.minuteNumberPicker);

            // Set NumberPicker values for time selection
            setNumberPickerValues(dayPicker,2, "d");
            setNumberPickerValues(hourPicker,23, "h");
            setNumberPickerValues(minutePicker,59, "m");

            // Create onSlideCompleted listener to see when user wants to start block
            confirmBlockSlideToActView.setOnSlideCompleteListener(slideToActView -> {
                if (Settings.canDrawOverlays(getApplicationContext()) && isAccessGranted()) {
                    sharedPreferencesEditor.putLong("blockedUntil", modifyCalendarInstance(Calendar.getInstance(), dayPicker.getValue(), hourPicker.getValue(), minutePicker.getValue())).apply();
                    runPreliminaryChecks();
                } else {
                    checkUsagePermission();
                    checkOverlayPermission();
                    confirmBlockSlideToActView.setCompleted(false, true);
                }
            });
        }));
        showLayoutThread.start();
    }

    // Create method to set NumberPicker values
    private void setNumberPickerValues(NumberPicker numberPicker,  int maxValue, String timeUnit) {
        String[] timePickerArray = new String[maxValue + 1];
        for (int i=0; i <= maxValue; i++) {
            timePickerArray[i] = i + " " + timeUnit;
        }

        numberPicker.setValue(0);
        numberPicker.setMaxValue(maxValue);
        numberPicker.setDisplayedValues(timePickerArray);
    }

    // Create method to show and return dialogs
    private Dialog createDialog(int layoutFile) {
        Dialog dialog = new Dialog(StartMenuActivity.this);
        dialog.setContentView(layoutFile);
        Objects.requireNonNull(dialog.getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.setCancelable(false);
        dialog.show();
        return dialog;
    }

    // Create method to show disclaimer dialog on first boot
    private void showDisclaimer () {
        Dialog dialog1 = createDialog(R.layout.disclaimer_dialog);

        dialog1.findViewById(R.id.button).setOnClickListener( v -> {
            dialog1.dismiss();
            initializeLayout();
            sharedPreferencesEditor.putBoolean("firstLaunch", false).apply();
        });
    }

    // Create method to show confirmation dialog before the phone block starts
    private void createConfirmationDialog () {
        Dialog dialog = createDialog(R.layout.block_confirmation_dialog);
        TextView tv = dialog.findViewById(R.id.timeTextView);

        // Show decreases in blocking time every second and close dialog if time runs out
        new CountDownTimer(sharedPreferences.getLong("blockedUntil", 0) - System.currentTimeMillis(), 1000) {
            @Override
            public void onTick(long l) {
                int hours = (int) l / 3600000;
                int minutes = (int) (l - hours * 3600000) / 60000;
                int seconds = (int) (l - hours * 3600000 - minutes * 60000) / 1000;

                String timeText = hours + "h " + minutes + "m " + seconds + "s";
                tv.setText(timeText);
            }

            @Override
            public void onFinish () {
                dialog.dismiss();
                sharedPreferencesEditor.putBoolean("isBlocking", false).apply();
                confirmBlockSlideToActView.setCompleted(false, true);
            }
        }.start();

        // Create OnClickListener for confirming the block
        dialog.findViewById(R.id.confirmImageButton).setOnClickListener( v -> {
            dialog.dismiss();
            confirmBlockSlideToActView.setCompleted(false, true);
            sharedPreferencesEditor.putBoolean("isBlocking", true).apply();
            startService();
        });

        // Create OnClickListener for canceling the block
        dialog.findViewById(R.id.cancelImageButton).setOnClickListener( v -> {
            dialog.dismiss();
            confirmBlockSlideToActView.setCompleted(false, true);
            sharedPreferencesEditor.putBoolean("isBlocking", false).apply();
        });
    }

    // Create method to return the time requested in milliseconds with the Calendar utility
    private long modifyCalendarInstance (Calendar calendar, int daysToAdd, int hourOfDay, int minuteOfHour) {
        calendar.add(Calendar.DATE, daysToAdd);
        calendar.add(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.add(Calendar.MINUTE, minuteOfHour);
        return calendar.getTimeInMillis();
    }

    // Create method to check if recorded time exists and is not after tomorrow
    private void runPreliminaryChecks () {
        if (sharedPreferences.getLong("blockedUntil", 0) <= modifyCalendarInstance(Calendar.getInstance(), 1, 23, 59) && sharedPreferences.getLong("blockedUntil", 0) > System.currentTimeMillis()) {
            if (sharedPreferences.getBoolean("isBlocking", false)) {
                if (mainContainer != null) {
                    mainContainer.setVisibility(View.GONE);
                    showLayoutThread.interrupt();
                }
                startService();
            } else {
                createConfirmationDialog();
            }
        } else {
            if (!sharedPreferences.getBoolean("isBlocking", false)) {
                confirmBlockSlideToActView.setCompleted(false, true);
                Toast.makeText(getApplicationContext(), "Unable to start block. Please select a valid time.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Create method for launching the screen overlay service
    private void startService () {

        // Check for overlay permission and restart overlay screen service
        if (Settings.canDrawOverlays(this)) {
            startSystemNavigationListener();
            stopService(new Intent(this, OverlayService.class));
            startForegroundService(new Intent(this, OverlayService.class));
        }

        // Create a fullscreen interface
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    // Create method to identify system navigation clicks and set default launcher apps
    private void startSystemNavigationListener() {
        systemNavigationTool = new SystemNavigationTool(this);
        systemNavigationTool.setNavigationListener(new SystemNavigationListener() {

            @Override
            public void onHomePressed() {
                homePressedTime = System.currentTimeMillis();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (tasksPressedTime + 500 <= homePressedTime) {

                        // Initialize objects for identifying most recent running app
                        long currentTime = System.currentTimeMillis();
                        Map<String, Long> appNameTimeMap = new HashMap<>();

                        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
                        List<UsageStats> appList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, homePressedTime, currentTime);

                        // Loop through list to identify apps visible since button click
                        for (UsageStats app : appList) {
                            if (!app.getPackageName().equals("com.pathfoss.savageblocker") && app.getLastTimeUsed() >= homePressedTime) {
                                appNameTimeMap.put(app.getPackageName(), app.getLastTimeUsed());
                            }
                        }

                        // Sort app list by latest time visible
                        List<Map.Entry<String, Long>> list = new ArrayList<>(appNameTimeMap.entrySet());
                        list.sort(Map.Entry.comparingByValue());

                        // Set first visible app as default since button click
                        if (list.size() > 0) {
                            sharedPreferencesEditor.putString("defaultHome", list.get(list.size() - 1).getKey()).apply();
                        }
                    }
                }, 1000);
            }

            @Override
            public void onRecentAppsPressed() {
                tasksPressedTime = System.currentTimeMillis();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (homePressedTime + 500 <= tasksPressedTime) {

                        // Initialize objects for identifying most recent running app
                        long currentTime = System.currentTimeMillis();
                        Map<String, Long> appNameTimeMap = new HashMap<>();

                        UsageStatsManager usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
                        List<UsageStats> appList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, tasksPressedTime, currentTime);

                        // Loop through list to identify apps visible since button click
                        for (UsageStats app : appList) {
                            if (!app.getPackageName().equals("com.pathfoss.savageblocker") && app.getLastTimeUsed() >= tasksPressedTime) {
                                appNameTimeMap.put(app.getPackageName(), app.getLastTimeUsed());
                            }
                        }

                        // Sort app list by latest time visible
                        List<Map.Entry<String, Long>> list = new ArrayList<>(appNameTimeMap.entrySet());
                        list.sort(Map.Entry.comparingByValue());

                        // Set first visible app as default since button click
                        if (list.size() > 0) {
                            sharedPreferencesEditor.putString("defaultTaskManager", list.get(list.size() - 1).getKey()).apply();
                        }
                    }
                }, 1000);
            }
        });
        systemNavigationTool.startNavigationListener();
    }

    // Create method for creating a dialog to enable "Display over other apps" permission
    private void checkOverlayPermission (){

        // Check if app can draw overlays and create OnClickListener for "Open Settings" link
        if (!Settings.canDrawOverlays(this)) {
            Dialog dialog = createDialog(R.layout.screen_overlay_permission_dialog);

            dialog.findViewById(R.id.linkTextView).setOnClickListener( v -> {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
                dialog.dismiss();
            });
        }
    }

    // Create method for creating a dialog to enable "Permit usage access" permission
    private void checkUsagePermission (){

        // Check if app permits usage access and create OnClickListener for "Open Settings" link
        if (!isAccessGranted()) {
            Dialog dialog = createDialog(R.layout.usage_access_permission_dialog);

            dialog.findViewById(R.id.linkTextView).setOnClickListener( v -> {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
                dialog.dismiss();
            });
        }
    }

    // Create boolean method for checking if the "Permit usage access" permission is granted
    private boolean isAccessGranted () {
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
            return (appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName) == AppOpsManager.MODE_ALLOWED);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // Create onDestroy method to stop any vital tasks when app is closed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (systemNavigationTool != null) {
            systemNavigationTool.stopNavigationListener();
        }
    }
}