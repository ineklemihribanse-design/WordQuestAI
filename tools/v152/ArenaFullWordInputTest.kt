package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class ArenaFullWordInputTest {
    @Test fun writesWholeAnswerEvenWhenTheFirstLetterWasRevealed() {
        assertEquals("CLOSE", crosswordFullWordPreview("C....", "CLOSE"))
        assertTrue(crosswordFullWordMatches("C....", "CLOSE", "CLOSE"))
    }

    @Test fun previouslyRevealedCrossingsStayFixedOnTheBoard() {
        assertEquals("HOUSE", crosswordFullWordPreview("H.U.E", "HOUSE"))
        assertTrue(crosswordFullWordMatches("H.U.E", "HOUSE", "HOUSE"))
    }

    @Test fun partialAnswerKeepsPositionWithoutShiftingCells() {
        assertEquals("CL...", crosswordFullWordPreview("C....", "CL"))
        assertFalse(crosswordFullWordReady(5, "CL"))
    }

    @Test fun wrongFirstLetterDoesNotOverrideCrossingOrCountAsCorrect() {
        assertEquals("CLOSE", crosswordFullWordPreview("C....", "XLOSE"))
        assertFalse(crosswordFullWordMatches("C....", "XLOSE", "CLOSE"))
    }

    @Test fun aWrongCompleteWordIsStillAnActualGuess() {
        assertTrue(crosswordFullWordReady(5, "CLOTH"))
        assertFalse(crosswordFullWordMatches("C....", "CLOTH", "CLOSE"))
    }

    @Test fun inputIsCaseInsensitive() {
        assertEquals("CLOSE", crosswordFullWordPreview("C....", "close"))
        assertTrue(crosswordFullWordMatches("C....", "close", "CLOSE"))
    }

    @Test fun noLockedLettersStillAllowsNormalTyping() {
        assertEquals("APPLE", crosswordFullWordPreview(".....", "APPLE"))
        assertTrue(crosswordFullWordMatches(".....", "APPLE", "APPLE"))
    }

    @Test fun midWordLockedLettersAreNeverDoubleCounted() {
        assertEquals("CLOSE", crosswordFullWordPreview("C.O.E", "CLOSE"))
        assertFalse(crosswordFullWordMatches("C.O.E", "CLOTH", "CLOSE"))
    }
}
