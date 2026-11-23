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

    @Test fun phoneShowsWholeBoardWhenThereIsRoom() {
        assertEquals(350f, Wq190BoardPolicy.phoneHeightDp(
            350f, 760f, 20, 20, false), 0.01f)
    }

    @Test fun phoneLimitsBoardHeightWhenScreenIsShort() {
        assertEquals(215f, Wq190BoardPolicy.phoneHeightDp(
            350f, 500f, 20, 20, false), 0.01f)
    }

    @Test fun keyboardReservesSpaceForTextEditor() {
        assertEquals(218f, Wq190BoardPolicy.phoneHeightDp(
            350f, 390f, 20, 20, true), 0.01f)
    }

    @Test fun mobileHeightRespectsUnusuallyTallBoard() {
        assertEquals(435f, Wq190BoardPolicy.phoneHeightDp(
            350f, 720f, 25, 15, false), 0.01f)
    }

    @Test fun mobileHeightRejectsInvalidDimensions() {
        assertEquals(0f, Wq190BoardPolicy.phoneHeightDp(
            350f, 390f, 0, 20, true), 0.01f)
    }
}
