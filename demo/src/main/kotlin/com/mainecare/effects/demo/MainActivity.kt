package com.mainecare.effects.demo

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mainecare.effects.motion.MotionTrace

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialScreen = intent.getStringExtra("screen")
        if (initialScreen?.startsWith("ab") == true || initialScreen == "liquid" || initialScreen == "gate") {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        val abBg = intent.getStringExtra("bg")
        val abLight = abBg == "light" || abBg == "showcase"
        val abFlat = abBg == "flat"
        val abLine = abBg == "line"
        val abColor = abBg == "color"
        val abShowcase = abBg == "showcase"
        val gateDispScale = intent.getStringExtra("dispScale")?.toFloatOrNull() ?: 70f
        val displacement = intent.getStringExtra("displacement") != "0"
        val gateText = intent.getStringExtra("text") == "1"
        val gateDebug = intent.getStringExtra("debug")?.toFloatOrNull() ?: 0f
        val traceEnabled = intent.getStringExtra("trace") != "0"
        val gateShadow = intent.getStringExtra("shadow") != "0"
        val gateBefore = intent.getStringExtra("before") != "0"
        val gateSheen = intent.getStringExtra("sheen") != "0"
        val gateBorder = intent.getStringExtra("border") != "0"
        val gateBlur = intent.getStringExtra("blur") != "0"
        val gateScene = intent.getStringExtra("scene")
        val gateBlurMode = intent.getStringExtra("blurMode") ?: "skia"
        val gateBackground = when (intent.getStringExtra("bg")) {
            "photo" -> "backdrop.png"
            "obsidian" -> "backdrop-obsidian.png"
            else -> "backdrop-gate.png"
        }
        val gateBeforeStrength = intent.getStringExtra("beforeStrength")?.toFloatOrNull()
        val gateBlurSigmaScale = intent.getStringExtra("blurScale")?.toFloatOrNull() ?: 1.4f
        MotionTrace.sink = if (traceEnabled) {
            { line -> Log.i("MCEffectsMotion", line) }
        } else {
            null
        }
        setContent {
            MaineEffectsTheme {
                DemoRoot(
                    initialScreen = initialScreen,
                    gateDisplacement = displacement,
                    gateText = gateText,
                    gateDebug = gateDebug,
                    traceEnabled = traceEnabled,
                    gateShadow = gateShadow,
                    gateBefore = gateBefore,
                    gateSheen = gateSheen,
                    gateBorder = gateBorder,
                    gateBlur = gateBlur,
                    gateScene = gateScene,
                    gateBlurMode = gateBlurMode,
                    gateBeforeStrength = gateBeforeStrength,
                    abLight = abLight,
                    abFlat = abFlat,
                    abLine = abLine,
                    abColor = abColor,
                    abShowcase = abShowcase,
                    gateDispScale = gateDispScale,
                    abBlurSigmaScale = gateBlurSigmaScale,
                    gateBackground = gateBackground,
                    gateBlurSigmaScale = gateBlurSigmaScale,
                )
            }
        }
    }
}

@Composable
private fun DemoRoot(
    initialScreen: String?,
    gateDisplacement: Boolean,
    gateText: Boolean,
    gateDebug: Float,
    traceEnabled: Boolean,
    gateShadow: Boolean,
    gateBefore: Boolean,
    gateSheen: Boolean,
    gateBorder: Boolean,
    gateBlur: Boolean,
    gateScene: String?,
    gateBlurMode: String,
    gateBeforeStrength: Float?,
    gateBackground: String,
    gateBlurSigmaScale: Float,
    abLight: Boolean,
    abFlat: Boolean,
    abLine: Boolean,
    abColor: Boolean,
    abShowcase: Boolean,
    gateDispScale: Float,
    abBlurSigmaScale: Float,
) {
    if (initialScreen?.startsWith("ab") == true) {
        AbScreen(
            light = abLight,
            mode = initialScreen.removePrefix("ab-").ifEmpty { "ab" },
            debugOutput = gateDebug,
            forceDisplacement = gateDisplacement,
            blurEnabled = gateBlur,
            displacementScale = gateDispScale,
            flat = abFlat,
            line = abLine,
            color = abColor,
            showcase = abShowcase,
            blurSigmaScale = abBlurSigmaScale,
        )
        return
    }
    if (initialScreen == "gate") {
        GateScreen(
            forceDisplacement = gateDisplacement,
            showText = gateText,
            debugOutput = gateDebug,
            shadowEnabled = gateShadow,
            beforeEnabled = gateBefore,
            sheenEnabled = gateSheen,
            borderEnabled = gateBorder,
            blurEnabled = gateBlur,
            sceneAsset = gateScene,
            blurMode = gateBlurMode,
            beforeStrength = gateBeforeStrength,
            backgroundAsset = gateBackground,
            blurSigmaScale = gateBlurSigmaScale,
        )
        return
    }
    var tab by rememberSaveable {
        mutableIntStateOf(
            when (initialScreen) {
                "deck" -> 2
                "obsidian", "v21", "gallery" -> 1
                else -> 0
            },
        )
    }
    val liquidTab = tab == 0
    val view = LocalView.current
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = liquidTab
            isAppearanceLightNavigationBars = liquidTab
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (liquidTab) Color(0xFFFBF6EE) else Obsidian.Bg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        DemoHeader(tab, liquidTab) { tab = it }
        when (tab) {
            0 -> LiquidShowcaseScreen()
            1 -> GlassGalleryScreen()
            else -> DeckLabScreen()
        }
    }
}

@Composable
private fun DemoHeader(tab: Int, light: Boolean, onTab: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = "MAINECARE",
                fontSize = 9.sp,
                letterSpacing = 2.6.sp,
                color = if (light) LiquidInk.Faint else Obsidian.Faint,
            )
            Text(
                text = "Effects Lab",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (light) LiquidInk.Fg else Obsidian.Fg,
            )
        }
        TabButton("LIQUID", tab == 0, light) { onTab(0) }
        Spacer(Modifier.width(5.dp))
        TabButton("V21", tab == 1, light) { onTab(1) }
        Spacer(Modifier.width(5.dp))
        TabButton("DECK", tab == 2, light) { onTab(2) }
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, light: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    val selectedColor = if (light) LiquidInk.Fg else Obsidian.Gold
    val idleColor = if (light) LiquidInk.Faint else Obsidian.Faint
    val selectedFill = if (light) LiquidInk.Tint else Color(0x1FE3C08A)
    val selectedBorder = if (light) Color(0x4D1A1A1E) else Color(0x66E3C08A)
    val idleBorder = if (light) LiquidInk.Hair else Obsidian.Hair
    Text(
        text = label,
        fontSize = 9.sp,
        letterSpacing = 1.1.sp,
        color = if (selected) selectedColor else idleColor,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) selectedFill else Color.Transparent)
            .border(1.dp, if (selected) selectedBorder else idleBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp),
    )
}
