package com.pathfoss.savageblocker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // Start overlay as a new intent if phone is booted and the block is unfinished
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            // Initialize SharedPreferences
            SharedPreferences sharedPreferences = context.getSharedPreferences("SavageBlocker", Context.MODE_PRIVATE);

            if (sharedPreferences.getBoolean("isBlocking", false) && sharedPreferences.getLong("blockedUntil",0) > System.currentTimeMillis()) {
                Intent activityIntent = new Intent(context, StartMenuActivity.class);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(activityIntent);
            }
        }
    }
}