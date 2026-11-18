package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VocabularyAnswerCheckerTest {
    @Test fun exactAndCaseInsensitiveEnglish() {
        assertEquals(VocabularyGrade.CORRECT, VocabularyAnswerChecker.grade("  GrandMother  ", "grandmother", true))
    }

    @Test fun everydayGrandmotherSynonymsAreAccepted() {
        assertEquals(VocabularyGrade.CORRECT, VocabularyAnswerChecker.grade("granny", "grandmother", true))
        assertEquals(VocabularyGrade.CORRECT, VocabularyAnswerChecker.grade("grandma", "grandmother", true))
    }

    @Test fun explicitTurkishAlternativesAreAccepted() {
        assertEquals(VocabularyGrade.CORRECT, VocabularyAnswerChecker.grade(
            "Babaanne", "Anneanne veya babaanne", false))
        assertEquals(VocabularyGrade.CORRECT, VocabularyAnswerChecker.grade(
            "anneanne", "Anneanne veya babaanne", false))
    }

    @Test fun typoDoesNotCountAsCorrect() {
        assertEquals(VocabularyGrade.CLOSE, VocabularyAnswerChecker.grade("grandmothr", "grandmother", true))
    }

    @Test fun unrelatedWrongAnswerRejected() {
        assertEquals(VocabularyGrade.WRONG, VocabularyAnswerChecker.grade("cat", "grandmother", true))
    }

    @Test fun stableWordTypesAvoidGuessingUnknownOrAmbiguousWords() {
        assertEquals(VocabularyType.NOUN, VocabularyWordTypes.get("grandmother"))
        assertEquals(VocabularyType.ADJECTIVE, VocabularyWordTypes.get("beautiful"))
        assertEquals(VocabularyType.ADVERB, VocabularyWordTypes.get("quickly"))
        assertNull(VocabularyWordTypes.get("book"))
        assertNull(VocabularyWordTypes.get("made-up-word"))
    }
}
