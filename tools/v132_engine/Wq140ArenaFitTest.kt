package com.kelimebulmacasi.a1
import org.junit.Assert.*
import org.junit.Test
class Wq140ArenaFitTest {
    @Test fun landscapeBoardsFitBothAxes(){
        for(r in listOf(8,20,30,50,80))for(c in listOf(8,20,30,50,80)){
            val width=560f;val height=400f;val gap=.8f
            val cell=Wq140ArenaFit.cell(width,height,r,c,gap)
            assertTrue(cell>0f)
            assertTrue(cell*c+gap*(c-1)+8f<=width+.01f)
            assertTrue(cell*r+gap*(r-1)+8f<=height+.01f)
        }
    }
}
