package com.mainecare.effects.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos

@Composable
fun rememberFps(enabled: Boolean): Float {
    var fps by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            fps = 0f
            return@LaunchedEffect
        }
        var frames = 0
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            frames++
            val delta = now - last
            if (delta >= 1_000_000_000L) {
                fps = frames * 1_000_000_000f / delta
                frames = 0
                last = now
            }
        }
    }
    return fps
}
