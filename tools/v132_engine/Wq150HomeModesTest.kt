package com.kelimebulmacasi.a1
import org.junit.Assert.*
import org.junit.Test
class Wq150HomeModesTest{
    @Test fun retainedViewportSurvivesReentryAndReplacesOnlyOnDifferentViewport(){
        val cache=Wq150RetainedFrame<String>()
        val phone=setOf("phone","crest")
        val tablet=setOf("tablet","crest")
        val first=cache.publish(phone,mapOf("phone" to "p","crest" to "c"))
        assertSame(first,cache.frame(phone))
        assertEquals("c",cache.image("crest"))
        assertNull(cache.frame(tablet))
        val next=cache.publish(tablet,mapOf("tablet" to "t","crest" to "c"))
        assertSame(next,cache.frame(tablet))
        assertNull(cache.frame(phone))
        assertNull(cache.image("phone"))
    }
    @Test fun preparedHomeIncludesBothTabsAndFooter(){
        for(wide in listOf(false,true))for(newArt in listOf(false,true)){
            val keys=Wq150HomeArtPlan.keys("background","settingsBackground",wide,newArt)
            assertTrue(keys.containsAll(listOf("background","settingsBackground","crest","book","back",
                "phone_card_team","phone_card_duel","arena_v122/solo.webp","friends","modes","ranking","history","settings","info")))
            (Wq150HomeModes.online+Wq150HomeModes.solo).forEach{kind->
                assertTrue(keys.contains(Wq150HomeArtPlan.icon(kind,newArt)))
                Wq150HomeArtPlan.plate(kind,wide,newArt)?.let{assertTrue(keys.contains(it))}
            }
            if(newArt)assertTrue(keys.containsAll(listOf("arena_v150_premium/team_crest.webp",
                "arena_v150_premium/duel_plate.webp","arena_v150_premium/coop_plate.webp",
                "arena_v150_premium/quiz_plate.webp","arena_v150_premium/team_plate.webp")))
        }
    }
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
    @Test fun shortLandscapeTabletsFitAllControlsWithoutScrolling(){
        for(h in 460..900){
            val m=Wq150HomeLayout.measure(h.toFloat(),true)
            assertEquals("available safe height $h",h.toFloat(),m.height,.001f)
            assertTrue("card touch area at $h",m.cardHeight(true)>=88f)
            assertTrue("footer separation at $h",m.cardTop+m.cardArea<=m.footerTop)
            assertTrue("footer inside safe viewport at $h",m.footerTop+m.footerHeight<=h)
        }
        // 1724 x 1018 at 240 dpi, after status/navigation bar insets.
        for(h in listOf(600f,620f,630f,650f,678f)){
            assertEquals(h,Wq150HomeLayout.measure(h,true).height,.001f)
        }
    }
    @Test fun podiumCoordinatesMatchEachAsset(){
        val phone=Wq150Podium.anchor(1080f,2340f,false,true)
        assertEquals(540f,phone.first,.01f)
        assertEquals(807.3f,phone.second,.1f)
        val tablet=Wq150Podium.anchor(2560f,1600f,true,true)
        assertEquals(1280f,tablet.first,.01f)
        assertEquals(808f,tablet.second,.1f)
        val fallback=Wq150Podium.anchor(1080f,2340f,false,false)
        assertEquals(788.4f,fallback.first,.1f)
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
