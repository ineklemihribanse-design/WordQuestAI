package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq187SpokenReplyTest {
    @Test fun turkishExplanationsAreNotRead() {
        val written="Are you busy right now? (Şu an meşgul müsün?)"
        val spoken=Wq187SpokenReply.prepare(written,ConversationLanguage.English)
        assertTrue(spoken.startsWith("Are you busy right now?"))
        assertFalse(spoken.contains("Şu an"))
        assertTrue(spoken.contains("Turkish meaning in parentheses"))
    }
    @Test fun multiParenthesisAreFilteredEvenWhenNested() {
        val spoken=Wq187SpokenReply.prepare(
            "Hello (merhaba (günlük kullanım)) friend (arkadaşım)!",
            ConversationLanguage.English)
        assertFalse(spoken.contains("merhaba"))
        assertFalse(spoken.contains("arkadaşım"))
        assertTrue(spoken.contains("friend"))
        assertEquals(1,Regex("Turkish meaning").findAll(spoken).count())
    }
    @Test fun normalConversationStaysNatural() {
        val text="That's a lovely idea. What happened next?"
        assertEquals(text,Wq187SpokenReply.prepare(text,ConversationLanguage.English))
        assertEquals("",Wq187SpokenReply.prepare("(Açıklama)",ConversationLanguage.Turkish))
    }
}
