# Phase 1 comparison report — glass, deck, motion

Date: 2026-11-01. All results below come from the built demo APK running on the API 36
emulator and from the checked-in verification harness. Nothing in this report is estimated.

## What was delivered

- `:motion` — v18/v21 tokens, JS-exact spring, geometry helpers, opt-in frame telemetry.
- `:glass` — `GlassBackdrop` (bitmap capture + cached box blur), `GlassSurface` (AGSL
  refraction + v21 tint/sheen), `LiquidGlassParams`, six `MaineGlassMaterials` presets, three
  byte-identical liquid-glass-react displacement maps.
- `:deck` — `CardDeck` (Compose Pager + v13/v21 transforms), `Coverflow` (custom layout +
  v18 spring), `CardStack` (drag/throw/rebound with v18 constants).
- `:demo` — Glass sample (photo + glass cards), Deck sample (deck + stack + coverflow), and a
  fixed 390×844 gate screen for pixel and motion measurements.
- `verification/` — deterministic backdrop generator, Chromium gate reference, PNG metrics
  (MAE/RMSE/PSNR/SSIM/Δ-thresholds + diff image), motion trace capture, performance files.

## How to reproduce

```bash
./gradlew :demo:assembleDebug
adb install -r demo/build/outputs/apk/debug/demo-debug.apk
adb shell am start -n com.mainecare.effects.demo.debug/com.mainecare.effects.demo.MainActivity --es screen glass
adb shell am start -n com.mainecare.effects.demo.debug/com.mainecare.effects.demo.MainActivity --es screen deck
adb shell am start -n com.mainecare.effects.demo.debug/com.mainecare.effects.demo.MainActivity --es screen gate --es displacement 0 --es text 0
```

The gate reference is `verification/web-reference/gate.html`, rendered in headless Chromium at
390×844 CSS px and `deviceScaleFactor=2.625` (1024×2216). Android captures the same frame at
(28,92) in the 1080×2400 screenshot, giving an exact 1024×2216 crop.

## 1. Static material gate — FAIL

Android (left) vs Chromium reference (right), card plus surrounding backdrop:

![Card side-by-side, displacement off](evidence/gate-card-side-d0.png)

Difference image (bright = larger Δ; full frame):

![Gate difference, displacement off](evidence/gate-diff-d0.png)

| Region | MAE | RMSE | Max Δ | > Δ5 | > Δ10 |
| --- | --- | --- | --- | --- | --- |
| Whole 1024×2216 frame | 1.239 | 2.640 | 69 | 4.89% | 1.41% |
| Card (898×525) | 3.77 | 5.18 | 58 | 17.7% | 2.4% |
| Pill (347×95) | 12.55 | 14.62 | 69 | 82.8% | 62.6% |

Whole-frame SSIM 0.98839, PSNR 39.70 dB. Required: SSIM ≥ 0.995, MAE ≤ 1.0, ≥99% within Δ5.
**Not met**, but this iteration cut whole-frame MAE by 55% (2.775 → 1.239) and raised SSIM
from 0.9784 to 0.9884.

The background alignment is good, which isolates the mismatch to the material. Changes made in
this review iteration:

- **Blur model.** Replaced the 3-pass box with a screen-space Skia `GraphicsLayer` +
  `BlurEffect` at 540px, calibrated to 1.4× sigma. Against a blur-only web reference, the raw
  interior MAE fell from 5.28 (sRGB box) to 2.74. The remaining raw-blur gap is Chrome's
  resampling/edge handling, not the color space.
- **Reference bug fixed.** The web gate used default `content-box`, so its 1px border enlarged
  the card/pill by 2 CSS px. It now uses `border-box`; the card max Δ fell from 112 to 58.
- **Shadow.** Compose elevation was replaced with a RenderEffect `Decal` shadow (offset 24dp,
  Gaussian spread calibrated to 42dp, alpha .56). The earlier `Clamp` mode painted a solid
  halo over the frame; `Decal` removed that. The shadow tails still differ from CSS.
- **Edge model.** The web card now carries the same tint underlay as v21, and the Android top
  highlight was moved inside the border. Border/highlight still account for most of the card
  error.
- **Layer model.** `feature-card::before` is now ported at a calibrated 0.5 strength;
  `advice::after` is implemented. The browser mask and the shader mask still differ.
- **Pill.** Its 36dp height makes the inset shadow and the 1px border a larger fraction of the
  surface; the pill remains the worst region and is reported separately.

A debug shader output of constant 0.5 renders as 127, so the shader writes sRGB rather than
linear; the difference is not a gamma mismatch.

## 2. Displacement gate — FAIL

![Card side-by-side, displacement on](evidence/gate-card-side-d1.png)

![Gate difference, displacement on](evidence/gate-diff-d1.png)

| Region | MAE | RMSE | Max Δ | > Δ5 | > Δ10 |
| --- | --- | --- | --- | --- | --- |
| Whole frame | 1.623 | 3.784 | 71 | 9.27% | 3.44% |
| Card | 5.17 | 6.90 | 63 | 38.1% | 10.1% |
| Pill | 24.06 | 25.64 | 70 | 93.5% | 92.1% |

Whole-frame SSIM 0.98418, PSNR 36.57 dB. Displacement is active; the gate fails because the
material baseline already fails and the displacement/edge model still differs:

- `displacementScale` is now multiplied by display density, fixing a device-pixel vs CSS-pixel
  bug (the React scale is in CSS px). The visible change is small because the reference edge
  mask is only ~11% outside the map's extremes.
- The map is sampled over the card directly. A 170% filter-region window plus `slice` was
  tested with four candidate mappings; the MAE change was below 0.01, so it was reverted.
- The aberration scales use the correct sign (`S`, `S−a*.05`, `S−a*.1` for R/G/B) and the
  discrete edge mask is ported. The reference's 0.28px Gaussian on the aberrated channels is
  not.

## 3. Parameter gate — mostly exact, not complete

The values in `docs/golden-gate.md` are checked line by line. Highlights:

- Card tint `rgba(10,12,15,.56)`, blur 34, saturate 132%, brightness .82: values exact;
  blur is a screen-space Skia blur calibrated to 1.4× sigma.
- Advice tint `.66`, blur 32, saturate 134%, brightness .80, gold top edge: values exact;
  bottom `::after` glow implemented outside the card gate.
- Meal front `.52` / 22 / 132% / .84, radius 16: values exact; selected gold border partial.
- Insight `.76` / 26 / 132% / .84, radius 20: values exact.
- Dock `.40` / 24 / 138% / .88 plus glass-sheen: values exact; pill inset-shadow profile
  differs.
- Card `::before`: ported at 0.5 strength; browser mask vs shader mask still differs.
- Card shadow: RenderEffect Decal with calibrated spread; CSS tails still differ.
- Displacement maps: byte-identical to liquid-glass-react 1.1.1.

The gate is **FAIL** because “100%” includes the remaining `::before`/pill/border/shadow
differences, not just the numeric CSS values.

## 4. Motion gate — PASS for the measured runs

- Unit test: 20 consecutive 60 Hz spring samples match the JS `springStep` within 1e-4.
- Coverflow recorded run: release target 4.0, release velocity 3.248 index/s, 25 spring
  frames. Against the JS spring stepped with the measured frame intervals, the maximum
  deviation is **0.00402 index units = 0.47dp** at the 118dp card step; final value 4.0;
  settle 0.444s. A double-throw was triggered by the gesture and the final index was exact.
- Stack recorded run: release dx −171.3dp; throw to −531.1dp (1.4 × 379.4dp card width) with
  rotation −10°, alpha 0, incoming progress 1, reset at ~460ms (400ms throw + 60ms pause).
  This matches the v18 constants.
- The coverflow double-throw thresholds (.72 dp/ms or 1.15 index) and the stack threshold
  (22% width or 0.45 dp/ms) are unit tested.

Limitations: one gesture run per component, one device/refresh rate; the UI comparison script
scores the coverflow spring but not every deck/stack state. The frame-by-frame video was
captured locally but is not committed (binary size); the numeric trace is stronger evidence.

## 5. Performance gate — FAIL on this emulator run

10-second scripted interaction per screen, `trace=0`, API36 emulator at 1.5GB RAM, host GPU:

| Screen | Frames | Janky | P50 | P95 | P99 | Missed vsync | PSS |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Deck run 1 (deck+stack+coverflow) | 530 | 33 (6.23%) | 18 ms | 32 ms | 44 ms | 3 | 139.4 MB |
| Deck run 2 (telemetry off) | 526 | 35 (6.65%) | 24 ms | 36 ms | 57 ms | 3 | 139.7 MB |
| Glass (card drags) | 393 | 18 (4.58%) | 20 ms | 29 ms | 34 ms | 1 | 116.5 MB |

Required: 0 janky, P95 ≤ 17ms. **Not met.** These numbers predate the material fixes and were
not remeasured; per review, emulator performance is diagnostic only. The two deck runs bracket
host-load variance (P50 18–24ms, P95 32–36ms); the glass run is steadier (P50 20ms, P95 29ms).
The emulator is running with a reduced 1.5GB allocation because the host page file was
exhausted, and the host is shared with other workloads; this is not a V2528A result.

## 6. Comparison with the reference libraries

| Library | What it does | Why this spike does not use it | Difference to this library |
| --- | --- | --- | --- |
| Haze | Production Compose backdrop sampling/blur/glass; live source, dirty areas, fallbacks | No code copied; current Haze uses platform backdrop APIs (including reflective access on some versions), which is a release risk; the v21 photo state does not need live scrolling capture | Ours is a snapshot bitmap + single AGSL pass; no live foreground sampling yet |
| Cloudy | KMP blur/liquid-glass/custom shader surfaces with quality tiers and CPU fallback | No code copied; broader API and KMP architecture than the phase-1 need | Ours is Android-only, narrow, and tuned to the v21 map/filter math |
| Kyant0/AndroidLiquidGlass | SDF-texture refraction via `RuntimeShader` | No code copied; different refraction model | Ours uses liquid-glass-react's map and filter chain, not an SDF texture |
| NadeemIqbal/liquid-glass | `Modifier.liquidGlass()`, chroma lift, edge sheen, quality tiers | No code copied; modifier API and material model differ | Ours is a surface composable with v21 presets and map-based displacement |

## 7. What is required before an app integration / 100% claim

1. Close the remaining raw-blur gap (Skia blur MAE 2.74) and the card edge gap; the
   whole-frame MAE is 1.239, but the card is 3.77 and the pill is 12.55.
2. Refine `feature-card::before` / `advice::after` and the remaining per-layer textures, or
   explicitly descope them with the user. The current `::before` is a 0.5-strength
   approximation.
3. Tune the inset-shadow profile and the pill border/underlay; the pill is the worst region.
4. Port the reference's 0.28px Gaussian on the aberrated channels and re-check the map window.
5. Re-run the gate on V2528A at its native resolution/refresh rate; the emulator numbers are
   pre-acceptance only.
6. Add the reduced-transparency/reduced-motion switches and a live backdrop adapter before
   the App consumes the library.

## Evidence inventory

- `docs/evidence/sample-glass.png` — Glass sample.
- `docs/evidence/sample-deck.png` — Deck sample.
- `docs/evidence/gate-card-side-d0.png`, `gate-card-side-d1.png` — card side-by-side.
- `docs/evidence/gate-pill-side-d0.png`, `gate-pill-side-d1.png` — pill side-by-side.
- `docs/evidence/gate-diff-d0.png`, `gate-diff-d1.png` — full-frame difference images.
- `docs/evidence/gate-d0-metrics.json`, `gate-d1-metrics.json` — raw metric JSON.
- `verification/results/gfxinfo-*.txt`, `meminfo-*.txt` — raw device output (gitignored).
- `verification/results/motion-coverflow2.log`, `motion-stack.log` — frame traces.
