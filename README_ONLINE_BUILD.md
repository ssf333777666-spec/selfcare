# Online APK build

1. Upload this project to GitHub, keeping the `.github/workflows/build-apk.yml` path.
2. Open the Actions tab and run `Build APK` (or push to `main`).
3. Wait for the green check.
4. Open the successful run and download the `ThirtyMinuteRingtone-v7-APK` artifact.
5. Unzip it to get `app-debug.apk`.


Exact timing: on Android 12+ this version uses `SCHEDULE_EXACT_ALARM` and `setExactAndAllowWhileIdle()`. On first launch, Android opens the **Alarms & reminders** special-access screen; allow it so the 30-minute and 5-minute timers are scheduled as exact alarms.
