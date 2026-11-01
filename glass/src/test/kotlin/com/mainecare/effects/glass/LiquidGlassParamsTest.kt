package com.mainecare.effects.glass

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassParamsTest {
    @Test
    fun liquidBlurRadiusMatchesReactFormula() {
        val standard = LiquidGlassParams(blurAmount = 0.0625f)
        assertEquals(6f, standard.liquidBlurRadiusDp, 0.0001f)
        val dark = LiquidGlassParams(blurAmount = 0.055f)
        assertEquals(5.76f, dark.liquidBlurRadiusDp, 0.0001f)
        val light = LiquidGlassParams(blurAmount = 0.0625f, overLight = true)
        assertEquals(14f, light.liquidBlurRadiusDp, 0.0001f)
    }

    @Test
    fun displacementSignFollowsModeAndOverLight() {
        val standard = LiquidGlassParams(displacementScale = 22f)
        assertEquals(-22f, standard.signedDisplacementScale, 0f)
        val shader = LiquidGlassParams(displacementScale = 22f, mode = GlassMode.Shader)
        assertEquals(22f, shader.signedDisplacementScale, 0f)
        val light = LiquidGlassParams(displacementScale = 22f, overLight = true)
        assertEquals(-11f, light.signedDisplacementScale, 0f)
    }

    @Test
    fun v21CardPresetMatchesCss() {
        val card = MaineGlassMaterials.Card
        assertEquals(34f, card.blurRadiusDp, 0f)
        assertEquals(1.32f, card.saturation, 0.0001f)
        assertEquals(0.82f, card.brightness, 0.0001f)
        assertEquals(0.56f, card.tint.alpha, 0.002f)
        assertEquals(22f, card.liquid.displacementScale, 0f)
        assertTrue(card.liquid.mode == GlassMode.Standard)
        assertEquals(30.dp, card.liquid.cornerRadius)
    }

    @Test
    fun advicePresetKeepsGoldEdgeAndDarkerTint() {
        val advice = MaineGlassMaterials.Advice
        assertTrue(advice.goldTopEdge)
        assertEquals(0.66f, advice.tint.alpha, 0.002f)
        assertEquals(32f, advice.blurRadiusDp, 0f)
    }
}
