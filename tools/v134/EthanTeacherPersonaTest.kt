package com.kelimebulmacasi.a1

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EthanTeacherPersonaTest {
    @Test fun keepsAMemorableButTruthfulTeacherIdentity() {
        val prompt = EthanTeacherPersona.systemPrompt
        assertTrue(prompt.contains("Ethan"))
        assertTrue(prompt.contains("calm, lively"))
        assertTrue(prompt.contains("A1/A2"))
        assertTrue(prompt.contains("C1/C2"))
        assertTrue(prompt.contains("earlier turns"))
        assertTrue(prompt.contains("only when they are actually"))
        assertTrue(prompt.length > 250)
        assertTrue(prompt.length < 6000)
        assertTrue(prompt.contains("currently configured AI"))
        assertTrue(prompt.contains("not a conscious human"))
        assertTrue(prompt.contains("never pretend to remember information from other profiles") ||
            prompt.contains("Never invent prior meetings"))
    }

    @Test fun respectsExistingLessonAndMemoryControls() {
        val prompt = EthanTeacherPersona.systemPrompt
        assertTrue(prompt.contains("response language and lesson mode"))
        assertTrue(prompt.contains("'Bunu hatırla:'"))
        assertTrue(prompt.contains("Do not secretly"))
        assertTrue(prompt.contains("If YOU misunderstood"))
        assertTrue(prompt.contains("mastery"))
        assertTrue(prompt.contains("one useful hint"))
        assertTrue(prompt.contains("observable improvement"))
        assertTrue(prompt.contains("BEHAVIORAL PRESENCE"))
        assertTrue(prompt.contains("EMOTIONAL ATTUNEMENT"))
        assertTrue(prompt.contains("CONTINUITY AND SELF-CORRECTION"))
        assertTrue(prompt.contains("never pretend to be watching"))
        assertTrue(prompt.contains("Never claim to feel emotions"))
        assertTrue(prompt.contains("ask before saving new notes"))
        assertTrue(prompt.length < 6000)
    }
}
