package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq175FullYdsTest {
    @Test fun fullExamHasEnoughRealOriginalQuestions() {
        val bank = YdsContent.pool(null, null)
        assertTrue(bank.size >= 80)
        assertEquals(150, YdsContent.questions.size)
        assertEquals(bank.size, bank.map { it.id }.distinct().size)
        assertTrue(bank.all { it.explanation.length > 20 })
    }
    @Test fun fullExamTimerHasRealThreeHourDeadline() {
        val start = 1_000_000L
        val end = start + 180L * 60L * 1000L
        assertEquals(10800, ydsRemainingSeconds(end, start))
        assertEquals(3600, ydsRemainingSeconds(end, start + 120L * 60L * 1000L))
        assertEquals(0, ydsRemainingSeconds(end, end + 10))
    }
}
