package com.example.thirtyminuteringtone;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.provider.Settings;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int PICK_SOUND = 501;
    private TextView status;
    private TextView soundText;
    private Button toggleButton;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        ensureDefaults();
        updateStatus();
        requestExactAlarmAccessIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AppPrefs.isEnabled(this) && Scheduler.canUseExactAlarms(this) && !AppPrefs.hasStarted(this)) {
            if (Scheduler.scheduleNext(this)) {
                AppPrefs.setStarted(this, true);
            }
        }
        updateStatus();
    }

    private void requestExactAlarmAccessIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || Scheduler.canUseExactAlarms(this)) {
            if (AppPrefs.isEnabled(this) && !AppPrefs.hasStarted(this)) {
                if (Scheduler.scheduleNext(this)) {
                    AppPrefs.setStarted(this, true);
                }
            }
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Allow exact alarms")
                .setMessage("This app needs the Alarms & reminders access so the 30-minute and 5-minute timers can ring on time even when the phone is idle.\n\nOn the next screen, allow this app to set alarms and reminders.")
                .setPositiveButton("Open settings", (d, w) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } catch (Exception ignored) {
                        startActivity(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
                    }
                })
                .setNegativeButton("Not now", null)
                .show();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(48, 70, 48, 48);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("Every 30 minutes, a 3-second sound plays. Then the app stays completely quiet for 5 minutes, plays the same sound for 3 seconds again, and starts a fresh 30-minute countdown.");
        info.setTextSize(16);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 24, 0, 24);

        soundText = new TextView(this);
        soundText.setTextSize(16);
        soundText.setGravity(Gravity.CENTER);
        soundText.setPadding(0, 16, 0, 16);

        Button choose = new Button(this);
        choose.setText("Choose ringtone");
        choose.setOnClickListener(v -> chooseSound());

        toggleButton = new Button(this);
        toggleButton.setOnClickListener(v -> toggleReminder());

        status = new TextView(this);
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 30, 0, 0);

        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(info, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(soundText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(choose, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(toggleButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }

    private void ensureDefaults() {
        if (AppPrefs.getSoundUri(this) == null) {
            Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            if (defaultUri != null) {
                AppPrefs.saveSound(this, defaultUri.toString(), "System default");
            }
        }
        // Start the first 30-minute countdown only once. Reopening the app should not reset the timer.
        if (AppPrefs.isEnabled(this) && !AppPrefs.hasStarted(this) && Scheduler.canUseExactAlarms(this)) {
            if (Scheduler.scheduleNext(this)) {
                AppPrefs.setStarted(this, true);
            }
        }
    }

    private void updateStatus() {
        boolean enabled = AppPrefs.isEnabled(this);
        soundText.setText("Sound: " + AppPrefs.getSoundName(this));
        toggleButton.setText(enabled ? "Turn OFF" : "Turn ON");
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !Scheduler.canUseExactAlarms(this)) {
            status.setText("Reminder is ON, but Alarms & reminders access is needed.");
        } else {
            status.setText(enabled
                    ? "Reminder is ON\n30 min → 3 sec ring → 5 min cooldown → 3 sec ring → 30 min."
                    : "Reminder is OFF");
        }
    }

    private void toggleReminder() {
        boolean next = !AppPrefs.isEnabled(this);
        if (next && !Scheduler.canUseExactAlarms(this)) {
            AppPrefs.setEnabled(this, true);
            requestExactAlarmAccessIfNeeded();
            updateStatus();
            return;
        }

        AppPrefs.setEnabled(this, next);
        if (next) {
            Scheduler.cancel(this);
            if (Scheduler.scheduleNext(this)) {
                AppPrefs.setStarted(this, true);
            }
        } else {
            Scheduler.cancel(this);
            AppPrefs.setStarted(this, false);
            ReminderReceiver.stopPlayback();
        }
        updateStatus();
    }

    private void chooseSound() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("audio/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_SOUND);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_SOUND || resultCode != RESULT_OK || data == null || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
        }

        AppPrefs.saveSound(this, uri.toString(), getDisplayName(uri));
        // Do not reset an already-running 30-minute countdown or 5-minute cooldown.
        // The newly selected sound will be used on the next ring.
        updateStatus();
    }

    private String getDisplayName(Uri uri) {
        Cursor c = null;
        try {
            c = getContentResolver().query(
                    uri,
                    new String[]{OpenableColumns.DISPLAY_NAME},
                    null,
                    null,
                    null
            );
            if (c != null && c.moveToFirst()) {
                return c.getString(0);
            }
        } finally {
            if (c != null) c.close();
        }
        return "Selected sound";
    }
}
