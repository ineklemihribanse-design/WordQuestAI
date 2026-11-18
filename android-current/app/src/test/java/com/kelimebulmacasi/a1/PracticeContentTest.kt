package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class PracticeContentTest {
    @Test fun allOfflineExercisesHaveValidAnswersAndOptions() {
        assertTrue(PracticeContent.sentences.size >= 10)
        assertTrue(PracticeContent.grammar.size >= 10)
        assertTrue(PracticeContent.story.size >= 10)
        (PracticeContent.grammar + PracticeContent.story).forEach { question ->
            assertEquals(1, question.options.count { PracticeContent.correct(it, question.answer) })
            assertTrue(question.instruction.isNotEmpty())
        }
    }

    @Test fun exactAnswerIsRequiredWithoutPunctuationDifferences() {
        assertTrue(PracticeContent.correct("  She goes to SCHOOL every day!", "She goes to school every day"))
        assertFalse(PracticeContent.correct("She go school", "She goes to school"))
    }

    @Test fun sentenceScrambleKeepsEachWordExactlyOnce() {
        PracticeContent.sentences.forEachIndexed { i, question ->
            val shuffled = PracticeContent.mixedTokens(question, i + 12)
            assertEquals(question.answer.split(" ").size, shuffled.size)
            assertEquals((0 until shuffled.size).toSet(), shuffled.map { it.first }.toSet())
        }
    }

    @Test fun dailyPlanMinutesMatchThreeSupportedDurations() {
        assertEquals(5, PracticeContent.dailyGoal(15))
        assertEquals(10, PracticeContent.dailyGoal(30))
        assertEquals(20, PracticeContent.dailyGoal(60))
    }
}
