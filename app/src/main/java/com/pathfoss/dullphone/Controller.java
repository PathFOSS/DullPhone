package com.pathfoss.dullphone;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import java.util.Calendar;
import java.util.Objects;

public class Controller extends AppCompatActivity implements StartServiceListener {

    public static Vibrator vibrator;
    private static int phoneDPI;

    private PackageManager packageManager;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;
    private static UsageStatsManager usageStatsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize global items
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        sharedPreferences = getSharedPreferences("DullPhone", MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();
        packageManager = getPackageManager();

        // Check if user opens app first time or returns with an active block
        if (sharedPreferences.getLong("UnlockTime", 0) <= System.currentTimeMillis()) {
            if (!sharedPreferences.getBoolean("TermsAccepted", false)) {
                initializeLayout(new Disclaimer(this), "Disclaimer");
            } else {
                initializeLayout(new MainMenu(this), "MainMenu");
            }
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        phoneDPI = metrics.densityDpi;
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);

        // Create screen time notification if possible
        if (sharedPreferences.getBoolean("ScreenTimeEnabled", true)) {
            Intent intent = new Intent(this, ScreenTimeService.class);
            intent.setAction(ScreenTimeService.ACTION_STOP_FOREGROUND_SERVICE);
            startService(intent);
            intent.setAction(ScreenTimeService.ACTION_START_FOREGROUND_SERVICE);
            startService(intent);
        }
    }

    // Create method to display the main menu
    private void initializeLayout(Fragment fragment, String fragmentName) {

        // Determine default dialer
        String DefaultDialer = Objects.requireNonNull(packageManager.resolveActivity(new Intent(Intent.ACTION_DIAL), PackageManager.MATCH_DEFAULT_ONLY)).activityInfo.packageName;
        sharedPreferencesEditor.putString("DefaultDialer", DefaultDialer).apply();

        // Initialize entire layout to toggle
        setContentView(R.layout.activity_controller);

        getSupportFragmentManager().beginTransaction().replace(R.id.fcv, fragment, fragmentName).commit();
    }

    // Check for user exit signals
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        sharedPreferencesEditor.putLong("UserLeaveTime", System.currentTimeMillis()).apply();
    }

    // Check for user exit signals
    @Override
    protected void onStop() {
        super.onStop();
        sharedPreferencesEditor.putLong("UserLeaveTime", System.currentTimeMillis()).apply();
    }

    // Check for user exit signals
    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferencesEditor.putLong("UserLeaveTime", System.currentTimeMillis()).apply();
    }

    // Create method for launching the screen overlay service
    private void startService() {

        // Check for overlay permission and restart overlay screen service
        if (Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(this, OverlayService.class);
            intent.setAction(OverlayService.ACTION_START_FOREGROUND_SERVICE);
            startForegroundService(intent);
            sharedPreferencesEditor.putLong("TimeRestarted", System.currentTimeMillis()).apply();
        }

        // Create a fullscreen interface
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
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
        if (sharedPreferences.getLong("UnlockTime", 0) > System.currentTimeMillis()) {
            startService();
            findViewById(R.id.fcv).setVisibility(View.GONE);
        }
    }

    // Create method for creating a dialog to enable "Display over other apps" permission
    private void checkOverlayPermission() {

        // Check if app can draw overlays and create OnClickListener for "Open Settings" link
        if (!Settings.canDrawOverlays(this)) {
            new OverlayPermissionConfirm(this).show(getSupportFragmentManager(), "Overlay Permission Dialog");
        }
    }

    // Create method for creating a dialog to enable "Permit usage access" permission
    private void checkUsagePermission() {

        // Check if app permits usage access and create OnClickListener for "Open Settings" link
        if (!isAccessGranted()) {
            new UsagePermissionConfirm(this).show(getSupportFragmentManager(), "Usage Permission Dialog");
        }
    }

    // Create boolean method for checking if the "Permit usage access" permission is granted
    private boolean isAccessGranted() {
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
            return (appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName) == AppOpsManager.MODE_ALLOWED);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // Create method for checking if user requests a block start from MainMenu
    @Override
    public void serviceStartRequested(int days, int hours, int minutes) {
        if (Settings.canDrawOverlays(this) && isAccessGranted()) {
            sharedPreferencesEditor.putInt("TapsToUnlock", sharedPreferences.getInt("TapsToUnlockPreference", 5000));
            new BlockConfirm(this, modifyCalendarInstance(Calendar.getInstance(), days, hours, minutes)).show(getSupportFragmentManager(), "Confirm Block");
        } else {
            checkUsagePermission();
            checkOverlayPermission();
        }
    }

    // Create method for checking if user confirms a block start from BlockConfirm
    @Override
    public void serviceStartConfirmed() {
        runPreliminaryChecks();
    }

    // Create method for checking if user requests transfer to settings from OverlayPermissionConfirm
    @Override
    public void overlayPermissionRequested() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(intent);
    }

    // Create method for checking if user requests transfer to settings from UsagePermissionConfirm
    @Override
    public void usagePermissionRequested() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(intent);
    }

    // Create method for redirecting to MainMenu from lateral fragments

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        String tag = Objects.requireNonNull(getSupportFragmentManager().findFragmentById(R.id.fcv)).getTag();
        if (Objects.equals(tag, "Settings") || Objects.equals(tag, "WhiteList")) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fcv, new MainMenu(this), "MainMenu").commit();
        }
    }

    // Create method to convert pixels to DP for defining UI elements
    public static int convertDPtoPX (int dp) {
        return dp * phoneDPI / 160;
    }

    public static UsageStatsManager getUsageStatsManager () {
        return usageStatsManager;
    }
}