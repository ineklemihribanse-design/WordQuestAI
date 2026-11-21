package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Test

class Wq175TimerMathTest {
    @Test fun monotonicDeadlineDoesNotResetWhenUiRecomposes() {
        assertEquals(120_000L,
            Wq175TimerMath.remainingMillis(300_000L,1_000L,1_000_000L,
                181_000L,1_180_000L))
        assertEquals(0L,
            Wq175TimerMath.remainingMillis(300_000L,1_000L,1_000_000L,
                301_001L,1_300_001L))
    }
    @Test fun rebootFallsBackToWallClockAndNeverGoesNegative() {
        assertEquals(60_000L,
            Wq175TimerMath.remainingMillis(300_000L,500_000L,1_000_000L,
                2_000L,1_240_000L))
        assertEquals(0L,
            Wq175TimerMath.remainingMillis(300_000L,500_000L,1_000_000L,
                2_000L,1_600_000L))
    }
    @Test fun uiRoundsUpFullSeconds() {
        assertEquals(3L,Wq175TimerMath.secondsRoundedUp(2_001L))
        assertEquals(1L,Wq175TimerMath.secondsRoundedUp(1L))
        assertEquals(0L,Wq175TimerMath.secondsRoundedUp(0L))
    }
}
