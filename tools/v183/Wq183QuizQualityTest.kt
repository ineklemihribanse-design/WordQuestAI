package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq183QuizQualityTest {
    private val five = listOf("A", "B", "C", "D", "E")

    @Test fun fiveChoicesAndValidAnswerAreAccepted() {
        for (n in 0..4) assertTrue(Wq183QuizIsValid("Which word is correct?", five, n))
    }

    @Test fun emptyQuestionAndInvalidAnswerCannotScore() {
        assertFalse(Wq183QuizIsValid("", five, 0))
        assertFalse(Wq183QuizIsValid("   ", five, 0))
        assertFalse(Wq183QuizIsValid("Question", five, -1))
        assertFalse(Wq183QuizIsValid("Question", five, 5))
    }

    @Test fun missingAndDuplicatedOptionsCannotScore() {
        assertFalse(Wq183QuizIsValid("Question", listOf("A", "B", "C", "D"), 0))
        assertFalse(Wq183QuizIsValid("Question", five + "F", 0))
        assertFalse(Wq183QuizIsValid("Question", listOf("A", "B", "", "D", "E"), 0))
        assertFalse(Wq183QuizIsValid("Question", listOf("A", "B", "b ", "D", "E"), 0))
    }
}
