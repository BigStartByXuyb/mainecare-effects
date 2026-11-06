"""Generate the bright liquid-glass showcase backdrop.

Deterministic 1080x2400 gradient-mesh wallpaper: vivid coral/amber/teal/violet fields over a
cream base, soft bokeh, contour lines and grain. The card sits over the upper half, where the
contour lines and bokeh give the AGSL displacement and aberration visible high-frequency
detail. Requires Pillow + numpy.
"""

from __future__ import annotations

import pathlib
import sys

import numpy as np
from PIL import Image

W, H = 1080, 2400
OUT = pathlib.Path(__file__).resolve().parent.parent / "demo/src/main/assets/showcase.png"


def smoothstep(a: float, b: float, x: np.ndarray | float):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def main() -> None:
    y, x = np.mgrid[0:H, 0:W].astype(np.float64)
    u = x / (W - 1)
    v = y / (H - 1)

    # Gradient mesh: normalized gaussian color fields keep the hues vivid instead of muddy.
    fields = [
        (0.08, 0.16, 0.34, 0.30, (255, 82, 96), 1.15),
        (0.88, 0.10, 0.32, 0.26, (255, 178, 36), 1.05),
        (0.92, 0.42, 0.34, 0.30, (28, 198, 186), 1.00),
        (0.48, 0.02, 0.34, 0.26, (92, 142, 255), 0.85),
        (0.16, 0.52, 0.36, 0.32, (142, 96, 255), 0.90),
        (0.74, 0.70, 0.38, 0.34, (255, 110, 176), 0.90),
        (0.44, 0.36, 0.60, 0.52, (255, 248, 232), 0.75),
    ]
    acc = np.zeros((H, W, 3), dtype=np.float64)
    wsum = np.zeros((H, W), dtype=np.float64)
    for cx, cy, rx, ry, color, weight in fields:
        w = np.exp(-(((u - cx) / rx) ** 2 + ((v - cy) / ry) ** 2) * 1.7) * weight
        acc += w[..., None] * np.array(color, dtype=np.float64)
        wsum += w
    img = acc / wsum[..., None]
    # Lift toward cream so the wallpaper stays bright enough for the light glass treatment.
    img = img * 0.72 + np.array([255.0, 249.0, 238.0]) * 0.28
    lum = img @ np.array([0.2126, 0.7152, 0.0722])
    img = lum[..., None] + (img - lum[..., None]) * 1.12
    img = np.clip(img, 0, 255)

    # Broad diagonal ribbons give the glass a visible color boundary to refract.
    theta = -0.62
    ribbon = u * np.cos(theta) + v * np.sin(theta)
    for center, width, color, alpha in (
        (0.24, 0.055, (255, 78, 104), 0.42),
        (0.52, 0.075, (28, 190, 180), 0.30),
        (0.78, 0.050, (96, 128, 255), 0.30),
    ):
        m = np.exp(-(((ribbon - center) / width) ** 2))
        blend = (m * alpha)[..., None]
        img = img * (1.0 - blend) + np.array(color, dtype=np.float64) * blend

    # A warm sun disc with a defined rim; its curve is easy to see through the glass edge.
    d = np.hypot((u - 0.79) * (W / H), v - 0.15) / 0.105
    disc = 1.0 - smoothstep(0.86, 1.0, d)
    img = img * (1.0 - (disc * 0.55)[..., None]) + np.array([255, 226, 150.0]) * (disc * 0.55)[..., None]
    rim = np.exp(-((d - 0.96) / 0.035) ** 2)
    img += (rim * 0.25)[..., None] * np.array([255.0, 255.0, 255.0])

    # Fine contour lines across the upper half: high-frequency detail for refraction.
    warp = 0.010 * np.sin(u * 5.2) + 0.006 * np.sin(u * 13.0 + v * 4.0)
    lines = 0.5 + 0.5 * np.cos((v + warp) / 0.016 * 2.0 * np.pi)
    lines = lines**22
    reach = 1.0 - smoothstep(0.34, 0.55, v)
    img += (lines * reach)[..., None] * np.array([14.0, 11.0, 9.0])

    # Soft bokeh discs, mostly white with a few tinted ones. Screen blend brightens.
    bokeh = [
        (0.16, 0.10, 0.055, (255, 255, 255), 0.55),
        (0.34, 0.055, 0.035, (255, 255, 255), 0.45),
        (0.62, 0.09, 0.045, (255, 250, 240), 0.50),
        (0.83, 0.22, 0.070, (255, 214, 150), 0.45),
        (0.94, 0.36, 0.050, (255, 255, 255), 0.40),
        (0.08, 0.34, 0.065, (255, 168, 186), 0.40),
        (0.22, 0.24, 0.028, (255, 255, 255), 0.50),
        (0.70, 0.31, 0.024, (255, 255, 255), 0.40),
        (0.50, 0.42, 0.050, (255, 240, 220), 0.32),
        (0.87, 0.55, 0.075, (120, 255, 240), 0.30),
        (0.13, 0.62, 0.060, (180, 160, 255), 0.30),
    ]
    for cx, cy, r, color, strength in bokeh:
        d = np.hypot((u - cx) * (W / H), v - cy) / r
        disc = (1.0 - smoothstep(0.55, 1.0, d)) * strength
        c = np.array(color, dtype=np.float64)
        screen = 255.0 - (255.0 - img) * (255.0 - c) / 255.0
        img = img * (1.0 - disc[..., None]) + screen * disc[..., None]

    # Fine confetti bokeh across the showcase area: small color patches survive the blur and
    # make the edge displacement visible without turning the wallpaper into a test chart.
    rng = np.random.default_rng(4117)
    palette = [
        (255, 255, 255), (255, 86, 110), (255, 190, 60), (40, 200, 185),
        (130, 100, 255), (90, 140, 255), (255, 130, 180),
    ]
    for i in range(170):
        cx = rng.uniform(0.02, 0.98)
        cy = rng.uniform(0.02, 0.56)
        r = rng.uniform(0.006, 0.020)
        color = np.array(palette[i % len(palette)], dtype=np.float64)
        d = np.hypot((u - cx) * (W / H), v - cy) / r
        disc = (1.0 - smoothstep(0.35, 1.0, d)) * rng.uniform(0.22, 0.50)
        img = img * (1.0 - disc[..., None]) + color * disc[..., None]

    # Soft diagonal light bands.
    bands = 0.5 + 0.5 * np.sin((x * 0.62 + y * 0.20) * 0.0075 + np.sin(y * 0.0012) * 1.6)
    bands = bands**4
    img += (bands[..., None] - 0.25) * np.array([9.0, 7.0, 5.0])

    # Vignette-free edge lift and very slight grain.
    img += 4.0
    rng = np.random.default_rng(20261106)
    img += rng.normal(0.0, 1.4, (H, W, 1))

    out = np.clip(img, 0, 255).astype(np.uint8)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(out, "RGB").save(OUT, optimize=True)
    print(f"{OUT} {W}x{H}")


if __name__ == "__main__":
    sys.exit(main())
