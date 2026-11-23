package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Wq190BoardPolicyTest {
    @Test fun landscapeTabletWithHandwritingKeyboardUsesAvailableHeight() {
        val result = Wq190BoardPolicy.fittedHeightDp(
            allocatedWidthDp = 570f,
            availableHeightDp = 450f,
            rows = 20, columns = 20
        )
        assertEquals(438f, result, 0.01f)
        assertTrue(result > 234f)
    }

    @Test fun wideShortBoardPreservesAspectRatio() {
        assertEquals(200f, Wq190BoardPolicy.fittedHeightDp(
            500f, 700f, 8, 20
        ), 0.01f)
    }

    @Test fun tallBoardFitsAvailableViewportWithoutClipping() {
        assertEquals(388f, Wq190BoardPolicy.fittedHeightDp(
            500f, 400f, 22, 12
        ), 0.01f)
    }

    @Test fun degenerateBoardReturnsZero() {
        assertEquals(0f, Wq190BoardPolicy.fittedHeightDp(
            0f, 450f, 20, 20
        ), 0.01f)
    }
}
