package com.mainecare.effects.glass

/**
 * AGSL port of liquid-glass-react's SVG filter plus the v21 Obsidian Veil material.
 *
 * The SVG reference draws a displacement map, runs three feDisplacementMap passes with
 * R/G/B channel recombination (chromatic aberration), blurs, then composites the CSS
 * background-color and gradient sheen. On Android the blur is precomputed into the
 * backdrop bitmap, and this shader performs the map lookup, the three-channel
 * displacement, saturation/brightness, tint and sheen in one pass.
 */
internal object LiquidGlassShader {
    const val AGSL: String = """
uniform shader uBackdrop;
uniform shader uMap;
uniform shader uScene;

uniform float2 uSize;
uniform float2 uBackdropOrigin;
uniform float2 uBackdropScale;
uniform float uMapSize;
uniform float uDisplacement;
uniform float uAberration;
uniform float uSaturation;
uniform float uBrightness;
uniform float4 uTint;
uniform float2 uPointer;
uniform float uSheen;
uniform float uElasticity;
uniform float uProcedural;
uniform float uBeforeLayer;
uniform float uBeforeStrength;
uniform float uDensity;
uniform float uBlurSigma;
uniform float uOpaqueWarp;
uniform float uInset;
uniform float2 uWarpSize;
uniform float2 uSceneOrigin;
uniform float2 uSceneScale;
uniform float uUnderlayAlpha;
uniform float4 uBorderColor;
uniform float uBorderWidth;
uniform float uCornerRadius;
uniform float uAfterGlow;
uniform float uDebug;

uniform float4 uSheenTopRight;
uniform float2 uSheenTopCenter;
uniform float2 uSheenTopRadii;
uniform float uSheenTopFade;
uniform float4 uSheenBottom;
uniform float4 uSheenDiagonal;
uniform float uSheenDiagonalAngle;
uniform float uSheenDiagonalStart;
uniform float uSheenDiagonalPeak;
uniform float uSheenDiagonalEnd;
uniform float4 uSheenDiagonal2;
uniform float uSheenDiagonal2Start;
uniform float uSheenDiagonal2Peak;
uniform float uSheenDiagonal2End;
uniform float4 uSheenDark;
uniform float4 uSheenVerticalTop;
uniform float4 uSheenVerticalMid;
uniform float uSheenVerticalMidPos;
uniform float4 uSheenVerticalBottom;

float smoothStepCustom(float a, float b, float t) {
    float x = clamp((t - a) / (b - a), 0.0, 1.0);
    return x * x * (3.0 - 2.0 * x);
}

float roundedRectSDF(float2 p, float2 halfSize, float radius) {
    float2 q = abs(p) - halfSize + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - radius;
}

float linearBand(float t, float start, float peak, float end) {
    if (t <= start || t >= end) {
        return 0.0;
    }
    if (peak <= start) {
        return (end - t) / (end - start);
    }
    if (t < peak) {
        return (t - start) / (peak - start);
    }
    return (end - t) / (end - peak);
}

float radialFalloff(float2 p, float2 center, float2 radii, float fade) {
    float d = length((p - center) / radii);
    return clamp(1.0 - d / fade, 0.0, 1.0);
}

// CSS linear-gradient position: 0 at the start corner, 1 at the end corner.
float gradientPositionFor(float2 coord, float angleDeg, float2 size) {
    float angle = angleDeg * 3.14159265 / 180.0;
    float2 dir = float2(sin(angle), -cos(angle));
    float2 center = size * 0.5;
    float gradientLength = abs(size.x * dir.x) + abs(size.y * dir.y);
    return 0.5 + dot(coord - center, dir) / max(gradientLength, 1.0);
}

float gradientPosition(float2 coord, float angleDeg) {
    return gradientPositionFor(coord - uInset, angleDeg, uWarpSize);
}

// CSS filter saturate()/brightness() run before feDisplacementMap in the reference chain.
float3 applyGrade(half4 c) {
    float3 rgb = float3(c.r, c.g, c.b);
    float luma = dot(rgb, float3(0.2126, 0.7152, 0.0722));
    return mix(float3(luma), rgb, uSaturation) * uBrightness;
}

float erfApprox(float x) {
    float t = 1.0 / (1.0 + 0.3275911 * abs(x));
    float poly = (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t -
        0.284496736) * t + 0.254829592) * t;
    return (1.0 - poly * exp(-x * x)) * sign(x);
}

float gaussianCdf(float z) {
    return 0.5 * (1.0 + erfApprox(z * 0.70710678));
}

// Alpha of the blurred warp element: a rectangle convolved with the CSS blur.
float roundedBoxSdf(float2 p, float2 halfSize, float radius) {
    float2 q = abs(p) - halfSize + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, float2(0.0))) - radius;
}

float warpAlpha(float2 coord, float2 size, float sigma) {
    if (sigma <= 0.0) return 1.0;
    float ax = gaussianCdf(coord.x / sigma) -
        gaussianCdf((coord.x - size.x) / sigma);
    float ay = gaussianCdf(coord.y / sigma) -
        gaussianCdf((coord.y - size.y) / sigma);
    return clamp(ax * ay, 0.0, 1.0);
}

half4 main(float2 coord) {
    float2 warpCoord = coord - uInset;
    float2 uv = warpCoord / uWarpSize;
    float2 oR;
    float2 oG;
    float2 oB;
    float3 mapColor = float3(0.5);

    if (uProcedural > 0.5) {
        // liquid-glass-react shader mode: roundedRectSDF -> smoothStep displacement.
        float2 ix = uv - 0.5;
        float d = roundedRectSDF(ix, float2(0.3, 0.2), 0.6);
        float amount = smoothStepCustom(0.8, 0.0, d - 0.15);
        float scaled = smoothStepCustom(0.0, 1.0, amount);
        float2 raw = (ix * scaled + 0.5 - uv) * uWarpSize;
        float k = uDisplacement / 50.0;
        oR = raw * k;
        oG = raw * k * (1.0 - uAberration * 0.05);
        oB = raw * k * (1.0 - uAberration * 0.10);
    } else {
        float2 filterSize = uWarpSize * 1.7;
        float side = max(filterSize.x, filterSize.y);
        float2 mapWindow = (filterSize / side) * (1.0 / 1.7);
        float2 mapUv = 0.5 + (uv - 0.5) * mapWindow;
        half4 m = uMap.eval(mapUv * uMapSize);
        mapColor = float3(m.r, m.g, m.b);
        float2 d = float2(m.r, m.b) - 0.5;
        float direction = sign(uDisplacement);
        oR = d * uDisplacement;
        oG = d * uDisplacement * (1.0 - direction * uAberration * 0.05);
        oB = d * uDisplacement * (1.0 - direction * uAberration * 0.10);
    }

    float2 base = uBackdropOrigin + warpCoord * uBackdropScale;
    half4 orig = uBackdrop.eval(base);
    half4 cr = uBackdrop.eval(base + oR * uBackdropScale);
    half4 cg = uBackdrop.eval(base + oG * uBackdropScale);
    half4 cb = uBackdrop.eval(base + oB * uBackdropScale);

    // The CSS chain grades the blurred backdrop before the displacement/aberration passes.
    float3 origF = applyGrade(orig);
    float3 crF = applyGrade(cr);
    float3 cgF = applyGrade(cg);
    float3 cbF = applyGrade(cb);

    half4 sceneRaw = uScene.eval(uSceneOrigin + warpCoord * uSceneScale);
    float3 stageCol = mix(float3(sceneRaw.rgb), uTint.rgb, uUnderlayAlpha);

    float3 col;
    if (abs(uDisplacement) > 0.001) {
        // The SVG chain screen-blends three channel layers whose source alpha is the
        // blurred rect alpha. A short element (the v21 pill) is semi-transparent, so the
        // screen composite darkens it over the stage; a tall card stays opaque.
        float warpA = mix(
            warpAlpha(warpCoord, uWarpSize, uBlurSigma),
            1.0,
            uOpaqueWarp
        );
        float twoLayers = 1.0 - (1.0 - warpA) * (1.0 - warpA);
        float outA = 1.0 - (1.0 - twoLayers) * (1.0 - warpA);
        float3 premul = float3(warpA * crF.r, warpA * cgF.g, warpA * cbF.b);
        float3 screened = premul / max(outA, 0.0001);
        col = screened * outA + stageCol * (1.0 - outA);
    } else {
        float warpA0 = warpAlpha(warpCoord, uWarpSize, uBlurSigma);
        col = origF * warpA0 + stageCol * (1.0 - warpA0);
    }
    if (uDebug > 3.5 && uDebug < 4.5) {
        return half4(col, 1.0);
    }
    if (uDebug > 5.5 && uDebug < 6.5) {
        return half4(stageCol, 1.0);
    }
    col = mix(col, uTint.rgb, uTint.a);

    // Pointer elasticity shifts the sheen, mirroring the web transform on the glass layer.
    float2 p = uv - (uPointer - float2(0.72, 0.08)) * uElasticity * 0.1;
    float s = uSheen;
    // CSS paints background layers bottom-to-top; the first listed layer ends up on top.
    float a5;
    float3 shade;
    if (uSheenVerticalMidPos >= 0.0 && uv.y < uSheenVerticalMidPos) {
        float t = uv.y / max(uSheenVerticalMidPos, 0.0001);
        a5 = mix(uSheenVerticalTop.a, uSheenVerticalMid.a, t);
        shade = mix(
            uSheenVerticalTop.rgb * uSheenVerticalTop.a,
            uSheenVerticalMid.rgb * uSheenVerticalMid.a,
            t
        ) / max(a5, 0.0001);
    } else if (uSheenVerticalMidPos >= 0.0) {
        float t = (uv.y - uSheenVerticalMidPos) /
            max(1.0 - uSheenVerticalMidPos, 0.0001);
        a5 = mix(uSheenVerticalMid.a, uSheenVerticalBottom.a, t);
        shade = mix(
            uSheenVerticalMid.rgb * uSheenVerticalMid.a,
            uSheenVerticalBottom.rgb * uSheenVerticalBottom.a,
            t
        ) / max(a5, 0.0001);
    } else {
        a5 = mix(uSheenVerticalTop.a, uSheenVerticalBottom.a, uv.y);
        shade = mix(
            uSheenVerticalTop.rgb * uSheenVerticalTop.a,
            uSheenVerticalBottom.rgb * uSheenVerticalBottom.a,
            uv.y
        ) / max(a5, 0.0001);
    }
    a5 *= s;
    col = mix(col, shade, a5);

    float t4 = gradientPosition(coord, 325.0);
    float a4 = clamp(1.0 - t4 / 0.48, 0.0, 1.0) * uSheenDark.a * s;
    col = mix(col, uSheenDark.rgb, a4);

    float t3 = gradientPosition(coord, uSheenDiagonalAngle);
    float a3 = linearBand(
        t3,
        uSheenDiagonalStart,
        uSheenDiagonalPeak,
        uSheenDiagonalEnd
    ) * uSheenDiagonal.a * s;
    col = mix(col, uSheenDiagonal.rgb, a3);

    float a3b = linearBand(
        t3,
        uSheenDiagonal2Start,
        uSheenDiagonal2Peak,
        uSheenDiagonal2End
    ) * uSheenDiagonal2.a * s;
    col = mix(col, uSheenDiagonal2.rgb, a3b);

    float a2 = radialFalloff(p, float2(1.08, -0.08), float2(0.92, 0.74), 0.46) *
        uSheenBottom.a * s;
    col = mix(col, uSheenBottom.rgb, a2);

    float a1 = radialFalloff(p, uSheenTopCenter, uSheenTopRadii, uSheenTopFade) *
        uSheenTopRight.a * s;
    col = mix(col, uSheenTopRight.rgb, a1);

    if (uBeforeLayer > 0.5) {
        // .card::before is inset 2px from the border-box (1px border + 1px inset).
        float inset = 2.0 * uDensity;
        float2 beforeSize = max(uWarpSize - 2.0 * inset, float2(1.0));
        float2 beforeCoord = warpCoord - inset;
        float beforeY = beforeCoord.y / beforeSize.y;
        float mask = beforeY < 0.64
            ? mix(1.0, 0.65, beforeY / 0.64)
            : 0.65 * (1.0 - (beforeY - 0.64) / 0.36);
        mask = clamp(mask, 0.0, 1.0);

        float ta = gradientPositionFor(beforeCoord, 111.0, beforeSize);
        float aa = linearBand(ta, 0.09, 0.18, 0.34) * 0.08 * 0.85 *
            uBeforeStrength * mask;
        col = mix(col, float3(0.957, 0.969, 0.976), aa);

        float tb = gradientPositionFor(beforeCoord, 68.0, beforeSize);
        float ab = linearBand(tb, 0.70, 0.74, 0.80) * 0.035 * 0.85 *
            uBeforeStrength * mask;
        col = mix(col, float3(0.886, 0.910, 0.925), ab);

        float angle = 116.0 * 3.14159265 / 180.0;
        float2 dir = float2(sin(angle), -cos(angle));
        float2 center = beforeSize * 0.5;
        float gradientLength = abs(beforeSize.x * dir.x) + abs(beforeSize.y * dir.y);
        float posPx = (0.5 + dot(beforeCoord - center, dir) /
            max(gradientLength, 1.0)) * gradientLength;
        float period = 24.0 * uDensity;
        float lineWidth = 1.0 * uDensity;
        float phase = fract(posPx / period);
        float line = 1.0 - smoothStepCustom(0.0, lineWidth / period, phase);
        float al = line * mask * 0.85 * 0.012 * uBeforeStrength;
        col = mix(col, float3(1.0), al);
    }

    if (uAfterGlow > 0.5) {
        float glow = radialFalloff(
            uv,
            float2(0.92, 1.17),
            float2(0.386, 0.29),
            0.68
        ) * 0.46;
        col = mix(col, float3(1.0), glow);
    }

    float2 borderP = coord - uSize * 0.5;
    float borderD = roundedBoxSdf(borderP, uSize * 0.5, uCornerRadius);
    float borderInside = -borderD;
    float borderMask = 1.0 - smoothstep(
        uBorderWidth - 0.5,
        uBorderWidth + 0.5,
        borderInside
    );
    borderMask *= step(0.001, uBorderColor.a);
    // Chrome's bottom border reads brighter than the card background-color composite,
    // as if the stage contributes more at the bottom edge; blend the raw stage there.
    float bottomW = smoothstep(0.75, 1.0, uv.y) * 0.7;
    float3 borderBase = mix(stageCol, float3(sceneRaw.rgb), bottomW);
    float3 borderCol = mix(borderBase, uBorderColor.rgb, uBorderColor.a);
    col = mix(col, borderCol, borderMask);

    if (uDebug > 2.5 && uDebug < 3.5) {
        return half4(mapColor, 1.0);
    }
    if (uDebug > 1.5 && uDebug < 2.5) {
        return half4(orig.rgb, 1.0);
    }
    if (uDebug > 0.5 && uDebug < 1.5) {
        return half4(0.5, 0.5, 0.5, 1.0);
    }
    return half4(half3(col), 1.0);
}
"""
}
