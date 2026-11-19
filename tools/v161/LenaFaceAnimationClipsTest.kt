package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class LenaFaceAnimationClipsTest {
    private val actual = setOf("Lena_V161_Face_IdleBlink", "Lena_V161_Face_TalkBlink")

    @Test fun speakingUsesRealMouthAndBlinkMorphAnimation() {
        assertEquals("Lena_V161_Face_TalkBlink",
            LenaFaceAnimationClips.select(AvatarMode.SPEAKING, actual))
    }

    @Test fun idleAndListeningKeepMouthNeutralAndAllowBlinking() {
        for (mode in listOf(AvatarMode.IDLE, AvatarMode.THINKING,
                AvatarMode.LISTENING, AvatarMode.ERROR)) {
            assertEquals("Lena_V161_Face_IdleBlink",
                LenaFaceAnimationClips.select(mode, actual))
        }
    }

    @Test fun missingAnimationFailsSafelyWithoutFakeFaceMovement() {
        assertNull(LenaFaceAnimationClips.select(AvatarMode.SPEAKING, emptySet()))
    }
}
