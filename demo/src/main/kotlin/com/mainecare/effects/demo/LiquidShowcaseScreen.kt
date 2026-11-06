package com.mainecare.effects.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mainecare.effects.glass.GlassBackdrop
import com.mainecare.effects.glass.GlassSurface
import com.mainecare.effects.glass.MaineGlassMaterials

/** Dark ink for content over the bright showcase wallpaper. */
object LiquidInk {
    val Fg = Color(0xFF1A1A1E)
    val Muted = Color(0xB31A1A1E)
    val Faint = Color(0x801A1A1E)
    val Hair = Color(0x2E1A1A1E)
    val Tint = Color(0x141A1A1E)
}

private val showcaseGlass = MaineGlassMaterials.Upstream.copy(
    shadowOffsetYDp = 14f,
    shadowBlurSigmaDp = 38f,
    shadowAlpha = 0.32f,
)

/**
 * Public-first liquid-glass demo: the verified liquid-glass-react source defaults over a bright
 * wallpaper. It is the default tab; the v21 Obsidian gallery moved to the second tab.
 */
@Composable
fun LiquidShowcaseScreen(overLight: Boolean = false, photo: Boolean = false) {
    val backdrop = rememberBackdropAsset(if (photo) "photo-beach.jpg" else "showcase.png") ?: return
    GlassBackdrop(image = backdrop, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "LIQUID GLASS · UPSTREAM 1.1.1",
                fontSize = 9.sp,
                letterSpacing = 1.8.sp,
                color = LiquidInk.Faint,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "液态玻璃",
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.SemiBold,
                color = LiquidInk.Fg,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "亮背景上的边缘折射、RGB 色散与高光。拖动玻璃，观察光沿曲面移动。",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = LiquidInk.Muted,
            )
            Spacer(Modifier.height(18.dp))

            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(232.dp),
                material = showcaseGlass,
                params = showcaseGlass.liquid.copy(
                    cornerRadius = 30.dp,
                    overLight = overLight,
                    displacementScale = 70f,
                    aberrationIntensity = 2f,
                ),
                interactive = true,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "UPSTREAM · SOURCE DEFAULTS",
                            fontSize = 9.sp,
                            letterSpacing = 1.6.sp,
                            color = LiquidInk.Faint,
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC2FA36B)),
                        )
                    }
                    Column {
                        Text(
                            text = "Liquid Glass",
                            fontSize = 30.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LiquidInk.Fg,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "位移 70 · 色散 2.0 · 模糊 6 px",
                            fontSize = 11.sp,
                            letterSpacing = 0.4.sp,
                            color = LiquidInk.Muted,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        ShowcaseChip("折射")
                        ShowcaseChip("色散")
                        ShowcaseChip("高光")
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassSurface(
                    modifier = Modifier
                        .weight(1f)
                        .height(112.dp),
                    material = showcaseGlass,
                    params = showcaseGlass.liquid.copy(
                        cornerRadius = 26.dp,
                        overLight = overLight,
                        displacementScale = 70f,
                    ),
                    interactive = true,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "EDGE REFRACTION",
                            fontSize = 8.sp,
                            letterSpacing = 1.4.sp,
                            color = LiquidInk.Faint,
                        )
                        Column {
                            Text(
                                text = "70",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Light,
                                color = LiquidInk.Fg,
                            )
                            Text(
                                text = "displacementScale",
                                fontSize = 9.sp,
                                color = LiquidInk.Muted,
                            )
                        }
                    }
                }
                GlassSurface(
                    modifier = Modifier
                        .weight(1f)
                        .height(112.dp),
                    material = showcaseGlass,
                    params = showcaseGlass.liquid.copy(
                        cornerRadius = 26.dp,
                        overLight = overLight,
                        displacementScale = 70f,
                    ),
                    interactive = true,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "CHROMATIC",
                            fontSize = 8.sp,
                            letterSpacing = 1.4.sp,
                            color = LiquidInk.Faint,
                        )
                        Column {
                            Text(
                                text = "2.0",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Light,
                                color = LiquidInk.Fg,
                            )
                            Text(
                                text = "aberrationIntensity",
                                fontSize = 9.sp,
                                color = LiquidInk.Muted,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShowcasePill("拖动玻璃 · 高光跟随", overLight)
                Spacer(Modifier.width(8.dp))
                ShowcasePill("AGSL · API 33+", overLight)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ShowcaseChip(label: String) {
    Text(
        text = label,
        fontSize = 10.sp,
        letterSpacing = 1.0.sp,
        color = LiquidInk.Fg,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x1FFFFFFF))
            .padding(horizontal = 11.dp, vertical = 6.dp),
    )
}

@Composable
private fun ShowcasePill(label: String, overLight: Boolean) {
    val material = MaineGlassMaterials.Upstream
    GlassSurface(
        modifier = Modifier.height(36.dp),
        material = material,
        params = material.liquid.copy(
            cornerRadius = 18.dp,
            overLight = overLight,
            displacementScale = 70f,
        ),
        interactive = false,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                letterSpacing = 0.6.sp,
                color = LiquidInk.Muted,
            )
        }
    }
}
