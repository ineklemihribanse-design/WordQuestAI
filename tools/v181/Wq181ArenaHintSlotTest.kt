package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq181ArenaHintSlotTest {
    @Test fun revealedHintCannotMoveWhenCrossingWordIsSolved(){
        assertEquals(5,Wq181ArenaHintSlot.lockedPosition(5,2,8))
        assertEquals(5,Wq181ArenaHintSlot.lockedPosition(5,-1,8))
        assertEquals(5,Wq181ArenaHintSlot.lockedPosition(5,4,8))
    }
    @Test fun missingOrInvalidCacheFallsBackToCurrentHint(){
        assertEquals(2,Wq181ArenaHintSlot.lockedPosition(-1,2,8))
        assertEquals(3,Wq181ArenaHintSlot.lockedPosition(50,3,8))
        assertEquals(-1,Wq181ArenaHintSlot.lockedPosition(-1,-1,8))
    }
}
