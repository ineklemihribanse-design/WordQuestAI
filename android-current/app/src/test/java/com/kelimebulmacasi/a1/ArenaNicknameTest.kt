package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArenaNicknameTest {
    @Test fun supportsTurkishLettersAndSpaces() {
        assertTrue(ArenaNickname.isValid("Çağla Öztürk"))
        assertTrue(ArenaNickname.isValid("İpek_24"))
        assertTrue(ArenaNickname.isValid("Ali-7"))
        assertEquals("Ayşe K.", ArenaNickname.normalize("  Ayşe   K.  "))
    }
    @Test fun forbidsEmptyControlOrOversizeNicknames() {
        assertFalse(ArenaNickname.isValid(""))
        assertFalse(ArenaNickname.isValid("A"))
        assertFalse(ArenaNickname.isValid("Ali\nRakip"))
        assertFalse(ArenaNickname.isValid("/Odadan"))
        assertFalse(ArenaNickname.isValid("x".repeat(25)))
    }
    @Test fun oldRoomsHaveReadableFallbackLabels() {
        assertEquals("Misafir", ArenaNickname.safeDisplay("", "Misafir"))
        assertEquals("Oda sahibi", ArenaNickname.safeDisplay("  ", "Oda sahibi"))
        assertEquals("Ömer", ArenaNickname.safeDisplay("Ömer", "Misafir"))
    }
}
