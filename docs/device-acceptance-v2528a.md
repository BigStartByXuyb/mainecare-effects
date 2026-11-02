# V2528A acceptance — install, measure, return

This is the real-device acceptance step. Emulator numbers are diagnostic only and are not a
pass. The MaineCare App is not modified; this uses the standalone demo APK.

## Artifact

```
output/mainecare-effects-demo-0.1.0-debug.apk
sha256 906b59bed31bcdcce3b23bd868ffed9035de0737f40f52fa949660b1f4f5b3d4
```

The APK is generated from `:demo:assembleDebug` and is not committed (`output/` is ignored).
The device must run Android 13+ (API 33) for AGSL.

## Install

```bash
adb devices -l
adb -s <V2528A-serial> install -r output/mainecare-effects-demo-0.1.0-debug.apk
```

If the device is not listed: enable Developer options → USB debugging, authorize the RSA
fingerprint on the phone, use a data-capable USB cable, and run `adb kill-server` then
`adb devices -l` again. The current workspace has only emulators (`emulator-5556`,
`emulator-5564`); V2528A is **not connected**, so the run is blocked on device access.

## Measure

Run the helper (it writes cold-start, gfxinfo, meminfo and motion logs):

```bash
./verification/device-perf.sh <V2528A-serial> output/device
```

Manual equivalent:

```bash
PKG=com.mainecare.effects.demo.debug
adb -s <serial> shell am force-stop $PKG
adb -s <serial> shell am start -W -n $PKG/com.mainecare.effects.demo.MainActivity \
  --es screen glass
# repeat with --es screen deck
adb -s <serial> shell dumpsys gfxinfo $PKG reset
# perform card switches / swipes for 10 seconds
adb -s <serial> shell dumpsys gfxinfo $PKG > output/device/gfxinfo.txt
adb -s <serial> shell dumpsys meminfo $PKG > output/device/meminfo.txt
adb -s <serial> logcat -d -s MCEffectsMotion > output/device/motion.log
```

Report from `gfxinfo`: `Janky frames`, `50th/90th/95th/99th percentile`, `Number Missed Vsync`.
Report from `meminfo`: `TOTAL PSS` / `TOTAL RSS` after each screen. Cold start: the `TotalTime`
from `am start -W` after `force-stop` (repeat three times and report all three).

Acceptance thresholds remain the project gate: 0 janky frames, P95 ≤17 ms, no missed vsync,
PSS ≤160 MB. Do not relax them for the device run.

## Return

Place the files under `output/device/` and report:

- device model/build, display size/density;
- three cold-start totals per screen;
- gfxinfo janky count and P50/P90/P95/P99, missed vsync;
- PSS after each screen;
- `motion-*.log` if a deck/coverflow spring run is included;
- any visual issue on the actual display (pill edge halo, border, glass banding).

The APK is debug-signed; this is a test package, not a production release.
