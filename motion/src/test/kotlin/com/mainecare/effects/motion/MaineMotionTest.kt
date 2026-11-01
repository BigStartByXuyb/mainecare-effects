package com.mainecare.effects.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaineMotionTest {
    @Test
    fun cardDeckGeometryMatchesWebReference() {
        assertEquals(1f, cardDeckScale(0f), 0.0001f)
        assertEquals(0.97f, cardDeckScale(1f), 0.0001f)
        assertEquals(0.94f, cardDeckScale(2f), 0.0001f)
        assertEquals(0f, cardDeckDepth(0f), 0.0001f)
        assertEquals(-48f, cardDeckDepth(1f), 0.0001f)
        assertEquals(-90f, cardDeckDepth(2f), 0.0001f)
        assertEquals(1f, cardDeckAlpha(0f), 0.0001f)
        assertEquals(0.13f, cardDeckAlpha(1f), 0.0001f)
        assertEquals(0f, cardDeckAlpha(2f), 0.0001f)
    }

    @Test
    fun coverflowGeometryMatchesWebReference() {
        assertEquals(1f, coverflowScale(0f), 0.0001f)
        assertEquals(0.82f, coverflowScale(1f), 0.0001f)
        assertEquals(0.68f, coverflowScale(2f), 0.0001f)
        assertEquals(0f, coverflowDepth(0f), 0.0001f)
        assertEquals(-44f, coverflowDepth(1f), 0.0001f)
        assertEquals(0f, coverflowRotation(0f), 0.0001f)
        assertEquals(-9f, coverflowRotation(1f), 0.0001f)
        assertEquals(16f, coverflowRotation(-2f), 0.0001f)
        assertEquals(1f, coverflowAlpha(0f), 0.0001f)
        assertEquals(0.62f, coverflowAlpha(1f), 0.0001f)
        assertEquals(0.18f, coverflowAlpha(2.2f), 0.0001f)
    }

    @Test
    fun wrapDeltaWrapsAcrossEdges() {
        assertEquals(0f, wrapDelta(0f, 5), 0.0001f)
        assertEquals(2f, wrapDelta(2f, 5), 0.0001f)
        assertEquals(-2f, wrapDelta(3f, 5), 0.0001f)
        assertEquals(-1f, wrapDelta(4f, 5), 0.0001f)
        assertEquals(2f, wrapDelta(7f, 5), 0.0001f)
        assertEquals(0f, wrapDelta(7f, 0), 0.0001f)
    }

    @Test
    fun stackGeometryMatchesWebReference() {
        assertEquals(8f, stackRotation(100f, 100f), 0.0001f)
        assertEquals(-8f, stackRotation(-100f, 100f), 0.0001f)
        assertEquals(2f, stackRotation(25f, 100f), 0.0001f)
        assertEquals(0.5f, stackProgress(11f, 100f, true), 0.0001f)
        assertEquals(0f, stackProgress(11f, 100f, false), 0.0001f)
        assertEquals(2.6f, stackEffectiveDx(10f, false), 0.0001f)
        assertEquals(10f, stackEffectiveDx(10f, true), 0.0001f)
        assertEquals(4f, stackIncomingTranslateY(0.5f), 0.0001f)
        assertEquals(0.98f, stackIncomingScale(0.5f), 0.0001f)
        assertEquals(0.37f, stackIncomingAlpha(0.5f), 0.0001f)
        assertTrue(stackHasIntent(23f, 0f, 100f))
        assertTrue(stackHasIntent(0f, 0.45f, 100f))
        assertFalse(stackHasIntent(10f, 0.2f, 100f))
        assertEquals(1f, stackThrowDirection(-30f, -0.1f), 0f)
        assertEquals(-1f, stackThrowDirection(30f, 0.1f), 0f)
        assertEquals(1f, stackThrowDirection(0f, 0f), 0f)
    }

    @Test
    fun springStepMatchesSemiImplicitEuler() {
        // target 10, start 0, velocity 0, dt 16 ms, stiffness 220, damping 27.
        val first = springStep(
            position = 0f,
            velocity = 0f,
            target = 10f,
            dtSeconds = 0.016f,
            stiffness = 220f,
            damping = 27f,
            elapsedMs = 16,
            maxDurationMs = 760,
        )
        // v = 10*220*0.016 = 35.2; p = 35.2*0.016 = 0.5632
        assertEquals(35.2f, first.velocity, 0.001f)
        assertEquals(0.5632f, first.position, 0.001f)
        assertFalse(first.done)

        val settled = springStep(
            position = 0.001f,
            velocity = 0.001f,
            target = 0f,
            dtSeconds = 0.016f,
            stiffness = 220f,
            damping = 27f,
            elapsedMs = 100,
            maxDurationMs = 760,
        )
        assertTrue(settled.done)
        assertEquals(0f, settled.position, 0f)
    }

    @Test
    fun springMatchesWebReferenceSequence() {
        // Samples produced by src/shared/motion.ts springStep with target 10,
        // stiffness 220, damping 27 at 60 Hz.
        val expected = listOf(
            0.611114f to 36.666740f,
            1.520993f to 54.592660f,
            2.539590f to 61.115668f,
            3.555733f to 60.968456f,
            4.508428f to 57.161624f,
            5.368007f to 51.574645f,
            6.123842f to 45.350016f,
            6.776428f to 39.155074f,
            7.332347f to 33.355042f,
            7.801125f to 28.126658f,
            8.193329f to 23.532193f,
            8.519449f to 19.567158f,
            8.789293f to 16.190616f,
            9.011695f to 13.344091f,
            9.194413f to 10.963029f,
            9.344138f to 8.983481f,
            9.466567f to 7.345739f,
            9.566502f to 5.996075f,
            9.647958f to 4.887332f,
            9.714272f to 3.978853f,
        )
        var position = 0f
        var velocity = 0f
        expected.forEach { (expectedPosition, expectedVelocity) ->
            val sample = springStep(
                position = position,
                velocity = velocity,
                target = 10f,
                dtSeconds = 0.0166667f,
                stiffness = 220f,
                damping = 27f,
                elapsedMs = 0,
                maxDurationMs = 760,
            )
            position = sample.position
            velocity = sample.velocity
            assertEquals(expectedPosition, position, 0.0001f)
            assertEquals(expectedVelocity, velocity, 0.0001f)
        }
    }

    @Test
    fun springExpiresAtMaxDuration() {
        val expired = springStep(
            position = 0f,
            velocity = 0f,
            target = 100f,
            dtSeconds = 0.016f,
            stiffness = 1f,
            damping = 0f,
            elapsedMs = 761,
            maxDurationMs = 760,
        )
        assertTrue(expired.done)
        assertEquals(100f, expired.position, 0f)
    }
}
