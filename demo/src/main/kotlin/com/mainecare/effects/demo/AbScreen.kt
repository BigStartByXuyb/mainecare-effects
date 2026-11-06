package com.mainecare.effects.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mainecare.effects.glass.GlassBackdrop
import com.mainecare.effects.glass.GlassSurface
import com.mainecare.effects.glass.MaineGlassMaterials

/**
 * Upstream vs MaineCare v21 A/B. `mode` is "ab", "upstream" or "v21"; `light` switches the
 * high-contrast light backdrop and upstream overLight/shadow variant.
 *
 * The upstream preset is liquid-glass-react 1.1.1 source defaults; only cornerRadius is
 * overridden to 30dp so both cards have identical geometry for the screenshot gate.
 */
@Composable
fun AbScreen(
    light: Boolean,
    mode: String,
    debugOutput: Float = 0f,
    forceDisplacement: Boolean = true,
    blurEnabled: Boolean = true,
    displacementScale: Float = 70f,
    flat: Boolean = false,
    line: Boolean = false,
    color: Boolean = false,
    showcase: Boolean = false,
    blurSigmaScale: Float = 1.4f,
) {
    val backdrop = rememberBackdropAsset(
        when {
            line -> "ab-line.png"
            color -> "ab-color.png"
            flat -> "ab-flat.png"
            showcase -> "showcase.png"
            light -> "ab-light.png"
            else -> "ab-dark.png"
        },
    ) ?: return
    val radius = 30.dp
    val upstream = MaineGlassMaterials.Upstream.copy(
        blurRadiusDp = if (blurEnabled) MaineGlassMaterials.Upstream.blurRadiusDp else 0f,
        liquid = MaineGlassMaterials.Upstream.liquid.copy(
            cornerRadius = radius,
            overLight = light,
            displacementScale = if (forceDisplacement) displacementScale else 0f,
            aberrationIntensity = if (forceDisplacement) 2f else 0f,
        ),
        shadowOffsetYDp = if (light) 16f else 12f,
        shadowBlurSigmaDp = if (light) 56f else 32f,
        shadowAlpha = if (light) 0.75f else 0.25f,
    )
    val v21 = MaineGlassMaterials.Card.copy(
        liquid = MaineGlassMaterials.Card.liquid.copy(cornerRadius = radius),
    )
    val labelColor = if (light) Color(0xFF15181C) else Color(0xFFF2F0EA)

    BoxWithConstraints(Modifier.fillMaxSize().background(if (light) Color(0xFFF2EDE3) else Color(0xFF05070C))) {
        GlassBackdrop(
            image = backdrop,
            modifier = Modifier.fillMaxSize(),
            blurSigmaScale = blurSigmaScale,
        ) {
            when (mode) {
                "upstream" -> GlassSurface(
                    modifier = Modifier.offset(x = 24.dp, y = 120.dp).size(342.dp, 200.dp),
                    material = upstream,
                    interactive = false,
                    debugOutput = debugOutput,
                ) {}
                "v21" -> GlassSurface(
                    modifier = Modifier.offset(x = 24.dp, y = 120.dp).size(342.dp, 200.dp),
                    material = v21,
                    interactive = false,
                    debugOutput = debugOutput,
                ) {}
                else -> {
                    val cardWidth = (this@BoxWithConstraints.maxWidth - 48.dp) / 2
                    Box(Modifier.offset(x = 16.dp, y = 92.dp)) {
                        Text("UPSTREAM", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
                    }
                    Box(Modifier.offset(x = 32.dp + cardWidth, y = 92.dp)) {
                        Text("V21", color = labelColor, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp)
                    }
                    GlassSurface(
                        modifier = Modifier.offset(x = 16.dp, y = 120.dp).size(cardWidth, 200.dp),
                        material = upstream,
                        interactive = false,
                        debugOutput = debugOutput,
                    ) {}
                    GlassSurface(
                        modifier = Modifier.offset(x = 32.dp + cardWidth, y = 120.dp).size(cardWidth, 200.dp),
                        material = v21,
                        interactive = false,
                        debugOutput = debugOutput,
                    ) {}
                }
            }
        }
    }
}
