package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Wq175ParagraphTest {
    @Test fun punctuationAndCaseDoNotCountAsErrors() {
        val result = wq175GradeParagraph("Hello, my FRIEND!", "hello my friend")
        assertEquals(100, result.accuracy)
        assertTrue(result.missing.isEmpty())
    }

    @Test fun missingWordIsReported() {
        val result = wq175GradeParagraph("we all study English", "we study English")
        assertEquals(75, result.accuracy)
        assertEquals(listOf("all"), result.missing)
    }

    @Test fun substitutionsDoNotGiveFullCredit() {
        val result = wq175GradeParagraph("we enjoy reading books", "we enjoy watching books")
        assertEquals(75, result.accuracy)
        assertEquals(listOf("reading → watching"), result.different)
    }

    @Test fun extraWordsArePenalised() {
        val result = wq175GradeParagraph("read books", "read many books")
        assertEquals(50, result.accuracy)
        assertEquals(listOf("many"), result.extra)
    }

    @Test fun blankAttemptCannotScore() {
        assertEquals(0, wq175GradeParagraph("one two three", "").accuracy)
    }

    @Test fun paragraphsCoverAllSixLevels() {
        assertEquals(12, Wq175ParagraphLibrary.items.size)
        assertEquals(setOf("A1","A2","B1","B2","C1","C2"),
            Wq175ParagraphLibrary.items.map { it.level }.toSet())
    }

    @Test fun extendedYdsQuestionsHaveUniqueIdsAndExplanations() {
        val added = Wq175YdsExpansion.questions
        assertEquals(30, added.size)
        assertEquals(30, added.map { it.id }.toSet().size)
        assertTrue(added.all { it.options.distinct().size == 4 })
        assertTrue(added.all { it.explanation.isNotBlank() })
        assertTrue(added.all { it.answer in it.options.indices })
    }
}
