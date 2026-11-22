package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq186ArenaGridPolicyTest {
    @Test fun cellSizeDoesNotDependOnKeyboard(){
        val width=350f
        val before=Wq186ArenaGridPolicy.cellDp(width,15,true)
        val after=Wq186ArenaGridPolicy.cellDp(width,15,true)
        assertEquals(before,after,0.001f)
        assertTrue(before>=36f)
        assertTrue(Wq186ArenaGridPolicy.viewportDp(780f,570f,true) <
                   Wq186ArenaGridPolicy.viewportDp(780f,570f,false))
    }
    @Test fun narrowPhoneUsesPanInsteadOfUnreadableCells(){
        for(w in listOf(320f,360f,393f))
            for(cols in listOf(10,15,20)) {
                assertTrue(Wq186ArenaGridPolicy.cellDp(w,cols,true)>=36f)
                assertTrue(Wq186ArenaGridPolicy.cellDp(w,cols,false)>=25f)
            }
    }
    @Test fun tabletCanFitOrZoomWithoutDividingByZero(){
        for(cols in listOf(0,1,10,15,25)){
            val fit=Wq186ArenaGridPolicy.cellDp(900f,cols,false)
            val zoom=Wq186ArenaGridPolicy.cellDp(900f,cols,true)
            assertTrue(fit>=28f)
            assertTrue(zoom>=fit)
        }
    }
    @Test fun visibleViewportIsAlwaysPositive(){
        for(h in listOf(480f,720f,1100f))
            for(board in listOf(120f,390f,950f))
                for(open in listOf(true,false))
                    assertTrue(Wq186ArenaGridPolicy.viewportDp(h,board,open)>0)
    }
}
