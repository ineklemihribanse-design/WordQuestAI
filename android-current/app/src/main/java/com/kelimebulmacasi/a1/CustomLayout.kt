package com.kelimebulmacasi.a1

import kotlin.random.Random
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

private data class FilledCell(val letter: Char, val directions: Set<Direction>)
private data class Match(val placement: Placement, val area: Int, val crossings: Int)
internal const val MAX_BOARD_WORDS = 70
internal const val MAX_BOARD_SIDE = 78

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

/**
 * Packs 51–70 words into ONE real crossword, using a larger candidate pool.
 *
 * With a fixed random set of 70 words, many letter intersections are impossible.
 * This builder chooses exactly [requested] distinct entries as it places them;
 * entries that do not fit are skipped rather than silently dropping words.
 * Each placement uses the same crossing and adjacency validation as customPuzzle.
 * If all [requested] words cannot be placed, return null (never a partial board).
 */
internal suspend fun flexibleLargePuzzle(
    candidates: List<Entry>,
    requested: Int,
    seed: Int,
): Puzzle? {
    require(requested in 51..MAX_BOARD_WORDS)
    val unique = candidates.filter { it.letters.length in 2..22 && it.clue.isNotBlank() }
        .distinctBy(Entry::letters)
    if (unique.size < requested) return null

    repeat(3) { trial ->
        currentCoroutineContext().ensureActive()
        val random = Random(seed xor (trial * 104729 + 0x1942))
        val poolSize = minOf(unique.size, maxOf(requested * 3, 200))
        val sample = unique.shuffled(random).take(poolSize)
        val start = sample.filter { it.letters.length in 5..13 }
            .maxByOrNull { it.letters.toSet().size + random.nextInt(6) }
            ?: sample.first()
        val placed = mutableListOf(start to Placement(Cell(0, 0), Direction.Across))
        val board = mutableMapOf<Cell, FilledCell>()
        addWord(board, start.letters, placed.first().second)
        val remaining = sample.filterNot { it.letters == start.letters }.toMutableList()

        while (placed.size < requested && remaining.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val minRow = board.keys.minOf { it.row }
            val maxRow = board.keys.maxOf { it.row }
            val minColumn = board.keys.minOf { it.column }
            val maxColumn = board.keys.maxOf { it.column }
            var bestIndex = -1
            var bestPlacement: Placement? = null
            var bestScore = Int.MAX_VALUE

            // Sample different words each turn to keep complexity bounded on phones.
            val options = remaining.indices.shuffled(random).take(minOf(90, remaining.size))
            for (index in options) {
                val item = remaining[index]
                val arrangements = matches(
                    item.letters, board, minRow, maxRow, minColumn, maxColumn, MAX_BOARD_SIDE,
                )
                for (fit in arrangements) {
                    val end = fit.placement.cell(item.letters.lastIndex)
                    val height = maxOf(maxRow, end.row) - minOf(minRow, fit.placement.start.row) + 1
                    val width = maxOf(maxColumn, end.column) - minOf(minColumn, fit.placement.start.column) + 1
                    val score = fit.area + 2 * (height - width) * (height - width) -
                        fit.crossings * 24 - item.letters.length * 2 + random.nextInt(23)
                    if (score < bestScore) {
                        bestScore = score
                        bestIndex = index
                        bestPlacement = fit.placement
                    }
                }
            }

            // If the 90-word sample did not intersect, search all remaining words.
            if (bestIndex < 0) {
                for (index in remaining.indices) {
                    currentCoroutineContext().ensureActive()
                    val item = remaining[index]
                    for (fit in matches(
                        item.letters, board, minRow, maxRow, minColumn, maxColumn, MAX_BOARD_SIDE,
                    )) {
                        val end = fit.placement.cell(item.letters.lastIndex)
                        val height = maxOf(maxRow, end.row) - minOf(minRow, fit.placement.start.row) + 1
                        val width = maxOf(maxColumn, end.column) - minOf(minColumn, fit.placement.start.column) + 1
                        val score = fit.area + 2 * (height - width) * (height - width) -
                            fit.crossings * 24 - item.letters.length * 2
                        if (score < bestScore) {
                            bestScore = score
                            bestIndex = index
                            bestPlacement = fit.placement
                        }
                    }
                }
            }
            if (bestIndex < 0 || bestPlacement == null) break
            val chosen = remaining.removeAt(bestIndex)
            val placement = bestPlacement
            addWord(board, chosen.letters, placement)
            placed += chosen to placement
        }

        if (placed.size == requested) {
            val puzzle = Puzzle.create(
                placed.map { it.first },
                placed.map { it.second },
            )
            if (puzzle.words.size == requested &&
                puzzle.words.map { it.entry.letters }.toSet().size == requested &&
                puzzle.rows <= MAX_BOARD_SIDE && puzzle.columns <= MAX_BOARD_SIDE
            ) return puzzle
        }
    }
    return null
}
