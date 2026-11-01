import fs from "node:fs";
import path from "node:path";
import { PNG } from "pngjs";

const [leftPath, rightPath, outPath, gapArg = "8"] = process.argv.slice(2);
if (!leftPath || !rightPath || !outPath) {
  console.error("usage: node side-by-side.mjs left.png right.png out.png [gap]");
  process.exit(1);
}
const left = PNG.sync.read(fs.readFileSync(leftPath));
const right = PNG.sync.read(fs.readFileSync(rightPath));
if (left.height !== right.height) {
  throw new Error(`height mismatch ${left.height} vs ${right.height}`);
}
const gap = Number(gapArg);
const width = left.width + gap + right.width;
const out = new PNG({ width, height: left.height });
for (let y = 0; y < left.height; y++) {
  for (let x = 0; x < left.width; x++) {
    const source = (y * left.width + x) * 4;
    const destination = (y * width + x) * 4;
    for (let c = 0; c < 3; c++) out.data[destination + c] = left.data[source + c];
    out.data[destination + 3] = 255;
  }
  for (let x = 0; x < gap; x++) {
    const destination = (y * width + left.width + x) * 4;
    out.data[destination] = 24;
    out.data[destination + 1] = 24;
    out.data[destination + 2] = 24;
    out.data[destination + 3] = 255;
  }
  for (let x = 0; x < right.width; x++) {
    const source = (y * right.width + x) * 4;
    const destination = (y * width + left.width + gap + x) * 4;
    for (let c = 0; c < 3; c++) out.data[destination + c] = right.data[source + c];
    out.data[destination + 3] = 255;
  }
}
fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, PNG.sync.write(out));
console.log(`${outPath} (${width}x${left.height})`);
