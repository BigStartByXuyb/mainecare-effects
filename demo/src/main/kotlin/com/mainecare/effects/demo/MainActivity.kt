package com.mainecare.effects.demo

import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mainecare.effects.motion.MotionTrace

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialScreen = intent.getStringExtra("screen")
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
                    gateBeforeStrength = gateBeforeStrength,
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
    gateBeforeStrength: Float?,
    gateBackground: String,
    gateBlurSigmaScale: Float,
) {
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
            beforeStrength = gateBeforeStrength,
            backgroundAsset = gateBackground,
            blurSigmaScale = gateBlurSigmaScale,
        )
        return
    }
    var tab by rememberSaveable {
        mutableIntStateOf(if (initialScreen == "deck") 1 else 0)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Obsidian.Bg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        DemoHeader(tab) { tab = it }
        when (tab) {
            0 -> GlassGalleryScreen()
            else -> DeckLabScreen()
        }
    }
}

@Composable
private fun DemoHeader(tab: Int, onTab: (Int) -> Unit) {
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
                color = Obsidian.Faint,
            )
            Text(
                text = "Effects Lab",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Obsidian.Fg,
            )
        }
        TabButton("GLASS", tab == 0) { onTab(0) }
        Spacer(Modifier.width(6.dp))
        TabButton("DECK", tab == 1) { onTab(1) }
    }
}

@Composable
private fun TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Text(
        text = label,
        fontSize = 10.sp,
        letterSpacing = 1.4.sp,
        color = if (selected) Obsidian.Gold else Obsidian.Faint,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Color(0x1FE3C08A) else Color.Transparent)
            .border(1.dp, if (selected) Color(0x66E3C08A) else Obsidian.Hair, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}
