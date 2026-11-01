import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { PNG } from "pngjs";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const W = 1080;
const H = 2400;
const png = new PNG({ width: W, height: H });

const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
const mix = (a, b, t) => a + (b - a) * t;
const smooth = (a, b, x) => {
  const t = clamp((x - a) / (b - a), 0, 1);
  return t * t * (3 - 2 * t);
};
const glow = (x, y, cx, cy, radius, color, strength) => {
  const d = Math.hypot(x - cx, y - cy) / radius;
  const f = (1 - smooth(0, 1, d)) * strength;
  return [color[0] * f, color[1] * f, color[2] * f];
};

// Deterministic value noise for grain.
let seed = 0x9e3779b9;
const rand = () => {
  seed ^= seed << 13;
  seed ^= seed >>> 17;
  seed ^= seed << 5;
  return ((seed >>> 0) / 0xffffffff) * 2 - 1;
};

const ridge = (x, base, amp, freq, phase) =>
  base -
  amp * 0.55 * Math.sin(x * freq + phase) -
  amp * 0.30 * Math.sin(x * freq * 2.17 + phase * 1.7) -
  amp * 0.15 * Math.sin(x * freq * 4.31 + phase * 0.4);

for (let y = 0; y < H; y++) {
  const v = y / H;
  for (let x = 0; x < W; x++) {
    const u = x / W;

    // Night sky: deep blue-black with a slight teal horizon.
    let r = mix(7, 24, smooth(0.0, 0.62, v));
    let g = mix(10, 34, smooth(0.0, 0.62, v));
    let b = mix(20, 48, smooth(0.0, 0.62, v));

    // Warm champagne glow, upper right.
    const warm = glow(u, v, 0.84, 0.16, 0.62, [227, 192, 138], 0.85);
    r += warm[0]; g += warm[1]; b += warm[2];
    // Cool cyan glow, lower left.
    const cool = glow(u, v, 0.12, 0.58, 0.70, [84, 132, 154], 0.38);
    r += cool[0]; g += cool[1]; b += cool[2];

    // Moon disc and halo.
    const moonD = Math.hypot(u - 0.735, v - 0.135);
    const moon = (1 - smooth(0.012, 0.020, moonD)) * 0.95;
    const halo = (1 - smooth(0.02, 0.13, moonD)) * 0.18;
    r = mix(r, 246, moon); g = mix(g, 242, moon); b = mix(b, 228, moon);
    r += halo * 80; g += halo * 76; b += halo * 66;

    // Three mountain layers.
    const layers = [
      { horizon: ridge(u, 0.50, 0.055, 5.4, 0.8), color: [28, 38, 50], alpha: 0.85 },
      { horizon: ridge(u, 0.565, 0.080, 7.2, 2.1), color: [17, 24, 34], alpha: 0.94 },
      { horizon: ridge(u, 0.635, 0.095, 4.1, 4.4), color: [8, 12, 18], alpha: 0.97 },
    ];
    for (const layer of layers) {
      if (v > layer.horizon) {
        const depth = smooth(layer.horizon, layer.horizon + 0.02, v);
        r = mix(r, layer.color[0], layer.alpha * depth);
        g = mix(g, layer.color[1], layer.alpha * depth);
        b = mix(b, layer.color[2], layer.alpha * depth);
      }
    }

    // Water below the last ridge: reflection plus horizontal ripples.
    if (v > 0.70) {
      const water = smooth(0.70, 0.73, v);
      const reflectWarm = glow(u, v, 0.84, 0.70, 0.34, [196, 156, 102], 0.50);
      const reflectCool = glow(u, v, 0.20, 0.86, 0.44, [66, 104, 122], 0.35);
      const ripple =
        Math.sin(v * 120 + u * 18) * 0.5 +
        Math.sin(v * 47 - u * 31) * 0.5;
      const base = [6 + reflectWarm[0] + reflectCool[0] + ripple * 2.2,
                    10 + reflectWarm[1] + reflectCool[1] + ripple * 2.4,
                    16 + reflectWarm[2] + reflectCool[2] + ripple * 3.0];
      r = mix(r, base[0], water);
      g = mix(g, base[1], water);
      b = mix(b, base[2], water);
    }

    // Vignette.
    const dx = (u - 0.5) * 2;
    const dy = (v - 0.5) * 2;
    const vignette = 1 - 0.28 * smooth(0.35, 1.45, Math.hypot(dx, dy));
    r *= vignette; g *= vignette; b *= vignette;

    // Fine grain, stronger in shadows.
    const grain = rand() * 4.5 * (1 - 0.35 * (r + g + b) / 765);
    const idx = (y * W + x) * 4;
    png.data[idx] = clamp(Math.round(r + grain), 0, 255);
    png.data[idx + 1] = clamp(Math.round(g + grain), 0, 255);
    png.data[idx + 2] = clamp(Math.round(b + grain), 0, 255);
    png.data[idx + 3] = 255;
  }
}

const outputs = [
  "demo/src/main/assets/backdrop.png",
  "verification/web-reference/backdrop.png",
].map((out) => path.join(root, out));
for (const out of outputs) {
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, PNG.sync.write(png, { colorType: 6 }));
}
console.log(`wrote ${outputs.join(", ")} (${W}x${H})`);

// A quieter Obsidian Veil backdrop for the deck lab (540x1200, same 9:20 ratio).
{
  const W2 = 540;
  const H2 = 1200;
  const png2 = new PNG({ width: W2, height: H2 });
  for (let y = 0; y < H2; y++) {
    const v = y / H2;
    for (let x = 0; x < W2; x++) {
      const u = x / W2;
      let r = mix(5, 15, smooth(0, 1, v));
      let g = mix(7, 19, smooth(0, 1, v));
      let b = mix(10, 26, smooth(0, 1, v));
      const warm = glow(u, v, 0.9, 0.08, 0.5, [180, 148, 100], 0.30);
      const cool = glow(u, v, 0.1, 0.95, 0.6, [72, 100, 120], 0.20);
      r += warm[0] + cool[0];
      g += warm[1] + cool[1];
      b += warm[2] + cool[2];
      const vein = Math.abs(Math.sin((u * 3.1 + v * 1.35) * 6.0));
      const veinLine = (1 - smooth(0.0, 0.05, vein)) * 6.0;
      r += veinLine; g += veinLine; b += veinLine * 1.1;
      const grain = rand() * 3.5;
      const idx = (y * W2 + x) * 4;
      png2.data[idx] = clamp(Math.round(r + grain), 0, 255);
      png2.data[idx + 1] = clamp(Math.round(g + grain), 0, 255);
      png2.data[idx + 2] = clamp(Math.round(b + grain), 0, 255);
      png2.data[idx + 3] = 255;
    }
  }
  const darkOut = path.join(root, "demo/src/main/assets/backdrop-obsidian.png");
  fs.writeFileSync(darkOut, PNG.sync.write(png2, { colorType: 6 }));
  console.log(`wrote ${darkOut} (${W2}x${H2})`);
}
