package com.kelimebulmacasi.a1

import kotlin.random.Random
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

private data class FilledCell(val letter: Char, val directions: Set<Direction>)
private data class Match(val placement: Placement, val area: Int, val crossings: Int)
internal const val MAX_BOARD_WORDS = 50
internal const val MAX_BOARD_SIDE = 60

internal suspend fun customPuzzle(
    entries: List<Entry>,
    seed: Int = entries.map(Entry::letters).hashCode(),
    accept: (Puzzle) -> Boolean = { true },
): Puzzle? {
    require(entries.size in 6..MAX_BOARD_WORDS)
    require(entries.all { it.letters.length in 2..22 })
    require(entries.map { it.letters }.toSet().size == entries.size)
    val connectedLetters = entries.first().letters.toCollection(mutableSetOf())
    val unchecked = entries.drop(1).toMutableList()
    while (unchecked.isNotEmpty()) {
        val next = unchecked.indexOfFirst { word -> word.letters.any(connectedLetters::contains) }
        if (next == -1) return null
        unchecked.removeAt(next).letters.toCollection(connectedLetters)
    }
    val random = Random(seed)
    val byLength = entries.indices.sortedByDescending { entries[it].letters.length }
    val limit = when {
        entries.size <= 20 -> 30
        entries.size <= 30 -> 40
        else -> MAX_BOARD_SIDE
    }
    repeat(if (entries.size <= 20) 70 else 20) {
        currentCoroutineContext().ensureActive()
        val remaining = byLength.toMutableList()
        val first = remaining.removeAt(random.nextInt(minOf(4, remaining.size)))
        val firstDirection = Direction.entries[random.nextInt(2)]
        val placements = mutableMapOf(first to Placement(Cell(0, 0), firstDirection))
        val board = mutableMapOf<Cell, FilledCell>()
        addWord(board, entries[first].letters, placements.getValue(first))
        while (remaining.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val minRow = board.keys.minOf { it.row }
            val maxRow = board.keys.maxOf { it.row }
            val minColumn = board.keys.minOf { it.column }
            val maxColumn = board.keys.maxOf { it.column }
            var best: Match? = null
            var bestWord = -1
            var bestScore = Int.MAX_VALUE
            for (index in remaining) {
                val letters = entries[index].letters
                val options = matches(letters, board, minRow, maxRow, minColumn, maxColumn, limit)
                for (candidate in options) {
                    val score = candidate.area - candidate.crossings * 12 - letters.length +
                        options.size / 2 + random.nextInt(45)
                    if (score < bestScore) {
                        best = candidate
                        bestWord = index
                        bestScore = score
                    }
                }
            }
            val choice = best ?: break
            placements[bestWord] = choice.placement
            addWord(board, entries[bestWord].letters, choice.placement)
            remaining.remove(bestWord)
        }
        if (remaining.isEmpty()) {
            val puzzle = Puzzle.create(entries, entries.indices.map(placements::getValue))
            if (accept(puzzle)) return puzzle
        }
    }
    return null
}

private fun matches(
    word: String,
    board: Map<Cell, FilledCell>,
    minRow: Int,
    maxRow: Int,
    minColumn: Int,
    maxColumn: Int,
    limit: Int,
): List<Match> {
    val checked = mutableSetOf<Placement>()
    return buildList {
        for ((cross, existing) in board) {
            if (existing.directions.size != 1) continue
            val direction = existing.directions.first().perpendicular
            for ((index, letter) in word.withIndex()) {
                if (letter != existing.letter) continue
                val position = Placement(
                    Cell(cross.row - direction.dr * index, cross.column - direction.dc * index), direction,
                )
                if (!checked.add(position)) continue
                val end = position.cell(word.lastIndex)
                val height = maxOf(maxRow, end.row) - minOf(minRow, position.start.row) + 1
                val width = maxOf(maxColumn, end.column) - minOf(minColumn, position.start.column) + 1
                if (height > limit || width > limit) continue
                val crossingCount = crossings(word, position, board)
                if (crossingCount > 0) add(Match(position, height * width, crossingCount))
            }
        }
    }
}

private fun crossings(word: String, place: Placement, board: Map<Cell, FilledCell>): Int {
    if (place.cell(-1) in board || place.cell(word.length) in board) return -1
    val side = place.direction.perpendicular
    var count = 0
    for ((index, letter) in word.withIndex()) {
        val cell = place.cell(index)
        val existing = board[cell]
        if (existing != null) {
            if (existing.letter != letter || place.direction in existing.directions) return -1
            count++
        } else if (Cell(cell.row + side.dr, cell.column + side.dc) in board ||
            Cell(cell.row - side.dr, cell.column - side.dc) in board
        ) return -1
    }
    return count
}

private fun addWord(board: MutableMap<Cell, FilledCell>, word: String, place: Placement) {
    for ((index, letter) in word.withIndex()) {
        val cell = place.cell(index)
        val previous = board[cell]?.directions.orEmpty()
        board[cell] = FilledCell(letter, previous + place.direction)
    }
}
