import React from "react";
import { createRoot } from "react-dom/client";
import LiquidGlass from "liquid-glass-react";

const params = new URLSearchParams(location.search);
const bg = params.get("bg") || "dark";
const light = bg === "light";
const flat = bg === "flat";
const line = bg === "line";
const color = bg === "color";
const noBlur = params.get("noblur") === "1";
const displacement = params.get("disp") === "0" ? 0 : 70;
const aberration = params.get("disp") === "0" ? 0 : 2;
if (noBlur) {
  const style = document.createElement("style");
  style.textContent = ".glass__warp { backdrop-filter: none !important; }";
  document.head.appendChild(style);
}
const overLight = light;

function Card() {
  return (
    <div style={{ position: "absolute", left: 24, top: 120, width: 342, height: 200 }}>
      <LiquidGlass
        displacementScale={displacement}
        blurAmount={0.0625}
        saturation={140}
        aberrationIntensity={aberration}
        elasticity={0.15}
        cornerRadius={30}
        padding="0"
        mode="standard"
        overLight={overLight}
        style={{
          position: "absolute",
          left: "50%",
          top: "50%",
          width: 342,
          height: 200,
        }}
      >
        <div style={{ width: 342, height: 200 }} />
      </LiquidGlass>
    </div>
  );
}

function App() {
  return (
    <>
      <div
        style={{
          position: "absolute",
          inset: 0,
          background: flat ? "#808080" : undefined,
          backgroundImage: flat ? undefined : line ? "url(ab-line.png)" : color ? "url(ab-color.png)" : `url(${light ? "ab-light.png" : "ab-dark.png"})`,
          backgroundSize: "cover",
          backgroundPosition: "center",
        }}
      />
      <Card />
    </>
  );
}

createRoot(document.getElementById("root")).render(<App />);
