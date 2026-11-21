package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq175ImportPagingTest {
    @Test fun pagingKeepsGlobalIndicesForEditingAndExclusion() {
        val words=(0 until 25).map { "word$it" }
        assertEquals((0..7).toList(),
            wq175ImportPreviewPage(words,0).map { it.index })
        assertEquals((8..15).toList(),
            wq175ImportPreviewPage(words,1).map { it.index })
        assertEquals(listOf(24),
            wq175ImportPreviewPage(words,3).map { it.index })
        assertEquals("word24",wq175ImportPreviewPage(words,3).single().value)
    }
    @Test fun emptyAndOutOfRangePagesStaySafe() {
        assertTrue(wq175ImportPreviewPage(emptyList<Int>(),0).isEmpty())
        assertTrue(wq175ImportPreviewPage((0..5).toList(),99).isEmpty())
        assertEquals(0,wq175ImportPreviewPage((0..5).toList(),-1).first().index)
    }
    @Test fun customPageSizeIsBounded() {
        assertEquals(3,wq175ImportPreviewPage((0..12).toList(),2,3).size)
        assertEquals(6,wq175ImportPreviewPage((0..12).toList(),2,3).first().index)
    }
    @Test(expected=IllegalArgumentException::class)
    fun invalidPageSizeRejected() {
        wq175ImportPreviewPage((1..5).toList(),0,0)
    }
}
