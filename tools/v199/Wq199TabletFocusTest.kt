package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq199TabletFocusTest {
    @Test fun phoneAndTabletOverviewNeverEnableNewPolicy() {
        assertFalse(Wq199TabletFocus.enabled(411, true))
        assertFalse(Wq199TabletFocus.enabled(800, false))
        assertTrue(Wq199TabletFocus.enabled(600, true))
    }

    @Test fun longVerticalWordFitsAboveHandwritingKeyboard() {
        val height = Wq199TabletFocus.keyboardBoardHeightDp(320f, 308f)
        assertEquals(200f, height, 0.01f)
        val tile = Wq199TabletFocus.tileDp(30f, 420f, height, 1, 13, 1f)
        assertTrue(tile < 30f)
        assertTrue(13 * (tile + 1f) + 14f <= height)
    }

    @Test fun longHorizontalWordFitsAvailableWidth() {
        val tile = Wq199TabletFocus.tileDp(30f, 300f, 180f, 16, 1, 1f)
        assertTrue(16 * (tile + 1f) + 14f <= 300f)
    }

    @Test fun shortWordKeepsExistingZoom() {
        assertEquals(30f, Wq199TabletFocus.tileDp(30f, 420f, 200f, 5, 1, 1f), 0.01f)
    }

    @Test fun bothEndpointsVisibleAfterScroll() {
        val offset = Wq199TabletFocus.scrollPx(10, 22, 13f, 12f, 200, 300, 4f)
        assertTrue(134f >= offset)
        assertTrue(302f <= offset + 200)
    }

    @Test fun wordsOnEdgesClampToActualScrollBounds() {
        assertEquals(0, Wq199TabletFocus.scrollPx(0, 3, 20f, 19f, 180, 220))
        assertEquals(220, Wq199TabletFocus.scrollPx(17, 19, 20f, 19f, 180, 220))
        assertEquals(0, Wq199TabletFocus.scrollPx(17, 19, 20f, 19f, 180, Int.MAX_VALUE))
    }
}
