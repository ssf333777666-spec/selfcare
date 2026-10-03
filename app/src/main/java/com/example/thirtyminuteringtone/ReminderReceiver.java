package com.example.thirtyminuteringtone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

/**
 * Plays the short sound directly from the alarm broadcast.
 * goAsync() keeps the receiver alive for the 3-second playback window.
 */
public class ReminderReceiver extends BroadcastReceiver {
    public static final String EXTRA_SECOND_RING = "second_ring";
    private static final long PLAYBACK_MS = 3_000L;
    private static final Object LOCK = new Object();

    private static MediaPlayer activePlayer;
    private static Handler activeHandler;
    private static Runnable activeFinish;
    private static PendingResult activePendingResult;
    private static boolean activeSecondRing;
    private static boolean activeFinished;

    @Override
    public void onReceive(Context context, Intent intent) {
        PendingResult pendingResult = goAsync();
        Context app = context.getApplicationContext();
        boolean secondRing = intent != null && intent.getBooleanExtra(EXTRA_SECOND_RING, false);

        if (!AppPrefs.isEnabled(app)) {
            pendingResult.finish();
            return;
        }

        stopPlaybackInternal();

        String raw = AppPrefs.getSoundUri(app);
        if (raw == null) {
            scheduleAfterRing(app, secondRing);
            pendingResult.finish();
            return;
        }

        final Handler handler = new Handler(Looper.getMainLooper());
        final MediaPlayer player;
        try {
            player = MediaPlayer.create(app, Uri.parse(raw));
            if (player == null) {
                scheduleAfterRing(app, secondRing);
                pendingResult.finish();
                return;
            }
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
        } catch (Exception ignored) {
            scheduleAfterRing(app, secondRing);
            pendingResult.finish();
            return;
        }

        synchronized (LOCK) {
            activePlayer = player;
            activeHandler = handler;
            activePendingResult = pendingResult;
            activeSecondRing = secondRing;
            activeFinished = false;
        }

        Runnable finish = () -> finishPlayback(app);
        synchronized (LOCK) {
            activeFinish = finish;
        }

        player.setOnErrorListener((mp, what, extra) -> {
            finishPlayback(app);
            return true;
        });

        try {
            player.start();
            handler.postDelayed(finish, PLAYBACK_MS);
        } catch (Exception ignored) {
            finishPlayback(app);
        }
    }

    /** Stops a currently playing 3-second ring immediately when the user turns the app OFF. */
    public static void stopPlayback() {
        finishPlayback(null);
    }

    private static void stopPlaybackInternal() {
        MediaPlayer player;
        Handler handler;
        Runnable finish;
        PendingResult pending;
        boolean wasFinished;

        synchronized (LOCK) {
            if (activePlayer == null && activePendingResult == null) return;
            wasFinished = activeFinished;
            activeFinished = true;
            player = activePlayer;
            handler = activeHandler;
            finish = activeFinish;
            pending = activePendingResult;
            activePlayer = null;
            activeHandler = null;
            activeFinish = null;
            activePendingResult = null;
        }

        if (handler != null && finish != null) handler.removeCallbacks(finish);
        if (player != null) {
            try { if (player.isPlaying()) player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
        }
        if (pending != null && !wasFinished) pending.finish();
    }

    private static void finishPlayback(Context app) {
        MediaPlayer player;
        Handler handler;
        Runnable finish;
        PendingResult pending;
        boolean secondRing;

        synchronized (LOCK) {
            if (activeFinished) return;
            activeFinished = true;
            player = activePlayer;
            handler = activeHandler;
            finish = activeFinish;
            pending = activePendingResult;
            secondRing = activeSecondRing;
            activePlayer = null;
            activeHandler = null;
            activeFinish = null;
            activePendingResult = null;
        }

        if (handler != null && finish != null) handler.removeCallbacks(finish);
        if (player != null) {
            try { if (player.isPlaying()) player.stop(); } catch (Exception ignored) {}
            try { player.release(); } catch (Exception ignored) {}
        }

        if (app != null && AppPrefs.isEnabled(app)) {
            scheduleAfterRing(app, secondRing);
        }
        if (pending != null) pending.finish();
    }

    private static void scheduleAfterRing(Context app, boolean secondRing) {
        if (!AppPrefs.isEnabled(app)) return;
        if (secondRing) {
            Scheduler.scheduleNext(app);
        } else {
            Scheduler.scheduleQuietPeriod(app);
        }
    }
}
