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
uniform float uDebug;

uniform float4 uSheenTopRight;
uniform float2 uSheenTopCenter;
uniform float2 uSheenTopRadii;
uniform float uSheenTopFade;
uniform float4 uSheenBottom;
uniform float4 uSheenDiagonal;
uniform float4 uSheenDark;
uniform float4 uSheenVerticalTop;
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
float gradientPosition(float2 coord, float angleDeg) {
    float angle = angleDeg * 3.14159265 / 180.0;
    float2 dir = float2(sin(angle), -cos(angle));
    float2 center = uSize * 0.5;
    float gradientLength = abs(uSize.x * dir.x) + abs(uSize.y * dir.y);
    return 0.5 + dot(coord - center, dir) / max(gradientLength, 1.0);
}

half4 main(float2 coord) {
    float2 uv = coord / uSize;
    float2 oR;
    float2 oG;
    float2 oB;
    float edgeMask = 1.0;

    if (uProcedural > 0.5) {
        // liquid-glass-react shader mode: roundedRectSDF -> smoothStep displacement.
        float2 ix = uv - 0.5;
        float d = roundedRectSDF(ix, float2(0.3, 0.2), 0.6);
        float amount = smoothStepCustom(0.8, 0.0, d - 0.15);
        float scaled = smoothStepCustom(0.0, 1.0, amount);
        float2 raw = (ix * scaled + 0.5 - uv) * uSize;
        float k = uDisplacement / 50.0;
        oR = raw * k;
        oG = raw * k * (1.0 - uAberration * 0.05);
        oB = raw * k * (1.0 - uAberration * 0.10);
    } else {
        half4 m = uMap.eval(uv * uMapSize);
        float2 d = float2(m.r, m.b) - 0.5;
        float direction = sign(uDisplacement);
        oR = d * uDisplacement;
        oG = d * uDisplacement * (1.0 - direction * uAberration * 0.05);
        oB = d * uDisplacement * (1.0 - direction * uAberration * 0.10);
        // feComponentTransfer(discrete) mask from 0.3*(R+G+B) of the map:
        // [0,1/3)->0, [1/3,2/3)->aberration*0.05, [2/3,1]->1.
        float edge = clamp((float(m.r) + float(m.g) + float(m.b)) * 0.3, 0.0, 1.0);
        edgeMask = edge < 0.333333 ? 0.0 : (edge < 0.666667 ? uAberration * 0.05 : 1.0);
    }

    float2 base = uBackdropOrigin + coord * uBackdropScale;
    half4 orig = uBackdrop.eval(base);
    half4 cr = uBackdrop.eval(base + oR * uBackdropScale);
    half4 cg = uBackdrop.eval(base + oG * uBackdropScale);
    half4 cb = uBackdrop.eval(base + oB * uBackdropScale);

    float3 col = mix(float3(orig.r, orig.g, orig.b), float3(cr.r, cg.g, cb.b), edgeMask);
    float luma = dot(col, float3(0.2126, 0.7152, 0.0722));
    col = mix(float3(luma), col, uSaturation) * uBrightness;
    col = mix(col, uTint.rgb, uTint.a);

    // Pointer elasticity shifts the sheen, mirroring the web transform on the glass layer.
    float2 p = uv - (uPointer - float2(0.72, 0.08)) * uElasticity * 0.1;
    float s = uSheen;

    float a1 = radialFalloff(p, uSheenTopCenter, uSheenTopRadii, uSheenTopFade) *
        uSheenTopRight.a * s;
    col = mix(col, uSheenTopRight.rgb, a1);

    float a2 = radialFalloff(p, float2(1.08, -0.08), float2(0.92, 0.74), 0.46) *
        uSheenBottom.a * s;
    col = mix(col, uSheenBottom.rgb, a2);

    float t3 = gradientPosition(coord, 116.0);
    float a3 = linearBand(t3, 0.08, 0.18, 0.30) * uSheenDiagonal.a * s;
    col = mix(col, uSheenDiagonal.rgb, a3);

    float t4 = gradientPosition(coord, 325.0);
    float a4 = clamp(1.0 - t4 / 0.48, 0.0, 1.0) * uSheenDark.a * s;
    col = mix(col, uSheenDark.rgb, a4);

    float a5 = mix(uSheenVerticalTop.a, uSheenVerticalBottom.a, uv.y) * s;
    float3 shade = mix(uSheenVerticalTop.rgb, uSheenVerticalBottom.rgb, uv.y);
    col = mix(col, shade, a5);

    if (uDebug > 0.5) {
        return half4(0.5, 0.5, 0.5, 1.0);
    }
    return half4(half3(col), 1.0);
}
"""
}
