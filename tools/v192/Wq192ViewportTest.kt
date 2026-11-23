package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Wq192ViewportTest {
    @Test fun overviewDisplaysEveryColumnWithoutForcedMinimumCell() {
        assertEquals(14.4f, Wq192Viewport.overviewTileDp(300f, 300f, 20, 20), 0.01f)
    }

    @Test fun overviewRespectsShortPhoneHeight() {
        assertEquals(9.4f, Wq192Viewport.overviewTileDp(330f, 200f, 24, 20), 0.01f)
    }

    @Test fun overviewHandlesWideAndTallBoards() {
        assertEquals(16f, Wq192Viewport.overviewTileDp(332f, 340f, 20, 10), 0.01f)
        assertEquals(10.25f, Wq192Viewport.overviewTileDp(340f, 340f, 12, 32), 0.01f)
    }

    @Test fun focusCentersWholeWordInsteadOfOnlyFirstCharacter() {
        assertEquals(270, Wq192Viewport.focusScrollPx(10, 12, 30f, 150f, 500))
    }

    @Test fun focusOffsetClampsToGridEdges() {
        assertEquals(0, Wq192Viewport.focusScrollPx(0, 2, 30f, 150f, 500))
        assertEquals(500, Wq192Viewport.focusScrollPx(22, 24, 30f, 150f, 500))
    }

    @Test fun invalidInputsCannotProduceOffscreenScrolls() {
        assertEquals(0f, Wq192Viewport.overviewTileDp(0f, 100f, 20, 10), 0.01f)
        assertEquals(0, Wq192Viewport.focusScrollPx(-1, 5, 30f, 100f, 100))
        assertEquals(0, Wq192Viewport.focusScrollPx(3, 2, 30f, 100f, 100))
        assertEquals(0, Wq192Viewport.focusScrollPx(1, 2, Float.NaN, 100f, 100))
    }
}
