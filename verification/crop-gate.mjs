import fs from "node:fs";
import path from "node:path";
import { PNG } from "pngjs";

const [inputPath, outputPath, frameWidth = "1024", frameHeight = "2216"] = process.argv.slice(2);
if (!inputPath || !outputPath) {
  console.error("usage: node crop-gate.mjs input.png output.png [frameWidth] [frameHeight]");
  process.exit(1);
}

const img = PNG.sync.read(fs.readFileSync(inputPath));
const targetWidth = Number(frameWidth);
const targetHeight = Number(frameHeight);
const width = img.width;
const height = img.height;
const isBackground = (i) =>
  Math.abs(img.data[i] - 32) < 3 &&
  Math.abs(img.data[i + 1] - 32) < 3 &&
  Math.abs(img.data[i + 2] - 32) < 3;

let left = width;
let right = 0;
let top = height;
let bottom = 0;
for (let x = 0; x < width; x++) {
  let count = 0;
  for (let y = 100; y < height - 100; y += 4) {
    if (!isBackground((y * width + x) * 4)) count++;
  }
  if (count > 100) {
    left = Math.min(left, x);
    right = Math.max(right, x);
  }
}
for (let y = 0; y < height; y++) {
  let count = 0;
  for (let x = 0; x < width; x += 4) {
    if (!isBackground((y * width + x) * 4)) count++;
  }
  if (count > 100) {
    top = Math.min(top, y);
    bottom = Math.max(bottom, y);
  }
}

const foundWidth = right - left + 1;
const foundHeight = bottom - top + 1;
if (foundWidth < targetWidth * 0.9 || foundHeight < targetHeight * 0.9) {
  throw new Error(
    `gate frame not found (found ${foundWidth}x${foundHeight} at ${left},${top}); is the gate screen in front?`,
  );
}

const out = new PNG({ width: foundWidth, height: foundHeight });
for (let y = 0; y < foundHeight; y++) {
  for (let x = 0; x < foundWidth; x++) {
    const source = ((y + top) * width + (x + left)) * 4;
    const destination = (y * foundWidth + x) * 4;
    out.data[destination] = img.data[source];
    out.data[destination + 1] = img.data[source + 1];
    out.data[destination + 2] = img.data[source + 2];
    out.data[destination + 3] = 255;
  }
}
fs.mkdirSync(path.dirname(outputPath), { recursive: true });
fs.writeFileSync(outputPath, PNG.sync.write(out));
console.log(`gated frame ${foundWidth}x${foundHeight} at ${left},${top} -> ${outputPath}`);
