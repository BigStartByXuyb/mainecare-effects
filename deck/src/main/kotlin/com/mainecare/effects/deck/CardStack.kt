package com.mainecare.effects.deck

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mainecare.effects.motion.JsSpringAnimatable
import com.mainecare.effects.motion.MaineMotion
import com.mainecare.effects.motion.MotionTrace
import com.mainecare.effects.motion.stackEffectiveDx
import com.mainecare.effects.motion.stackHasIntent
import com.mainecare.effects.motion.stackIncomingAlpha
import com.mainecare.effects.motion.stackIncomingScale
import com.mainecare.effects.motion.stackIncomingTranslateY
import com.mainecare.effects.motion.stackProgress
import com.mainecare.effects.motion.stackRotation
import com.mainecare.effects.motion.stackThrowDirection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

/**
 * Analysis card stack: drag the top card horizontally, throw it past 22% of the card
 * width (or 0.45 dp/ms) to advance, otherwise spring it back. The incoming card rises
 * from 8dp/.96/0.12 to 0dp/1/0.62 while the top card follows the pointer.
 *
 * The two back cards reproduce the v21 .analysis-back-card insets (8/5/1 at 74%, and
 * 15/10/7 at 40%).
 */
@Composable
fun CardStack(
    count: Int,
    modifier: Modifier = Modifier,
    initialIndex: Int = 0,
    onIndexChanged: (Int) -> Unit = {},
    backCardColor: Color = Color(0xE6080A0D),
    backCardBorderColor: Color = Color(0x1FEBEFF4),
    cornerRadius: Dp = 21.dp,
    content: @Composable (Int) -> Unit,
) {
    if (count <= 0) return
    val density = LocalDensity.current
    val densityValue = density.density
    val scope = rememberCoroutineScope()
    val currentOnIndexChanged by rememberUpdatedState(onIndexChanged)
    var index by remember { mutableIntStateOf(initialIndex.coerceIn(0, count - 1)) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragRotation by remember { mutableFloatStateOf(0f) }
    var dragProgress by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var animating by remember { mutableStateOf(false) }
    var throwActive by remember { mutableStateOf(false) }
    var direction by remember { mutableIntStateOf(1) }
    val throwX = remember { Animatable(0f) }
    val throwRotation = remember { Animatable(0f) }
    val throwAlpha = remember { Animatable(1f) }
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    LaunchedEffect(initialIndex) {
        index = initialIndex.coerceIn(0, count - 1)
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            floatArrayOf(
                dragX,
                dragRotation,
                dragProgress,
                throwX.value,
                throwRotation.value,
                throwAlpha.value,
            )
        }.collect { values ->
            MotionTrace.emit("stack.state", *values)
        }
    }

    Box(
        modifier = modifier.pointerInput(count, index, density.density) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val startX = down.position.x
                val startY = down.position.y
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
                    if (axis == 0 && max(abs(dx), abs(dy)) > 4f * densityValue) {
                        axis = if (abs(dx) > abs(dy) * 1.08f) 1 else 2
                    }
                    if (axis == 1) {
                        change.consume()
                        val widthDp = size.width / densityValue
                        val dxDp = dx / densityValue
                        val candidate = stackThrowDirection(dxDp, 0f).toInt()
                        val hasIncoming = index + candidate in 0 until count
                        dragging = true
                        direction = candidate
                        dragX = stackEffectiveDx(dxDp, hasIncoming)
                        dragRotation = stackRotation(dxDp, widthDp)
                        dragProgress = stackProgress(dxDp, widthDp, hasIncoming)
                    }
                }

                if (axis != 1) {
                    dragging = false
                    dragX = 0f
                    dragRotation = 0f
                    dragProgress = 0f
                    return@awaitEachGesture
                }

                val widthDp = size.width / densityValue
                val dxDp = (lastX - startX) / densityValue
                val idleFactor = max(0f, 1f - (upAt - lastAt) / 140f)
                val releaseVelocity = (velocityX / densityValue) * idleFactor
                val candidate = stackThrowDirection(dxDp, releaseVelocity).toInt()
                val hasIncoming = index + candidate in 0 until count
                MotionTrace.emit(
                    "stack.release",
                    dxDp,
                    releaseVelocity,
                    if (hasIncoming) 1f else 0f,
                )
                dragging = false

                if (hasIncoming && stackHasIntent(dxDp, releaseVelocity, widthDp)) {
                    direction = candidate
                    throwActive = true
                    animating = true
                    scope.launch {
                        throwX.snapTo(dragX)
                        throwRotation.snapTo(dragRotation)
                        throwAlpha.snapTo(1f)
                        val targetX = -candidate * widthDp * 1.4f
                        val targetRotation = -candidate * MaineMotion.StackThrowRotationDeg
                        val duration = MaineMotion.StackThrowDurationMs.toInt()
                        launch {
                            throwX.animateTo(targetX, tween(duration, easing = MaineMotion.ThrowEasing))
                        }
                        launch {
                            throwRotation.animateTo(targetRotation, tween(duration, easing = MaineMotion.ThrowEasing))
                        }
                        launch {
                            throwAlpha.animateTo(0f, tween(duration, easing = MaineMotion.ThrowEasing))
                        }
                        val incoming = JsSpringAnimatable(dragProgress)
                        launch {
                            incoming.animateTo(
                                target = 1f,
                                stiffness = MaineMotion.StackStiffness,
                                damping = MaineMotion.StackDamping,
                                maxDurationMs = MaineMotion.StackIncomingDurationMs,
                            ) { dragProgress = it }
                        }
                        delay(MaineMotion.StackThrowDurationMs + MaineMotion.StackPostThrowPauseMs)
                        index = (index + candidate).coerceIn(0, count - 1)
                        currentOnIndexChanged(index)
                        dragX = 0f
                        dragRotation = 0f
                        dragProgress = 0f
                        throwX.snapTo(0f)
                        throwRotation.snapTo(0f)
                        throwAlpha.snapTo(1f)
                        throwActive = false
                        animating = false
                    }
                } else {
                    animating = true
                    scope.launch {
                        val xSpring = JsSpringAnimatable(dragX)
                        val rotationSpring = JsSpringAnimatable(dragRotation)
                        val progressSpring = JsSpringAnimatable(dragProgress)
                        launch {
                            xSpring.animateTo(
                                target = 0f,
                                stiffness = MaineMotion.StackStiffness,
                                damping = MaineMotion.StackDamping,
                                maxDurationMs = MaineMotion.StackReboundDurationMs,
                                initialVelocity = releaseVelocity,
                            ) { dragX = it }
                        }
                        launch {
                            rotationSpring.animateTo(
                                target = 0f,
                                stiffness = MaineMotion.StackStiffness,
                                damping = MaineMotion.StackDamping,
                                maxDurationMs = MaineMotion.StackReboundDurationMs,
                            ) { dragRotation = it }
                        }
                        launch {
                            progressSpring.animateTo(
                                target = 0f,
                                stiffness = MaineMotion.StackStiffness,
                                damping = MaineMotion.StackDamping,
                                maxDurationMs = MaineMotion.StackReboundDurationMs,
                            ) { dragProgress = it }
                        }
                        delay(MaineMotion.StackReboundDurationMs)
                        dragX = 0f
                        dragRotation = 0f
                        dragProgress = 0f
                        animating = false
                    }
                }
            }
        },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .padding(start = 15.dp, end = 10.dp, top = 7.dp)
                .clip(shape)
                .background(backCardColor)
                .border(1.dp, backCardBorderColor, shape)
                .graphicsLayer { alpha = 0.40f },
        )
        Box(
            Modifier
                .matchParentSize()
                .padding(start = 8.dp, end = 5.dp, top = 1.dp)
                .clip(shape)
                .background(backCardColor)
                .border(1.dp, backCardBorderColor, shape)
                .graphicsLayer { alpha = 0.74f },
        )

        val incomingIndex = index + direction
        if (incomingIndex in 0 until count) {
            key(incomingIndex) {
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val progress = dragProgress
                            translationY = stackIncomingTranslateY(progress) * densityValue
                            val scale = stackIncomingScale(progress)
                            scaleX = scale
                            scaleY = scale
                            alpha = if (dragging || animating) {
                                stackIncomingAlpha(progress)
                            } else {
                                0f
                            }
                            transformOrigin = TransformOrigin.Center
                        },
                ) {
                    content(incomingIndex)
                }
            }
        }

        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationX = if (throwActive) {
                        throwX.value * densityValue
                    } else {
                        dragX * densityValue
                    }
                    rotationZ = if (throwActive) throwRotation.value else dragRotation
                    alpha = if (throwActive) throwAlpha.value else 1f
                    transformOrigin = TransformOrigin.Center
                },
        ) {
            content(index)
        }
    }
}
