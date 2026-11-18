package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class AdvancedArenaPackTest {
    @Test fun upperLevelsHaveUsableCrosswordsAndQuizOptions() {
        val ctx = RuntimeEnvironment.getApplication()
        val content = PremiumArenaContent(ctx)
        assertTrue(content.wordCount >= 1500)
        assertTrue(content.boardCount >= 800)
        for (level in listOf("A2", "B1", "B2", "C1", "C2")) {
            assertTrue("No 20 word board for $level", 20 in content.availableSizes(level))
            val next = content.nextPuzzle(level, 20, PremiumArenaRules.allTopics, "test-$level")
            assertNotNull("Unable to open $level crossword", next)
            assertEquals(20, next!!.puzzle.words.size)
            val quiz = content.questionsForSeed(level, PremiumArenaRules.allTopics, 1000, 12)
            assertEquals("Insufficient quiz content for $level", 12, quiz.size)
        }
    }
}
