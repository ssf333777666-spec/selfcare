package com.example.thirtyminuteringtone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (AppPrefs.isEnabled(context) && Scheduler.canUseExactAlarms(context)) {
            Scheduler.cancel(context);
            if (Scheduler.scheduleNext(context)) {
                AppPrefs.setStarted(context, true);
            }
        }
    }
}
