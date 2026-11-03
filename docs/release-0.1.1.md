# MaineCare Effects 0.1.1 — upstream preset and A/B

Release date: 2026-11-03. Source branch: `release-0.1.1-upstream`. This release responds to the
real-device feedback that the demo glass did not look like liquid-glass-react. It adds the
actual upstream defaults and a visual A/B; the MaineCare App is still not modified.

## Coordinates

| Coordinate | Packaging |
| --- | --- |
| `com.mainecare.effects:motion:0.1.1` | AAR |
| `com.mainecare.effects:glass:0.1.1` | AAR |
| `com.mainecare.effects:deck:0.1.1` | AAR |

Maven Local: `./gradlew publishToMavenLocal` →
`~/.m2/repository/com/mainecare/effects/{motion,glass,deck}/0.1.1/`.

GitHub Packages:
`https://maven.pkg.github.com/BigStartByXuyb/mainecare-effects`
(`.github/workflows/publish.yml`, `permissions: packages: write`, verifies POM/AAR/module/
sources/license/notice/third-party-notices remotely). The 0.1.1 workflow run is recorded in
the PR; the 0.1.0 run confirmed the workflow path.

Consumer auth is unchanged: a token with **Packages: Read-only** (fine-grained) or
`read:packages` (classic), stored as `MAINECARE_EFFECTS_PACKAGES_TOKEN` in the consumer.
No token is committed.

## What changed

- **Upstream source defaults verified and applied**: displacementScale 70, blurAmount .0625
  (6px backdrop blur), saturation 140, aberrationIntensity 2, elasticity .15, cornerRadius 999,
  standard, overLight false. The previous library defaults were v21-dark values.
- **`MaineGlassMaterials.Upstream`**: clear glass, 6px blur, saturation 140, upstream outer
  shadow, upstream rim, no tint/sheen/inner shadow. All v21 presets are retained unchanged.
- **Upstream edge style**: 1.5px screen/overlay gradient rim + 0.5px white inset stroke + top
  highlight. v21 presets keep their border/rim.
- **Opaque backdrop-filter source** for upstream; the v21 blur-alpha model darkened upstream
  edges incorrectly.
- **A/B screen**: `--es screen ab` (left upstream, right v21), `ab-upstream`, `ab-v21`;
  `--es bg dark|light|color|flat|line`; `dispScale`/`blurScale` diagnostics.
- **Real upstream reference harness**: React 19 + `liquid-glass-react@1.1.1`, 390×844 and
  430×932, dark/light, source defaults. See `docs/upstream-ab-0.1.1.md`.

## A/B result

Same card geometry (342×200 at 24,120; radius 30 override for geometry only):

| Viewport / background | Card MAE | Card SSIM | Whole-frame MAE / SSIM |
| --- | --- | --- | --- |
| 390 dark | 16.52 | 0.852 | 5.55 / 0.899 |
| 390 light | 9.49 | 0.944 | 2.72 / 0.977 |
| 430 dark | 11.63 | 0.957 | 4.01 / 0.925 |
| 430 light | 9.24 | 0.959 | 2.26 / 0.991 |

Light is visually close. Dark high-frequency detail still shows stronger upstream edge
refraction/specular than our Compose/AGSL pass. This is a raster/compositing difference (CSS
backdrop-filter + SVG filter + DOM edge layers vs one AGSL pass), not a parameter value; no
upstream parameter was altered to make the comparison look better. Full details are in
`docs/upstream-ab-0.1.1.md`; evidence is in `docs/evidence/upstream-ab-*` and
`docs/evidence/preset-ab-*`.

The v21 canonical gate remains PASS after this release:
static SSIM 0.99548 / MAE 0.421 / 0.414% over Δ5; displacement 0.99542 / 0.416 / 0.346%.

## Adapter / performance / ceiling

`MaineGlassCard` remains the only glass seam. Presets: `Card`, `Advice`, `MealFront`,
`Insight`, `Dock`, `SoftControl`, plus `Upstream`. Performance tiers and the ≈0.994
static-backdrop ceiling are unchanged; V2528A is still pending. Emulator numbers are
diagnostic only.

## Licensing

Project Apache-2.0; liquid-glass-react 1.1.1 MIT. `LICENSE`, `NOTICE` and
`THIRD_PARTY_NOTICES.md` are published as classifier artifacts and in the sources jar
`META-INF/`. No secrets or personal data.
