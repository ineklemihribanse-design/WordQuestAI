package com.kelimebulmacasi.a1
import org.junit.Assert.*
import org.junit.Test
class Wq150HomeModesTest{
    @Test fun exclusiveLists(){
        assertEquals(listOf("duel","coop","quiz","team"),Wq150HomeModes.visible(false))
        assertEquals(listOf("classic","timed","test","obstacle"),Wq150HomeModes.visible(true))
        assertTrue(Wq150HomeModes.visible(true).intersect(Wq150HomeModes.visible(false).toSet()).isEmpty())
    }
    @Test fun cardsNeverOverlapFooterAndBothTabsUseSameGeometry(){
        listOf(480f,560f,640f,700f,780f,844f,920f).forEach{h->
            listOf(false,true).forEach{wide->
                val m=Wq150HomeLayout.measure(h,wide)
                assertTrue(m.height>=h)
                assertTrue(m.cardHeight(wide)>=if(wide)88f else 56f)
                assertTrue(m.cardTop+m.cardArea<=m.footerTop)
            }
        }
        assertEquals(780f,Wq150HomeLayout.measure(780f,false).height,0.001f)
        assertEquals(800f,Wq150HomeLayout.measure(800f,true).height,0.001f)
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
