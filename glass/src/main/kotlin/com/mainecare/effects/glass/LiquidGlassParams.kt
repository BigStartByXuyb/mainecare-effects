package com.mainecare.effects.glass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Refraction modes of liquid-glass-react 1.1.1. Standard, Polar and Prominent use the
 * three displacement maps shipped by that MIT-licensed package; Shader reproduces its
 * procedural rounded-rect SDF. The Android implementation samples the same map with the
 * same feDisplacementMap channel convention (R -> x, B -> y).
 */
enum class GlassMode {
    Standard,
    Polar,
    Prominent,
    Shader,
}

/** Parameter names and defaults mirror liquid-glass-react's <LiquidGlass> props. */
@Immutable
data class LiquidGlassParams(
    val displacementScale: Float = 70f,
    val blurAmount: Float = 0.0625f,
    val saturation: Float = 140f,
    val aberrationIntensity: Float = 2f,
    val elasticity: Float = 0.15f,
    val cornerRadius: Dp = 999.dp,
    val mode: GlassMode = GlassMode.Standard,
    val overLight: Boolean = false,
) {
    /**
     * CSS backdrop blur used by liquid-glass-react: (overLight ? 12 : 4) + blurAmount * 32.
     * MaineCare's v21 CSS overrides this per layer through [GlassMaterial.blurRadiusDp].
     */
    val liquidBlurRadiusDp: Float
        get() = (if (overLight) 12f else 4f) + blurAmount * 32f

    val saturationPercent: Float
        get() = saturation / 100f

    val signedDisplacementScale: Float
        get() {
            val base = if (overLight) displacementScale * 0.5f else displacementScale
            return if (mode == GlassMode.Shader) base else -base
        }
}

/** Edge treatment: MaineCare v21 tinted rim or liquid-glass-react's clear rim. */
enum class GlassEdgeStyle { V21, Upstream }

/**
 * The v21 Obsidian Veil material layer: tint, CSS backdrop filter, border, and inner
 * light/shadow. Keeping it separate from [LiquidGlassParams] lets the refraction stay
 * aligned with liquid-glass-react while each surface keeps its v21 CSS values.
 */
@Immutable
data class GlassMaterial(
    val name: String,
    val tint: Color,
    val blurRadiusDp: Float,
    val saturation: Float,
    val brightness: Float,
    val borderColor: Color,
    val backgroundUnderlayAlpha: Float = 0f,
    val topHighlightColor: Color,
    val bottomInnerShadowColor: Color,
    val innerShadowDepthDp: Float,
    val innerShadowOffsetYDp: Float = -innerShadowDepthDp * 0.65f,
    val shadowOffsetYDp: Float,
    val shadowBlurSigmaDp: Float,
    val shadowAlpha: Float,
    val sheenStrength: Float,
    val edgeStyle: GlassEdgeStyle = GlassEdgeStyle.V21,
    val beforeLayer: Boolean = false,
    val beforeStrength: Float = 1f,
    val afterGlow: Boolean = false,
    val liquid: LiquidGlassParams = LiquidGlassParams(),
    val goldTopEdge: Boolean = false,
    val sheenTopRight: Color = Color(0x1AF0F5F9),
    val sheenTopCenter: Offset = Offset(0.72f, 0.08f),
    val sheenTopRadii: Offset = Offset(0.82f, 1.40f),
    val sheenTopFade: Float = 0.60f,
    val sheenBottom: Color = Color(0x13CDD7E0),
    val sheenDiagonal: Color = Color(0x0DFFFFFF),
    val sheenDiagonalAngle: Float = 116f,
    val sheenDiagonalStart: Float = 0.08f,
    val sheenDiagonalPeak: Float = 0.18f,
    val sheenDiagonalEnd: Float = 0.30f,
    val sheenDiagonal2: Color = Color.Transparent,
    val sheenDiagonal2Start: Float = 0.52f,
    val sheenDiagonal2Peak: Float = 0.52f,
    val sheenDiagonal2End: Float = 0.74f,
    val sheenDark: Color = Color(0x4D000000),
    val sheenVerticalTop: Color = Color(0x3D22262B),
    val sheenVerticalMid: Color = Color.Transparent,
    val sheenVerticalMidPos: Float = -1f,
    val sheenVerticalBottom: Color = Color(0x8A030406),
)
