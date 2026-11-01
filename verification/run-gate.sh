#!/usr/bin/env bash
# Build, install, capture and score the 390x844 glass gate on an attached API36 emulator.
set -euo pipefail

cd "$(dirname "$0")/.."
ADB="${ADB:-C:/Users/xuyb/AppData/Local/Android/Sdk/platform-tools/adb.exe}"
CHROME="${CHROME:-C:/Program Files/Google/Chrome/Application/chrome.exe}"
PKG=com.mainecare.effects.demo.debug

"$ADB" get-state >/dev/null
./gradlew :demo:assembleDebug --console=plain --no-daemon
"$ADB" install -r demo/build/outputs/apk/debug/demo-debug.apk

for d in 0 1; do
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars \
    --force-device-scale-factor=2.625 --window-size=390,844 \
    --user-data-dir="C:/Users/xuyb/AppData/Local/Temp/chrome-gate-$d" \
    --screenshot="D:/maine/mainecare-effects/verification/web-reference/gate-web-d$d.png" \
    "file:///D:/maine/mainecare-effects/verification/web-reference/gate.html?displacement=$d" \
    >/dev/null 2>&1 || true

  "$ADB" shell am force-stop $PKG
  "$ADB" shell am start -n $PKG/com.mainecare.effects.demo.MainActivity \
    --es screen gate --es displacement $d --es text 0 >/dev/null
  sleep 8
  "$ADB" exec-out screencap -p > "verification/results/android-gate-d$d.png"
  node verification/crop-gate.mjs \
    "verification/results/android-gate-d$d.png" \
    "verification/results/android-gate-d$d-crop.png"
  node verification/compare-png.mjs \
    "verification/results/android-gate-d$d-crop.png" \
    "verification/web-reference/gate-web-d$d.png" \
    verification/results
done
