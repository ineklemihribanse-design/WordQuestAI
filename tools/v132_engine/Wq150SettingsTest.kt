package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test
class Wq150SettingsTest {
    @Test fun allTwentyModesAreRepresented(){
        assertEquals(20,Wq150ModeCatalog.kinds.keys.sumOf{Wq150ModeCatalog.modes(it).size})
        assertTrue(Wq150ModeCatalog.modes("quiz").containsKey("simultaneous"))
        assertFalse(Wq150ModeCatalog.modes("quiz").containsKey("strategy"))
    }
    @Test fun multiLevelSelectionCannotBeEmpty(){
        val s=Wq150Settings().toggleLevel("B1").toggleLevel("A1").toggleLevel("B1")
        assertEquals(listOf("B1"),s.levels)
        assertTrue(s.valid())
        val y=s.changeScale("yds").toggleLevel("hard")
        assertEquals(listOf("easy","hard"),y.levels)
    }
    @Test fun modeAndFormatTransitionsStayValid(){
        val t=Wq150Settings().changeKind("coop").copy(seconds=0).changeMode("timed")
        assertEquals(300,t.seconds);assertTrue(t.valid())
        assertTrue(t.changeKind("team").changeFormat("quiz").valid())
        assertFalse(t.copy(topic="daily",wordType="noun").valid())
    }
    @Test fun invalidLimitsAreRejected(){
        assertFalse(Wq150Settings(count=71).valid())
        assertFalse(Wq150Settings(limit=4).valid())
        assertFalse(Wq150Settings(hints=21).valid())
        assertFalse(Wq150Settings(levels=emptyList()).valid())
    }
}
