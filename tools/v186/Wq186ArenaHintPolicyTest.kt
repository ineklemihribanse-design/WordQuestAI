package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq186ArenaHintPolicyTest {
    @Test fun validClueCanReceiveHintInRegularOrReplayTurn() {
        assertTrue(Wq186ArenaHintPolicy.canUse(true, null, false,
            false, "..E..", true, 2))
        assertTrue(Wq186ArenaHintPolicy.canUse(true, 30, false,
            false, ".....", true, 1))
    }

    @Test fun passedOrFailedClueCannotConsumeHintBeforeReplay() {
        assertFalse(Wq186ArenaHintPolicy.canUse(true, 30, false,
            false, "...", false, 2))
    }

    @Test fun alreadyRevealedOrExhaustedClueCannotSpendTwice() {
        assertFalse(Wq186ArenaHintPolicy.canUse(true, 30, false,
            true, "..E..", true, 2))
        assertFalse(Wq186ArenaHintPolicy.canUse(true, 30, false,
            false, "KNIFE", true, 2))
        assertFalse(Wq186ArenaHintPolicy.canUse(true, 30, false,
            false, ".....", true, 0))
        assertFalse(Wq186ArenaHintPolicy.canUse(true, 30, true,
            false, ".....", true, 2))
    }

    @Test fun onlyOwnerOfActiveTurnMayAskAndDeadlineIsRespected() {
        assertFalse(Wq186ArenaHintPolicy.canUse(false, null, false,
            false, ".....", true, 2))
        assertFalse(Wq186ArenaHintPolicy.canUse(true, 0, false,
            false, ".....", true, 2))
        assertFalse(Wq186ArenaHintPolicy.canUse(true, -2, false,
            false, ".....", true, 2))
    }
}
