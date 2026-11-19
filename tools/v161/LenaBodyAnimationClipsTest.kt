package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class LenaAnimationClipsTest {
    private val clips = setOf(
        "09_Resting_With_Blinks", "10_Wave_With_Smile",
        "11_Talking_With_Blinks", "12_Listening_With_Blinks",
        "13_Thinking_Softly",
    )

    @Test fun speakingAndListeningPreserveNaturalBlinking() {
        assertEquals("11_Talking_With_Blinks",
            LenaAnimationClips.select(AvatarMode.SPEAKING, clips))
        assertEquals("12_Listening_With_Blinks",
            LenaAnimationClips.select(AvatarMode.LISTENING, clips))
    }

    @Test fun restsAndGreetingsUsePrecomposedAnimations() {
        assertEquals("09_Resting_With_Blinks",
            LenaAnimationClips.select(AvatarMode.IDLE, clips))
        assertEquals("10_Wave_With_Smile",
            LenaAnimationClips.select(AvatarMode.GREETING, clips))
    }

    @Test fun safelyFallsBackIfPreferredClipIsMissing() {
        assertEquals("09_Resting_With_Blinks",
            LenaAnimationClips.select(AvatarMode.THINKING, setOf("09_Resting_With_Blinks")))
        assertNull(LenaAnimationClips.select(AvatarMode.GREETING, emptySet()))
    }
}
