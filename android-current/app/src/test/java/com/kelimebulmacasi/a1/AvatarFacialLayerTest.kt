package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

/** Pure JVM tests. Runtime 3D/Filament appearance still requires a device. */
class AvatarFacialLayerTest {
    private fun cue(
        viseme: AvatarViseme = AvatarViseme.A,
        atMs: Long = 950L,
    ) = AvatarMouthFrame(
        viseme = viseme,
        weight = 1f,
        quality = LipSyncQuality.TEXT_RANGE_APPROXIMATION,
        cueSequence = 1L,
        cueAtMs = atMs
    )

    @Test fun idleFaceIsNeutralWithoutUnpromptedMouthMovements() {
        val mixer = AvatarFacialLayer()
        assertEquals("Teacher_Viseme_NEUTRAL",
            mixer.frame("Elif",AvatarMode.IDLE,AvatarMouthFrame(),1000L))
    }

    @Test fun realTtsWordCueMovesMouthForBriefWindowThenReturnsToNeutral() {
        val mixer=AvatarFacialLayer()
        assertEquals("Teacher_Viseme_A",
            mixer.frame("Elif",AvatarMode.SPEAKING,cue(atMs=950),1000L))
        assertEquals("Teacher_Viseme_NEUTRAL",
            mixer.frame("Elif",AvatarMode.SPEAKING,cue(atMs=950),1150L))
    }

    @Test fun canceledOrStoppedSpeechNeverKeepsMouthOpen() {
        val mixer=AvatarFacialLayer()
        val a=cue(atMs=1000)
        assertEquals("Teacher_Viseme_A",
            mixer.frame("Elif",AvatarMode.SPEAKING,a,1050L))
        assertEquals("Teacher_Viseme_NEUTRAL",
            mixer.frame("Elif",AvatarMode.IDLE,a,1060L))
        assertEquals("Teacher_Viseme_NEUTRAL",
            mixer.frame("Elif",AvatarMode.LISTENING,a,1061L))
    }

    @Test fun blinkComposesWithCurrentVowelInsteadOfOverwritingIt() {
        val mixer=AvatarFacialLayer()
        mixer.frame("Elif",AvatarMode.IDLE,AvatarMouthFrame(),1000L)
        val blinkTime=5150L  // Elif cycle 4650, seed 500 -> phase 0
        assertEquals("Teacher_Viseme_U_Blink",
            mixer.frame("Elif",AvatarMode.SPEAKING,cue(AvatarViseme.U,blinkTime-25L),blinkTime))
        assertEquals("Teacher_Viseme_NEUTRAL_Blink",
            mixer.frame("Elif",AvatarMode.LISTENING,cue(AvatarViseme.U,blinkTime-25L),blinkTime))
    }

    @Test fun blinkEndsWithoutResettingEntireBodyAnimation() {
        val mixer=AvatarFacialLayer()
        mixer.frame("Elif",AvatarMode.IDLE,AvatarMouthFrame(),1000L)
        assertEquals("Teacher_Viseme_NEUTRAL_Blink",
            mixer.frame("Elif",AvatarMode.IDLE,AvatarMouthFrame(),5150L))
        assertEquals("Teacher_Viseme_NEUTRAL",
            mixer.frame("Elif",AvatarMode.IDLE,AvatarMouthFrame(),5500L))
    }

    @Test fun actorsHaveDistinctBlinkTimingAndSwitchResetsFaceClock() {
        val mixer=AvatarFacialLayer()
        mixer.frame("Elif",AvatarMode.IDLE,AvatarMouthFrame(),1000L)
        assertEquals("Teacher_Viseme_NEUTRAL",
            mixer.frame("Deniz",AvatarMode.IDLE,AvatarMouthFrame(),6000L))
        assertEquals("Teacher_Viseme_NEUTRAL_Blink",
            mixer.frame("Deniz",AvatarMode.IDLE,AvatarMouthFrame(),9200L))
    }

    @Test fun errorStateDoesNotBlinkAndPastCueCannotAnimate() {
        val mixer=AvatarFacialLayer()
        assertEquals("Teacher_Viseme_NEUTRAL",
            mixer.frame("Elif",AvatarMode.ERROR,cue(atMs=950),1000L))
        assertEquals("Teacher_Viseme_NEUTRAL",
            mixer.frame("Elif",AvatarMode.SPEAKING,cue(atMs=1800),1000L))
    }

    @Test fun realCueTimestampIsPreservedFromLipSyncReducer() {
        val lips=AvatarLipSyncController()
        lips.start(17L)
        assertTrue(lips.push(17L,AvatarViseme.O,123456L,LipSyncQuality.TEXT_RANGE_APPROXIMATION))
        assertEquals(123456L,lips.frame(123456L).cueAtMs)
        assertEquals(AvatarViseme.O,lips.frame(123458L).viseme)
        lips.stop()
        assertEquals(0L,lips.frame(123500L).cueAtMs)
    }
}
