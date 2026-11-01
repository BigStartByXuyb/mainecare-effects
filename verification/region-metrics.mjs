import fs from "node:fs";
import { PNG } from "pngjs";

const [leftPath, rightPath, ...regionArgs] = process.argv.slice(2);
if (!leftPath || !rightPath) {
  console.error("usage: node region-metrics.mjs left.png right.png [name=x,y,w,h ...]");
  process.exit(1);
}

const left = PNG.sync.read(fs.readFileSync(leftPath));
const right = PNG.sync.read(fs.readFileSync(rightPath));
if (left.width !== right.width || left.height !== right.height) {
  throw new Error(`size mismatch ${left.width}x${left.height} vs ${right.width}x${right.height}`);
}

const regions = regionArgs.length
  ? regionArgs.map((spec) => {
      const [name, rect] = spec.split("=");
      const [x, y, w, h] = rect.split(",").map(Number);
      return { name, x, y, w, h };
    })
  : [
      { name: "card", x: 63, y: 315, w: 898, h: 525 },
      { name: "pill", x: 63, y: 919, w: 347, h: 95 },
    ];

const luma = (data, i) =>
  0.2126 * data[i] + 0.7152 * data[i + 1] + 0.0722 * data[i + 2];

function stats(x0, y0, w, h) {
  let sumAbs = 0;
  let sumSq = 0;
  let over5 = 0;
  let pixels = 0;
  const lumaLeft = [];
  const lumaRight = [];
  for (let y = y0; y < y0 + h; y++) {
    for (let x = x0; x < x0 + w; x++) {
      const i = (y * left.width + x) * 4;
      const dr = Math.abs(left.data[i] - right.data[i]);
      const dg = Math.abs(left.data[i + 1] - right.data[i + 1]);
      const db = Math.abs(left.data[i + 2] - right.data[i + 2]);
      sumAbs += dr + dg + db;
      sumSq += dr * dr + dg * dg + db * db;
      if (Math.max(dr, dg, db) > 5) over5++;
      lumaLeft.push(luma(left.data, i));
      lumaRight.push(luma(right.data, i));
      pixels++;
    }
  }
  const mse = sumSq / (pixels * 3);
  let mx = 0;
  let my = 0;
  for (let i = 0; i < pixels; i++) {
    mx += lumaLeft[i];
    my += lumaRight[i];
  }
  mx /= pixels;
  my /= pixels;
  let vx = 0;
  let vy = 0;
  let cxy = 0;
  for (let i = 0; i < pixels; i++) {
    const a = lumaLeft[i] - mx;
    const b = lumaRight[i] - my;
    vx += a * a;
    vy += b * b;
    cxy += a * b;
  }
  vx /= pixels;
  vy /= pixels;
  cxy /= pixels;
  const c1 = (0.01 * 255) ** 2;
  const c2 = (0.03 * 255) ** 2;
  const ssim =
    ((2 * mx * my + c1) * (2 * cxy + c2)) /
    ((mx * mx + my * my + c1) * (vx + vy + c2));
  return {
    mae: sumAbs / (pixels * 3),
    rmse: Math.sqrt(mse),
    psnr: mse === 0 ? 99 : 10 * Math.log10((255 * 255) / mse),
    ssim,
    over5Percent: (over5 / pixels) * 100,
  };
}

const out = {};
for (const { name, x, y, w, h } of regions) {
  out[name] = stats(x, y, w, h);
}
console.log(JSON.stringify(out, null, 2));
