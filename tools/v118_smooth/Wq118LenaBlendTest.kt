package com.kelimebulmacasi.a1
import org.junit.Assert.*
import org.junit.Test

class Wq118LenaBlendTest {
    @Test fun startsAtExactPreviousPoseAndFinishesSmoothly() {
        val b=Wq118LenaBlend()
        b.request(0,4f,true)
        assertEquals(0,b.frame(0L)!!.current)
        var prior=b.frame(100_000_000L)!!
        assertEquals(.1f,prior.time,.001f)
        b.request(1,5f,false)
        val first=b.frame(200_000_000L)!!
        assertEquals(0,first.previous)
        assertEquals(prior.time,first.previousTime,.0001f)
        assertEquals(0f,first.alpha,.0001f)
        var alpha=0f
        for (i in 3..9) {
            val s=b.frame(i*100_000_000L)!!
            assertEquals(1,s.current)
            assertTrue(s.alpha>=alpha)
            alpha=s.alpha
        }
        assertEquals(1f,alpha,.0001f)
        assertNull(b.frame(1_000_000_000L)!!.previous)
    }
    @Test fun rapidRequestsAreCoalescedWithoutHardCuts() {
        val b=Wq118LenaBlend()
        b.request(0,5f,true); b.frame(0L)
        b.request(1,5f,true); b.frame(100_000_000L)
        b.request(2,5f,true);b.request(3,5f,true)
        for(i in 2..7) assertEquals(1,b.frame(i*100_000_000L)!!.current)
        val s=b.frame(800_000_000L)!!
        assertEquals(3,s.current)
        assertEquals(1,s.previous)
        assertEquals(0f,s.alpha,.001f)
    }
    @Test fun loopingClipDoesNotSnapToItsFirstPose() {
        val b=Wq118LenaBlend()
        b.request(7,1f,true);b.frame(0L)
        var s=b.frame(100_000_000L)!!
        for(i in 2..14) {
            s=b.frame(i*100_000_000L)!!
            if(s.previous!=null) {
                assertEquals(7,s.previous)
                assertEquals(1f,s.previousTime,.001f)
                assertEquals(0f,s.alpha,.001f)
                return
            }
        }
        fail("Loop seam was not blended")
    }
    @Test fun nonloopHoldsAndPausedRendererDoesNotJump() {
        val b=Wq118LenaBlend()
        b.request(4,.2f,false);b.frame(0L)
        assertEquals(.1f,b.frame(5_000_000_000L)!!.time,.001f)
        b.frame(5_100_000_000L)
        val s=b.frame(5_200_000_000L)!!
        assertEquals(.2f,s.time,.001f)
        assertNull(s.previous)
    }
}
