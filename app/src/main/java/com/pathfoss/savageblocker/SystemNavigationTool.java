package com.pathfoss.savageblocker;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import java.util.Objects;

public class SystemNavigationTool {

    // Create global objects and variables
    private final Context currentContext;
    private final IntentFilter currentFilter;
    private SystemNavigationListener currentListener;
    private InnerReceiver currentReceiver;

    // Set context and filter
    public SystemNavigationTool(Context context) {
        currentContext = context;
        currentFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
    }

    // Create method to set listeners for system navigation buttons
    public void setNavigationListener(SystemNavigationListener listener) {
        currentListener = listener;
        currentReceiver = new InnerReceiver();
    }

    // Create method to register receiver
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void startNavigationListener() {
        if (currentReceiver != null) {
            currentContext.registerReceiver(currentReceiver, currentFilter);
        }
    }

    // Create method to unregister receiver
    public void stopNavigationListener() {
        if (currentReceiver != null) {
            currentContext.unregisterReceiver(currentReceiver);
        }
    }

    // Create class to distinguish between task manager and home button clicks
    class InnerReceiver extends BroadcastReceiver {
        final String SYSTEM_DIALOG_REASON_KEY = "reason";
        final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
        final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reasonKey = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (reasonKey != null && currentListener != null) {
                    if (reasonKey.equals(SYSTEM_DIALOG_REASON_HOME_KEY)) {
                        currentListener.onHomePressed();
                    } else if (reasonKey.equals(SYSTEM_DIALOG_REASON_RECENT_APPS)) {
                        currentListener.onRecentAppsPressed();
                    }
                }
            }
        }
    }
}