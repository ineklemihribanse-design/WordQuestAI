package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class AvatarMotionCoreTest {
    @Test fun micOnlyEntersListeningAfterLaunchAndReturnsToIdle() {
        val avatar=AvatarStateController("Elif")
        assertEquals(AvatarMode.IDLE,avatar.mode)
        avatar.micStarted()
        assertEquals(AvatarMode.LISTENING,avatar.mode)
        avatar.micCancelled()
        assertEquals(AvatarMode.IDLE,avatar.mode)
    }

    @Test fun thinkingDoesNotPretendAudioAlreadyStarted() {
        val avatar=AvatarStateController("Elif")
        avatar.responsePending()
        assertEquals(AvatarMode.THINKING,avatar.mode)
        avatar.responseAvailable()
        assertEquals(AvatarMode.IDLE,avatar.mode)
        assertNull(avatar.activeAudioId)
    }

    @Test fun speakingRequiresRealPlaybackEventAndStopsImmediately() {
        val avatar=AvatarStateController("Elif")
        avatar.responsePending()
        assertTrue(avatar.audioStarted(912L))
        assertEquals(AvatarMode.SPEAKING,avatar.mode)
        avatar.audioFinished(912L)
        assertEquals(AvatarMode.IDLE,avatar.mode)
        assertNull(avatar.activeAudioId)
    }

    @Test fun staleFinishNeverEndsNextUtterance() {
        val avatar=AvatarStateController("Elif")
        assertTrue(avatar.audioStarted(12L))
        avatar.audioFinished(12L)
        assertTrue(avatar.audioStarted(13L))
        avatar.audioFinished(12L)
        assertEquals(13L,avatar.activeAudioId)
        assertEquals(AvatarMode.SPEAKING,avatar.mode)
    }

    @Test fun avatarChangeRejectsLastActorsDelayedAudioCallback() {
        val avatar=AvatarStateController("Elif")
        assertTrue(avatar.audioStarted(42L))
        avatar.selectActor("Deniz")
        assertFalse(avatar.audioStarted(42L))
        assertEquals(AvatarMode.IDLE,avatar.mode)
        assertTrue(avatar.audioStarted(43L))
    }

    @Test fun playbackIsSuppressedDuringRecording() {
        val avatar=AvatarStateController("Mert")
        avatar.micStarted()
        assertFalse(avatar.audioStarted(5L))
        assertEquals(AvatarMode.LISTENING,avatar.mode)
    }

    @Test fun lipSyncRejectsOldWordsAndResetsToNeutral() {
        val lips=AvatarLipSyncController()
        lips.start(21L)
        assertFalse(lips.push(20L,AvatarViseme.A,100L,LipSyncQuality.TEXT_RANGE_APPROXIMATION))
        assertTrue(lips.push(21L,AvatarViseme.E,100L,LipSyncQuality.TEXT_RANGE_APPROXIMATION))
        assertEquals(AvatarViseme.E,lips.frame(120L).viseme)
        assertTrue(lips.frame(120L).weight>0f)
        assertEquals(0f,lips.frame(1000L).weight,0.001f)
        lips.stop(21L)
        assertEquals(AvatarViseme.NEUTRAL,lips.frame(1100L).viseme)
    }

    @Test fun lipSyncDoesNotMisrepresentTextAsPhonemes() {
        assertEquals(AvatarViseme.MBP,AvatarLipSyncController.fromTextRange("Please"))
        assertEquals(AvatarViseme.A,AvatarLipSyncController.fromTextRange("Ask"))
        val lips=AvatarLipSyncController()
        lips.start(8L)
        assertTrue(lips.push(8L,AvatarViseme.A,200L,LipSyncQuality.TEXT_RANGE_APPROXIMATION))
        assertEquals(LipSyncQuality.TEXT_RANGE_APPROXIMATION,lips.frame(230L).quality)
        assertFalse(lips.push(8L,AvatarViseme.E,199L,LipSyncQuality.TIMED_VISEME))
    }

    @Test fun gestureIsCalmWhileListeningAndMovesOnlyWhenSpeaking() {
        val gestures=AvatarGestureController()
        val idle=gestures.frame(AvatarMode.IDLE,1750L,"Elif")
        assertEquals(AvatarGesture.REST,idle.gesture)
        val speaking=gestures.frame(AvatarMode.SPEAKING,1650L,"Elif")
        assertEquals(AvatarGesture.OPEN_PALM,speaking.gesture)
        val listening=gestures.frame(AvatarMode.LISTENING,2100L,"Deniz")
        assertEquals(AvatarGesture.SMALL_NOD,listening.gesture)
    }

    @Test fun cameraAlwaysHasSafeZoomAndReset() {
        val camera=AvatarCameraController()
        camera.changePreset(AvatarCameraController.Preset.FACE)
        camera.drag(-15f)
        camera.pinch(100000f)
        assertEquals(2.5f,camera.zoom,.001f)
        camera.reset()
        assertEquals(AvatarCameraController.Preset.FULL_BODY,camera.preset)
        assertEquals(1f,camera.zoom,.001f)
        assertEquals(0f,camera.orbitYawDegrees,.001f)
    }

    @Test fun greetingUsesRealModeAndReturnsToIdleAfterOneWave() {
        val avatar = AvatarStateController("Elif")
        avatar.greeting()
        assertEquals(AvatarMode.GREETING, avatar.mode)
        avatar.greetingFinished()
        assertEquals(AvatarMode.IDLE, avatar.mode)
        avatar.micStarted()
        avatar.greeting()
        assertEquals(AvatarMode.LISTENING, avatar.mode)
    }

}
