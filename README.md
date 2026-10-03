# 30 Minute Ringtone — v7

Behavior:

30 minutes countdown -> 3-second selected sound -> 5-minute completely silent cooldown -> same sound for 3 seconds again -> new 30-minute countdown.

The app has an ON/OFF button and lets the user select an audio file from the phone. Changing the sound does not reset the current countdown/cooldown. OFF cancels both pending timers. If the device reboots, an enabled reminder is scheduled again.

This version is written in Java.


Exact timing: on Android 12+ this version uses `SCHEDULE_EXACT_ALARM` and `setExactAndAllowWhileIdle()`. On first launch, Android opens the **Alarms & reminders** special-access screen; allow it so the 30-minute and 5-minute timers are scheduled as exact alarms.
