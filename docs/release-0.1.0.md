# MaineCare Effects 0.1.0 — consumable artifact

Release date: 2026-11-01. Source: `main` at the merge of PR #1 (15091ed) plus this release
branch. This is a **local Maven artifact** for the merged test package; the MaineCare App is
not modified and no dependency has been added to it.

## Coordinates

| Coordinate | Packaging | Notes |
| --- | --- | --- |
| `com.mainecare.effects:motion:0.1.0` | AAR | springs, tokens, geometry, telemetry |
| `com.mainecare.effects:glass:0.1.0` | AAR | backdrop, `MaineGlassCard`, AGSL glass, presets |
| `com.mainecare.effects:deck:0.1.0` | AAR | `CardDeck`, `Coverflow`, `CardStack` |

`glass` and `deck` depend on `motion` transitively. Compose is exposed as `api` with the
Compose BOM 2026.03.00; consumers need `android.useAndroidX=true`.

GitHub Packages remote (cross-machine):

```
https://maven.pkg.github.com/BigStartByXuyb/mainecare-effects
com.mainecare.effects:motion:0.1.0
com.mainecare.effects:glass:0.1.0
com.mainecare.effects:deck:0.1.0
```

The remote publish is `.github/workflows/publish.yml` (workflow_dispatch or a published
release) using `GITHUB_TOKEN` with `permissions: packages: write`; it also verifies the remote
POM, AAR, module metadata, sources, license, notice and third-party notices. Consumers need a
token with **Packages: Read-only** (fine-grained) or `read:packages` (classic), stored as the
consumer secret `MAINECARE_EFFECTS_PACKAGES_TOKEN`; never commit a token. A consumer
repository's own `GITHUB_TOKEN` normally cannot read another private repository's package.

Artifacts are also published by `./gradlew publishToMavenLocal` to:

```
~/.m2/repository/com/mainecare/effects/{motion,glass,deck}/0.1.0/
```

Each version directory contains the AAR, POM, Gradle module metadata, sources jar, and the
`-license.txt`, `-notice.txt`, `-third-party-notices.md` classifier artifacts. The sources jar
also carries `META-INF/LICENSE`, `META-INF/NOTICE` and `META-INF/THIRD_PARTY_NOTICES.md`.

Maven Local was verified with a separate Android consumer build and needs no credentials.
GitHub Packages is the cross-machine path; JitPack remains a follow-up. No token or credential
is stored in the repository.

## Consumption

Consumer `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}
```

Consumer module:

```kotlin
dependencies {
    implementation("com.mainecare.effects:glass:0.1.0")
    implementation("com.mainecare.effects:deck:0.1.0")
    implementation(platform("androidx.compose:compose-bom:2026.03.00"))
    implementation("androidx.compose.material3:material3")
}
```

`motion` is transitive; add it explicitly only if page code uses the motion tokens directly.

## MaineGlassCard adapter API

`MaineGlassCard` is the only glass surface the App adapter should call.

```kotlin
GlassBackdrop(
    image = currentBackdropBitmap,          // ImageBitmap snapshot supplied by the adapter
    modifier = Modifier.fillMaxSize(),
) {
    MaineGlassCard(
        modifier = Modifier.fillMaxWidth().height(196.dp),
        material = MaineGlassMaterials.Advice,
        params = MaineGlassMaterials.Advice.liquid, // optional; preset is the source of truth
        interactive = true,
        contentAlignment = Alignment.TopStart,
    ) {
        // page content
    }
}
```

Signature:

```kotlin
@Composable
fun MaineGlassCard(
    modifier: Modifier = Modifier,
    material: GlassMaterial = MaineGlassMaterials.Card,
    params: LiquidGlassParams = material.liquid,
    interactive: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
)
```

Page code must not construct `GlassSurface`, `RuntimeShader`, displacement maps or
`GlassBackdropState` internals.

### Presets

`MaineGlassMaterials.Card`, `Advice`, `MealFront`, `Insight`, `Dock`, `SoftControl`. Each preset
carries the v21 tint, CSS blur/saturation/brightness, border, inner shadow, sheen geometry,
outer shadow and radius.

### LiquidGlassParams

Mirrors liquid-glass-react props:

| Field | Default | Meaning |
| --- | --- | --- |
| `displacementScale` | 22 | refraction strength in CSS px |
| `blurAmount` | .055 | liquid-glass-react fallback blur |
| `saturation` | 150 | percent |
| `aberrationIntensity` | 2.2 | channel separation |
| `elasticity` | .18 | pointer sheen shift |
| `cornerRadius` | 30.dp | shape |
| `mode` | Standard | Standard / Polar / Prominent / Shader |
| `overLight` | false | liquid-glass-react light mode |

Prefer a preset plus `params.copy(cornerRadius = ...)`; do not reconstruct materials in page
code.

### Backdrop

`GlassBackdrop(image = bitmap)` supplies a static `ImageBitmap` snapshot of the page
background to all descendant `MaineGlassCard`s. The App adapter must refresh it when the
background changes. Live capture over scrolling content is phase-2 work.

## Performance tiers

There is no quality switch in 0.1.0; choose usage by tier.

| Tier | Usage | Notes |
| --- | --- | --- |
| High (recommended for v21 cards) | 1–3 glass surfaces, static snapshot backdrop, `interactive=true`, blur cached per material radius | full-resolution blur; cards re-sample on interaction only |
| Medium | static cards (`interactive=false`), fewer distinct materials, avoid changing backdrop on scroll | fewer blur radii and shader updates |
| Low / reduced transparency | not implemented in 0.1.0 | phase-2 `enabled=false` tint-only fallback is reserved in the adapter contract |

The blur result is cached per rounded radius. Do not rebuild the `ImageBitmap` per frame; a
live backdrop would defeat the cache. Emulator performance (deck 6.23–6.65% janky, P95
32–36ms; glass 4.58%, P95 29ms) is diagnostic and predates the final material changes.
V2528A performance is still pending and is the acceptance device.

## Known restoration ceiling

- Canonical smooth v21 feeding surface: static **PASS** (SSIM 0.99536 / MAE 0.428 / 0.429%
  over Δ5); displacement **PASS** (0.99531 / 0.422 / 0.355%).
- Photo stress: SSIM 0.99318 / MAE 0.796 / 0.994% over Δ5 (Δ5 passes, SSIM fails).
- Veined Obsidian: SSIM 0.99147 / MAE 0.792 / 0.410% (SSIM fails).
- Controls: shadow-off 0.99369 / 0.99299; live shadowed scene 0.99327 / 0.99155;
  Chromium-aligned CPU box-Gaussian 0.99324 / 0.99167; blur-off is worse.
- The measured ceiling for the static-backdrop/per-surface-shadow path is ≈0.994 SSIM. The
  residual is the card shadow on the shared CSS stage, the short pill’s border/highlight/alpha
  rasterization, and CSS-vs-Android edge compositing. It is visually localized to the pill
  border: >99.4% of pixels within Δ5, >Δ20 only 0.03–0.04%, not visible at 1:1.

Do not describe the stress scenes as passing. The App integration decision must account for
this ceiling; see `docs/golden-gate.md` for the full control matrix.

## Licensing

- Project code: Apache-2.0 (`LICENSE`).
- Displacement maps and filter math: liquid-glass-react 1.1.1 (MIT).
- See `NOTICE` and `THIRD_PARTY_NOTICES.md`; both are published as classifier artifacts next
  to each AAR and inside the sources jar `META-INF/`.
- Haze, Cloudy, AndroidLiquidGlass and NadeemIqbal/liquid-glass were reviewed for design only;
  no source was copied.
- No test accounts, credentials or personal data are stored in the artifact or repository.

## V2528A acceptance

The demo APK for device acceptance is built outside the repository:

```
output/mainecare-effects-demo-0.1.0-debug.apk
sha256 906b59bed31bcdcce3b23bd868ffed9035de0737f40f52fa949660b1f4f5b3d4
```

Install, cold-start, gfxinfo and meminfo steps are in `docs/device-acceptance-v2528a.md` and
`verification/device-perf.sh`. V2528A is not connected in this workspace; the device run is
blocked on USB access. Emulator numbers are diagnostic only.

## Acceptance sequence

1. Build/install the merged test package and accept the canonical visual result.
2. Review the stress-scene ceiling and choose accept / change render path / V2528A-only.
3. Only then add the dependency to the MaineCare App through the `MaineGlassCard` adapter.
4. Run the V2528A performance and visual gate after integration.

This release does **not** modify the MaineCare App and is not a production sign-off.
