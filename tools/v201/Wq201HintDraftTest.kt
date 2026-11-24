package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq201HintDraftTest {
    @Test fun revealedNonLockedLetterIsImmediatelyAuthoritative() {
        val visible = mapOf(0 to 'V', 1 to 'I', 2 to 'S')
        assertEquals(mapOf(0 to 'V', 2 to 'S'),
            Wq201HintDraft.authoritative(visible, setOf(0), mapOf(2 to 'S')))
    }
    @Test fun repeatedHintsKeepUnrevealedDraftPositions() {
        val first = Wq201HintDraft.remap(5, emptyMap(), mapOf(0 to 'V'), "VIS")
        assertEquals("IS", first)
        val second = Wq201HintDraft.remap(5, mapOf(0 to 'V'), mapOf(0 to 'V', 1 to 'I'), first)
        assertEquals("S", second)
        assertEquals("VISIT", Wq190CrosswordHintInput.completedWord(5,
            mapOf(0 to 'V', 1 to 'I'), second + "IT"))
    }
    @Test fun hintOverridesFullDraftAtSubmission() {
        val draft = Wq201HintDraft.remap(5, emptyMap(), mapOf(0 to 'V'), "XISIT")
        assertEquals("VISIT", Wq190CrosswordHintInput.completedWord(5, mapOf(0 to 'V'), draft))
    }
    @Test fun clearDraftDoesNotEraseHintAndWrongLettersStayEditable() {
        val fixed = Wq201HintDraft.authoritative(mapOf(0 to 'V', 1 to 'X'), emptySet(), mapOf(0 to 'V'))
        assertEquals(mapOf(0 to 'V'), fixed)
        assertTrue(Wq190CrosswordHintInput.typedPositions(5, fixed, "").isEmpty())
        assertEquals("VISIT", Wq190CrosswordHintInput.completedWord(5, fixed, "ISIT"))
    }
    @Test fun revealDiffDoesNotMarkUnrelatedWrongLetters() {
        assertEquals(mapOf(1 to 'I'), Wq201HintDraft.newlyRevealed(
            mapOf(0 to 'X'), mapOf(0 to 'X', 1 to 'I')))
        assertTrue(Wq201HintDraft.authoritative(mapOf(1 to 'X'), emptySet(), mapOf(1 to 'I')).isEmpty())
    }
}
