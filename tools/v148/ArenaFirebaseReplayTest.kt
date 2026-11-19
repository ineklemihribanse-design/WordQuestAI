package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class ArenaFirebaseReplayTest {
    private fun state(
        version: Int = 2,
        unknown: Map<Int,Set<String>> = emptyMap(),
        replay: Map<Int,Map<String,String>> = emptyMap()
    ) = PremiumOnlineMatch(
        code="ABC123456",hostUid="A",guestUid="B",
        hostName="Alice",guestName="Bob",phase="playing",mode="duel",
        boardId="puzzle",level="A1",topic="All",size=20,seconds=30,quizSeed=0,
        turnUid="A",turnUntil=0L,solvedBy=emptyMap(),answers=emptyMap(),
        unknownBy=unknown,rulesVersion=version,replayBy=replay
    )

    @Test fun firstRoundBothPassMustNotRevealImmediately() {
        val m=state(unknown=mapOf(0 to setOf("A","B")))
        assertFalse(PremiumOnlineLimits.replayRound(m))
        assertFalse(PremiumOnlineLimits.shouldReveal(m,0))
        assertFalse(PremiumOnlineLimits.canAttempt(m,0,"A"))
        assertTrue(PremiumOnlineLimits.canAttempt(m,1,"A"))
    }

    @Test fun allUnsolvedWordsFailedThenFinalReplayBegins() {
        val m=state(unknown=(0 until 20).associateWith {setOf("A","B")})
        assertTrue(PremiumOnlineLimits.replayRound(m))
        assertFalse(PremiumOnlineLimits.shouldReveal(m,0))
        assertTrue(PremiumOnlineLimits.canAttempt(m,0,"A"))
    }

    @Test fun revealOnlyAfterBothReplays() {
        val unknown=(0 until 20).associateWith {setOf("A","B")}
        val m=state(unknown=unknown,replay=mapOf(0 to mapOf("A" to "pass")))
        assertFalse(PremiumOnlineLimits.shouldReveal(m,0))
        assertFalse(PremiumOnlineLimits.canAttempt(m,0,"A"))
        assertTrue(PremiumOnlineLimits.canAttempt(m,0,"B"))
        val both=m.copy(replayBy=mapOf(0 to mapOf("A" to "pass","B" to "wrong")))
        assertTrue(PremiumOnlineLimits.shouldReveal(both,0))
    }

    @Test fun oldRoomsKeepTheirOldRevealRules() {
        val m=state(version=1,unknown=mapOf(0 to setOf("A","B")))
        assertTrue(PremiumOnlineLimits.shouldReveal(m,0))
    }

    @Test fun oneHundredPointsAndOnlyWinnerPaysHintCost() {
        val m=state().copy(solvedBy=mapOf(0 to "A"),hintBy=mapOf("A" to setOf(0)))
        assertEquals(80,m.score("A",emptyList()))
        assertEquals(0,m.score("B",emptyList()))
    }

    @Test fun wrongAnswerIsSingleGuessAndTenPointPenalty() {
        val m=state().copy(mistakesBy=mapOf(0 to mapOf("A" to 1)))
        assertFalse(PremiumOnlineLimits.canAttempt(m,0,"A"))
        assertEquals(0,m.score("A",emptyList()))
    }
}
