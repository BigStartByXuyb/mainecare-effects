# 100% restoration gate — definition and status

This file defines what “100%” means for each ported effect. A gate is only **PASS** when every
criterion in its section is met on the stated device. Differences are reported, never rounded
up to a pass.

Reference device: `Medium_Phone_API_36.0` emulator, 1080×2400 @420dpi (2.625×), 60 Hz, host GPU
(NVIDIA RTX 5060). Target device V2528A (Android 16 / API 36) is not connected yet; emulator
results are a pre-acceptance gate, not the final device sign-off.

## 1. Static material gate — glass card and pill

Scene: 390×844dp fixed frame, synthetic photo backdrop, card at (24,120) 342×200dp, pill at
(24,350) 132×36dp. Android shader runs with `displacementScale=0` and
`aberrationIntensity=0`; Chromium reference (`gate.html?displacement=0`) applies the same
v21 tint, gradients, border and inner shadows without displacement.

Criteria (whole 1024×2216 frame, and separately card/pill regions):

| Metric | Threshold |
| --- | --- |
| SSIM (windowed 8×8, luminance) | ≥ 0.995 |
| Mean absolute RGB error | ≤ 1.0 / 255 |
| Pixels with max channel Δ ≤ 5 | ≥ 99% |
| Max channel Δ in card/pill | ≤ 24 |
| Frame bounds | exactly 1024×2216 at (28,92) in the 1080×2400 capture |

Current status: **FAIL**.

- Whole frame: SSIM 0.97836, MAE 2.775, PSNR 31.37 dB, 86.5% of pixels within Δ5.
- Card region: MAE 3.59, RMSE 6.68, max Δ 105, 12.7% above Δ5, 2.8% above Δ10.
- Pill region: MAE 12.16, RMSE 15.35, max Δ 60, 81.3% above Δ5, 49.2% above Δ10.
- Background outside the card matches closely (top: MAE 1.04, 0.7% above Δ5; bottom:
  MAE 1.26, 3.6% above Δ5; the bottom difference is PNG scaling/noise), so the backdrop
  crop mapping is correct and the remaining difference is material compositing.
- Improvement from the first gate run: MAE 3.97 → 2.78 and SSIM 0.9710 → 0.9784 after fixing
  the CSS linear-gradient projection (the 325° dark layer had been painted from the wrong
  corner).

Evidence: `docs/evidence/gate-card-side-d0.png`, `docs/evidence/gate-diff-d0.png`.

## 2. Displacement gate — refraction and aberration

Same scene with `displacementScale=22` (standard map) and `aberrationIntensity=2.2`; Chromium
reference runs the full SVG filter chain from liquid-glass-react 1.1.1.

Criteria:

| Metric | Threshold |
| --- | --- |
| SSIM | ≥ 0.99 |
| MAE | ≤ 1.5 / 255 |
| Pixels within Δ5 | ≥ 98% |
| Edge ring Δ (8 px band) | reported separately; no pass until the ring is within Δ5 |
| Displacement map identity | byte-identical to liquid-glass-react 1.1.1 |

Current status: **FAIL**.

- Whole frame: SSIM 0.97540, MAE 3.053, PSNR 30.83 dB, 84.3% within Δ5.
- Card region: MAE 4.51, 21.9% above Δ5; pill: MAE 23.83, 99.7% above Δ5.
- The displacement itself is functioning (with/without images differ by 420,403 pixels, max
  channel Δ 55 at the edges), but the material baseline already fails, and the pill exposes a
  displacement-map window mismatch: the web filter places the map in a 170% filter region
  with `preserveAspectRatio="slice"`, while the shader currently samples the map over the
  card directly.

Evidence: `docs/evidence/gate-card-side-d1.png`, `docs/evidence/gate-diff-d1.png`.

## 3. Layer-by-layer parameter gate

Every value is transcribed from `feeding-v21-obsidian.css` / `feeding-v6-shell.css`.

| Layer | Value | Implementation | Status |
| --- | --- | --- | --- |
| Card tint | `rgba(10,12,15,.56)` | `MaineGlassMaterials.Card.tint` | value exact; blending under investigation |
| Card blur | 34 px + saturate 132% + brightness .82 | `blurRadiusDp=34`, shader saturation/brightness | value exact; box-blur approximation |
| Card border | `rgba(235,239,244,.16)` | `borderColor` + 1dp stroke | value exact; rasterization differs |
| Card inner top | `inset 0 1px 0 rgba(255,255,255,.24)` | `topHighlightColor` | approximate line |
| Card inner bottom | `inset 0 -22px 34px rgba(0,0,0,.36)` | `bottomInnerShadowColor`, `innerShadowDepthDp=34` | approximate gradient |
| Card outer shadow | `0 24px 52px rgba(0,0,0,.56)` | `Modifier.shadow(24.dp)` | **differs** (elevation, no spread/blur match) |
| Card sheen | 5 CSS layers: 280px radial at 72%/8%, 92%×74% radial at 108%/-8%, 116°, 325°, 180° | shader uniforms per material | values now mapped; CSS-linear falloff |
| Card `::before` | diagonal + repeating texture, masked | not implemented | **missing** |
| Advice tint/blur | `.66` / 32px / 134% / .80 | `MaineGlassMaterials.Advice` | value exact |
| Advice gold top line | 18px inset, champagne gradient | `goldTopEdge` | implemented |
| Advice `::after` | bottom white glow | not implemented | **missing** |
| Meal front | `.52` / 22px / 132% / .84, radius 16 | `MealFront` | value exact |
| Meal selected border | `rgba(227,192,138,.42)` | demo applies a gold border copy | partial (no inset gold) |
| Insight | `.76` / 26px / 132% / .84, radius 20 | `Insight` | value exact |
| Dock | `.40` / 24px / 138% / .88, glass-sheen | `Dock` | value exact |
| AGSL mode | `standard` map, scale -22, aberration 2.2 | shader + raw asset | map byte-identical; edge mask ported |

## 4. Motion gate

Criteria:

- Spring samples at 60 Hz must match `src/shared/motion.ts` within 1e-4 for position and
  velocity over the full settle.
- Coverflow constants: stiffness 220, damping 27, max duration 760 ms, max velocity 4.4,
  threshold .22, velocity threshold .34, double-throw .72/1.15.
- Stack constants: width threshold .22, velocity .45, boundary damping .26, rotation 8°,
  throw rotation 10°, throw 400 ms, pause 60 ms, rebound 320 ms, incoming 320 ms,
  cubic-bezier(.18,.75,.22,1).
- Recorded UI frame log must follow the reference curve within 0.5dp and settle within one
  frame of the reference duration; final index must be exact.

Current status: **PASS for the measured run**.

- `MaineMotionTest.springMatchesWebReferenceSequence` matches the JS reference for 20
  consecutive 60 Hz frames (position/velocity within 1e-4). **PASS**.
- Geometry/constant tests (`cardDeck*`, `coverflow*`, `stack*`, `wrapDelta`) pass. **PASS**.
- Recorded coverflow spring: release target 4.0, release velocity 3.248 index/s; 25 spring
  frames compared against the JS `springStep` using the measured frame intervals. Maximum
  positional deviation is 0.00402 index units = 0.47dp at the 118dp step; final 4.0; settle
  0.444s. **PASS**.
- Recorded stack: release dx −171.3dp, throw distance −531.1dp (1.4 × 379.4dp width),
  rotation −10°, alpha 0, incoming progress 1, reset at ~460ms (400ms throw + 60ms pause).
  **PASS**.

## 5. Performance gate

10-second scripted interaction per screen, emulator with host GPU:

| Metric | Threshold |
| --- | --- |
| Rendered frames | ≥ 590 / 10 s |
| Janky frames | 0 |
| Missed vsync | 0 |
| P95 frame time | ≤ 17 ms |
| PSS | ≤ 160 MB |
| Software-GPU runs | not accepted as evidence |

Current status: **FAIL** on the emulator run.

Deck screen (10s of swipes, `trace=0`) run 1: 530 frames, 33 janky (6.23%), P50 18ms,
P95 32ms, P99 44ms, 3 missed vsync, PSS 139.4MB. Run 2 after gating the telemetry collectors:
526 frames, 35 janky (6.65%), P50 24ms, P95 36ms, P99 57ms, 3 missed vsync, PSS 139.7MB. The
spread is host-load variance on the shared emulator; both runs fail the thresholds.

Glass screen (10s of drags, `trace=0`, 393 frames): 18 janky frames (4.58%), P50 20ms,
P95 29ms, P99 34ms, 1 missed vsync, PSS 116.5MB.

The emulator was restarted at 1.5GB RAM (host page-file exhaustion) and shared with other
workloads; the numbers are a pre-acceptance measurement, not a V2528A result. The 0-janky and
P95 ≤17ms thresholds are not met.

## 6. Code-level checks

| Check | Status |
| --- | --- |
| Sampling coordinates use the blurred bitmap, crop offset and density correctly | PASS (card bounds and backdrop alignment exact) |
| Color space / alpha compositing matches CSS sRGB | **PASS for output space** — a debug shader output of 0.5 renders as 127, so the shader writes sRGB, not linear. Residual card differences are compositing/blur approximations, not gamma. |
| Edge sampling clamped | PASS (`BitmapShader` CLAMP) |
| Displacement channels R=x, B=y; aberration sign | PASS after sign correction (`feDisplacementMap` uses `S` for R, `S - a*.05` for G, `S - a*.1` for B) |
| Edge mask (discrete 0 / a*.05 / 1) | ported in shader |
| Missing/absent backdrop fallback | tint-only branch implemented |
| Reduced transparency / reduced motion switch | **MISSING** (phase-2 API requirement) |
| API 33+ requirement | enforced by `minSdk=33`; tested on API 36 |
| No GPL/unknown code, no secrets | PASS |

## Reproduction

Build and install:

```bash
./gradlew :demo:assembleDebug
adb install -r demo/build/outputs/apk/debug/demo-debug.apk
```

Android gate (displacement off/on):

```bash
adb shell am force-stop com.mainecare.effects.demo.debug
adb shell am start -n com.mainecare.effects.demo.debug/com.mainecare.effects.demo.MainActivity --es screen gate --es displacement 0 --es text 0
adb exec-out screencap -p > verification/results/android-gate-d0.png
```

Chromium reference:

```bash
"C:/Program Files/Google/Chrome/Application/chrome.exe" --headless=new --disable-gpu --hide-scrollbars --force-device-scale-factor=2.625 --window-size=390,844 --screenshot="D:/maine/mainecare-effects/verification/web-reference/gate-web-d0.png" "file:///D:/maine/mainecare-effects/verification/web-reference/gate.html?displacement=0"
```

Metrics:

```bash
node verification/crop-gate.mjs verification/results/android-gate-d0.png verification/results/android-gate-d0-crop.png
node verification/compare-png.mjs verification/results/android-gate-d0-crop.png verification/web-reference/gate-web-d0.png verification/results
```

The comparison script writes `<name>-metrics.json` and `<name>-diff.png`.
