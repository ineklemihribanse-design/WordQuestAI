package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq175YdsVisibilityTest {
    @Test fun timedExamsMustHideCorrectAnswersAndExplanations() {
        assertFalse(Wq175YdsVisibility.reveal(true))
    }
    @Test fun ordinaryStudyRetainsDetailedFeedback() {
        assertTrue(Wq175YdsVisibility.reveal(false))
    }
}
