package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq179ArenaOrderTest {
    @Test fun shuffleIsStableAndNeverSelectsASolvedWord() {
        val code="DUELTEST"
        val first=Wq179ArenaOrder.next(code,"player",20,emptySet(),-1)
        assertEquals(first,Wq179ArenaOrder.next(code,"player",20,emptySet(),-1))
        val solved=(0 until 20).filter{it!=3}.toSet()
        assertEquals(3,Wq179ArenaOrder.next(code,"player",20,solved,first))
    }
    @Test fun randomOrderIsNotSimplyLinearForEveryRoom() {
        val first=(0..40).map{Wq179ArenaOrder.next("ROOM"+it,"player",30,emptySet(),-1)}
        assertTrue(first.toSet().size>5)
    }
    @Test fun hintOnlyChoosesUnrevealedPositionsAndIsStable(){
        val pattern="A.B..D."
        val pos=Wq179ArenaOrder.hintPosition("DUEL",4,pattern)
        assertTrue(pos in listOf(1,3,4,6))
        assertEquals(pos,Wq179ArenaOrder.hintPosition("DUEL",4,pattern))
        assertEquals(-1,Wq179ArenaOrder.hintPosition("DUEL",4,"ABCDEFG"))
    }
    @Test fun noUnsolvedWordLeavesCurrentSelection() {
        assertEquals(4,Wq179ArenaOrder.next("XYZ","abc",5,(0 until 5).toSet(),4))
    }
}
