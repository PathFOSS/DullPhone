package com.pathfoss.dullphone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        // Start overlay as a new intent if phone is booted and the block is unfinished
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            if (context.getSharedPreferences("DullPhone", Context.MODE_PRIVATE).getLong("blockedUntil",0) > System.currentTimeMillis()) {
                Intent activityIntent = new Intent(context, StartMenuActivity.class);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(activityIntent);
                context.startForegroundService(new Intent(context, OverlayService.class));
            }
        }
    }
}