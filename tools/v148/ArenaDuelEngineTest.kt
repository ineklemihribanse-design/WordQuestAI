package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class ArenaDuelEngineTest {
    @Test fun wrongAnswerEndsTheTurnWithoutSecondGuess() {
        var s=ArenaDuelEngine.newGame(1,"A","B")
        s=ArenaDuelEngine.submit(s,"A",0,ArenaDuelMove.WRONG)
        assertEquals("B",s.turn)
        assertEquals(0,s.score["A"])
        assertEquals(1,s.words.getValue(0).firstAttempts.size)
        assertEquals(s,ArenaDuelEngine.submit(s,"A",0,ArenaDuelMove.CORRECT))
    }

    @Test fun twoPassesMoveToFinalRoundWithoutRevealingAnswer() {
        var s=ArenaDuelEngine.newGame(1,"A","B")
        s=ArenaDuelEngine.submit(s,"A",0,ArenaDuelMove.PASS)
        s=ArenaDuelEngine.submit(s,"B",0,ArenaDuelMove.PASS)
        assertEquals(2,s.round)
        assertEquals("B",s.turn)
        assertFalse(s.completed)
        assertNull(s.words.getValue(0).owner)
        assertFalse(s.words.getValue(0).closed)
    }

    @Test fun finalRoundClosesOnlyAfterBothFail() {
        var s=ArenaDuelEngine.newGame(1,"A","B")
        s=ArenaDuelEngine.submit(s,"A",0,ArenaDuelMove.PASS)
        s=ArenaDuelEngine.submit(s,"B",0,ArenaDuelMove.PASS)
        s=ArenaDuelEngine.submit(s,"B",0,ArenaDuelMove.WRONG)
        assertFalse(s.completed)
        s=ArenaDuelEngine.submit(s,"A",0,ArenaDuelMove.PASS)
        assertTrue(s.completed)
        assertTrue(s.words.getValue(0).closed)
    }

    @Test fun aCorrectWordAllowsSamePlayerToContinue() {
        var s=ArenaDuelEngine.newGame(2,"A","B")
        s=ArenaDuelEngine.submit(s,"A",0,ArenaDuelMove.CORRECT)
        assertEquals("A",s.turn)
        s=ArenaDuelEngine.submit(s,"A",1,ArenaDuelMove.CORRECT)
        assertTrue(s.completed)
        assertEquals(200,s.score["A"])
        assertEquals(0,s.score["B"])
    }

    @Test fun opponentCapturesAfterFirstWrongGuess() {
        var s=ArenaDuelEngine.newGame(1,"A","B")
        s=ArenaDuelEngine.submit(s,"A",0,ArenaDuelMove.WRONG)
        s=ArenaDuelEngine.submit(s,"B",0,ArenaDuelMove.CORRECT)
        assertTrue(s.completed)
        assertEquals("B",s.words.getValue(0).owner)
        assertEquals(110,s.score["B"])
    }

    @Test fun hintIsUsedOnlyOncePerWordAndReducesPotentialReward() {
        var s=ArenaDuelEngine.newGame(1,"A","B")
        s=ArenaDuelEngine.useHint(s,"A",0)
        val one=s.hints.getValue("A")
        assertEquals(0,one)
        assertEquals(s,ArenaDuelEngine.useHint(s,"A",0))
        s=ArenaDuelEngine.submit(s,"A",0,ArenaDuelMove.CORRECT)
        assertEquals(80,s.score["A"])
    }

    @Test fun timeOutIsAPassNotAnExtraGuess() {
        var s=ArenaDuelEngine.newGame(1,"A","B")
        s=ArenaDuelEngine.submit(s,"A",0,ArenaDuelMove.TIMEOUT)
        assertEquals("B",s.turn)
        assertEquals(0,s.score["A"])
        assertEquals(ArenaDuelMove.TIMEOUT,s.words.getValue(0).firstAttempts["A"])
    }

    @Test fun gameSizesReceiveScaledHints() {
        assertEquals(3,ArenaDuelEngine.hintsFor(20))
        assertEquals(4,ArenaDuelEngine.hintsFor(30))
        assertEquals(5,ArenaDuelEngine.hintsFor(40))
        assertEquals(6,ArenaDuelEngine.hintsFor(50))
    }
}
