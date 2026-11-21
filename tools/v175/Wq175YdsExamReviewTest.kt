package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq175YdsExamReviewTest {
    @Test fun wrongAndUnansweredAreNotTreatedAsCorrect() {
        val all=YdsContent.questions.take(4)
        assertEquals(4,all.size)
        val answers=mapOf(all[0].id to all[0].answer)
        val incorrect=all.filter { answers[it.id]!=it.answer }
        assertEquals(3,incorrect.size)
        assertFalse(incorrect.contains(all[0]))
    }
    @Test fun everyQuestionHasReviewableCorrectOptionAndExplanation() {
        assertTrue(YdsContent.questions.all {
            it.answer in it.options.indices && it.explanation.isNotBlank()
        })
    }
}
