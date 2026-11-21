package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq179ArenaFinaleTest {
    @Test fun victoryAndDefeatArePersonalized() {
        val host=Wq179ArenaResultCopy.make(false,true,120,80,"A","B")
        val guest=Wq179ArenaResultCopy.make(false,false,120,80,"A","B")
        assertEquals(Wq179ArenaOutcome.Victory,host.outcome)
        assertEquals(Wq179ArenaOutcome.Defeat,guest.outcome)
        assertTrue(host.subline.contains("40 puan"))
        assertTrue(guest.subline.contains("40 puan"))
        assertTrue(guest.subline.contains("A"))
    }
    @Test fun drawAndCooperativeAreDistinct() {
        assertEquals(Wq179ArenaOutcome.Draw,
            Wq179ArenaResultCopy.make(false,true,55,55,"A","B").outcome)
        assertEquals(Wq179ArenaOutcome.Cooperation,
            Wq179ArenaResultCopy.make(true,false,25,40,"A","B").outcome)
    }
}
