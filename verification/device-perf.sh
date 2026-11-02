#!/usr/bin/env bash
# V2528A acceptance helper. Requires an authorized physical device; emulator results are
# diagnostic only and are not a pass.
set -euo pipefail

SERIAL="${1:?usage: device-perf.sh <adb-serial> [out-dir]}"
OUT="${2:-output/device}"
PKG=com.mainecare.effects.demo.debug
ADB="${ADB:-adb}"
mkdir -p "$OUT"

"$ADB" -s "$SERIAL" shell getprop ro.product.model > "$OUT/model.txt"
"$ADB" -s "$SERIAL" shell getprop ro.build.version.release >> "$OUT/model.txt"
"$ADB" -s "$SERIAL" shell wm size >> "$OUT/model.txt"
"$ADB" -s "$SERIAL" shell wm density >> "$OUT/model.txt"

for screen in glass deck; do
  "$ADB" -s "$SERIAL" shell am force-stop "$PKG"
  "$ADB" -s "$SERIAL" logcat -c
  "$ADB" -s "$SERIAL" shell am start -W -n "$PKG/com.mainecare.effects.demo.MainActivity" \
    --es screen "$screen" > "$OUT/cold-start-$screen.txt"
  sleep 4
  "$ADB" -s "$SERIAL" shell dumpsys gfxinfo "$PKG" reset >/dev/null
  # Scripted interaction: three swipes across the deck/glass area.
  for i in 1 2 3; do
    "$ADB" -s "$SERIAL" shell input swipe 850 1500 250 1500 240
    sleep 1
  done
  "$ADB" -s "$SERIAL" shell dumpsys gfxinfo "$PKG" > "$OUT/gfxinfo-$screen.txt"
  "$ADB" -s "$SERIAL" shell dumpsys meminfo "$PKG" > "$OUT/meminfo-$screen.txt"
  "$ADB" -s "$SERIAL" logcat -d -s MCEffectsMotion > "$OUT/motion-$screen.log"
done

echo "Wrote $OUT. Report cold-start total time, gfxinfo janky/P95, and PSS."
