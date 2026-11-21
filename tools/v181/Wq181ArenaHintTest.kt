package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq181ArenaHintTest {
    @Test fun stableRandomHintIsAlwaysAnUnopenedCharacter(){
        val pattern="..A...B"
        val pos=Wq179ArenaOrder.hintPosition("ROOM1",5,pattern)
        assertTrue(pos in pattern.indices)
        assertEquals('.',pattern[pos])
        assertEquals(pos,Wq179ArenaOrder.hintPosition("ROOM1",5,pattern))
    }
    @Test fun fullWordCannotReceiveASecondHint(){
        assertEquals(-1,Wq179ArenaOrder.hintPosition("ROOM1",5,"COMPLETE"))
    }
    @Test fun hintPositionsVaryWithDifferentRoomAndWord(){
        val positions=(0..28).map{
            Wq179ArenaOrder.hintPosition("ROOM_$it",it,"........")
        }
        assertTrue(positions.toSet().size>2)
    }
}
