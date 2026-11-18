package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class PremiumArenaRulesTest {
    @Test fun levelSelectionsAreSeparateOrCombined() {
        assertTrue(PremiumArenaRules.matchesLevel("A1", "A1"))
        assertFalse(PremiumArenaRules.matchesLevel("A2", "A1"))
        assertTrue(PremiumArenaRules.matchesLevel("A2", "A1–A2"))
        assertTrue(PremiumArenaRules.matchesLevel("A1", "A1–A2"))
        assertFalse(PremiumArenaRules.matchesLevel("B1", "A1–A2"))
        assertTrue(PremiumArenaRules.crosswordLevel("A1–A2", "A1–A2"))
        assertFalse(PremiumArenaRules.crosswordLevel("A1", "A1–A2"))
    }

    @Test fun rotationAvoidsRecentIdsWhileCandidatesRemain() {
        val pool = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "d"), PremiumArenaRules.prioritizeUnseen(pool, setOf("a", "c")) { it })
        assertEquals(pool, PremiumArenaRules.prioritizeUnseen(pool, pool.toSet()) { it })
    }

    @Test fun allowedSizesAndTimesMatchArenaSpec() {
        assertEquals(listOf(20,30,40,50), PremiumArenaRules.allowedSizes)
        assertEquals(listOf(0,20,30,45), PremiumArenaRules.allowedTimes)
        assertTrue(PremiumArenaRules.canUseTopic("Yiyecek ve İçecek", "Tüm konular"))
        assertFalse(PremiumArenaRules.canUseTopic("Okul ve Sınıf", "Yiyecek ve İçecek"))
    }
}
