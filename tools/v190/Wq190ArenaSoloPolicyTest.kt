package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Wq190ArenaSoloPolicyTest {
    @Test fun supportsAllThreeModesWithoutNetwork() {
        assertTrue(Wq190ArenaSoloPolicy.canPlaySolo("duel"))
        assertTrue(Wq190ArenaSoloPolicy.canPlaySolo("quiz"))
        assertTrue(Wq190ArenaSoloPolicy.canPlaySolo("team"))
        assertEquals("duel", Wq190ArenaSoloPolicy.localEngineMode("team"))
    }

    @Test fun retainsAllSelectedLevelsAndDeduplicates() {
        assertEquals(listOf("A1","B1","C2"),
            Wq190ArenaSoloPolicy.levelPool(listOf("A1","B1","B1","C2"),"A1"))
    }

    @Test fun localRoundChoosesOnlyOneOfSelectedLevels() {
        val levels=listOf("A1","A2","B1")
        for (seed in listOf("1","2","ABC","round_3")) {
            assertTrue(Wq190ArenaSoloPolicy.roundLevel(levels,"C2",seed) in levels)
        }
    }

    @Test fun noImaginaryPartnerIsShown() {
        assertTrue(Wq190ArenaSoloPolicy.displayName("team",true)
            .contains("Tek kişilik"))
    }
}
