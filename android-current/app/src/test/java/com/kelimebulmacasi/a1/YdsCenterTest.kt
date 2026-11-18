package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class YdsCenterTest {
    @Test fun completeExamBankHasOriginalUniqueQuestionsAndTurkishSolutions() {
        assertEquals(105, YdsContent.questions.size)
        assertEquals(105, YdsContent.questions.map { it.id }.toSet().size)
        for (topic in YdsTopic.entries) for (difficulty in YdsDifficulty.entries) {
            assertEquals(5, YdsContent.pool(topic, difficulty).size)
        }
        for (question in YdsContent.questions) {
            assertEquals(4, question.options.size)
            assertEquals(4, question.options.distinct().size)
            assertTrue(question.explanation.length > 20)
            assertEquals(1, YdsContent.choices(question).count { it.first == question.answer })
        }
    }

    @Test fun ydsProgressNeverMixesTwoPlayersAndBackupImportIsIdempotent() {
        val context = RuntimeEnvironment.getApplication()
        val question = YdsContent.questions.first()
        val profileA = "testplayerA-${System.nanoTime()}"
        val profileB = "testplayerB-${System.nanoTime()}"
        val first = YdsProgressStore(context, profileA)
        val second = YdsProgressStore(context, profileB)
        first.record(question, false)
        first.record(question, true)
        assertEquals(2, first.attempts(question))
        assertEquals(1, first.correct(question))
        assertEquals(0, second.attempts(question))
        val archive = first.exportStats()
        second.importStats(archive)
        second.importStats(archive)
        assertEquals(2, second.attempts(question))
        assertEquals(1, second.correct(question))
        assertTrue(second.weakestTopic() == question.topic)
    }

    @Test fun timedExamCannotBeExtendedByPausingApp() {
        val start = 1_000_000L
        val deadline = start + 15L * 60L * 1000L
        assertEquals(900, ydsRemainingSeconds(deadline, start))
        assertEquals(300, ydsRemainingSeconds(deadline, start + 600_000L))
        assertEquals(0, ydsRemainingSeconds(deadline, deadline + 1_000L))
    }

    @Test fun dailyProgressIsProfileIsolatedAndImportIsIdempotent() {
        val ctx = RuntimeEnvironment.getApplication()
        val a = DailyPlanStore(ctx, "daily-a-" + System.nanoTime())
        val b = DailyPlanStore(ctx, "daily-b-" + System.nanoTime())
        a.minutes = 30
        a.addTime(60)
        val archive = a.exportDays()
        assertEquals(0L, b.seconds)
        b.importDays(archive)
        b.importDays(archive)
        assertEquals(60L, b.seconds)
        assertEquals(30, b.minutes)
    }

    @Test fun dailyAndYdsBackupRoundTrip() {
        val daily = mapOf("2026-11-15" to DailyDay(30, 600L, setOf("LISTEN_WRITE:123456789abc")))
        val yds = mapOf("v01" to (2 to 3))
        val backup = PlayerBackup(emptyList(), emptyMap(), null, 0L, yds = yds, daily = daily)
        val decoded = decodeBackup(backup.encode())
        assertEquals(yds, decoded.yds)
        assertEquals(daily, decoded.daily)
    }

    @Test fun adaptiveDifficultyMovesFromEasyToMediumToHard() {
        val context = RuntimeEnvironment.getApplication()
        val store = YdsProgressStore(context, "adaptive-${System.nanoTime()}")
        assertEquals(YdsDifficulty.Easy, store.levelRecommendation())
        YdsContent.questions.take(12).forEach { store.record(it, true) }
        assertEquals(YdsDifficulty.Hard, store.levelRecommendation())
        assertEquals(100, store.percent())
    }
}
