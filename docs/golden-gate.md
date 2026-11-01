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
| SSIM (windowed 8×8, luminance) | ≥ 0.995 | **0.99536** |
| Mean absolute RGB error | ≤ 1.0 / 255 | **0.428** |
| Pixels with max channel Δ ≤ 5 | ≥ 99% | **99.571%** (0.429% above Δ5) |
| Frame bounds | exactly 1024×2216 at (28,92) | exact |

Status: **PASS**.

- Whole frame: MAE 0.428, PSNR 42.8 dB.
- Card (898×525): MAE 1.185, SSIM 0.99184, 1.22% above Δ5.
- Pill (347×95): MAE 3.180, SSIM 0.92953, 12.08% above Δ5. The adaptive border raises the
  pill’s MAE versus the previous constant border, but the whole-frame SSIM still passes.
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
| SSIM | ≥ 0.99 | **0.99531** |
| MAE | ≤ 1.5 / 255 | **0.422** |
| Pixels within Δ5 | ≥ 98% | **99.645%** (0.355% above Δ5) |

Status: **PASS**.

- Whole frame: PSNR 42.8 dB.
- Card: MAE 1.179, SSIM 0.99166, 1.10% above Δ5.
- Pill: MAE 2.876, SSIM 0.92860, 8.75% above Δ5.
- Pill max Δ is 47 on the same 1px bottom-border row; card max Δ is 24.

Evidence: `docs/evidence/gate-smooth-side-d1.png`, `gate-smooth-diff-d1.png`.

## 3. Stress scenes — reported, not accepted as passes

| Scene | Displacement | SSIM | MAE | > Δ5 | Status |
| --- | --- | --- | --- | --- | --- |
| Photo | off | 0.99318 | 0.796 | 0.994% | **FAIL SSIM only** (Δ5 passes) |
| Photo | on | 0.99326 | 0.771 | 0.520% | **FAIL SSIM only** |
| Obsidian veins | off | 0.99147 | 0.792 | 0.410% | **FAIL SSIM** |
| Obsidian veins | on | 0.99145 | 0.790 | 0.343% | **FAIL SSIM** |

The stress residuals are now concentrated almost entirely in the **pill**:
photo d0 card SSIM 0.99600 / MAE 1.196, pill SSIM 0.87596 / MAE 5.231. The pill is small but
its block SSIM drags the frame below 0.995.

Two measured causes:

1. **Sibling surface shadow.** CSS paints the card’s outer shadow on the shared stage before
   the pill is composited. Android draws each surface’s shadow inside that surface’s own box,
   so the card shadow never reaches the stage behind the pill. With shadows disabled on both
   sides, photo d0 rises to SSIM 0.99369 / MAE 0.678 / 0.633% > Δ5, and pill core MAE falls
   from 3.29 to 1.81. The residual after that is the pill alpha/blur composition, not the tint.
2. **Warp element alpha.** A Chromium probe of a 132×36 white div with `filter: blur(24px)`
   measures center alpha ≈ 0.56; the shader now models that alpha with an unblurred scene
   input and the card background-color underlay. This fixed the one-sided photo edge error
   (photo card SSIM 0.981 → 0.996) but cannot recover the sibling shadow without a shared,
   live backdrop render.

Node fits of the blurred photo against sRGB and linear Gaussian candidates gave MAE 0.062 and
0.068 at the best sigma, with the residual insensitive to sigma between 20–44 CSS px; the
remaining difference is compositing/alpha, not a simple Gaussian kernel offset. The fitted
sigma remains 1.4× the CSS radius for the Skia effect. The 0.28px aberration Gaussian is the
only unported filter primitive.

The attainable ceiling with the current static-backdrop architecture is therefore around
SSIM 0.994 for the photo scene; crossing 0.995 requires the shared-stage shadow/live backdrop
work (phase 2), not another tint parameter.

Evidence: `gate-photo-side-d0.png`, `gate-photo-diff-d0.png`,
`gate-obsidian-side-d0.png`, `gate-obsidian-diff-d0.png`.

These scenes are part of the phase-1 report. They must pass before the gate can be called
“100% on arbitrary backdrops”; the canonical v21 pass is not a substitute.

### Stress control matrix

Each row changes one variable on both renderers; no threshold is changed.

| Control | Photo SSIM | Photo MAE | Photo >Δ5 | Obsidian SSIM | Obsidian MAE | Obsidian >Δ5 |
| --- | --- | --- | --- | --- | --- | --- |
| Baseline (current) | 0.99318 | 0.796 | 0.994% | 0.99147 | 0.792 | 0.410% |
| No card/outer shadow | **0.99369** | **0.678** | 0.633% | **0.99299** | **0.663** | 0.368% |
| Live shadowed stage (diagnostic) | 0.99327 | 0.769 | 0.575% | 0.99155 | 0.783 | 0.354% |
| No blur (grade only) | 0.99240 | 0.818 | 1.025% | 0.99088 | 0.799 | 0.407% |
| Offscreen CPU box-Gaussian, sigma 1.0 | 0.99324 | 0.779 | 0.883% | 0.99167 | 0.768 | 0.356% |

- **Card shadow** is real: removing it raises photo SSIM .99318 → .99369 and pill core MAE
  3.29 → 1.81. The live-stage control (a scene bitmap that already contains the card shadow)
  recovers the pill core to 1.62 but does not cross the frame threshold.
- **Blur kernel** is not the limiter: removing blur lowers SSIM (.99318 → .99240,
  .99147 → .99088) instead of improving it. A separate offscreen CPU box-Gaussian
  (three separable passes, Chromium-aligned sigma = 1.0× CSS radius, no 1.4 calibration)
  improves the raw photo card MAE 4.37 → 3.58 but only moves the frame to 0.99324 / 0.99167.
  Sweeping its sigma 0.8–1.2 leaves SSIM at 0.99323–0.99324, so the kernel is not the
  remaining limit.
- **Static backdrop** is part of the residual but not the whole: the live shadowed stage
  improves the pill’s core and over-Δ5 count, yet the frame remains at .99327/.99155 because
  the short pill’s border/highlight/alpha rasterization and the CSS vs Android edge
  compositing still differ.
- No single control, nor the shadow-off upper bound, reaches 0.995. The measured ceiling for
  this static-backdrop/per-surface-shadow architecture is ≈**0.994 SSIM** on the photo scene.

### Visual impact at the ceiling

Photo baseline: 99.4% of pixels within Δ5, 0.21% above Δ10, 0.042% above Δ20, max Δ73.
Obsidian: 99.6% within Δ5, 0.13% above Δ10, 0.030% above Δ20, max Δ62. The tail is localized
to the pill border/top highlight and the shadowed edge; at 1:1 viewing it is not perceptible,
and only close-up inspection of the pill edge shows a slight halo/contrast shift. The
canonical scene does not show this tail because its backdrop is low-frequency.

### Options

1. **Accept the measured ceiling** — keep the canonical PASS, document the stress scenes as
   non-passes, and do not merge/integrate until the other gates are accepted.
2. **Change the rendering path** — add a shared live backdrop that captures rendered surface
   shadows and implement the short-element alpha/border compositing natively (phase 2). This
   is the only path found that can plausibly cross 0.995; it is not a parameter tweak.
3. **Device-only comparison** — rerun the same thresholds on V2528A at native scale. This is
   required regardless, but the controls above suggest rasterization/compositing differences
   will persist, so it is not a substitute for option 1 or 2.

## 4. Layer-by-layer parameter gate

Values transcribed from `feeding-v21-obsidian.css` / `feeding-v6-shell.css`; compositing
changes below are measured against the Chromium reference.

| Layer | Value | Implementation | Status |
| --- | --- | --- | --- |
| Card tint | `rgba(10,12,15,.56)` | `Card.tint` | exact |
| Card blur | 34px + saturate 132% + brightness .82 | full-resolution Skia `BlurEffect`, 1.4× sigma calibration; warp alpha modeled with an unblurred scene | canonical card SSIM 0.995; photo card 0.996 |
| Card border | `rgba(235,239,244,.16)` over the card background-color | 2px rounded-box SDF in the shader over `mix(unblurred stage, tint, .56)`; the bottom edge blends .7 of the raw stage | top/left/right match the web raster; bottom edge remains a reported residual |
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
