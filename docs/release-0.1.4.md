# MaineCare Effects 0.1.4 — crystal glass

Release date: 2026-11-06. Source branch: `release-0.1.4`. Supersedes 0.1.3, which used the
wrong overLight treatment on the bright showcase.

## Coordinates

| Coordinate | Packaging |
| --- | --- |
| `com.mainecare.effects:motion:0.1.4` | AAR |
| `com.mainecare.effects:glass:0.1.4` | AAR |
| `com.mainecare.effects:deck:0.1.4` | AAR |

Maven Local: `./gradlew publishToMavenLocal` →
`~/.m2/repository/com/mainecare/effects/{motion,glass,deck}/0.1.4/`.

GitHub Packages:
`https://maven.pkg.github.com/BigStartByXuyb/mainecare-effects`
(workflow on `release-0.1.4`, verifies POM/AAR/module/sources/license/notice/third-party
notices remotely).

## What changed from 0.1.3

- **The default showcase uses the source-default `overLight=false`.** Full `displacementScale`
  70, 6px blur, clear glass. The bright wallpaper no longer gets the overLight treatment,
  which halved displacement to 35 and raised blur to 14px — the milky “transparency only”
  result reported by the user.
- **`screen=liquid-photo`** adds a real CC0 beach photo. The glass sits over clouds and
  foliage, so edge distortion, chromatic fringing and the moving highlight are visible on a
  photograph, not only on a synthetic wallpaper.
- **A/B harness `bg=showcase|photo`** and `--es overlight 0|1`; the upstream reference and the
  Android card use the same background, card geometry and overLight setting.
- **Refraction audit** confirms the Standard mode runs the AGSL displacement map with the
  full 70 scale, R/B channel sampling, and no blur-only/RenderEffect fallback. See
  `docs/liquid-glass-0.1.4.md`.
- v21 Obsidian remains the second tab; the deck remains the third. The v21 material and
  canonical gate are unchanged.

## Measured result

Against the real `liquid-glass-react@1.1.1` over the same background at `overLight=false`:

| Background | Card MAE | Card SSIM | Inner MAE | Inner SSIM |
| --- | --- | --- | --- | --- |
| Bright showcase | 7.63 | 0.888 | 7.37 | 0.894 |
| CC0 beach photo | 7.32 | 0.900 | 6.93 | 0.907 |

The 0.1.3 overLight comparison had lower MAE (3.51 / 0.951) because it matched the milky
overLight reference. 0.1.4 deliberately selects the crystal source-default look; the on/off
line probe (card MAE 18.35) and the side-by-side recordings show displacement, aberration and
highlight are active. The v21 canonical gate remains **PASS**: static 0.99548 / 0.421 /
0.414% over Δ5; displacement 0.99542 / 0.416 / 0.346%.

## Demo APK

`output/mainecare-effects-demo-0.1.4-debug.apk`, versionName `0.1.4-debug`, versionCode 4,
sha256 `0699683e9a87d5e7665467485661a57d5ab4519093dc5111965c314fa980bdac`.

Verified on emulator-5554 at 975×2110 / 400 dpi: default launch, source-default showcase,
photo showcase, A/B showcase and photo, v21 tab, displacement on/off line probe, and screen
recordings. V2528A is **not connected**, so real-device performance acceptance remains
blocked; steps are in `docs/device-acceptance-v2528a.md`.

## Licensing

Project Apache-2.0; liquid-glass-react 1.1.1 MIT. The beach photo is “Crystal Beach 2” by
Syced, Wikimedia Commons, CC0 1.0; the notice is in `THIRD_PARTY_NOTICES.md` and
`third_party/licenses/photo-beach-CC0.txt`. Every AAR carries the project license/notice and
third-party notices, and the sources jar carries them under `META-INF/`.
