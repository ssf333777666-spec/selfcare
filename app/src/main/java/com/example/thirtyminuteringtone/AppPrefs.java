package com.example.thirtyminuteringtone;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppPrefs {
    private static final String FILE = "settings";
    private static final String KEY_URI = "sound_uri";
    private static final String KEY_NAME = "sound_name";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_STARTED = "started";

    private AppPrefs() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String getSoundUri(Context c) {
        return p(c).getString(KEY_URI, null);
    }

    public static String getSoundName(Context c) {
        return p(c).getString(KEY_NAME, "System default");
    }

    public static boolean isEnabled(Context c) {
        return p(c).getBoolean(KEY_ENABLED, true);
    }

    public static void saveSound(Context c, String uri, String name) {
        p(c).edit()
                .putString(KEY_URI, uri)
                .putString(KEY_NAME, name)
                .apply();
    }

    public static boolean hasStarted(Context c) {
        return p(c).getBoolean(KEY_STARTED, false);
    }

    public static void setStarted(Context c, boolean started) {
        p(c).edit().putBoolean(KEY_STARTED, started).apply();
    }

    public static void setEnabled(Context c, boolean enabled) {
        p(c).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }
}
