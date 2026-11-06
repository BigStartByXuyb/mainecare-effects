import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { PNG } from "pngjs";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const W = 1080;
const H = 2400;

const clamp = (v, a = 0, b = 255) => Math.max(a, Math.min(b, v));
const mix = (a, b, t) => a + (b - a) * t;
const smooth = (a, b, x) => {
  const t = clamp((x - a) / (b - a), 0, 1);
  return t * t * (3 - 2 * t);
};

function make({ light }) {
  const png = new PNG({ width: W, height: H });
  for (let y = 0; y < H; y++) {
    const v = y / H;
    for (let x = 0; x < W; x++) {
      const u = x / W;
      let r, g, b;
      if (light) {
        r = mix(245, 255, v);
        g = mix(241, 250, v);
        b = mix(232, 245, v);
        // Saturated color bands and a large off-center disc.
        const band = Math.floor(u * 6);
        const colors = [
          [232, 68, 92], [246, 143, 42], [248, 214, 62],
          [62, 183, 118], [54, 124, 231], [138, 84, 210],
        ];
        // Saturated vertical bars across the card area.
        const bandTop = smooth(0.13, 0.17, v) * (1 - smooth(0.43, 0.47, v));
        const bar = smooth(0.01, 0.04, u) * (1 - smooth(0.96, 0.99, u));
        if (bandTop > 0 && bar > 0) {
          const c = colors[band % colors.length];
          const t = bar * bandTop;
          r = mix(r, c[0], t);
          g = mix(g, c[1], t);
          b = mix(b, c[2], t);
        }
        // A high-contrast checker strip below the card.
        if (v > 0.50 && v < 0.62) {
          const cell = (Math.floor(u * 18) + Math.floor(v * 60)) % 2;
          if (cell) {
            r = mix(r, 20, 0.9);
            g = mix(g, 20, 0.9);
            b = mix(b, 20, 0.9);
          }
        }
        const disc = Math.hypot((u - 0.72) / 0.22, (v - 0.2) / 0.13);
        if (disc < 1) {
          const t = (1 - smooth(0.7, 1, disc)) * 0.85;
          r = mix(r, 30, t);
          g = mix(g, 100, t);
          b = mix(b, 235, t);
        }
      } else {
        r = mix(5, 18, v);
        g = mix(8, 26, v);
        b = mix(18, 46, v);
        const blobs = [
          [0.18, 0.18, 0.16, [236, 38, 120]],
          [0.82, 0.25, 0.20, [26, 214, 196]],
          [0.68, 0.62, 0.24, [250, 142, 36]],
          [0.25, 0.82, 0.18, [92, 196, 48]],
          [0.88, 0.88, 0.14, [88, 96, 255]],
        ];
        for (const [cx, cy, rad, c] of blobs) {
          const d = Math.hypot((u - cx) / rad, (v - cy) / rad);
          if (d < 1) {
            const t = (1 - smooth(0.55, 1, d)) * 0.9;
            r = mix(r, c[0], t);
            g = mix(g, c[1], t);
            b = mix(b, c[2], t);
          }
        }
        // Thin bright grid and diagonal stripes.
        const grid = (Math.abs((u * 52) % 1 - 0.5) < 0.035 || Math.abs((v * 110) % 1 - 0.5) < 0.02) ? 0.35 : 0;
        if (grid) {
          r = mix(r, 255, grid);
          g = mix(g, 255, grid);
          b = mix(b, 255, grid);
        }
        const stripe = Math.abs((((u * 8 + v * 3) % 1) + 1) % 1 - 0.5) < 0.03 ? 0.28 : 0;
        if (stripe) {
          r = mix(r, 255, stripe);
          g = mix(g, 255, stripe);
          b = mix(b, 255, stripe);
        }
      }
      const idx = (y * W + x) * 4;
      png.data[idx] = clamp(Math.round(r));
      png.data[idx + 1] = clamp(Math.round(g));
      png.data[idx + 2] = clamp(Math.round(b));
      png.data[idx + 3] = 255;
    }
  }
  return png;
}

const outputs = [
  ["verification/upstream-demo/ab-dark.png", false],
  ["verification/upstream-demo/ab-light.png", true],
  ["demo/src/main/assets/ab-dark.png", false],
  ["demo/src/main/assets/ab-light.png", true],
];
for (const [file, light] of outputs) {
  const out = path.join(root, file);
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, PNG.sync.write(make({ light }), { colorType: 6 }));
  console.log(out);
}
