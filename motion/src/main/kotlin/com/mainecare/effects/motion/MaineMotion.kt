package com.mainecare.effects.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Motion tokens extracted from the MaineCare feeding v18 prototype, the same values
 * used by the React reference implementation.
 *
 * Units follow the web source: distances are dp at the 390dp reference viewport,
 * velocities are dp/ms where the web used px/ms, and spring velocities are in
 * "index units per second" for the coverflow.
 */
object MaineMotion {
    // Coverflow: spring and gesture thresholds.
    const val CoverflowStiffness = 220f
    const val CoverflowDamping = 27f
    const val CoverflowMaxDurationMs = 760L
    const val CoverflowMaxVelocity = 4.4f
    const val CoverflowThreshold = 0.22f
    const val CoverflowVelocityThreshold = 0.34f
    const val CoverflowDoubleThrowVelocity = 0.72f
    const val CoverflowDoubleThrowDistance = 1.15f
    const val CoverflowCardStepNarrowDp = 118f
    const val CoverflowCardStepWideDp = 132f

    // Analysis card stack.
    const val StackClickThresholdDp = 8f
    const val StackWidthThreshold = 0.22f
    const val StackVelocityThreshold = 0.45f
    const val StackBoundaryDamping = 0.26f
    const val StackMaxRotationDeg = 8f
    const val StackThrowRotationDeg = 10f
    const val StackThrowDurationMs = 400L
    const val StackPostThrowPauseMs = 60L
    const val StackReboundDurationMs = 320L
    const val StackIncomingDurationMs = 320L
    const val StackStiffness = 200f
    const val StackDamping = 26f

    // Recipe drum (vertical wheel), kept for parity with v18.
    const val DrumItemStepDp = 46f
    const val DrumWheelSettleDelayMs = 140L
    const val DrumProjectionMs = 110L
    const val DrumMaxThrowVelocity = 5.2f
    const val DrumStiffness = 185f
    const val DrumDamping = 25f
    const val DrumMaxDurationMs = 720L

    val ThrowEasing = CubicBezierEasing(0.18f, 0.75f, 0.22f, 1f)
}

fun modulo(value: Float, length: Int): Float {
    if (length <= 0) return 0f
    return ((value % length) + length) % length
}

fun wrapDelta(value: Float, length: Int): Float {
    if (length <= 1) return 0f
    return modulo(value + length / 2f, length) - length / 2f
}

fun cardDeckScale(distance: Float): Float =
    1f -
        min(distance, 1f) * 0.03f -
        min(max(distance - 1f, 0f), 1f) * 0.03f

fun cardDeckDepth(distance: Float): Float =
    -min(distance, 1f) * 48f - min(max(distance - 1f, 0f), 1f) * 42f

fun cardDeckAlpha(distance: Float): Float = max(0f, 1f - distance * 0.87f)

fun cardDeckZIndex(distance: Float): Int = max(1, 30 - (distance * 10f).roundToInt())

fun coverflowScale(offset: Float): Float =
    max(0.68f, 1f - min(abs(offset), 1.8f) * 0.18f)

fun coverflowDepth(offset: Float): Float = -min(abs(offset), 1.8f) * 44f

fun coverflowRotation(offset: Float): Float =
    (-offset * 9f).coerceIn(-16f, 16f)

fun coverflowAlpha(offset: Float): Float =
    max(0.18f, 1f - min(abs(offset), 2.2f) * 0.38f)

fun coverflowZIndex(offset: Float): Int =
    (20f - min(abs(offset), 3f) * 8f).roundToInt()

fun coverflowStepDp(viewportWidthDp: Float): Float =
    if (viewportWidthDp >= 400f) {
        MaineMotion.CoverflowCardStepWideDp
    } else {
        MaineMotion.CoverflowCardStepNarrowDp
    }

fun stackRotation(rawDx: Float, widthPx: Float): Float =
    (rawDx / max(1f, widthPx) * MaineMotion.StackMaxRotationDeg)
        .coerceIn(-MaineMotion.StackMaxRotationDeg, MaineMotion.StackMaxRotationDeg)

fun stackProgress(rawDx: Float, widthPx: Float, hasIncoming: Boolean): Float {
    if (!hasIncoming) return 0f
    return (abs(rawDx) / max(1f, widthPx * MaineMotion.StackWidthThreshold))
        .coerceIn(0f, 1f)
}

fun stackEffectiveDx(rawDx: Float, hasIncoming: Boolean): Float =
    if (hasIncoming) rawDx else rawDx * MaineMotion.StackBoundaryDamping

fun stackIncomingTranslateY(progress: Float): Float = 8f * (1f - progress)

fun stackIncomingScale(progress: Float): Float = 0.96f + 0.04f * progress

fun stackIncomingAlpha(progress: Float): Float = 0.12f + 0.50f * progress

fun stackHasIntent(dxPx: Float, velocityPxPerMs: Float, widthPx: Float): Boolean =
    abs(dxPx) >= widthPx * MaineMotion.StackWidthThreshold ||
        abs(velocityPxPerMs) >= MaineMotion.StackVelocityThreshold

/** Web helper: a leftward movement advances to the next card (direction 1). */
fun stackThrowDirection(dx: Float, velocityX: Float): Float {
    val movement = sign(if (dx != 0f) dx else velocityX).let { if (it == 0f) -1f else it }
    return if (movement < 0f) 1f else -1f
}

data class SpringSample(val position: Float, val velocity: Float, val done: Boolean)

/**
 * One semi-implicit Euler step, matching `springStep` in src/shared/motion.ts
 * exactly: dt is clamped to 1..32 ms, settlement is checked against the
 * pre-update displacement, and the state snaps to target when settled or expired.
 */
@Suppress("LongParameterList")
fun springStep(
    position: Float,
    velocity: Float,
    target: Float,
    dtSeconds: Float,
    stiffness: Float,
    damping: Float,
    elapsedMs: Long,
    maxDurationMs: Long,
    settlePosition: Float = 0.0015f,
    settleVelocity: Float = 0.018f,
): SpringSample {
    val dt = dtSeconds.coerceIn(0.001f, 0.032f)
    val displacement = target - position
    val nextVelocity = velocity + (displacement * stiffness - velocity * damping) * dt
    val nextPosition = position + nextVelocity * dt
    val settled = abs(displacement) < settlePosition && abs(nextVelocity) < settleVelocity
    val expired = elapsedMs > maxDurationMs
    return if (settled || expired) {
        SpringSample(position = target, velocity = 0f, done = true)
    } else {
        SpringSample(position = nextPosition, velocity = nextVelocity, done = false)
    }
}

/**
 * Frame-clock-driven spring whose samples match the web prototype frame by frame.
 * It is intentionally not a Compose AnimationSpec: the prototype integrates its own
 * Euler spring, and the gate compares positions at the same vsync timestamps.
 */
@Stable
class JsSpringAnimatable(initialValue: Float) {
    var value by mutableFloatStateOf(initialValue)
        private set

    var velocity: Float = 0f
        private set

    fun snapTo(target: Float) {
        value = target
        velocity = 0f
    }

    suspend fun animateTo(
        target: Float,
        stiffness: Float = MaineMotion.CoverflowStiffness,
        damping: Float = MaineMotion.CoverflowDamping,
        maxDurationMs: Long = MaineMotion.CoverflowMaxDurationMs,
        initialVelocity: Float = velocity,
        maxVelocity: Float = MaineMotion.CoverflowMaxVelocity,
        onFrame: (Float) -> Unit = {},
    ) {
        velocity = initialVelocity.coerceIn(-maxVelocity, maxVelocity)
        val startedAt = withFrameNanos { it }
        var previous = startedAt
        while (true) {
            val now = withFrameNanos { it }
            val elapsedMs = (now - startedAt) / 1_000_000L
            val dtSeconds = (now - previous) / 1_000_000_000f
            previous = now
            val sample = springStep(
                position = value,
                velocity = velocity,
                target = target,
                dtSeconds = dtSeconds,
                stiffness = stiffness,
                damping = damping,
                elapsedMs = elapsedMs,
                maxDurationMs = maxDurationMs,
            )
            value = sample.position
            velocity = sample.velocity
            onFrame(value)
            if (sample.done) return
        }
    }
}

@Composable
fun rememberJsSpring(initialValue: Float): JsSpringAnimatable =
    remember { JsSpringAnimatable(initialValue) }
