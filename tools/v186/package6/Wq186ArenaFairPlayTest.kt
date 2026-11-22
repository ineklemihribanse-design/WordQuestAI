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
        val room="DUEL",count=6
        val deferred=setOf(0,1)
        val solved=setOf(2,3,4,5)
        assertNull(Wq186ArenaFairPlay.nextWord(room,count,solved,deferred,-1,false))
        assertTrue(Wq186ArenaFairPlay.finalRoundNeeded(count,solved,deferred))
        assertTrue(Wq186ArenaFairPlay.nextWord(room,count,solved,deferred,-1,true) in deferred)
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
