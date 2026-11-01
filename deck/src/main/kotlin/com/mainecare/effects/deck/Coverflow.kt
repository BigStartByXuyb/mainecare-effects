package com.mainecare.effects.deck

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mainecare.effects.motion.JsSpringAnimatable
import com.mainecare.effects.motion.MaineMotion
import com.mainecare.effects.motion.MotionTrace
import com.mainecare.effects.motion.coverflowAlpha
import com.mainecare.effects.motion.coverflowDepth
import com.mainecare.effects.motion.coverflowRotation
import com.mainecare.effects.motion.coverflowScale
import com.mainecare.effects.motion.coverflowStepDp
import com.mainecare.effects.motion.coverflowZIndex
import com.mainecare.effects.motion.modulo
import com.mainecare.effects.motion.wrapDelta
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Horizontal coverflow with the exact v18 geometry and gesture thresholds. Positions and
 * the spring run in index space; each card is placed with a 3D layer so the y-rotation,
 * depth and scale mirror the web's translateX/translateZ/rotateY/scale string.
 */
@Composable
fun <T> Coverflow(
    items: List<T>,
    modifier: Modifier = Modifier,
    initialIndex: Int = 0,
    itemWidth: Dp = 154.dp,
    itemHeight: Dp = 72.dp,
    onIndexChanged: (Int) -> Unit = {},
    content: @Composable (T, Int) -> Unit,
) {
    if (items.isEmpty()) return
    val density = LocalDensity.current
    val center = remember { JsSpringAnimatable(initialIndex.toFloat()) }
    val scope = rememberCoroutineScope()
    val currentOnIndexChanged by rememberUpdatedState(onIndexChanged)
    var dragging by remember { mutableStateOf(false) }
    var animationJob by remember { mutableStateOf<Job?>(null) }

    fun startMotion(target: Float, initialVelocity: Float) {
        animationJob?.cancel()
        MotionTrace.emit("coverflow.release", target, initialVelocity)
        animationJob = scope.launch {
            center.animateTo(
                target = target,
                stiffness = MaineMotion.CoverflowStiffness,
                damping = MaineMotion.CoverflowDamping,
                maxDurationMs = MaineMotion.CoverflowMaxDurationMs,
                initialVelocity = initialVelocity,
                maxVelocity = MaineMotion.CoverflowMaxVelocity,
                onFrame = { MotionTrace.emit("coverflow.spring", it) },
            )
            currentOnIndexChanged(modulo(center.value, items.size).roundToInt())
        }
    }

    LaunchedEffect(initialIndex) {
        center.snapTo(initialIndex.toFloat())
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        startMotion(center.value.roundToInt() - 1f, 0f); true
                    }
                    Key.DirectionRight -> {
                        startMotion(center.value.roundToInt() + 1f, 0f); true
                    }
                    else -> false
                }
            }
            .pointerInput(items.size, density.density) {
                val stepDp = coverflowStepDp(size.width / density.density)
                val stepPx = stepDp * density.density
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    animationJob?.cancel()
                    val startX = down.position.x
                    val startY = down.position.y
                    val startCenter = center.value
                    var lastX = startX
                    var lastY = startY
                    var lastAt = down.uptimeMillis
                    var velocityX = 0f
                    var velocityY = 0f
                    var axis = 0 // 0 unknown, 1 horizontal, 2 vertical
                    var upAt = down.uptimeMillis
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            upAt = change.uptimeMillis
                            break
                        }
                        val now = change.uptimeMillis
                        val dt = max(1L, now - lastAt).toFloat()
                        velocityX = velocityX * 0.58f +
                            ((change.position.x - lastX) / dt) * 0.42f
                        velocityY = velocityY * 0.58f +
                            ((change.position.y - lastY) / dt) * 0.42f
                        lastX = change.position.x
                        lastY = change.position.y
                        lastAt = now
                        val dx = change.position.x - startX
                        val dy = change.position.y - startY
                        if (axis == 0 && max(abs(dx), abs(dy)) > 4f * density.density) {
                            axis = if (abs(dx) > abs(dy) * 1.08f) 1 else 2
                        }
                        if (axis == 1) {
                            change.consume()
                            dragging = true
                            center.snapTo(startCenter - dx / stepPx)
                            MotionTrace.emit("coverflow.drag", center.value)
                        }
                    }
                    val dx = lastX - startX
                    val idleFactor = max(0f, 1f - (upAt - lastAt) / 140f)
                    val releaseVelocityPx = velocityX * idleFactor
                    if (axis != 1 && abs(dx) <= 8f * density.density) {
                        // Tap: bring the tapped side card to center.
                        if (axis == 0) {
                            val tapOffset = (down.position.x - size.width / 2f) / stepPx
                            val tapped = modulo(center.value + tapOffset, items.size).roundToInt()
                            if (tapped != center.value.roundToInt()) {
                                val target = center.value +
                                    wrapDelta(tapped - center.value, items.size)
                                startMotion(target, 0f)
                            }
                        }
                        return@awaitEachGesture
                    }
                    if (axis == 2) {
                        center.snapTo(startCenter)
                        dragging = false
                        return@awaitEachGesture
                    }
                    val step = stepPx
                    val changed = center.value - startCenter
                    val speed = abs(releaseVelocityPx) / density.density
                    val hasIntent = abs(changed) >= MaineMotion.CoverflowThreshold ||
                        speed >= MaineMotion.CoverflowVelocityThreshold
                    dragging = false
                    if (!hasIntent) {
                        startMotion(startCenter, (-releaseVelocityPx / step) * 500f)
                        return@awaitEachGesture
                    }
                    val direction = sign(changed)
                        .let { if (it == 0f) sign(-releaseVelocityPx) else it }
                        .let { if (it == 0f) 1f else it }
                    val steps = if (
                        speed >= MaineMotion.CoverflowDoubleThrowVelocity ||
                        abs(changed) >= MaineMotion.CoverflowDoubleThrowDistance
                    ) {
                        2f
                    } else {
                        1f
                    }
                    startMotion(
                        startCenter + direction * steps,
                        (-releaseVelocityPx / step) * 500f,
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Layout(
            content = {
                items.forEachIndexed { index, item ->
                    Box(Modifier) { content(item, index) }
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val stepPx = coverflowStepDp(width / density.density) * density.density
            val placeables = measurables.map {
                it.measure(Constraints.fixed(itemWidth.roundToPx(), itemHeight.roundToPx()))
            }
            layout(width, height) {
                placeables.forEachIndexed { index, placeable ->
                    val offset = wrapDelta(index - center.value, items.size)
                    if (abs(offset) > 3f) return@forEachIndexed
                    val x = ((width - placeable.width) / 2f + offset * stepPx).roundToInt()
                    val y = (height - placeable.height) / 2
                    placeable.placeWithLayer(
                        x = x,
                        y = y,
                        zIndex = coverflowZIndex(offset).toFloat(),
                    ) {
                        rotationY = coverflowRotation(offset)
                        cameraDistance = 720f * density.density
                        val scale = coverflowScale(offset)
                        scaleX = scale
                        scaleY = scale
                        alpha = coverflowAlpha(offset)
                        transformOrigin = TransformOrigin.Center
                    }
                }
            }
        }
    }
}
