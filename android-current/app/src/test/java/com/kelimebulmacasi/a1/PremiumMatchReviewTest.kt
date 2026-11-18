package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class PremiumMatchReviewTest {
    private val entries = listOf(
        Entry("CHOCOLATE", "Çikolata"), Entry("FOREST", "Orman"), Entry("SEA", "Deniz"),
    )

    private fun room() = PremiumOnlineMatch(
        "ABCDE2222", "host", "guest", "Ayşe", "Ali", "finished", "duel", "board", "A1",
        "Tüm konular", 3, 30, 3, "guest", 0,
        mapOf(0 to "reveal", 1 to "host", 2 to "guest"), emptyMap(),
        mapOf(0 to setOf("host"), 2 to setOf("guest")),
        mapOf(0 to mapOf("guest" to 1), 1 to mapOf("guest" to 2)),
    )

    @Test fun crosswordSummaryIncludesOnlyOwnMistakesAndUnknowns() {
        val host = PremiumMatchReview.crossword(room(), entries, "host")
        assertEquals(1, host.correct)
        assertEquals(0, host.wrong)
        assertEquals(1, host.unknown)
        assertEquals(listOf("CHOCOLATE"), host.review.map { it.entry.answer })
        assertTrue(host.review[0].unknown)
        assertEquals(0, host.review[0].wrongAttempts)
        val guest = PremiumMatchReview.crossword(room(), entries, "guest")
        assertEquals(1, guest.correct)
        assertEquals(3, guest.wrong)
        assertEquals(1, guest.unknown)
        assertEquals(3, guest.review.size)
        assertTrue(guest.review.last().unknown)
        assertEquals(3, guest.reviewedWords)
    }

    @Test fun quizCountsWrongAndTimeOutWithoutCountingOpponents() {
        val questions = listOf(
            PremiumQuestion("Deniz — İngilizcesi hangisidir?", listOf("a", "b", "c", "SEA", "d"), 3, "SEA"),
            PremiumQuestion("Orman — İngilizcesi hangisidir?", listOf("a", "b", "FOREST", "c", "d"), 2, "FOREST"),
            PremiumQuestion("Kuş — İngilizcesi hangisidir?", listOf("a", "BIRD", "b", "c", "d"), 1, "BIRD"),
        )
        val match = room().copy(mode = "quiz", answers = mapOf("host" to mapOf(0 to 3, 1 to 0, 2 to -1)))
        val summary = PremiumMatchReview.quiz(match, questions, "host")
        assertEquals(1, summary.correct)
        assertEquals(1, summary.wrong)
        assertEquals(1, summary.unknown)
        assertEquals(2, summary.review.size)
        assertTrue(PremiumMatchReview.prompt(summary).contains("FOREST"))
    }

    @Test fun unknownLearningEventIsUniqueAndDueForReview() {
        val signal = LearningSignal(LearningKind.UNKNOWN, entries[0], boardUpdatedAt = 1L)
        val first = learningEventId("premium-ABC-host", signal)
        assertEquals(first, learningEventId("premium-ABC-host", signal))
        assertNotEquals(first, learningEventId("premium-OTHER-host", signal))
        val memory = LearningSchedule.next(LearningCard(learningWordKey(entries[0]), "CHOCOLATE", "Çikolata", ""),
            LearningKind.UNKNOWN, 1_000_000L)
        assertEquals(1, memory.forgotten)
        assertTrue(memory.dueAt > 1_000_000L)
    }
}
