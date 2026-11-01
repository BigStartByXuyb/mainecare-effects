import fs from "node:fs";
import path from "node:path";
import { PNG } from "pngjs";

const [leftPath, rightPath, outDir = "verification/results"] = process.argv.slice(2);
if (!leftPath || !rightPath) {
  console.error("usage: node compare-png.mjs left.png right.png [outDir]");
  process.exit(1);
}

const left = PNG.sync.read(fs.readFileSync(leftPath));
const right = PNG.sync.read(fs.readFileSync(rightPath));
if (left.width !== right.width || left.height !== right.height) {
  throw new Error(`size mismatch ${left.width}x${left.height} vs ${right.width}x${right.height}`);
}

const width = left.width;
const height = left.height;
const diff = new PNG({ width, height });
let sumAbs = 0;
let sumSq = 0;
let maxDiff = 0;
let over2 = 0;
let over5 = 0;
let over10 = 0;
let over20 = 0;
const lumaLeft = new Float64Array(width * height);
const lumaRight = new Float64Array(width * height);

for (let y = 0; y < height; y++) {
  for (let x = 0; x < width; x++) {
    const index = (y * width + x) * 4;
    const dr = Math.abs(left.data[index] - right.data[index]);
    const dg = Math.abs(left.data[index + 1] - right.data[index + 1]);
    const db = Math.abs(left.data[index + 2] - right.data[index + 2]);
    const pixelMax = Math.max(dr, dg, db);
    sumAbs += dr + dg + db;
    sumSq += dr * dr + dg * dg + db * db;
    maxDiff = Math.max(maxDiff, pixelMax);
    if (pixelMax > 2) over2++;
    if (pixelMax > 5) over5++;
    if (pixelMax > 10) over10++;
    if (pixelMax > 20) over20++;
    diff.data[index] = Math.min(255, pixelMax * 4);
    diff.data[index + 1] = Math.min(255, Math.max(0, (dr + dg + db) * 2));
    diff.data[index + 2] = Math.min(255, Math.max(0, (dg + db) * 2));
    diff.data[index + 3] = 255;
    lumaLeft[y * width + x] =
      0.2126 * left.data[index] + 0.7152 * left.data[index + 1] + 0.0722 * left.data[index + 2];
    lumaRight[y * width + x] =
      0.2126 * right.data[index] + 0.7152 * right.data[index + 1] + 0.0722 * right.data[index + 2];
  }
}

const pixels = width * height;
const mse = sumSq / (pixels * 3);
const psnr = mse === 0 ? 99 : 10 * Math.log10((255 * 255) / mse);

// Windowed SSIM over 8x8 blocks.
function ssimBlock(x0, y0, w, h) {
  let mx = 0;
  let my = 0;
  const n = w * h;
  for (let y = y0; y < y0 + h; y++) {
    for (let x = x0; x < x0 + w; x++) {
      mx += lumaLeft[y * width + x];
      my += lumaRight[y * width + x];
    }
  }
  mx /= n;
  my /= n;
  let vx = 0;
  let vy = 0;
  let cxy = 0;
  for (let y = y0; y < y0 + h; y++) {
    for (let x = x0; x < x0 + w; x++) {
      const a = lumaLeft[y * width + x] - mx;
      const b = lumaRight[y * width + x] - my;
      vx += a * a;
      vy += b * b;
      cxy += a * b;
    }
  }
  vx /= n;
  vy /= n;
  cxy /= n;
  const c1 = (0.01 * 255) ** 2;
  const c2 = (0.03 * 255) ** 2;
  return ((2 * mx * my + c1) * (2 * cxy + c2)) /
    ((mx * mx + my * my + c1) * (vx + vy + c2));
}

let ssimSum = 0;
let ssimCount = 0;
for (let y = 0; y < height; y += 8) {
  for (let x = 0; x < width; x += 8) {
    ssimSum += ssimBlock(x, y, Math.min(8, width - x), Math.min(8, height - y));
    ssimCount++;
  }
}

const result = {
  left: leftPath,
  right: rightPath,
  width,
  height,
  meanAbsoluteError: sumAbs / (pixels * 3),
  rmse: Math.sqrt(mse),
  psnr,
  ssim: ssimSum / ssimCount,
  maxChannelDiff: maxDiff,
  percentOver2: (over2 / pixels) * 100,
  percentOver5: (over5 / pixels) * 100,
  percentOver10: (over10 / pixels) * 100,
  percentOver20: (over20 / pixels) * 100,
};

fs.mkdirSync(outDir, { recursive: true });
const base = path.basename(leftPath, ".png").replace(/^android-/, "");
fs.writeFileSync(path.join(outDir, `${base}-diff.png`), PNG.sync.write(diff));
fs.writeFileSync(path.join(outDir, `${base}-metrics.json`), JSON.stringify(result, null, 2));
console.log(JSON.stringify(result, null, 2));
