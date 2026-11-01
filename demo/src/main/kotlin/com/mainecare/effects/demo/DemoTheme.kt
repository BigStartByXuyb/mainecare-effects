package com.mainecare.effects.demo

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Obsidian {
    val Bg = Color(0xFF07090C)
    val Fg = Color(0xFFF2F0EA)
    val Muted = Color(0xC2F2F0EA)
    val Faint = Color(0x7AF2F0EA)
    val Gold = Color(0xFFE3C08A)
    val GoldDeep = Color(0xFFD9B77C)
    val Hair = Color(0x1FEBEFF4)
    val Cool = Color(0xFFB6C6D3)
}

@Composable
fun MaineEffectsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Obsidian.Gold,
            onPrimary = Color(0xFF14100A),
            background = Obsidian.Bg,
            onBackground = Obsidian.Fg,
            surface = Color(0xFF0B0D10),
            onSurface = Obsidian.Fg,
            outline = Color(0x33EBEFF4),
        ),
        content = content,
    )
}
