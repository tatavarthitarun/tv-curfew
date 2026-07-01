# TV Curfew — Project Context

**One-liner:** Personal Android TV app that forces the TV into standby whenever anyone turns it on
during the nightly curfew window **9:00 PM → 10:00 AM** — no warning, no prompt. Not for Play Store.

- **Package / applicationId:** `com.tatav.tvcurfew`
- **Code location:** `~/Projects/tv-curfew`
- **LifeOS notes:** `~/LifeOS/04_Projects/tv-curfew` (note + symlinks to code / explainer / this file)
- **Status:** Built, unit-tested (9/9), and verified live on an Android TV emulator (API 34). Not yet
  installed on the real TV.

---

## The key constraint (read first)
Android exposes **no public API to fully power a device off**. Real `SHUTDOWN`/`REBOOT` are
`signature|privileged` perms (platform-signed system apps or root only). So "shut down" is implemented as
**DeviceAdmin → `DevicePolicyManager.lockNow()` → standby (screen off)** — identical to the remote's power
button — paired with an instant re-lock on every wake. A genuine power-off would need root
(`su -c reboot -p`); this build is the no-root path chosen deliberately.

## How enforcement works (4 layers)
1. **Instant re-lock** — a foreground service (`CurfewService`) holds a runtime `ACTION_SCREEN_ON` receiver;
   any wake during curfew → `lockNow()`.
2. **60s heartbeat** — `CurfewAlarmReceiver` re-arms an exact alarm every 60s while in curfew; sleeps to the
   next 9 PM otherwise.
3. **Survives reboot** — `BootReceiver` restarts everything on `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`.
4. **Kill-resistant watchdog** — `CurfewWorker` (WorkManager, 15-min floor) persists across app-death and
   reboot; locks directly + re-arms the service and alarms if an OEM power manager kills the app.

The single time-gate is `CurfewLogic.isCurfew()`: `hour >= 21 || hour < 10`, window `[21:00, 10:00)`.
**To change the hours,** edit `START_HOUR` / `END_HOUR` in `CurfewLogic.kt` and rebuild.

## Toolchain / build
- Kotlin, no runtime deps except AndroidX WorkManager. minSdk 21, compile/target 34.
- Gradle 8.13 + AGP 8.9 + Kotlin 2.1 (chosen to run on the only local JDK, 23).
- SDK at `~/Library/Android/sdk`; TV image `system-images;android-34;android-tv;arm64-v8a`.

```bash
# build
./gradlew :app:assembleDebug          # -> app/build/outputs/apk/debug/app-debug.apk
# unit tests (the 9 PM–10 AM boundary, both branches)
./gradlew :app:testDebugUnitTest
# emulator
emulator -avd tv_curfew -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect
```

## Install on the real TV
Enable Developer options + network debugging on the TV, then from the Mac (same network):
```bash
adb connect <TV-IP>:5555
adb install -r ~/Projects/tv-curfew/app/build/outputs/apk/debug/app-debug.apk
adb shell dpm set-active-admin com.tatav.tvcurfew/.CurfewAdminReceiver   # works from adb shell, no root
adb shell am start-foreground-service com.tatav.tvcurfew/.CurfewService
```
Or open the **TV Curfew** app and use its two setup buttons. If the TV brand aggressively kills background
apps, also whitelist it from battery optimization.

**Remove later:**
```bash
adb shell dpm remove-active-admin com.tatav.tvcurfew/.CurfewAdminReceiver
adb uninstall com.tatav.tvcurfew
```

## Verification done (on emulator, clock was 23:03 = in curfew)
- Service start → `lockNow()` → `mWakefulness=Asleep`.
- Woke the TV → `SCREEN_ON` receiver → `lockNow()` → back to `Asleep` in ~4s.
- Watchdog registered in JobScheduler: `JOB … com.tatav.tvcurfew/…SystemJobService … RUNNABLE`.
- 9 JUnit tests over the boundary, 0 failures.
- Out-of-curfew branch proven by unit test (emulator force-syncs its clock to host, blocking on-device
  time changes without root).

## See also
- `EXPLAINER.html` — full illustrated walkthrough (constraint, architecture, every command + output).
