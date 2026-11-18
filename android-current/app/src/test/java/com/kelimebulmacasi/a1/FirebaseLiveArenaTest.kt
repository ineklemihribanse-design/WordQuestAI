package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FirebaseLiveArenaTest {
    @Test fun modesContainDistinctSameLengthQuestions() {
        val words = LiveQuestionBank.forMode("words")
        val grammar = LiveQuestionBank.forMode("grammar")
        val mixed = LiveQuestionBank.forMode("mixed")
        assertEquals(6, words.size)
        assertEquals(6, grammar.size)
        assertEquals(12, mixed.size)
        assertTrue(words.all { it.type == "words" })
        assertTrue(grammar.all { it.type == "grammar" })
        assertEquals(12, mixed.map { it.text }.toSet().size)
        assertTrue(mixed.all { it.choices.size == 4 && it.correct in 0..3 })
    }
    @Test fun allCompetitionModesHaveValidChoicesAndBoundedQuestionCounts() {
        val modes = listOf("fast", "duel", "team", "yds", "oral", "daily")
        for (mode in modes) {
            val q = LiveQuestionBank.forMode(mode, "2026-11-15")
            assertTrue("$mode", q.size in 6..12)
            assertTrue(q.all { it.choices.size == 4 && it.correct in 0..3 })
            assertEquals(q.size, q.map { it.text }.toSet().size)
        }
        assertEquals("fast", FirebaseCompetitionMode.of("fast").key)
        assertEquals(10, SparkArenaLimits.roomsPerAccountPerUtcDay)
        assertEquals(12, SparkArenaLimits.maxAnswersPerPlayer)
    }
    @Test fun roomInvitesAreShareableAndCodesAreNormalized() {
        val code = "abcd23456"
        assertTrue(RoomInvite.isValid(code))
        assertEquals("ABCD23456", RoomInvite.normalizedCode(code))
        assertTrue(RoomInvite.message(code).contains("Oda kodu: ABCD23456"))
        assertTrue(!RoomInvite.isValid("ABCDEFGH0"))
        assertEquals(10, SparkArenaLimits.roomsPerAccountPerUtcDay)
    }
    @Test fun quotaDayIndexIsStableAcrossDevicesInUtc() {
        val sample = 1_763_200_000_000L
        assertEquals(sample / 86_400_000L, SparkArenaLimits.utcDayIndex(sample))
        assertEquals(SparkArenaLimits.utcDayIndex(sample).toString(), SparkArenaLimits.utcDayIndexKey(sample))
    }
    @Test fun teamCorrectResponseFromEitherDeviceCountsOnce() {
        val questions = LiveQuestionBank.forMode("team")
        assertEquals(10, LiveQuestionBank.teamScore("team", mapOf(0 to questions[0].correct),
            mapOf(0 to questions[0].correct)))
    }
    @Test fun dailyQuestionOrderIsStableForRoomDate() {
        assertEquals(LiveQuestionBank.forMode("daily", "2026-11-15"),
            LiveQuestionBank.forMode("daily", "2026-11-15"))
    }
    @Test fun scoresOnlyCorrectAnswersOnceByIndex() {
        assertEquals(20, LiveQuestionBank.score("words", mapOf(0 to 1, 1 to 2, 2 to 0)))
        assertEquals(0, LiveQuestionBank.score("grammar", mapOf(100 to 1, 0 to 3)))
    }
}
