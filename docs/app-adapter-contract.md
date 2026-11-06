# App adapter contract — 0.1.4

Status: **0.1.4 artifact available (Maven Local + GitHub Packages); App integration still blocked**. Do not add the
dependency or integrate into MaineCare until the merged test package is accepted, the stress
ceiling is accepted, and the V2528A performance run is reviewed.

Date: 2026-11-06. Owner: mainecare-effects session. Consumer: MaineCare Android v21 session.

## 1. Gate status and dependency form

The canonical smooth v21 feeding surface now passes; the stress scenes and device
performance do not, so integration remains blocked:

- Canonical static material: PASS (SSIM 0.99548, MAE 0.421, 0.414% above Δ5).
- Canonical displacement: PASS (SSIM 0.99542, MAE 0.416, 0.346% above Δ5).
- Photo stress scene: FAIL SSIM only (0.99318; MAE 0.796 and Δ5 pass); veined Obsidian FAIL
  SSIM (0.99147). The residual is the card shadow on the shared CSS stage behind the pill and
  the pill’s blurred-alpha composition; the static-backdrop ceiling is about 0.994.
- Motion: PASS for the measured coverflow/stack runs.
- Emulator performance: FAIL (deck 6.23–6.65% janky, P95 32–36ms; glass 4.58%, P95 29ms);
  these numbers predate the final material changes and the V2528A device run.

`MaineGlassCard` is the only adapter seam. Do not call `GlassSurface` from page code and do
not add the effects modules to the app dependency graph yet.

Dependency form for the merged test package:

- **Current:** `com.mainecare.effects:motion:0.1.4`, `:glass:0.1.4`, `:deck:0.1.4`, in Maven
  Local (`./gradlew publishToMavenLocal`) and GitHub Packages
  (`https://maven.pkg.github.com/BigStartByXuyb/mainecare-effects`; 0.1.0, 0.1.1, 0.1.3 and 0.1.4 remote
  verified by the publish workflow). A separate Android consumer build against Maven Local
  compiles `MaineGlassCard`, `GlassBackdrop`, `CardDeck` and passes.
- **Fallback:** Gradle included build or vendored source modules.
- **Fallback:** Gradle included build or vendored source modules.

The license/notice classifier artifacts and `META-INF` copies in the sources jar travel with
each AAR. See `docs/release-0.1.4.md`.

## 2. Public surface the adapter may use

Page wrapper and the only glass seam:

```kotlin
GlassBackdrop(
    image: ImageBitmap,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    content: @Composable BoxScope.() -> Unit,
)

MaineGlassCard(
    modifier: Modifier = Modifier,
    material: GlassMaterial = MaineGlassMaterials.Card,
    params: LiquidGlassParams = material.liquid,
    interactive: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
)
```

`GlassSurface` remains public for the demo/gate but is not an adapter seam. Presets:
`MaineGlassMaterials.Card`, `Advice`, `MealFront`, `Insight`, `Dock`, `SoftControl`, plus
`Upstream` (liquid-glass-react 1.1.1 source defaults: 70 / .0625 / 140 / 2 / .15 / 999,
clear glass; blur is 6px dark and 14px with overLight, per the source formula; v21 presets keep
their own values).
`LiquidGlassParams` mirrors the React props (`displacementScale`, `blurAmount`, `saturation`,
`aberrationIntensity`, `elasticity`, `cornerRadius`, `mode`, `overLight`).

The adapter should keep a single mapping from MaineCare theme/state to a preset plus optional
`params.copy(...)` overrides. It must not use `RuntimeShader`, the raw maps, or
`GlassBackdropState` internals.

### Performance tiers (0.1.4, no quality switch)

| Tier | Usage |
| --- | --- |
| High | 1–3 glass surfaces, static `ImageBitmap` backdrop, `interactive=true`, distinct radii cached |
| Medium | static cards (`interactive=false`), fewer materials/radii, no backdrop refresh per frame |
| Low / reduced transparency | not implemented; phase-2 `enabled=false` tint-only fallback |

Do not refresh the backdrop per frame; the blur cache is per rounded radius. Emulator
performance is diagnostic (deck 6.23–6.65% janky, P95 32–36ms; glass 4.58%, P95 29ms);
V2528A is pending. See `docs/release-0.1.4.md` for the known ceiling and license carriage;
the liquid-glass audit and its remaining edge differences are in
`docs/liquid-glass-0.1.4.md`.

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
