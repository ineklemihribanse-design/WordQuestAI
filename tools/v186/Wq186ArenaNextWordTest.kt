package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq186ArenaNextWordTest {
    @Test fun neverReturnsAlreadyFailedOrPassedWords() {
        val blocked = setOf(1, 3, 4)
        val solved = setOf(0, 7)
        for (current in 0 until 8) {
            val result = Wq186ArenaNextWord.next("ARENA", "player", 8,
                solved, current) { it !in blocked }
            if (result != null) {
                assertFalse(result in blocked)
                assertFalse(result in solved)
                assertNotEquals(current, result)
            }
        }
    }

    @Test fun allWordsWaitingCannotBeMistakenForFreeSkip() {
        val next = Wq186ArenaNextWord.next("ROOM", "p", 20,
            emptySet(), 5) { false }
        assertNull(next)
    }

    @Test fun currentWordAloneCannotTriggerSkipOrSpendAllowance() {
        val next = Wq186ArenaNextWord.next("ROOM", "p", 6,
            emptySet(), 2) { it == 2 }
        assertNull(next)
    }

    @Test fun chosenWordIsDeterministicForSameServerState() {
        val f: (Int) -> Boolean = { it in setOf(1, 6, 8, 9) }
        val first = Wq186ArenaNextWord.next("ROOM", "p", 10,
            setOf(1), 8, f)
        assertNotNull(first)
        repeat(5) {
            assertEquals(first, Wq186ArenaNextWord.next("ROOM", "p",
                10, setOf(1), 8, f))
        }
    }

    @Test fun boundaryCasesDoNotCrashOrSelectPhantomWords() {
        assertNull(Wq186ArenaNextWord.next("ROOM", "p", 0,
            emptySet(), -1) { true })
        assertNull(Wq186ArenaNextWord.next("ROOM", "p", -1,
            emptySet(), 2) { true })
        assertEquals(0, Wq186ArenaNextWord.next("ROOM", "p", 1,
            emptySet(), -1) { true })
    }
}
