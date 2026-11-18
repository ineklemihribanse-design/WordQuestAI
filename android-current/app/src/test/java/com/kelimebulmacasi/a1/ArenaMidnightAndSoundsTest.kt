package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class ArenaMidnightAndSoundsTest {
    @Test fun quotaResetsAtMidnightInTurkeyNotUtc() {
        // 2026-11-15 23:59:59 in Istanbul = 20:59:59 UTC.
        val before = Instant.parse("2026-11-15T20:59:59Z").toEpochMilli()
        val after = Instant.parse("2026-11-15T21:00:00Z").toEpochMilli()
        assertEquals(SparkArenaLimits.turkeyQuotaDayIndex(before) + 1L,
            SparkArenaLimits.turkeyQuotaDayIndex(after))
        assertEquals(SparkArenaLimits.utcDayIndex(before), SparkArenaLimits.utcDayIndex(after))
        assertEquals(SparkArenaLimits.turkeyQuotaDayIndexKey(after),
            SparkArenaLimits.turkeyQuotaDayIndex(after).toString())
    }
    @Test fun quotaDoesNotResetAt03WhenTurkeyDayHasAlreadyChanged() {
        val before = Instant.parse("2026-11-15T23:59:59Z").toEpochMilli()
        val after = Instant.parse("2026-11-16T00:00:00Z").toEpochMilli()
        assertEquals(SparkArenaLimits.turkeyQuotaDayIndex(before), SparkArenaLimits.turkeyQuotaDayIndex(after))
        assertNotEquals(SparkArenaLimits.utcDayIndex(before), SparkArenaLimits.utcDayIndex(after))
    }
    @Test fun eventSoundsRespectExistingSoundToggleAndZeroVolume() {
        assertTrue(GameEffectPolicy.canPlay(true, .28f))
        assertFalse(GameEffectPolicy.canPlay(false, 1f))
        assertFalse(GameEffectPolicy.canPlay(true, 0f))
    }
}
