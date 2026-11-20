package com.kelimebulmacasi.a1

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class Wq175DailyGoalsTest {
    @Test fun customGoalPersistsPerProfileAndBackupRoundTrips() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val id = UUID.randomUUID().toString()
        val first = DailyPlanStore(ctx, id + "A")
        val other = DailyPlanStore(ctx, id + "B")
        first.minutes = 47
        assertEquals(47, DailyPlanStore(ctx, id + "A").minutes)
        assertEquals(15, other.minutes)
        first.mark(PracticeKind.Dictation, "a1-routine")
        assertEquals(1, first.activeDaysLast7())
        other.importDays(first.exportDays())
        assertEquals(47, other.minutes)
        assertEquals(1, other.activeDaysLast7())
        other.importDays(first.exportDays())
        assertEquals(1, other.activeDaysLast7())
    }

    @Test fun invalidCustomGoalsAreIgnored() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val store = DailyPlanStore(ctx, UUID.randomUUID().toString())
        store.minutes = 4
        assertEquals(15, store.minutes)
        store.minutes = 181
        assertEquals(15, store.minutes)
        store.minutes = 180
        assertEquals(180, store.minutes)
    }
}
