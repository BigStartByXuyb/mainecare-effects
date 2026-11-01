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

for (let y = 0; y < H; y++) {
  const v = y / H;
  for (let x = 0; x < W; x++) {
    const u = x / W;
    let r = 7 + 10 * smooth(0, 1, v);
    let g = 10 + 14 * smooth(0, 1, v);
    let b = 15 + 22 * smooth(0, 1, v);

    const warmD = Math.hypot((u - 0.9) / 0.6, (v - 0.1) / 0.6);
    const warm = (1 - smooth(0, 1, warmD)) * 0.32;
    r += 178 * warm;
    g += 150 * warm;
    b += 110 * warm;

    const coolD = Math.hypot((u - 0.1) / 0.7, (v - 0.9) / 0.7);
    const cool = (1 - smooth(0, 1, coolD)) * 0.22;
    r += 70 * cool;
    g += 100 * cool;
    b += 120 * cool;

    const dx = (u - 0.5) * 2;
    const dy = (v - 0.5) * 2;
    const vignette = 1 - 0.24 * smooth(0.35, 1.45, Math.hypot(dx, dy));
    r *= vignette;
    g *= vignette;
    b *= vignette;

    const idx = (y * W + x) * 4;
    png.data[idx] = clamp(Math.round(r), 0, 255);
    png.data[idx + 1] = clamp(Math.round(g), 0, 255);
    png.data[idx + 2] = clamp(Math.round(b), 0, 255);
    png.data[idx + 3] = 255;
  }
}

const outputs = [
  "demo/src/main/assets/backdrop-gate.png",
  "verification/web-reference/backdrop-gate.png",
].map((out) => path.join(root, out));
for (const out of outputs) {
  fs.mkdirSync(path.dirname(out), { recursive: true });
  fs.writeFileSync(out, PNG.sync.write(png, { colorType: 6 }));
}
console.log(`wrote ${outputs.join(", ")} (${W}x${H})`);
