import fs from "node:fs";
import path from "node:path";
import { PNG } from "pngjs";

const [inputPath, outputPath, xArg, yArg, wArg, hArg, scaleArg = "3"] = process.argv.slice(2);
if (!inputPath || !outputPath) {
  console.error("usage: node crop-region.mjs input.png output.png x y w h [scale]");
  process.exit(1);
}
const img = PNG.sync.read(fs.readFileSync(inputPath));
const x0 = Number(xArg);
const y0 = Number(yArg);
const w = Number(wArg);
const h = Number(hArg);
const scale = Math.max(1, Number(scaleArg));
const out = new PNG({ width: w * scale, height: h * scale });
for (let y = 0; y < h * scale; y++) {
  for (let x = 0; x < w * scale; x++) {
    const sx = x0 + Math.floor(x / scale);
    const sy = y0 + Math.floor(y / scale);
    const source = (sy * img.width + sx) * 4;
    const destination = (y * out.width + x) * 4;
    out.data[destination] = img.data[source];
    out.data[destination + 1] = img.data[source + 1];
    out.data[destination + 2] = img.data[source + 2];
    out.data[destination + 3] = 255;
  }
}
fs.mkdirSync(path.dirname(outputPath), { recursive: true });
fs.writeFileSync(outputPath, PNG.sync.write(out));
console.log(`${outputPath} (${w}x${h} @ ${x0},${y0} x${scale})`);
