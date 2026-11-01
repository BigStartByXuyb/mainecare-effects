package com.mainecare.effects.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mainecare.effects.glass.GlassBackdrop
import com.mainecare.effects.glass.GlassSurface
import com.mainecare.effects.glass.MaineGlassMaterials

/**
 * Fixed 390x844dp surface for the pixel gate. Launch with
 * `am start -n .../.MainActivity --es screen gate [--es displacement 0] [--es text 1]`.
 * The frame is centered on a neutral gray field so the screenshot can be cropped exactly.
 */
@Composable
fun GateScreen(
    forceDisplacement: Boolean,
    showText: Boolean,
    debugOutput: Float = 0f,
    shadowEnabled: Boolean = true,
    beforeEnabled: Boolean = true,
    beforeStrength: Float? = null,
) {
    val backdrop = rememberBackdropAsset("backdrop.png") ?: return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF202020)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(390.dp, 844.dp)
                .clipToBounds(),
        ) {
            GlassBackdrop(image = backdrop, modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize()) {
                    val base = MaineGlassMaterials.Card.copy(
                        shadowAlpha = if (shadowEnabled) MaineGlassMaterials.Card.shadowAlpha else 0f,
                        beforeLayer = beforeEnabled && MaineGlassMaterials.Card.beforeLayer,
                        beforeStrength = beforeStrength ?: MaineGlassMaterials.Card.beforeStrength,
                    )
                    val params = base.liquid.copy(
                        displacementScale = if (forceDisplacement) base.liquid.displacementScale else 0f,
                        aberrationIntensity = if (forceDisplacement) base.liquid.aberrationIntensity else 0f,
                    )
                    GlassSurface(
                        modifier = Modifier
                            .offset(x = 24.dp, y = 120.dp)
                            .size(342.dp, 200.dp),
                        material = base,
                        params = params,
                        interactive = false,
                        debugOutput = debugOutput,
                    ) {
                        if (showText) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(18.dp),
                            ) {
                                Text(
                                    "Obsidian Veil",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Obsidian.Fg,
                                )
                                Text(
                                    "Liquid glass · v21",
                                    fontSize = 11.sp,
                                    color = Obsidian.Muted,
                                )
                            }
                        }
                    }
                    val dock = MaineGlassMaterials.Dock.copy(
                        shadowAlpha = if (shadowEnabled) MaineGlassMaterials.Dock.shadowAlpha else 0f,
                    )
                    GlassSurface(
                        modifier = Modifier
                            .offset(x = 24.dp, y = 350.dp)
                            .size(132.dp, 36.dp),
                        material = dock,
                        params = dock.liquid.copy(
                            displacementScale = if (forceDisplacement) dock.liquid.displacementScale else 0f,
                            aberrationIntensity = if (forceDisplacement) dock.liquid.aberrationIntensity else 0f,
                            cornerRadius = 18.dp,
                        ),
                        interactive = false,
                        debugOutput = debugOutput,
                    ) {
                        if (showText) {
                            Text(
                                "Obsidian",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                fontSize = 11.sp,
                                color = Obsidian.Muted,
                            )
                        }
                    }
                }
            }
        }
    }
}
