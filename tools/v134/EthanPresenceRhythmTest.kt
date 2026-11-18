package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Test

class EthanPresenceRhythmTest {
    private val word = AvatarMouthFrame(
        viseme = AvatarViseme.A,
        weight = .8f,
        quality = LipSyncQuality.TEXT_RANGE_APPROXIMATION,
        cueSequence = 1,
        cueAtMs = 1000L,
    )

    @Test fun startsNeutralUntilRealVoiceRangeArrives() {
        val rhythm = EthanPresenceRhythm()
        assertEquals(1f, rhythm.speed(AvatarMode.IDLE, word, 1010L), .001f)
        assertEquals(1f, rhythm.speed(AvatarMode.SPEAKING, AvatarMouthFrame(), 1010L), .001f)
        assertEquals(1.055f, rhythm.speed(AvatarMode.SPEAKING, word, 1010L), .001f)
        assertEquals(1.015f, rhythm.speed(AvatarMode.SPEAKING, word, 1280L), .001f)
        assertEquals(.96f, rhythm.speed(AvatarMode.SPEAKING, word, 2500L), .001f)
    }

    @Test fun oldOrFutureCuesNeverInventSpeakingGestures() {
        val rhythm = EthanPresenceRhythm()
        assertEquals(1f, rhythm.speed(AvatarMode.SPEAKING, word, 2000L), .001f)
        assertEquals(1f, rhythm.speed(AvatarMode.SPEAKING, word, 500L), .001f)
        assertEquals(1f, rhythm.speed(AvatarMode.LISTENING, word, 1100L), .001f)
        assertEquals(1f, rhythm.speed(AvatarMode.THINKING, word, 1100L), .001f)
    }

    @Test fun stoppingVoiceClearsPreviousTurnCadence() {
        val rhythm = EthanPresenceRhythm()
        rhythm.speed(AvatarMode.SPEAKING, word, 1020L)
        assertEquals(1f, rhythm.speed(AvatarMode.IDLE, word, 1030L), .001f)
        assertEquals(1f, rhythm.speed(AvatarMode.SPEAKING, AvatarMouthFrame(), 1040L), .001f)
        val next = word.copy(cueSequence = 2, cueAtMs = 2000L)
        assertEquals(1.055f, rhythm.speed(AvatarMode.SPEAKING, next, 2020L), .001f)
    }
}
