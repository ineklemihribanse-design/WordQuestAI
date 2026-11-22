package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq187LenaSpeechTest {
    @Test fun translatesRemainVisibleButAreNotReadByEnglishVoice() {
        val displayed = "Are you busy right now? (Şu an meşgul müsün?)"
        val spoken = Wq187LenaSpeech.forSpeech(displayed, ConversationLanguage.English)
        assertTrue(spoken.startsWith("Are you busy right now?"))
        assertFalse(spoken.contains("Şu an"))
        assertTrue(spoken.contains("Turkish explanation in parentheses"))
    }
    @Test fun realEnglishParentheticalsAndTurkishModeStayIntact() {
        assertEquals("I work (part-time) now.",
            Wq187LenaSpeech.forSpeech("I work (part-time) now.", ConversationLanguage.English))
        assertEquals("Merhaba (nasılsın?)",
            Wq187LenaSpeech.forSpeech("Merhaba (nasılsın?)", ConversationLanguage.Turkish))
    }
    @Test fun moreThanOneTranslationAddsOnlyOnePointer() {
        val spoken=Wq187LenaSpeech.forSpeech(
            "Hi (Merhaba)! Let's go (Hadi gidelim).",ConversationLanguage.English)
        assertEquals(1,"Turkish explanation".toRegex().findAll(spoken).count())
        assertFalse(spoken.contains("Merhaba"))
    }
}
