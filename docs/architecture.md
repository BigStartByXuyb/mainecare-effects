# Architecture — phase 1

## Goal

A reusable Android Compose effects library for MaineCare, consumed as Gradle modules instead of
hand-written effects inside the app. Phase 1 is a verifiable spike: the glass material, the
card deck, the draggable stack and the coverflow, with a restoration gate against the v21 web
reference. It is not connected to the MaineCare app yet.

## Modules

```
:mainecare-effects
├── :motion   springs, easing, geometry, motion telemetry       (no Android UI deps)
├── :glass    backdrop capture, blur, AGSL refraction, presets  (depends on :motion)
├── :deck     CardDeck, Coverflow, CardStack                    (depends on :motion)
└── :demo     two samples + fixed-size pixel/motion gate        (depends on all)
```

All modules use `compileSdk = 36`, `minSdk = 33`, JDK 17 bytecode, Compose BOM 2026.03.00,
AGP 8.13.0 and Kotlin 2.2.21. API 33 is required for `RuntimeShader` / AGSL.

## Public surface

### glass

```kotlin
@Composable
fun GlassBackdrop(
    image: ImageBitmap,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    content: @Composable BoxScope.() -> Unit,
)

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    material: GlassMaterial = MaineGlassMaterials.Card,
    params: LiquidGlassParams = material.liquid,
    backdrop: GlassBackdropState? = LocalGlassBackdrop.current,
    interactive: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
)
```

`LiquidGlassParams` mirrors liquid-glass-react's props: `displacementScale`, `blurAmount`,
`saturation`, `aberrationIntensity`, `elasticity`, `cornerRadius`, `mode`, `overLight`.
`GlassMaterial` carries the v21 layer values (tint, CSS blur/saturation/brightness, border,
inner shadow, sheen gradients, gold top edge, shadow elevation). Presets live in
`MaineGlassMaterials` (`Card`, `Advice`, `MealFront`, `Insight`, `Dock`, `SoftControl`).

`GlassBackdropState` is a Compose state holder: it stores the full bitmap, the backdrop bounds
in root coordinates, and a per-radius cache of downscaled blurred bitmaps. `GlassSurface`
computes its source window from its own root bounds, so cards can move or scroll without
re-recording the backdrop.

### deck

```kotlin
@Composable fun CardDeck(pageCount, modifier, state, pageSpacing, userScrollEnabled, content)
@Composable fun <T> Coverflow(items, modifier, initialIndex, itemWidth, itemHeight, onIndexChanged, content)
@Composable fun CardStack(count, modifier, initialIndex, onIndexChanged, backCardColor, backCardBorderColor, cornerRadius, content)
```

- `CardDeck` uses Compose Foundation `HorizontalPager` plus the v13/v21 per-page transform
  (`scale 1 → .97 → .94`, depth `0 → -48 → -90dp`, alpha `1 → .13 → 0`).
- `Coverflow` is a custom `Layout`: `offset`, `rotateY`, `scale`, alpha and z-index are the v18
  formulas; dragging and release use the v18 thresholds and the JS spring. It does not depend
  on Embla or a third-party carousel.
- `CardStack` is a pointer-input stack: top card follows the pointer with 8° maximum rotation;
  the incoming card rises from 8dp/.96/.12 to 0dp/1/.62; a throw beyond 22% width or 0.45 dp/ms
  flies the card out with the v18 cubic-bezier and advances the index.

### motion

`MaineMotion` holds every v18/v21 token. `springStep` is a semi-implicit Euler step with the
same dt clamp (1–32 ms), settlement test and maximum duration as `src/shared/motion.ts`.
`JsSpringAnimatable` drives it from `withFrameNanos`, so frame samples are comparable with the
web instead of approximating with a Compose `spring()` spec. `MotionTrace` is an opt-in
telemetry sink used by the demo to record `deck.progress`, `coverflow.center` and
`stack.state` with `System.nanoTime()`.

## Rendering pipeline (glass)

1. **Backdrop.** The page provides a bitmap (photo or generated obsidian). `GlassBackdrop`
   draws it, records its root bounds and exposes `GlassBackdropState` through
   `LocalGlassBackdrop`.
2. **Blur.** `GlassBackdropState.blurred(radiusPx)` renders the backdrop at screen geometry
   into a Compose `GraphicsLayer`, applies a Skia `BlurEffect` at full screen resolution
   (2160px cap) with a calibrated 1.4× sigma, then reads it back with `toImageBitmap()`. There
   is no CPU or API-version fallback: API 33+ is the only target. Results are cached by
   rounded radius; the work runs once per distinct material, not per frame.
3. **Source mapping.** A `GlassSurface` records its own root bounds. With `ContentScale.Crop`
   the mapping from surface pixels to bitmap pixels is computed from the drawn image size and
   the centered crop offset. The shader receives `uBackdropOrigin`, `uBackdropScale` and
   `uSize`.
4. **AGSL pass.** `LiquidGlassShader` samples the displacement map through the SVG filter's
   170% region and `slice` window, grades the blurred source (saturate/brightness) before
   displacing, screen-blends the three channel layers with the blurred element alpha, then
   applies tint and the v21 sheen layers in CSS paint order. The shader output is clipped by
   the Compose shape.
5. **Compose edges.** A RenderEffect `Decal` shadow sits behind the card (offset/blur/alpha
   per material); border, top highlight, gold top line and the Gaussian inner shadow are
   drawn by Compose after the shader, so they stay crisp and are not displaced. The border is
   a 2px rounded-box SDF in the shader over the unblurred stage, so it follows the local
   backdrop brightness; the top/left/right edges match the web raster, the bottom edge is a
   reported residual.

The blur is precomputed into the bitmap, so the shader is a single pass with map + backdrop
lookups. `interactive=true` tracks a pointer without consuming the gesture; the pointer shifts
the sheen by `elasticity * 0.1` in UV space.

## Coordinate and density rules

- All v21 CSS values are in dp at the 390dp reference viewport. Compose dp equals CSS px at
  that viewport; the 390dp gate frame maps to 390 CSS px.
- Gesture distances are converted from device px to dp before applying v18 thresholds; layer
  translations convert back to px. This keeps `22% of card width` and `0.45 dp/ms` meaningful
  on any density.
- The gate frame is 390×844dp, rendered at 1024×2216 px on the 420dpi emulator
  (2.625×), matching the headless Chromium screenshot at `deviceScaleFactor=2.625`.

## Known limitations / phase-2 work

1. **Snapshot backdrop, not live content.** The shader consumes an `ImageBitmap`. This is exact
   for the photo and static artwork, and good enough for the v21 feeding page's photo state,
   but the dock over scrolling content needs a live source (the Haze/`GraphicsLayer` path is
   the follow-up).
2. **Color space.** A debug shader output of constant 0.5 renders as 127, so the shader writes
   sRGB rather than linear; gamma conversion is ruled out. The remaining card difference is in
   the material/blur approximation, not the output color space.
3. **Missing / approximate v21 layers.** `feature-card::before` is ported with the inset
   2px geometry, all-gradient vertical mask and a gate-calibrated 0.75 strength;
   `advice::after` is implemented. The browser mask and shader phase still differ slightly.
4. **Shared stage shadows.** CSS paints the card’s outer shadow on the shared stage before
   the pill is composited. Android draws each surface’s shadow inside its own box, so a sibling
   shadow cannot reach another surface’s stage sample. This is the dominant photo/veined-pill
   residual; fixing it needs a live backdrop that captures rendered surface shadows (phase 2).
   The inset shadow uses the Gaussian profile; the card shadow uses a Decal blur.
5. **Reduced transparency / reduced motion.** Not exposed yet. The app adapter must be able to
   disable the shader (tint-only fallback) and zero animations; this is an API requirement for
   phase 2.
6. **Deck snap behavior.** `CardDeck` delegates snapping to Pager, whose fling curve is not
   Embla's. The per-page transform is exact; the snap duration is reported as a difference.

## 0.1.1 packaging

- Artifacts: `com.mainecare.effects:motion:0.1.1`, `:glass:0.1.1`, `:deck:0.1.1`, in Maven
  Local (`./gradlew publishToMavenLocal`) and GitHub Packages
  (`https://maven.pkg.github.com/BigStartByXuyb/mainecare-effects`). Each version directory has the AAR, POM,
  Gradle module metadata, sources jar, and license/notice classifier artifacts. The sources
  jar carries `META-INF/LICENSE`, `META-INF/NOTICE` and `META-INF/THIRD_PARTY_NOTICES.md`.
- `glass` and `deck` expose Compose as `api` with the 2026.03.00 BOM; the POM imports the BOM
  and the Gradle metadata carries the platform constraint.
- A separate Android consumer build against Maven Local compiles `MaineGlassCard`,
  `GlassBackdrop` and `CardDeck`. The MaineCare App is not modified.
- GitHub Packages is live for 0.1.0 (remote verified) and 0.1.1 (workflow recorded in the
  PR); JitPack remains a follow-up; no credentials are stored.
- App integration goes through `MaineGlassCard` only; `GlassSurface` stays a demo/gate seam.
  See `docs/release-0.1.1.md` for the adapter API, performance tiers, known ceiling and
  license carriage, and `docs/upstream-ab-0.1.1.md` for the liquid-glass-react A/B.

## Verification assets

- `verification/web-reference/gate.html` — Chromium reference at 390×844, optional
  `?displacement=1`.
- `verification/compare-png.mjs` — MAE/RMSE/PSNR/SSIM and threshold percentages, plus a
  difference image.
- `verification/generate-backdrop.mjs` — deterministic synthetic photo and obsidian backdrops.
- `docs/golden-gate.md` — pass/fail thresholds and the current status.
