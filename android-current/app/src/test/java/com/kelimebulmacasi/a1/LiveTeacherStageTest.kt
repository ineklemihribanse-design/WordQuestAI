package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class LiveTeacherStageTest {
    @Test fun offlineStageHasOriginalThreeDimensionalWebGlAndNoNetworkCalls() {
        val html = RuntimeEnvironment.getApplication().assets.open("teacher_3d_stage.html")
            .bufferedReader().use { it.readText() }
        assertTrue(html.contains("getContext('webgl'"))
        assertTrue(html.contains("requestAnimationFrame(frame)"))
        assertTrue(html.contains("window.teacherState"))
        assertTrue(html.contains("'Elif'"))
        assertTrue(html.contains("Elif:"))
        assertTrue(html.contains("Deniz:"))
        assertTrue(html.contains("Mert:"))
        assertTrue(html.contains("connect-src 'none'"))
        assertFalse(html.contains("fetch("))
        assertFalse(html.contains("https://"))
    }

    @Test fun characterAndPersonalityAreSavedOnlyForTheBoundProfile() {
        val context = RuntimeEnvironment.getApplication()
        val p1 = "teacher_3d_profile_one"
        val p2 = "teacher_3d_profile_two"
        context.deleteDatabase("wq_teacher_${stableHash(p1).take(24)}.db")
        context.deleteDatabase("wq_teacher_${stableHash(p2).take(24)}.db")
        TeacherStore(context, p1).use { first ->
            first.setSetting("avatar", TeacherAvatar.Deniz.name)
            first.setSetting("personality", TeacherPersonality.Curious.name)
            assertEquals("Deniz", first.setting("avatar"))
            assertEquals("Curious", first.setting("personality"))
        }
        TeacherStore(context, p2).use { second ->
            assertEquals("", second.setting("avatar"))
            assertEquals("", second.setting("personality"))
        }
    }

    @Test fun teachingStylesHaveDistinctUserFacingTurkishChoices() {
        assertEquals(3, TeacherPersonality.entries.size)
        assertTrue(TeacherPersonality.entries.map { it.label }.containsAll(listOf("Sabırlı", "Sohbetçi", "Disiplinli")))
        assertEquals(3, TeacherAvatar.entries.size)
    }
}
