package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Teacher2DAnimatorTest {
    @Test fun speaksVisiblyEvenWithoutTtsWordRangeCallbacks() {
        val motions = (0L..15L).map { slot ->
            Teacher2DAnimator.mouthIndex(
                AvatarMode.SPEAKING, AvatarMouthFrame(),
                1000L + slot * 125L, 1000L,
            )
        }
        assertTrue("No-range Android TTS must not leave a fixed photo mouth",
            motions.distinct().size >= 4)
        assertTrue(motions.count { it != 0 } >= 9)
        assertEquals(0, Teacher2DAnimator.mouthIndex(
            AvatarMode.IDLE, AvatarMouthFrame(), 1700L, 1000L,
        ))
        assertEquals(0, Teacher2DAnimator.mouthIndex(
            AvatarMode.LISTENING, AvatarMouthFrame(), 1700L, 1000L,
        ))
        assertEquals(0, Teacher2DAnimator.mouthIndex(
            AvatarMode.SPEAKING, AvatarMouthFrame(), 1700L, 0L,
        ))
    }

    @Test fun authenticAndroidWordCueOverridesApproximation() {
        val cue = AvatarMouthFrame(
            viseme = AvatarViseme.O,
            weight = .7f,
            quality = LipSyncQuality.TEXT_RANGE_APPROXIMATION,
            cueSequence = 8L,
            cueAtMs = 1170L,
        )
        assertEquals(4, Teacher2DAnimator.mouthIndex(
            AvatarMode.SPEAKING, cue, 1250L, 1000L,
        ))
        assertEquals(0, Teacher2DAnimator.mouthIndex(
            AvatarMode.IDLE, cue, 1250L, 1000L,
        ))
        assertTrue(Teacher2DAnimator.mouthIndex(
            AvatarMode.SPEAKING, cue, 2200L, 1000L,
        ) in 0..6)
    }

    @Test fun realLidFramesBlinkAndReturnOpen() {
        val poses = (0L..4400L step 25).map {
            Teacher2DAnimator.blinkIndex(it)
        }.toSet()
        assertEquals(setOf(0, 1, 2), poses)
    }

    @Test fun armMovementIsEasedAndOnlyWhenSpeaking() {
        val gest = Teacher2DAnimator.gesture(AvatarMode.SPEAKING, 2300L, 1000L)
        assertTrue(gest.pose in 1..3)
        assertTrue(gest.alpha > .1f)
        assertTrue(gest.alpha <= .88f)
        assertEquals(0f, Teacher2DAnimator.gesture(
            AvatarMode.IDLE, 2300L, 1000L,
        ).alpha, 0f)
        assertEquals(0f, Teacher2DAnimator.gesture(
            AvatarMode.SPEAKING, 2300L, 0L,
        ).alpha, 0f)
    }

    @Test fun headCannotTiltExcessivelyLikeArchived3dEthan() {
        for (t in 1L..10_000L step 170L) {
            val angle = Teacher2DAnimator.headTiltDegrees(
                AvatarMode.SPEAKING, t,
            )
            assertTrue(angle in -1.11f..1.11f)
        }
    }
}
