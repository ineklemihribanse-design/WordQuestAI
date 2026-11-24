package com.kelimebulmacasi.a1
import org.junit.Assert.*
import org.junit.Test
class Wq119LenaFaceTest {
    @Test fun genuineSpeechOnlyAndNaturalClosure() {
        val f=Wq119LenaFace()
        f.frame(1000,false,"A",1f,1000,"TEXT_RANGE_APPROXIMATION",false)
        assertEquals(0f,f.frame(1040,false,"A",1f,1000,"TEXT_RANGE_APPROXIMATION",false)[0],.0001f)
        assertTrue(f.frame(1080,true,"A",1f,1060,"TEXT_RANGE_APPROXIMATION",false)[0]>.3f)
        assertTrue(f.frame(1120,true,"O",.9f,1100,"TEXT_RANGE_APPROXIMATION",false)[2]>.3f)
        for(i in 1..12) f.frame(1120+i*40L,false,"A",1f,1100,"TEXT_RANGE_APPROXIMATION",false)
        assertEquals(0f,f.frame(1640,false,"A",1f,1100,"TEXT_RANGE_APPROXIMATION",false)[0],.003f)
        assertEquals(0f,f.frame(1680,true,"A",1f,1000,"TEXT_RANGE_APPROXIMATION",false)[0],.003f)
        assertEquals(0f,f.frame(1720,true,"A",1f,1720,"NONE",false)[0],.003f)
    }
    @Test fun closedConsonantsAndNonfiniteCues() {
        val f=Wq119LenaFace()
        f.frame(1000,true,"MBP",1f,1000,"TIMED_VISEME",false)
        assertTrue(f.frame(1040,true,"MBP",1f,1040,"TIMED_VISEME",false).take(4).all{it==0f})
        assertTrue(f.frame(1080,true,"A",Float.NaN,1080,"TIMED_VISEME",false).take(4).all{it==0f})
    }
    @Test fun blinkAndSmileAreIndependentAndBounded() {
        val f=Wq119LenaFace()
        f.frame(1000,false,"NEUTRAL",0f,0,"NONE",false)
        assertTrue(f.frame(5240,false,"NEUTRAL",0f,0,"NONE",true)[5] in .4f..1f)
        val w=f.frame(5270,false,"NEUTRAL",0f,0,"NONE",true)
        assertEquals(1f,w[5],.001f)
        assertTrue(w[4] in 0f.. .32f)
        assertTrue(w.take(4).all{it==0f})
        assertEquals(0f,f.frame(5410,false,"NEUTRAL",0f,0,"NONE",false)[5],.001f)
        assertTrue(f.frame(5450,true,"A",1f,5450,"TIMED_VISEME",true,false).all{it==0f})
    }
}
