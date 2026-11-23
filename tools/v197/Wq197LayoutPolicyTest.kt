package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Wq197LayoutPolicyTest {
    @Test fun cefrHasSingleRowOnTabletAndTwoOnPhone() {
        assertEquals(6, Wq197LayoutPolicy.columns(900f, 6, 2))
        assertEquals(3, Wq197LayoutPolicy.columns(360f, 6, 2))
    }

    @Test fun ydsHasCompactRowEvenOnPhone() {
        assertEquals(3, Wq197LayoutPolicy.columns(360f, 3, 9))
    }

    @Test fun longerTopicNamesNeverNeedSixTinyColumns() {
        assertEquals(2, Wq197LayoutPolicy.columns(360f, 12, 19))
        assertEquals(4, Wq197LayoutPolicy.columns(800f, 12, 19))
    }

    @Test fun wordCountAndGameModeRemainTappable() {
        assertEquals(3, Wq197LayoutPolicy.columns(360f, 6, 4))
        assertEquals(3, Wq197LayoutPolicy.columns(360f, 3, 7))
        assertEquals(6, Wq197LayoutPolicy.columns(900f, 6, 4))
    }

    @Test fun selectedItemsAreAlwaysPreviewedBeforeUnselectedOnes() {
        val preview = Wq197LayoutPolicy.preview(
            listOf("A", "B", "C", "D", "E", "F"), setOf("F", "D"), 4
        )
        assertEquals(4, preview.size)
        assertTrue("F" in preview && "D" in preview)
        assertFalse("E" in preview)
    }

    @Test fun smallPhonePreviewNeedsOnlyFourChoiceButtons() {
        assertEquals(4, Wq197LayoutPolicy.previewLimit(359f))
        assertEquals(6, Wq197LayoutPolicy.previewLimit(500f))
        assertEquals(12, Wq197LayoutPolicy.previewLimit(900f))
        assertEquals(0, Wq197LayoutPolicy.columns(300f, 0, 0))
    }
}
