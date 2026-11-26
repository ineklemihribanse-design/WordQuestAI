package com.kelimebulmacasi.a1
import org.junit.Test
import org.junit.Assert.*
class Wq121ArenaLayoutTest {
    @Test fun phoneAndTabletUseDifferentArtwork() {
        assertFalse(Wq121ArenaLayout.wide(400f,850f,400))
        assertTrue(Wq121ArenaLayout.wide(1280f,800f,800))
        assertFalse(Wq121ArenaLayout.wide(800f,1280f,800))
    }
    @Test fun podiumTracksCenterCropWithoutStretch() {
        val p=Wq121ArenaLayout.project(1080f,2340f,false,.73f,.385f)
        assertEquals(788.4f,p.first,.01f)
        assertEquals(900.9f,p.second,.01f)
        val t=Wq121ArenaLayout.project(2560f,1600f,true,.5f,.505f)
        assertEquals(1280f,t.first,.01f);assertEquals(808f,t.second,.01f)
        val narrow=Wq121ArenaLayout.project(400f,800f,false,.73f,.385f)
        assertTrue(narrow.first in 0f..400f && narrow.second in 0f..800f)
    }
}
