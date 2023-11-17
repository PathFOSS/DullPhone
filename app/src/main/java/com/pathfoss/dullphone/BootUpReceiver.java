package com.pathfoss.dullphone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

public class BootUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, @NonNull Intent intent) {

        // Start overlay as a new intent if phone is booted and the block is unfinished
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (context.getSharedPreferences("DullPhone", Context.MODE_PRIVATE).getLong("UnlockTime",0) > System.currentTimeMillis()) {
                Intent serviceIntent = new Intent(context, OverlayService.class);
                serviceIntent.setAction(OverlayService.ACTION_START_FOREGROUND_SERVICE);
                context.startForegroundService(serviceIntent);
            }
        }
    }
}