# App adapter contract — proposal

Status: **proposal, not final**. Do not add a dependency or integrate the library into
MaineCare until the restoration gate passes and the user accepts phase 1.

Date: 2026-11-01. Owner: mainecare-effects session. Consumer: MaineCare Android v21 session.

## 1. Gate status and dependency form

The gate is **not passed**:

- Static material: FAIL (SSIM 0.98832, MAE 1.243; target SSIM ≥0.995, MAE ≤1.0).
- Displacement: FAIL (SSIM 0.98406, MAE 1.641; target ≥0.99, ≤1.5).
- Motion: PASS for the measured coverflow/stack runs.
- Emulator performance: FAIL (deck 6.23–6.65% janky, P95 32–36ms; glass 4.58%, P95 29ms).

Keep `MaineGlassCard` as the only adapter seam. Do not call `GlassSurface` from page code and
do not add the effects modules to the app dependency graph yet.

Dependency form after acceptance (final choice still open):

- **Development:** Gradle included build or vendored source modules `:motion`, `:glass`,
  `:deck` from this repository. This is the lowest-risk way to iterate before publishing.
- **Distribution (recommended):** private GitHub Packages
  `com.mainecare.effects:motion:0.1.0`, `:glass:0.1.0`, `:deck:0.1.0` (version 0.1.0 is the
  first proposed release, subject to the gate). The app adds the repository and credentials.
- **Fallback:** vendored modules if GitHub Packages access is a problem; the license/NOTICE
  files must travel with the source.

No version is published today. PR #1 is the review artifact.

## 2. Public surface the adapter may use

Current phase-1 signature (implemented):

```kotlin
GlassBackdrop(
    image: ImageBitmap,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    content: @Composable BoxScope.() -> Unit,
)

GlassSurface(
    modifier: Modifier = Modifier,
    material: GlassMaterial = MaineGlassMaterials.Card,
    params: LiquidGlassParams = material.liquid,
    backdrop: GlassBackdropState? = LocalGlassBackdrop.current,
    interactive: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
)
```

Presets: `MaineGlassMaterials.Card`, `Advice`, `MealFront`, `Insight`, `Dock`, `SoftControl`.
`LiquidGlassParams` mirrors the React props (`displacementScale`, `blurAmount`, `saturation`,
`aberrationIntensity`, `elasticity`, `cornerRadius`, `mode`, `overLight`).

The adapter should keep a single mapping from MaineCare theme/state to a preset plus optional
`params.copy(...)` overrides. It should not use `RuntimeShader`, the raw maps, or
`GlassBackdropState` internals.

## 3. Proposed `enabled` / `quality` and reduced-motion/transparency

These are **not implemented in phase 1**; they are the phase-2 API proposal so the App adapter
can reserve the seam:

```kotlin
enum class GlassQuality { Auto, High, Medium, Low }

GlassSurface(
    // ...current parameters...
    enabled: Boolean = true,
    quality: GlassQuality = GlassQuality.Auto,
)
```

Proposed behavior:

- `enabled = false`: no shader, no blur, no backdrop sampling. Draw the material tint, border,
  top highlight and inner shadow only. This is the reduced-transparency fallback and also the
  safe fallback when no backdrop bitmap is available.
- `quality = High`: AGSL refraction + cached blur + full sheen. Requires API 33+ (already the
  library minimum).
- `quality = Medium`: cached blur + tint/sheen, displacement disabled. For low-end devices or
  when the user disables refraction.
- `quality = Low`: tint/border only (same rendering path as `enabled = false`), no shader.
- `Auto`: High on the target device; the app may choose Medium/Low from its own device tier or
  settings. No automatic frame-rate heuristic in phase 1.

Reduced motion (proposed `MaineMotionPolicy`, a CompositionLocal or explicit parameter):

- Coverflow: snap to the target index, no spring; no double-throw.
- CardStack: no throw; 120ms opacity crossfade to the next card (the v18 reduced-motion
  behavior).
- CardDeck: Pager scroll with `reduceMotion` snap; no depth/scale animation.
- `GlassSurface`: pointer sheen is disabled (it is the only glass animation).
- Read the system animator duration scale / accessibility reduce-motion signal in the adapter,
  not inside the shader.

Reduced transparency is an app/theme setting (Android does not expose a single global
reduced-transparency switch). Map it to `enabled = false` or `quality = Low`; the adapter owns
the setting and can persist it. The library will not read app preferences.

## 4. Live backdrop / dock over scrolling content

Phase 1 supports a **static `ImageBitmap` snapshot** only. It is exact for the photo state and
static artwork; it does not sample live scrolling content.

Phase 2 target: a live backdrop source (Compose `GraphicsLayer` / `RenderEffect` capture) so a
dock or card over scrolling content refracts the content underneath. This is a proposed
follow-up and is **not committed**; it depends on the user accepting phase 2 and the gate fix
work. If phase 2 is descoped, the App must keep using the static snapshot and accept that the
dock over scrolling content will not refract live content.

The shader already consumes a bitmap; a live adapter can refresh that bitmap without changing
the public `GlassSurface` material/params surface.

## 5. Compatibility, license, maintenance

- `minSdk = 33` (AGSL requirement), `compileSdk = 36`, JDK 17 bytecode.
- Kotlin 2.2.21, AGP 8.13.0, Compose BOM 2026.03.00; the MaineCare android-native project uses
  the same BOM and minSdk 33, so the module versions are compatible.
- Runtime dependencies: Compose UI/foundation/animation only; no third-party runtime library.
- Project license: Apache-2.0. liquid-glass-react 1.1.1 map assets/filter math are MIT with the
  notice in `THIRD_PARTY_NOTICES.md` and `third_party/licenses/`; no GPL/unknown code.
- Maintenance: this is a project-internal library, maintained with MaineCare; there is no
  external support SLA. Versioned releases will be cut only after the gate passes, and the
  gate must be rerun on material changes.
- No test account, credential, token, or personal data is stored in this repository.

## 6. Pre-integration checklist

1. User accepts phase 1 and the gate report.
2. Material/displacement gate passes (or the user explicitly accepts a documented exception).
3. Dependency form is chosen and a version is published or vendored.
4. `enabled`/`quality` and reduced-motion/transparency behavior are implemented and tested.
5. Live backdrop decision is recorded (supported or explicitly static-only).
6. Adapter-only change in the App; no page business logic changes.
7. Re-run the gate on V2528A and record the device/refresh-rate result.
