package com.kelimebulmacasi.a1
import org.junit.Assert.*
import org.junit.Test
class Wq182IconRoutingTest {
    @Test fun userPackHasExactlyThirtyThreeDistinctIllustrations(){
        assertEquals(33,Wq182Icons.catalogSize())
        val mapped=listOf("arena_duel","arena_team","arena_quiz",
            "packages","assistant","sentence","story","speak",
            "mistakes","analytics","insights","account",
            "level_a1","level_a2","level_b1","level_b2","level_c1","level_c2")
        assertTrue(mapped.mapNotNull{Wq182Icons.preferred(it)}.all{Wq182Icons.has(it)})
        assertEquals("wq_ortak_bulmaca",Wq182Icons.preferred("arena_team"))
        assertEquals("wq_bes_sikli_duello",Wq182Icons.preferred("arena_quiz"))
        assertEquals("wq_bulmaca_duellosu",Wq182Icons.preferred("arena_duel"))
    }
    @Test fun premiumApprovedRoomIconsStayUntouched(){
        for(name in listOf("trophy","puzzle","cards","yds","grammar","listen","plan","synonym"))
            assertNull(Wq182Icons.preferred(name))
    }
}