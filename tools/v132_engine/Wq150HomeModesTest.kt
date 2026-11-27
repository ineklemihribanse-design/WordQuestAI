package com.kelimebulmacasi.a1
import org.junit.Assert.*
import org.junit.Test
class Wq150HomeModesTest{
    @Test fun exclusiveLists(){
        assertEquals(listOf("duel","coop","quiz","team"),Wq150HomeModes.visible(false))
        assertEquals(listOf("classic","timed","test","obstacle"),Wq150HomeModes.visible(true))
        assertTrue(Wq150HomeModes.visible(true).intersect(Wq150HomeModes.visible(false).toSet()).isEmpty())
    }
    @Test fun localRulesUseRealSupportedModes(){
        assertEquals("quiz",Wq150HomeModes.localEngine("test"))
        assertEquals("duel",Wq150HomeModes.localEngine("classic"))
        assertEquals(listOf(0),Wq150HomeModes.localTimes("classic"))
        assertTrue(Wq150HomeModes.localTimes("timed").all{it>0})
        assertEquals(listOf(20),Wq150HomeModes.localTimes("obstacle"))
        assertEquals(listOf(0),Wq150HomeModes.localHints("obstacle"))
    }
}
