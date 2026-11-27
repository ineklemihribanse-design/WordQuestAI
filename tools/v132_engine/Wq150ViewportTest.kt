package com.kelimebulmacasi.a1
import org.junit.Assert.*
import org.junit.Test
class Wq150ViewportTest {
    @Test fun soundPacksHaveStableDefaults(){
        assertEquals(5,Wq150SoundPacks.names.size)
        assertEquals("crystal",Wq150SoundPacks.normalize(null))
        assertEquals("crystal",Wq150SoundPacks.normalize("unknown"))
        assertEquals("silent",Wq150SoundPacks.normalize("silent"))
        assertEquals(5,Wq150SoundPacks.events.size)
    }
    @Test fun feedbackOnlyPlaysForNewConfirmedCounts(){
        assertEquals(0,wq150Feedback(null,3 to 2));assertEquals(0,wq150Feedback(3 to 2,3 to 2));assertEquals(1,wq150Feedback(3 to 2,4 to 2));assertEquals(-1,wq150Feedback(3 to 2,3 to 3));assertEquals(0,wq150Feedback(3 to 2,null))
    }
    @Test fun completeBoardFitsPhoneAndTablet(){
        for((w,h) in listOf(320f to 200f,1100f to 640f,320f to 70f)){
            val v=wq150Viewport(w,h,22,17,0,11f,8.5f)
            assertTrue(v.cell*17<=w+.01f);assertTrue(v.cell*22<=h+.01f)
            assertTrue(v.x>=0f);assertTrue(v.y>=0f)
        }
    }
    @Test fun zoomCentersSelectedWordWithinBounds(){
        val normal=wq150Viewport(320f,300f,20,20,0,10f,10f)
        val medium=wq150Viewport(320f,300f,20,20,1,10f,10f)
        val close=wq150Viewport(320f,300f,20,20,2,10f,10f)
        assertEquals(normal.cell*1.55f,medium.cell,.01f)
        assertEquals(normal.cell*2.35f,close.cell,.01f)
        assertEquals(160f,close.x+10f*close.cell,.01f)
        assertEquals(150f,close.y+10f*close.cell,.01f)
    }
    @Test fun edgeWordCannotPanBeyondBoard(){
        val v=wq150Viewport(320f,250f,20,20,2,19.5f,19.5f)
        assertTrue(v.x in v.minX..v.maxX);assertTrue(v.y in v.minY..v.maxY)
        assertEquals(v.minX,v.x,.01f);assertEquals(v.minY,v.y,.01f)
    }
    @Test fun narrowBoardRemainsCentered(){
        val v=wq150Viewport(800f,100f,20,2,2,10f,1f)
        assertEquals((800f-2*v.cell)/2f,v.x,.01f)
        assertEquals(v.minX,v.maxX,.01f)
    }
    @Test fun audioFeedbackOutranksClockAndAmbient(){
        for(event in listOf("correct","wrong")){
            assertTrue(wq150SoundPriority(event)>wq150SoundPriority("urgent"))
            assertTrue(wq150SoundPriority(event)>wq150SoundPriority("ambient"))
        }
        assertTrue(wq150SoundPriority("tick")>wq150SoundPriority("ambient"))
        assertTrue(wq150SoundVolume("ambient")<wq150SoundVolume("tick"))
        assertTrue(wq150SoundVolume("tick")<wq150SoundVolume("urgent"))
        assertTrue(wq150SoundVolume("urgent")<wq150SoundVolume("correct"))
    }
    @Test fun compressedArenaBoardKeepsWholeGridInFitMode(){
        for((w,h) in listOf(280f to 55f,360f to 110f,800f to 200f,1200f to 320f)){
            for((r,c) in listOf(8 to 12,22 to 17,30 to 30)){
                val v=wq150Viewport(w,h,r,c,0,r/2f,c/2f)
                assertTrue(v.cell>0f)
                assertTrue(v.x>=0f&&v.y>=0f)
                assertTrue(v.x+v.cell*c<=w+.01f)
                assertTrue(v.y+v.cell*r<=h+.01f)
            }
        }
    }
}
