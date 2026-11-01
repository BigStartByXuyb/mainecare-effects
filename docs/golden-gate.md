# 100% restoration gate — definition and status

This file defines what “100%” means for each ported effect. A gate is **PASS** only when every
listed criterion is met. Results are reported per scene; a pass on one scene never hides a
failure on another.

Reference device for this round: `Medium_Phone_API_36.0` emulator, 1080×2400 @420dpi
(2.625×), 60 Hz, headless SwiftShader GPU. The V2528A target device (Android 16 / API 36) is
not connected; emulator numbers are a pre-acceptance gate, not the final device sign-off.

## Scenes

| Scene | Feeding surface | Role |
| --- | --- | --- |
| **Canonical** | `backdrop-gate.png`, the smooth warm/cool Obsidian-style gradient that v21 actually feeds the glass | acceptance gate |
| Photo | `backdrop.png`, deterministic mountains/moon/water | stress gate for high-frequency detail and blur |
| Obsidian veins | `backdrop-obsidian.png`, deterministic veined texture | stress gate for fine structure |

The canonical scene is 390×844dp: card at (24,120) 342×200dp, pill at (24,350) 132×36dp.
Chromium renders `verification/web-reference/gate.html` at 390×844 CSS px with
`deviceScaleFactor=2.625`; Android captures the same 1024×2216 frame at (28,92).

## 1. Static material gate — canonical scene

Android shader runs with `displacementScale=0` and `aberrationIntensity=0`; the Chromium
reference runs `gate.html?displacement=0` with the same v21 tint, gradients, border and
inner shadows.

| Metric | Threshold | Final |
| --- | --- | --- |
| SSIM (windowed 8×8, luminance) | ≥ 0.995 | **0.99559** |
| Mean absolute RGB error | ≤ 1.0 / 255 | **0.440** |
| Pixels with max channel Δ ≤ 5 | ≥ 99% | **99.452%** (0.548% above Δ5) |
| Frame bounds | exactly 1024×2216 at (28,92) | exact |

Status: **PASS**.

- Whole frame: MAE 0.440, RMSE 1.48, PSNR 42.5 dB, max Δ43.
- Card (898×525): MAE 1.300, SSIM 0.99224, 2.16% above Δ5.
- Pill (347×95): MAE 2.337, SSIM 0.93730, 6.84% above Δ5.
- A second capture produced byte-identical metrics, so the result is deterministic.

The card-relative max Δ is 24, at the upper edge of the allowed “≤24 in card/pill” check.
The pill bottom border still reaches Δ43 on one 1px row because Compose and Chromium round the
36dp bottom edge to different device rows; that row is reported below, not hidden.

Evidence: `docs/evidence/gate-smooth-side-d0.png`, `gate-smooth-diff-d0.png`.

## 2. Displacement gate — canonical scene

Same scene with `displacementScale=22` (standard map) and `aberrationIntensity=2.2`; the
Chromium reference runs the full liquid-glass-react 1.1.1 SVG filter chain.

| Metric | Threshold | Final |
| --- | --- | --- |
| SSIM | ≥ 0.99 | **0.99556** |
| MAE | ≤ 1.5 / 255 | **0.428** |
| Pixels within Δ5 | ≥ 98% | **99.576%** (0.423% above Δ5) |

Status: **PASS**.

- Whole frame: RMSE 1.46, PSNR 42.7 dB, max Δ47.
- Card: MAE 1.238, SSIM 0.99272, 1.68% above Δ5.
- Pill: MAE 2.449, SSIM 0.93442, 5.18% above Δ5.
- Pill max Δ is 47 on the same 1px bottom-border row; card max Δ is 24.

Evidence: `docs/evidence/gate-smooth-side-d1.png`, `gate-smooth-diff-d1.png`.

## 3. Stress scenes — reported, not accepted as passes

| Scene | Displacement | SSIM | MAE | > Δ5 | Status |
| --- | --- | --- | --- | --- | --- |
| Photo | off | 0.98857 | 1.027 | 2.86% | **FAIL** SSIM/MAE/Δ5 |
| Photo | on | 0.98942 | 0.870 | 0.92% | **FAIL** SSIM |
| Obsidian veins | off | 0.99185 | 0.793 | 0.55% | **FAIL** SSIM |
| Obsidian veins | on | 0.99176 | 0.794 | 0.44% | **FAIL** SSIM |

Photo residuals concentrate at the card bottom and pill bottom, where the high-frequency
water line changes fastest. The raw blur comparison (shadow and tint disabled) gives card
interior MAE 4.37, so the remaining gap is the blur/compositing kernel on fine detail, not the
tint values. Obsidian is close but its veined detail produces the same class of residual at a
smaller amplitude.

Evidence: `gate-photo-side-d0.png`, `gate-photo-diff-d0.png`,
`gate-obsidian-side-d0.png`, `gate-obsidian-diff-d0.png`.

These scenes are part of the phase-1 report. They must pass before the gate can be called
“100% on arbitrary backdrops”; the canonical v21 pass is not a substitute.

## 4. Layer-by-layer parameter gate

Values transcribed from `feeding-v21-obsidian.css` / `feeding-v6-shell.css`; compositing
changes below are measured against the Chromium reference.

| Layer | Value | Implementation | Status |
| --- | --- | --- | --- |
| Card tint | `rgba(10,12,15,.56)` | `Card.tint` | exact |
| Card blur | 34px + saturate 132% + brightness .82 | full-resolution Skia `BlurEffect`, 1.4× sigma calibration | canonical raw MAE 1.68 at card center; photo raw MAE 4.37 |
| Card border | `rgba(235,239,244,.16)` over the card background-color | Chrome snaps the 1px border to 2 device px at 2.625×; the border is precomposited to the measured constant `0xFF323436` | matches the web raster; documented deviation from a translucent runtime border |
| Card inner top | `inset 0 1px 0 rgba(255,255,255,.24)` | 1 CSS px highlight at the padding-box top, snapped to device rows | canonical top band SSIM 0.98 |
| Card inner bottom | `inset 0 -22px 34px rgba(0,0,0,.36)` | Gaussian inset profile, sigma = blur/2, offset -22dp, full-height tail | matches the web profile; pill relies on the same model |
| Card outer shadow | `0 24px 52px rgba(0,0,0,.56)` | Decal `BlurEffect`, offset 24dp, sigma 42dp, alpha .56 | still differs in the tails; background-only regions are within Δ5 |
| Card sheen | 280px radial 72%/8%, 92%×74% radial 108%/-8%, 116°, 325°, 180° | shader, CSS paint order (first layer on top), premultiplied 180° gradient | layer isolation: A1 .99, A3 1.00, A4 .92, A5 (after premultiply) matches sign and profile |
| Card `::before` | 111° + 68° + 116° repeating texture, vertical mask | inset 2px, all gradients masked, 0.75 strength | canonical card MAE 1.30; residual is browser mask/phase |
| Advice tint/blur | `.66` / 32px / 134% / .80 | `Advice` | values exact; outside this gate |
| Meal / Insight / Dock | v21 values | `MealFront`, `Insight`, `Dock` | values exact; Dock shares the pill gate |
| Displacement map | standard map, 256×256, R=x/B=y, scale -22, aberration 2.2 | shader + byte-identical asset | PASS after filter-region window fix |
| SVG filter region | 170% of element, map `slice` | centered window `(W,H)/max(1.7W,1.7H)` per surface | implemented |
| Aberration channels | `S`, `S−a*.05`, `S−a*.1` | per-channel samples, graded before recombination | implemented |
| Screen blend alpha | three channel layers with the blurred element alpha | modeled; short elements darken, tall cards do not | canonical d1 pill/card match |
| 0.28px Gaussian on aberrated channels | `stdDeviation=.28` | not applied | residual, below the measured Δ5 threshold on this gate |

## 5. Motion gate — PASS for the measured run

- `MaineMotionTest.springMatchesWebReferenceSequence` matches the JS reference for 20
  consecutive 60 Hz frames (position/velocity within 1e-4). **PASS**.
- Geometry/constant tests (`cardDeck*`, `coverflow*`, `stack*`, `wrapDelta`) pass. **PASS**.
- Recorded coverflow spring: release target 4.0, release velocity 3.248 index/s; 25 spring
  frames compared against `springStep`; maximum deviation 0.00402 index = 0.47dp at 118dp
  step; final 4.0; settle 0.444s. **PASS**.
- Recorded stack: release dx −171.3dp, throw −531.1dp (1.4 × 379.4dp), rotation −10°,
  alpha 0, incoming 1, reset ~460ms (400ms throw + 60ms pause). **PASS**.

## 6. Performance gate — diagnostic only on the emulator

10-second scripted interaction per screen. These numbers predate the final material and
full-resolution blur changes and have not been remeasured; emulator performance is diagnostic
per review. V2528A remains the acceptance device.

| Screen | Frames | Janky | P50 | P95 | PSS |
| --- | --- | --- | --- | --- | --- |
| Deck run 1 | 530 | 6.23% | 18ms | 32ms | 139.4MB |
| Deck run 2 | 526 | 6.65% | 24ms | 36ms | 139.7MB |
| Glass | 393 | 4.58% | 20ms | 29ms | 116.5MB |

The 0-janky / P95 ≤17ms thresholds are not met on this emulator; no V2528A result exists yet.

## 7. Code-level checks

| Check | Status |
| --- | --- |
| Screen-space sampling, crop offset and density | PASS (background-only regions within Δ5; photo background SSIM .999) |
| Shader output color space | PASS (constant 0.5 renders 127) |
| Edge sampling clamped | PASS (`BitmapShader` CLAMP) |
| Displacement channels and sign | PASS (R=x, B=y; sign corrected) |
| Displacement scale in device pixels | PASS |
| Filter-region map window | PASS (centered `1/1.7` × element/filter aspect) |
| CSS background paint order | PASS (verified in Chromium; first image on top) |
| Premultiplied gradient interpolation | PASS for the 180° card sheen; pill 3-stop vertical uses premultiplied |
| SVG screen-blend alpha for short surfaces | PASS (d1 pill warp MAE 1.71, Δ5 0.009%) |
| Missing backdrop fallback | tint-only branch |
| Reduced transparency / reduced motion switch | MISSING (phase-2 API requirement) |
| API 33+ only, no CPU blur fallback | enforced by `minSdk=33`; CPU fallback deleted |
| No GPL/unknown code, no secrets | PASS |

## Reproduction

```bash
./gradlew :demo:assembleDebug
adb install -r demo/build/outputs/apk/debug/demo-debug.apk
```

Canonical Android capture (add `--es bg photo` or `--es bg obsidian` for stress scenes):

```bash
adb shell am force-stop com.mainecare.effects.demo.debug
adb shell am start -n com.mainecare.effects.demo.debug/com.mainecare.effects.demo.MainActivity \
  --es screen gate --es displacement 0 --es text 0 --es shadow 1 --es before 1
adb exec-out screencap -p > verification/results/android-smooth-d0.png
```

Chromium reference:

```bash
"C:/Program Files/Google/Chrome/Application/chrome.exe" --headless=new --disable-gpu \
  --hide-scrollbars --force-device-scale-factor=2.625 --window-size=390,844 \
  --screenshot="D:/maine/mainecare-effects/verification/results/ws-current-d0.png" \
  "file:///D:/maine/mainecare-effects/verification/web-reference/gate.html?displacement=0"
```

Metrics:

```bash
node verification/crop-gate.mjs verification/results/android-smooth-d0.png verification/results/android-smooth-d0-crop.png
node verification/compare-png.mjs verification/results/android-smooth-d0-crop.png verification/results/ws-current-d0.png verification/results
node verification/region-metrics.mjs verification/results/android-smooth-d0-crop.png verification/results/ws-current-d0.png
```

`compare-png.mjs` writes `<name>-metrics.json` and `<name>-diff.png`; `region-metrics.mjs`
prints card/pill (or named) regions.
