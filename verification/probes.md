# Blur/compositing probes

Optional tools used to separate a tint error from a blur/compositing error. They are not part
of the build.

- `filter-alpha-probe.html` — Chromium-only probe. A 132×36 white div with
  `filter: blur(24px)` over black measures an effective center alpha of ≈0.56 (143/255); a
  340×198 div with `blur(34px)` stays opaque (255). This confirms CSS blur also blurs the warp
  element’s alpha and explains why short surfaces need the alpha/scene model.
- `fit-blur.py` — samples the photo at the v21 warp mapping and compares Chromium’s warp
  against sRGB and linear-light Gaussian candidates. Requires `numpy`, `scipy` and `Pillow`.
  Result: best MAE 0.062 (sRGB) / 0.068 (linear) over sigma 20–44 CSS px; the residual is
  insensitive to sigma and stays as a compositing/alpha difference, not a kernel offset.

The shadow-off comparison in the report uses the gate’s `--es shadow 0` and the Chromium
`?shadow=0` query. It isolates the sibling card shadow on the shared stage.
