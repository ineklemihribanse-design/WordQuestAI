package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Wq193EditableDraftTest {
    @Test fun previouslyTypedWrongLettersNeverBecomeProtected() {
        val previousWrongGuess = mapOf(0 to 'S', 1 to 'I', 2 to 'N', 3 to 'G')
        assertEquals(emptyMap<Int, Char>(),
            Wq193EditableDraft.protectedPositions(previousWrongGuess, emptySet()))
        assertEquals("SONG",
            Wq190CrosswordHintInput.completedWord(
                4, Wq193EditableDraft.protectedPositions(previousWrongGuess, emptySet()), "SONG"))
    }

    @Test fun onlyRealCrossingsAreProtectedWhileReplacingWrongAnswer() {
        val previous = mapOf(0 to 'S', 1 to 'O', 2 to 'N', 3 to 'G')
        val protected = Wq193EditableDraft.protectedPositions(previous, setOf(1))
        assertEquals(mapOf(1 to 'O'), protected)
        assertEquals("SONG", Wq190CrosswordHintInput.completedWord(4, protected, "SNG"))
        assertEquals(null, Wq190CrosswordHintInput.completedWord(4, protected, "SI"))
    }

    @Test fun backspaceAndClearAreAlwaysAllowedBeforeSolutionIsLocked() {
        assertEquals("SIN", Wq193EditableDraft.normalizedText("SIN", 4))
        assertEquals("", Wq193EditableDraft.normalizedText("", 4))
        assertEquals("SONG", Wq193EditableDraft.normalizedText("SONG", 4))
        assertTrue(Wq193EditableDraft.needsCorrection(false))
        assertFalse(Wq193EditableDraft.needsCorrection(true))
    }
}
