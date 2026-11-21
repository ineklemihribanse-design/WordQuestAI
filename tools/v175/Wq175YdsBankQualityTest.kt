package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

/** Structural quality gate: never misrepresent automated checks as human linguistic review. */
class Wq175YdsBankQualityTest {
    @Test fun offlineQuestionBankHasNoDuplicateIdsPromptsOrChoices() {
        val questions = YdsContent.questions
        assertEquals("Only the existing 150 authored questions are installed; 2000 remains a target", 150, questions.size)
        assertEquals(questions.size, questions.map { it.id }.toSet().size)
        val normalizedPrompts = questions.map { it.question.lowercase().replace(Regex("\\s+"), " ").trim() }
        assertEquals("Duplicate question prompts must be reviewed before release", questions.size, normalizedPrompts.toSet().size)
        questions.forEach { q ->
            assertTrue("Missing question text: ${q.id}", q.question.trim().length >= 12)
            assertEquals("Expected four choices: ${q.id}", 4, q.options.size)
            assertEquals("Case-insensitive duplicate choice: ${q.id}", 4,
                q.options.map { it.trim().lowercase() }.toSet().size)
            assertTrue("Answer index out of range: ${q.id}", q.answer in q.options.indices)
            assertTrue("Missing Turkish explanation: ${q.id}", q.explanation.trim().length >= 20)
        }
    }

    @Test fun eachDifficultyAndTopicIsRepresented() {
        YdsDifficulty.entries.forEach { difficulty ->
            assertTrue("Missing difficulty ${difficulty.name}", YdsContent.questions.any { it.difficulty == difficulty })
        }
        YdsTopic.entries.forEach { topic ->
            assertTrue("Missing topic ${topic.name}", YdsContent.questions.any { it.topic == topic })
        }
    }
}
