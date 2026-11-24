package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq213TypingContinuityTest {
    private val answers = listOf("ONE","TWO","THREE","FOUR","FIVE","SIX","SEVEN","EIGHT","NINE","TEN","ELEVEN")
    private fun game() = Crossword(Puzzle(answers.size,6,answers.mapIndexed { i,a ->
        Word(i+1,Entry(a,"clue ${i+1}"),Placement(Cell(i,0),Direction.Across))
    }))

    @Test fun tenConsecutiveAnswersKeepSessionAndAdvanceExactlyOnce() {
        val engine=game()
        repeat(10) { index ->
            assertEquals(index,engine.wordIndex)
            engine.enter(answers[index])
            engine.check()
            assertTrue(Wq213TypingContinuity.advanceSolved(engine,index))
            assertEquals(index+1,engine.wordIndex)
            assertTrue(Wq213TypingContinuity.layoutActive(true,true,true))
            assertTrue(Wq213TypingContinuity.layoutActive(false,true,true))
        }
        assertEquals(10,engine.view().solvedIndices.size)
        assertEquals(100,engine.view().points)
        assertFalse(engine.complete)
        engine.enter(answers.last())
        engine.check()
        assertTrue(engine.complete)
        assertFalse(Wq213TypingContinuity.advanceSolved(engine,10))
        assertEquals(110,engine.view().points)
    }

    @Test fun wrongAnswerStaysOnSameClueAndCanBeCorrected() {
        val engine=game()
        engine.enter("ZZZ")
        engine.check()
        assertFalse(Wq213TypingContinuity.advanceSolved(engine,0))
        assertEquals(0,engine.wordIndex)
        assertTrue(engine.view().hasErrors)
        assertTrue(Wq213TypingContinuity.layoutActive(true,true,true))
        engine.enter("ONE")
        engine.check()
        assertTrue(Wq213TypingContinuity.advanceSolved(engine,0))
        assertEquals(1,engine.wordIndex)
    }

    @Test fun explicitDismissRestoresNormalLayoutAndTransientInsetDoesNot() {
        assertTrue(Wq213TypingContinuity.layoutActive(false,true,true))
        assertFalse(Wq213TypingContinuity.layoutActive(false,false,false))
        assertTrue(Wq213TypingContinuity.layoutActive(true,false,false))
        assertFalse(Wq213TypingContinuity.layoutActive(false,true,false))
    }

    @Test fun numberedSelectionAndNextWordStillUseExistingEngine() {
        val engine=game()
        engine.selectCell(Cell(4,0))
        assertEquals(5,engine.word.number)
        engine.moveToNextWord()
        assertEquals(6,engine.word.number)
        assertTrue(Wq213TypingContinuity.layoutActive(true,true,true))
    }
}
