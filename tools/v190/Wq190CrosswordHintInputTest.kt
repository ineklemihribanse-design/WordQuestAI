package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Wq190CrosswordHintInputTest {
    @Test fun revealThirdLetterAllowsTypingOnlyRemainingSix() {
        val locked=mapOf(2 to 'C')
        assertEquals("FACTORY",Wq190CrosswordHintInput.completedWord(7,locked,"FATORY"))
        assertEquals('T',Wq190CrosswordHintInput.typedPositions(7,locked,"FATORY")[4])
    }
    @Test fun fullTypedAnswerCannotOverrideRevealedLetter() {
        val locked=mapOf(2 to 'C')
        assertEquals("FACTORY",Wq190CrosswordHintInput.completedWord(7,locked,"FAXTORY"))
    }
    @Test fun incompleteDraftCannotSubmit() {
        assertNull(Wq190CrosswordHintInput.completedWord(7,mapOf(2 to 'C'),"FA"))
        assertTrue(Wq190CrosswordHintInput.typedPositions(7,mapOf(2 to 'C'),"FA")[2]==null)
    }
    @Test fun noHintsStillSupportsFullWord() {
        assertEquals("FACTORY",Wq190CrosswordHintInput.completedWord(7,emptyMap(),"factory"))
    }
}
