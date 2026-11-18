package com.kelimebulmacasi.a1

import kotlin.random.Random

// Awards, solved words and hints in storage use the original puzzle's indices.
internal class BoardRound(val original: Puzzle, val puzzle: Puzzle, val layout: List<Placement>) {
    private val baseIndices = original.words.map { it.entry.letters }.withIndex().associate { it.value to it.index }
    private val boardIndices = puzzle.words.map { it.entry.letters }.withIndex().associate { it.value to it.index }
    private val toBase = puzzle.words.map { baseIndices.getValue(it.entry.letters) }
    private val toBoard = original.words.map { boardIndices.getValue(it.entry.letters) }

    fun onBoard(saved: Progress): Progress = saved.remap(toBoard).copy(
        grid = saved.grid.takeIf { saved.layout == layout }.orEmpty(), layout = emptyList(), layoutAt = 0,
    )

    fun inStore(played: Progress, layoutAt: Long): Progress = played.remap(toBase).copy(layout = layout, layoutAt = layoutAt)

    fun moveProgress(saved: Progress, next: Puzzle): Progress {
        val previous = onBoard(saved)
        val placements = next.words.associateBy { it.entry.letters }
        val nextLayout = original.words.map { placements.getValue(it.entry.letters).placement }
        val nextRound = BoardRound(original, next, nextLayout)
        val grid = CharArray(next.rows * next.columns) { '.' }
        for (word in puzzle.words) {
            val newWord = placements.getValue(word.entry.letters)
            word.cells.forEachIndexed { index, cell ->
                val letter = previous.grid.getOrNull(cell.row * puzzle.columns + cell.column)
                if (letter == word.entry.letters[index]) {
                    val target = newWord.cells[index]
                    grid[target.row * next.columns + target.column] = letter
                }
            }
        }
        val changedAt = maxOf(System.currentTimeMillis(), saved.updatedAt + 1, saved.layoutAt + 1, saved.resetAt + 1)
        val moved = saved.copy(grid = String(grid), layout = nextLayout, layoutAt = changedAt, updatedAt = changedAt)
        return nextRound.inStore(Crossword(next, nextRound.onBoard(moved)).progress(), changedAt)
    }

    companion object {
        fun open(base: Puzzle, layout: List<Placement>): BoardRound {
            val arranged = if (layout.size == base.words.size) runCatching {
                Puzzle.create(base.words.map(Word::entry), layout)
            }.getOrNull() else null
            return BoardRound(base, arranged ?: base, if (arranged == null) emptyList() else layout)
        }
    }
}

private fun Progress.remap(indices: List<Int>): Progress = copy(
    hinted = hinted.mapNotNull(indices::getOrNull).toSet(),
    awards = awards.mapNotNull { (index, award) -> indices.getOrNull(index)?.let { it to award } }.toMap(),
    lockedWords = lockedWords.mapNotNull(indices::getOrNull).toSet(),
)

internal suspend fun shuffleBoard(board: Puzzle, random: Random = Random.Default): Puzzle? {
    val order = board.words.map { it.entry.letters }
    fun changesBoth(candidate: Puzzle) =
        candidate.solution.keys != board.solution.keys && candidate.words.map { it.entry.letters } != order

    val entries = board.words.map(Word::entry)
    val generated = customPuzzle(entries.shuffled(random), random.nextInt(), ::changesBoth)
    if (generated != null) return generated

    val transposed = board.words.map { Placement(Cell(it.start.column, it.start.row), it.direction.perpendicular) }
    return Puzzle.create(entries, transposed).takeIf(::changesBoth)
}
