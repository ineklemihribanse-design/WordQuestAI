package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Test

class ArenaCrosswordInputTest {
    @Test fun knownPrefixIsNotTypedTwice() {
        assertEquals("OFFICE", fillCrosswordGaps("O.....", "FFICE"))
    }

    @Test fun intersectingLettersRemainLockedInTheMiddle() {
        assertEquals("HOUSE", fillCrosswordGaps("H.U.E", "OS"))
    }

    @Test fun partialDraftKeepsItsCellPositions() {
        assertEquals("OF....", fillCrosswordGaps("O.....", "F"))
    }

    @Test fun fullyRevealedWordNeedsNoKeyboardInput() {
        assertEquals("OFFICE", fillCrosswordGaps("OFFICE", ""))
    }

    @Test fun extraCharactersNeverOverwriteRevealedSquares() {
        assertEquals("OFFICE", fillCrosswordGaps("O.....", "FFICEXYZ"))
    }

    @Test fun tapAtIntersectionSwitchesDirection() {
        assertEquals(4, nextCrosswordSelection(listOf(2, 4), emptySet(), 2))
        assertEquals(2, nextCrosswordSelection(listOf(2, 4), emptySet(), 4))
    }

    @Test fun tapSameWordPreservesSelection() {
        assertEquals(2, nextCrosswordSelection(listOf(2), emptySet(), 2))
    }

    @Test fun solvedWordsAreNotSelectable() {
        assertEquals(4, nextCrosswordSelection(listOf(2, 4), setOf(2), 2))
        assertEquals(null, nextCrosswordSelection(listOf(2, 4), setOf(2, 4), 2))
    }
}
