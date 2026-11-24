package com.kelimebulmacasi.a1

import org.junit.Assert.*
import org.junit.Test

class Wq212NumberedCellSelectionTest {
    private fun crossingPuzzle() = Puzzle(6, 5, listOf(
        Word(10, Entry("TRUST", "Güven"), Placement(Cell(1, 0), Direction.Across)),
        Word(11, Entry("RIVER", "Nehir"), Placement(Cell(1, 1), Direction.Down)),
        Word(9, Entry("AS", "Gibi"), Placement(Cell(0, 3), Direction.Down))
    ))

    @Test fun tappingNumber11SelectsItsClueInsteadOfAcross10() {
        val engine = Crossword(crossingPuzzle())
        engine.selectWord(0)
        engine.selectCell(Cell(1, 1))
        assertEquals(11, engine.word.number)
        assertEquals(Direction.Down, engine.word.direction)
        assertEquals(Cell(1, 1), engine.view().cell)
        engine.selectCell(Cell(1, 1))
        assertEquals(11, engine.word.number)
        engine.selectCell(Cell(1, 0))
        assertEquals(10, engine.word.number)
    }

    @Test fun ordinaryCellsAndUnnumberedCrossingKeepExistingSelectionBehavior() {
        val engine = Crossword(crossingPuzzle())
        engine.selectWord(0)
        engine.selectCell(Cell(1, 2))
        assertEquals(10, engine.word.number)
        assertEquals(Cell(1, 2), engine.view().cell)
        engine.selectCell(Cell(1, 3))
        assertEquals(10, engine.word.number)
        engine.selectCell(Cell(1, 3))
        assertEquals(9, engine.word.number)
    }

    @Test fun numberedSolvedWordRemainsExplicitlySelectable() {
        val engine = Crossword(crossingPuzzle(), Progress(lockedWords = setOf(1)))
        engine.selectWord(0)
        engine.selectCell(Cell(1, 1))
        assertEquals(11, engine.word.number)
        assertTrue(engine.view().wordIndex in engine.view().solvedIndices)
    }

    @Test fun twoCluesSharingOneNumberStillToggleDirection() {
        val puzzle = Puzzle(3, 3, listOf(
            Word(1, Entry("CAT", "Kedi"), Placement(Cell(0, 0), Direction.Across)),
            Word(1, Entry("CAR", "Araba"), Placement(Cell(0, 0), Direction.Down))
        ))
        val engine = Crossword(puzzle)
        engine.selectCell(Cell(0, 0))
        assertEquals(Direction.Across, engine.word.direction)
        engine.selectCell(Cell(0, 0))
        assertEquals(Direction.Down, engine.word.direction)
        engine.selectCell(Cell(0, 0))
        assertEquals(Direction.Across, engine.word.direction)
    }

    @Test fun blankCellDoesNotChangeSelectionOrProgress() {
        val engine = Crossword(crossingPuzzle())
        val before = engine.view()
        engine.selectCell(Cell(5, 4))
        assertEquals(before, engine.view())
    }
}
