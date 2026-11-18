package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Teacher2D100TimelineTest {
    @Test fun everyStateUsesValidAtlasIndex() {
        for (mode in AvatarMode.entries) {
            for (now in 1_000L..5_000L step 49L) {
                val result = Teacher2D100Timeline.select(
                    mode, AvatarMouthFrame(), now, 1_000L)
                assertTrue("$mode at $now = $result", result in 0..99)
            }
        }
    }

    @Test fun idleBlinkAndListeningNeverUseSpeakingVisemes() {
        assertEquals(0, Teacher2D100Timeline.select(
            AvatarMode.IDLE, AvatarMouthFrame(), 0L, 0L))
        assertTrue(Teacher2D100Timeline.select(
            AvatarMode.LISTENING, AvatarMouthFrame(), 3_000L, 0L) in 70..79)
        assertTrue(Teacher2D100Timeline.select(
            AvatarMode.THINKING, AvatarMouthFrame(), 3_000L, 0L) in 80..89)
    }

    @Test fun ttsRangeChoosesMatchingVisemeWithoutSeparateFaceLayers() {
        val cue = AvatarMouthFrame(viseme = AvatarViseme.O, weight = .8f,
            quality = LipSyncQuality.TEXT_RANGE_APPROXIMATION, cueAtMs = 2_000L)
        assertTrue(Teacher2D100Timeline.select(
            AvatarMode.SPEAKING, cue, 2_070L, 1_000L) in 40..49)
        assertTrue(Teacher2D100Timeline.select(
            AvatarMode.IDLE, cue, 2_070L, 0L) in 0..9)
    }

    @Test fun noTtsRangeStillAnimatesOnlyDuringRealPlayback() {
        assertTrue(Teacher2D100Timeline.select(
            AvatarMode.SPEAKING, AvatarMouthFrame(), 6_000L, 5_000L) in 10..69)
        assertTrue(Teacher2D100Timeline.select(
            AvatarMode.SPEAKING, AvatarMouthFrame(), 6_000L, 0L) in 0..9)
    }
}