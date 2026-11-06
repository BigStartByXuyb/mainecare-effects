import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

// Set LIQUID_GLASS_APP to a React app directory that has react, react-dom and
// liquid-glass-react in node_modules. ESBUILD defaults to that app's binary.
const app = process.env.LIQUID_GLASS_APP;
if (!app) {
  console.error("Set LIQUID_GLASS_APP to the app directory containing node_modules");
  process.exit(1);
}
const root = path.dirname(fileURLToPath(import.meta.url));
const entry = path.join(app, ".effects-upstream-entry.jsx");
fs.copyFileSync(path.join(root, "main.jsx"), entry);
try {
  const esbuild = process.env.ESBUILD || path.join(app, "node_modules/.bin/esbuild");
  execFileSync(
    esbuild,
    [
      entry,
      "--bundle",
      "--format=iife",
      "--loader:.jsx=jsx",
      "--define:process.env.NODE_ENV=\"production\"",
      `--outfile=${path.join(root, "bundle.js")}`,
    ],
    { stdio: "inherit" },
  );
} finally {
  fs.rmSync(entry, { force: true });
}
console.log("wrote verification/upstream-demo/bundle.js");
