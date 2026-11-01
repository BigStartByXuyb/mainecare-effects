import fs from "node:fs";

const [logPath] = process.argv.slice(2);
if (!logPath) {
  console.error("usage: node motion-fit.mjs verification/results/motion-coverflow2.log");
  process.exit(1);
}
const lines = fs.readFileSync(logPath, "utf8").split("\n");
const release = lines.find((line) => line.includes("coverflow.release"));
if (!release) throw new Error("no coverflow.release line in log");
const parts = release.split("coverflow.release,")[1].split(",");
const target = Number(parts[0]);
const initialVelocity = Number(parts[1]);
const releaseTime = Number(parts[2]) / 1e9;
const samples = lines
  .filter((line) => line.includes("coverflow.spring"))
  .map((line) => {
    const p = line.split("coverflow.spring,")[1].split(",");
    return { value: Number(p[0]), time: Number(p[1]) / 1e9 };
  });

function step(position, velocity, dt) {
  const displacement = target - position;
  const nextVelocity = velocity + (displacement * 220 - velocity * 27) * dt;
  const nextPosition = position + nextVelocity * dt;
  const settled = Math.abs(displacement) < 0.0015 && Math.abs(nextVelocity) < 0.018;
  return {
    position: settled ? target : nextPosition,
    velocity: settled ? 0 : nextVelocity,
  };
}

let best = { deviation: Number.POSITIVE_INFINITY, start: 0 };
for (let start = -5; start <= 6; start += 0.001) {
  let position = start;
  let velocity = initialVelocity;
  let previous = releaseTime;
  let maxDeviation = 0;
  for (const sample of samples) {
    const dt = Math.min(0.032, Math.max(0.001, sample.time - previous));
    previous = sample.time;
    const next = step(position, velocity, dt);
    position = next.position;
    velocity = next.velocity;
    maxDeviation = Math.max(maxDeviation, Math.abs(position - sample.value));
    if (maxDeviation > best.deviation) break;
  }
  if (maxDeviation < best.deviation) best = { deviation: maxDeviation, start };
}

const result = {
  target,
  initialVelocity,
  samples: samples.length,
  inferredStart: best.start,
  maxDeviationIndex: best.deviation,
  maxDeviationDpAt118Step: best.deviation * 118,
  finalValue: samples.at(-1)?.value,
  durationSeconds: samples.at(-1) ? samples.at(-1).time - releaseTime : null,
};
console.log(JSON.stringify(result, null, 2));
