package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class PremiumOnlineRulesTest {
    @Test fun initialTurnCanSelectEitherPlayerAndClockHasBoundaries() {
        assertEquals("host", PremiumOnlineLimits.firstTurn("host", "guest", true))
        assertEquals("guest", PremiumOnlineLimits.firstTurn("host", "guest", false))
        assertEquals(30, PremiumOnlineLimits.remainingSeconds(130001, 100001))
        assertEquals(1, PremiumOnlineLimits.remainingSeconds(100001, 100000))
        assertEquals(0, PremiumOnlineLimits.remainingSeconds(100000, 100000))
        assertTrue(PremiumOnlineLimits.timedOut(100000, 100000))
        assertFalse(PremiumOnlineLimits.timedOut(0, 99999))
        assertFalse(PremiumOnlineLimits.timedOut(100001, 100000))
    }

    @Test fun supportsOnlyRealPremiumModesAndValidSettings() {
        assertTrue(PremiumOnlineLimits.validConfig("A1", 20, 30, "duel"))
        assertTrue(PremiumOnlineLimits.validConfig("A2", 50, 0, "team"))
        assertTrue(PremiumOnlineLimits.validConfig("A1–A2", 12, 45, "quiz"))
        assertFalse(PremiumOnlineLimits.validConfig("A1", 30, 30, "quiz"))
        assertTrue(PremiumOnlineLimits.validConfig("C2", 20, 30, "duel"))
        assertFalse(PremiumOnlineLimits.validConfig("A1", 20, 12, "duel"))
    }

    @Test fun aSharedCrosswordCreditsEachCorrectlySolvedCellOnlyOnce() {
        val game = PremiumOnlineMatch("ABCDEF234", "host", "guest", "Ali", "Ayşe",
            "finished", "duel", "board", "A1", "Tüm konular", 20, 30, 0,
            "host", 0L, mapOf(0 to "host", 1 to "guest", 2 to "host"), emptyMap())
        assertEquals(20, game.score("host", emptyList()))
        assertEquals(10, game.score("guest", emptyList()))
        assertEquals("guest", PremiumOnlineLimits.otherPlayer(game, "host"))
        assertFalse(game.complete)
    }

    @Test fun revealedWordsStayNeutralButWrongAnswersCarryPenalty() {
        val game = PremiumOnlineMatch("ABCDEF234", "host", "guest", "Ali", "Ayşe",
            "playing", "duel", "board", "A1", "Tüm konular", 20, 30, 0,
            "host", 0L, mapOf(0 to "reveal", 1 to "host", 2 to "guest"), emptyMap(),
            mapOf(0 to setOf("host", "guest")),
            mapOf(0 to mapOf("host" to 1), 4 to mapOf("guest" to 2)))
        assertEquals(7, game.score("host", emptyList())) // 10 correct - 3 incorrect
        assertEquals(4, game.score("guest", emptyList())) // 10 correct - 6 incorrect
        assertEquals(setOf("host", "guest"), game.unknownBy[0])
        assertEquals("reveal", game.solvedBy[0])
    }

    @Test fun unknownThenWrongRevealsAndPenalizesOnlyWrongPlayer() {
        val game = PremiumOnlineMatch("ABCDEF234", "host", "guest", "Ali", "Ayşe",
            "playing", "duel", "board", "A1", "Tüm konular", 20, 30, 0,
            "guest", 0L, emptyMap(), emptyMap(),
            mapOf(3 to setOf("host")), mapOf(3 to mapOf("guest" to 1)))
        assertTrue(PremiumOnlineLimits.shouldReveal(game, 3))
        assertEquals(0, game.score("host", emptyList()))
        assertEquals(-3, game.score("guest", emptyList()))
    }

    @Test fun wrongThenUnknownAlsoRevealsWithoutPenalizingUnknownPlayer() {
        val game = PremiumOnlineMatch("ABCDEF234", "host", "guest", "Ali", "Ayşe",
            "playing", "duel", "board", "A1", "Tüm konular", 20, 30, 0,
            "guest", 0L, emptyMap(), emptyMap(),
            mapOf(7 to setOf("guest")), mapOf(7 to mapOf("host" to 1)))
        assertTrue(PremiumOnlineLimits.shouldReveal(game, 7))
        assertEquals(-3, game.score("host", emptyList()))
        assertEquals(0, game.score("guest", emptyList()))
    }

    @Test fun oneUnsuccessfulPlayerDoesNotRevealEarlyAndSolvedWordStaysSolved() {
        val base = PremiumOnlineMatch("ABCDEF234", "host", "guest", "Ali", "Ayşe",
            "playing", "duel", "board", "A1", "Tüm konular", 20, 30, 0,
            "host", 0L, emptyMap(), emptyMap(), mapOf(1 to setOf("host")))
        assertFalse(PremiumOnlineLimits.shouldReveal(base, 1))
        val solved = base.copy(solvedBy = mapOf(1 to "guest"),
            mistakesBy = mapOf(1 to mapOf("guest" to 1)))
        assertFalse(PremiumOnlineLimits.shouldReveal(solved, 1))
    }

    @Test fun twoWrongGuessesAlsoRevealBecauseNeitherPlayerSolvedIt() {
        val game = PremiumOnlineMatch("ABCDEF234", "host", "guest", "Ali", "Ayşe",
            "playing", "duel", "board", "A1", "Tüm konular", 20, 30, 0,
            "host", 0L, emptyMap(), emptyMap(), emptyMap(),
            mapOf(2 to mapOf("host" to 1, "guest" to 1)))
        assertTrue(PremiumOnlineLimits.shouldReveal(game, 2))
        assertEquals(-3, game.score("host", emptyList()))
        assertEquals(-3, game.score("guest", emptyList()))
    }

    @Test fun fiveChoiceWrongAnswerHasPenaltyButTimeoutHasNone() {
        val questions = listOf(PremiumQuestion("Q", listOf("a", "b", "c", "d", "e"), 4, "e"))
        val game = PremiumOnlineMatch("ABCDEF234", "host", "guest", "Ali", "Ayşe",
            "playing", "quiz", "", "A1", "Tüm konular", 12, 30, 17,
            "host", 0L, emptyMap(), mapOf("host" to mapOf(0 to 1), "guest" to mapOf(0 to -1)))
        assertEquals(-3, game.score("host", questions))
        assertEquals(0, game.score("guest", questions))
    }

    @Test fun fiveChoiceAnswerIndexesAreScoredPerAccount() {
        val questions = listOf(PremiumQuestion("Question", listOf("a","b","c","d","e"), 4, "e"))
        val game = PremiumOnlineMatch("ABCDEF234", "host", "guest", "Ali", "Ayşe",
            "playing", "quiz", "", "A1", "Tüm konular", 12, 30, 1234,
            "host", 0L, emptyMap(), mapOf("host" to mapOf(0 to 4), "guest" to mapOf(0 to -1)))
        assertEquals(10, game.score("host", questions))
        assertEquals(0, game.score("guest", questions))
    }
}
