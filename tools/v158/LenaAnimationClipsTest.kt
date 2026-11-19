package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class LenaAnimationClipsTest {
    private val clips = setOf("01_Idle_Natural_Breathing", "03_Talking_Hand_Gestures", "04_Listening_Nod")
    @Test fun usesRealSpeechAndListenClips() {
        assertEquals("03_Talking_Hand_Gestures", LenaAnimationClips.select(AvatarMode.SPEAKING, clips))
        assertEquals("04_Listening_Nod", LenaAnimationClips.select(AvatarMode.LISTENING, clips))
    }
    @Test fun fallsBackToIdleInsteadOfUsingUnknownAnimation() {
        assertEquals("01_Idle_Natural_Breathing", LenaAnimationClips.select(AvatarMode.THINKING, clips))
        assertNull(LenaAnimationClips.select(AvatarMode.GREETING, emptySet()))
    }
}
