# Source and license audit — phase 1

Date: 2026-11-01. Scope: the reference implementations named in the phase-1 brief, plus the
MaineCare v21 design source. This audit decides what may be reused, what must be rewritten,
and what attribution ships with the library.

## Summary

| Source | Version / commit | License | What we reuse | What we do not reuse |
| --- | --- | --- | --- | --- |
| `liquid-glass-react` | 1.1.1 (npm) | MIT, Copyright 2025 Max Rovensky | Displacement-map assets (`standard`, `polar`, `prominent`), the SVG filter chain and parameters, prop names/defaults, the procedural rounded-rect SDF formula | React/DOM component code, React state/pointer handling, the browser canvas map generator |
| `chrisbanes/haze` | shallow clone of `main`, 2026-11-01 | Apache-2.0 | Design review only: source-backed backdrop capture vs `RenderEffect` blur, dirty-area and fallback strategies | No source code copied. Haze 2 uses platform backdrop APIs (including reflective access on some versions); this library does not. |
| `skydoves/Cloudy` | shallow clone of `main`, 2026-11-01 | Apache-2.0 | Design review only: KMP blur/glass/custom-shader split, quality tiers, CPU fallback | No source code copied. Cloudy's surface/shader architecture is broader than this phase-1 spike. |
| `Kyant0/AndroidLiquidGlass` | shallow clone of `main`, 2026-11-01 | Apache-2.0 | Design review only: SDF texture sampled by a `RuntimeShader`, `BitmapShader` + `setInputBuffer` coordinate convention | No source code copied. Its normal/refraction model and effect scopes are not used. |
| `NadeemIqbal/liquid-glass` | shallow clone of `main`, 2026-11-01 | Apache-2.0 | Design review only: `Modifier.liquidGlass()` surface, chroma/edge-sheen decomposition, quality tiers | No source code copied. It is a KMP modifier API with a different material model. |
| MaineCare feeding v21 | local design source | Internal project asset | CSS values, layer map, golden screenshots as the restoration target | The pet avatar and any user data are not copied into this repository. |

No GPL, AGPL, or unknown-license code is used. No test account, credential, token, or personal
data is present in this repository.

## liquid-glass-react 1.1.1 (MIT)

The package is a React/DOM component. Its visual result is produced by:

1. A displacement map image (`standard`, `polar`, or `prominent`; `shader` mode generates one
   from a rounded-rect SDF in a canvas).
2. An SVG `feImage` → three `feDisplacementMap` passes (R/G/B channel recombination) →
   `feBlend` (screen) → small `feGaussianBlur` → edge-mask composite.
3. CSS `backdrop-filter: blur((overLight ? 12 : 4) + blurAmount * 32)px saturate(saturation%)`.
4. Tint, gradients, border and shadows from the consuming CSS.

We reuse items 1 and 2 as data and algorithm, reimplemented in Kotlin/AGSL:
`glass/src/main/res/raw/liquid_glass_displacement_*.{jpg,png}` are byte-identical copies of the
package's map assets; `LiquidGlassShader.kt` implements the channel displacement, aberration
scales, edge mask and map lookup. The MIT notice is reproduced in
`third_party/licenses/liquid-glass-react-MIT.txt` and `THIRD_PARTY_NOTICES.md`.

We do not copy the React component or its pointer/elasticity code. The public Kotlin parameter
names (`displacementScale`, `blurAmount`, `saturation`, `aberrationIntensity`, `elasticity`,
`cornerRadius`, `mode`, `overLight`) deliberately mirror the React props so a parameter-by-
parameter check is possible.

Known browser caveat from the upstream README: Safari and Firefox only partially support the
effect. The web golden reference is therefore rendered with Chromium and the same SVG filter
chain, on an aligned copy of the backdrop (see `verification/web-reference/gate.html`).

## Haze (Apache-2.0)

Haze is the closest production-grade Android reference for backdrop sampling. The current
`main` branch includes a `setBackdropRenderEffect` path (reflective access to a platform API)
and a source/backdrop renderer with explicit dirty-area handling. Phase 1 does **not** copy or
call any of it. Reasons:

- The spike needs deterministic, reproducible gate images; a static snapshot is easier to
  compare pixel by pixel.
- Hidden/reflective platform APIs are a release risk for the app.
- The v21 card set sits on a photo or an obsidian gradient, not on live scrolling content.

The live backdrop path remains the main follow-up for the MaineCare app (dock over scrolling
content). The shader here can consume a refreshed bitmap without changing its public surface.

## Cloudy (Apache-2.0)

Cloudy is a Kotlin Multiplatform surface-effects library with blur, liquid glass and custom
shader APIs plus CPU fallbacks. No code is copied. Its quality-tier idea informs the phase-2
plan (blur on/off, low-end fallback), not the phase-1 implementation.

## Kyant0/AndroidLiquidGlass (Apache-2.0)

The repository demonstrates an SDF texture passed into a `RuntimeShader` with
`setInputBuffer("sdfTex", BitmapShader(...))`, sampled at `coord / size * textureSize`. That
confirmed the Android `BitmapShader` coordinate convention used by this library. No shader or
Kotlin code is copied; our shader is derived from liquid-glass-react's map/filter math.

## NadeemIqbal/liquid-glass (Apache-2.0)

A Compose Multiplatform `Modifier.liquidGlass()` with backdrop blur, chroma lift, edge sheen
and quality tiers. No code is copied. The API shape (modifier plus surface presets) is noted
as an alternative consumer-facing design for phase 2.

## MaineCare v21 design source

Values are transcribed from:

- `docs/reference/design/feeding/v21/v21/feeding-v21-obsidian.css`
- `docs/reference/design/feeding/v21/v6/feeding-v6-shell.css`
- `docs/reference/design/feeding/v21/v12/feeding-v12-cards.css`
- `docs/reference/design/feeding/v21/evidence/screenshots/*`

Only numeric/color/material values and the layer map are used. The pet avatar and any account
or personal data are not copied. The demo backdrop is generated by
`verification/generate-backdrop.mjs` and is deterministic and synthetic.

## Attribution obligations

- Ship the MIT notice and copyright for liquid-glass-react (done).
- Ship Apache-2.0 license texts for the reviewed Apache projects if any of their code is ever
  copied. Phase 1 copies none; the texts are staged under `third_party/licenses/` for the
  review record.
- Keep the project's own `LICENSE` (Apache-2.0) and `THIRD_PARTY_NOTICES.md` in every
  distributed artifact (AAR/source bundle).
