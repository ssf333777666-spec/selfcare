package com.example.thirtyminuteringtone;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

public final class Scheduler {
    public static final long REMINDER_INTERVAL_MS = 30L * 60L * 1000L;
    public static final long COOLDOWN_MS = 5L * 60L * 1000L;
    private static final int REMINDER_REQUEST_CODE = 3001;
    private static final int SECOND_RING_REQUEST_CODE = 3003;

    private Scheduler() {}

    public static boolean canUseExactAlarms(Context c) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        return am != null && am.canScheduleExactAlarms();
    }

    private static PendingIntent reminderPendingIntent(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.putExtra(ReminderReceiver.EXTRA_SECOND_RING, false);
        return PendingIntent.getBroadcast(
                c,
                REMINDER_REQUEST_CODE,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static PendingIntent secondRingPendingIntent(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.putExtra(ReminderReceiver.EXTRA_SECOND_RING, true);
        return PendingIntent.getBroadcast(
                c,
                SECOND_RING_REQUEST_CODE,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /** Starts a fresh 30-minute exact countdown until the first ring. */
    public static boolean scheduleNext(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null || !AppPrefs.isEnabled(c) || !canUseExactAlarms(c)) return false;

        long triggerAt = SystemClock.elapsedRealtime() + REMINDER_INTERVAL_MS;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    reminderPendingIntent(c)
            );
        } else {
            am.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    reminderPendingIntent(c)
            );
        }
        return true;
    }

    /** After the first 3-second ring, schedule the second 3-second ring exactly 5 minutes later. */
    public static boolean scheduleQuietPeriod(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null || !AppPrefs.isEnabled(c) || !canUseExactAlarms(c)) return false;

        long triggerAt = SystemClock.elapsedRealtime() + COOLDOWN_MS;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    secondRingPendingIntent(c)
            );
        } else {
            am.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAt,
                    secondRingPendingIntent(c)
            );
        }
        return true;
    }

    public static void cancel(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(reminderPendingIntent(c));
            am.cancel(secondRingPendingIntent(c));
        }
    }
}
