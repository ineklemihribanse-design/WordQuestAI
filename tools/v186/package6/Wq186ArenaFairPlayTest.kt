package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq186ArenaFairPlayTest {
    @Test fun pointsDependOnLettersAndCefr() {
        assertEquals(12, Wq186ArenaFairPlay.wordPoints(4,"A1"))
        assertEquals(26, Wq186ArenaFairPlay.wordPoints(7,"B1"))
        assertEquals(36, Wq186ArenaFairPlay.wordPoints(6,"C2"))
        val score=Wq186ArenaFairPlay.totalScore(
            mapOf(0 to "one",1 to "two",2 to "one"),
            "one",{ if(it==0)4 else 6 },{ if(it==2)"B2" else "A1" },
            mapOf(0 to mapOf("one" to 1))
        )
        assertEquals(37,score) // A1 4 letters=12, B2 6 letters=28, minus 3
    }

    @Test fun shuffledOrderIsStableAndUnique() {
        val a=Wq186ArenaFairPlay.order("ROOM1",20)
        assertEquals(20,a.toSet().size)
        assertEquals(a,Wq186ArenaFairPlay.order("ROOM1",20))
        assertEquals((0 until 20).toSet(),a.toSet())
        assertTrue(Wq186ArenaFairPlay.order("EMPTY",0).isEmpty())
    }

    @Test fun deferredWordsOnlyAppearInFinalRound() {
        val room = "DUEL"
        val count = 6
        val deferred=setOf(0,1)
        val solved=setOf(2,3,4,5)
        assertNull(Wq186ArenaFairPlay.nextWord(room,count,solved,deferred,-1,false))
        assertTrue(Wq186ArenaFairPlay.finalRoundNeeded(count,solved,deferred))
        assertTrue(Wq186ArenaFairPlay.nextWord(room,count,solved,deferred,-1,true) in deferred)
    }

    @Test fun roomPresetRejectsDuplicatesAndClampsLimits() {
        val preset=Wq186MatchPreset(
            listOf("A1","A1","B2","C2","invalid","B2"),99,9).sanitized("A2")
        assertEquals(listOf("A1","B2","C2"),preset.levels)
        assertEquals(20,preset.hints)
        assertEquals(3,preset.retries)
        val fallback=Wq186MatchPreset(emptyList(),-4,-9).sanitized("B1")
        assertEquals(listOf("B1"),fallback.levels)
        assertEquals(0,fallback.hints)
        assertEquals(0,fallback.retries)
    }

    @Test fun finalRoundAndHintsHaveStrictBoundaries() {
        assertTrue(Wq186ArenaFairPlay.canRetry(0,3))
        assertTrue(Wq186ArenaFairPlay.canRetry(2,3))
        assertFalse(Wq186ArenaFairPlay.canRetry(3,3))
        assertFalse(Wq186ArenaFairPlay.canRetry(0,0))
        assertFalse(Wq186ArenaFairPlay.canSkip(2,2))
        assertEquals(0,Wq186ArenaFairPlay.hintsLeft(21,20))
        val used=mutableSetOf<Int>()
        repeat(5) {
            val position=Wq186ArenaFairPlay.hintPosition("room",5,used,".....")!!
            assertTrue(used.add(position))
        }
        assertNull(Wq186ArenaFairPlay.hintPosition("room",5,used,"....."))
    }

    @Test fun hintsAreDifferentAndSkipBounded() {
        val first=Wq186ArenaFairPlay.hintPosition("abc",3,emptySet(),".AB..")!!
        assertEquals(first,Wq186ArenaFairPlay.hintPosition("abc",3,emptySet(),".AB.."))
        val next=Wq186ArenaFairPlay.hintPosition("abc",3,setOf(first),".AB..")
        assertNotEquals(first,next)
        assertTrue(Wq186ArenaFairPlay.canSkip(1))
        assertFalse(Wq186ArenaFairPlay.canSkip(2))
        assertEquals(2,Wq186ArenaFairPlay.hintsLeft(3,5))
        assertFalse(Wq186ArenaFairPlay.validEmoji("x"))
        assertTrue(Wq186ArenaFairPlay.validEmoji("🏆"))
    }
}
