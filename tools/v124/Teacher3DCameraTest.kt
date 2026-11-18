package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Teacher3DCameraTest {
    @Test fun persistedCharacterIdentifiersKeepForeignDisplayNames() {
        assertEquals("Emma", TeacherAvatar.Elif.label)
        assertEquals("Sophia", TeacherAvatar.Deniz.label)
        assertEquals("Ethan", TeacherAvatar.Mert.label)
    }

    @Test fun touchCameraReturnsToFullBodyFraming() {
        val camera = Teacher3DSettings()
        camera.yaw = 173f
        camera.pitch = 14f
        camera.distance = 2.2f
        camera.reset()
        assertEquals(0f, camera.yaw, 0.01f)
        assertEquals(0f, camera.pitch, 0.01f)
        assertEquals(7.2f, camera.distance, 0.01f)
    }

    @Test fun animationStateAcceptsLiveVoiceAndAvatarSelection() {
        val settings = Teacher3DSettings()
        settings.actor = TeacherAvatar.Mert.name
        settings.speaking = true
        settings.speechRate = 1.2f
        assertTrue(settings.speaking)
        assertEquals("Mert",settings.actor)
    }
}
