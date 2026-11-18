package com.kelimebulmacasi.a1

import java.util.UUID
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class PuzzleBatch(val games: List<Game>, val remaining: List<WordPair>, val used: Int)

internal val answerCounts = 8..20

internal fun automaticSplit(total: Int, preferred: Int): List<Int> {
    require(total >= 0 && preferred in answerCounts)
    if (total == 0) return emptyList()
    val boards = minOf((total - 1) / preferred + 1, (total / answerCounts.first).coerceAtLeast(1))
    val last = total - (boards - 1) * preferred
    if (last in answerCounts) return List(boards) { if (it == boards - 1) last else preferred }
    val even = total / boards
    val remainder = total % boards
    return List(boards) { even + if (it < remainder) 1 else 0 }
}

internal fun manualSplit(input: String, total: Int): List<Int>? {
    val values = input.trim().split(Regex("\\s*[+,;]\\s*|\\s+"))
    val sizes = values.map { it.toIntOrNull()?.takeIf(answerCounts::contains) ?: return null }
    return sizes.takeIf { sizes.sumOf(Int::toLong) == total.toLong() }
}

internal fun Game.hasSameAnswers(rows: List<WordPair>): Boolean =
    puzzle.words.map { it.entry.letters }.toSet() == rows.map { englishLetters(cleanAnswer(it.answer)) }.toSet()

internal suspend fun reviseCustomGame(original: Game, rows: List<WordPair>, title: String): Game? {
    require(original.mode == GameMode.Custom && rows.size in 6..20 && rows.all(WordPair::valid))
    val entries = rows.map { Entry(cleanAnswer(it.answer).uppercase(), it.clue.trim()) }
    val byAnswer = entries.associateBy(Entry::letters)
    require(byAnswer.size == rows.size)
    val keepBoard = original.hasSameAnswers(rows)
    val puzzle = if (keepBoard) {
        val words = original.puzzle.words
        Puzzle.create(words.map { byAnswer.getValue(it.entry.letters).copy(context = it.entry.context) }, words.map(Word::placement))
    } else customPuzzle(entries) ?: return null
    return Game(
        original.id, GameMode.Custom, original.level, original.category, title, puzzle, original.destination,
        progressId = if (keepBoard) original.progressId else original.id + ":" + UUID.randomUUID(),
        modifiedAt = maxOf(System.currentTimeMillis(), original.modifiedAt + 1),
    )
}

internal suspend fun pairBatches(
    rows: List<WordPair>,
    title: String,
    destination: Destination?,
    sizes: List<Int>,
): PuzzleBatch {
    require(sizes.isNotEmpty() && sizes.all(answerCounts::contains) && sizes.sum() == rows.size)
    val entries = rows.map { Entry(cleanAnswer(it.answer).uppercase(), it.clue.trim()) }
    require(rows.all(WordPair::valid) && entries.distinctBy(Entry::letters).size == entries.size)
    val ranges = sizes.runningFold(0, Int::plus).zipWithNext()
    val groups = ranges.map { (start, end) -> entries.subList(start, end) }
    var boards = emptyList<Pair<Int, Puzzle>>()
    // Try the original order first. Regroup when consecutive rows do not share letters.
    val pool = entries.shuffled(kotlin.random.Random(entries.hashCode()))
    for (grouping in listOf(groups, ranges.map { (start, end) -> pool.subList(start, end) })) {
        currentCoroutineContext().ensureActive()
        val found = grouping.mapIndexedNotNull { index, group -> customPuzzle(group)?.let { index to it } }
        if (found.sumOf { it.second.words.size } <= boards.sumOf { it.second.words.size }) continue
        boards = found
        if (boards.size == groups.size) break
    }
    val games = boards.map { (index, puzzle) ->
        Game(
            "custom_" + UUID.randomUUID(), GameMode.Custom, "", GameMode.Custom.title,
            if (sizes.size == 1) title else title + " · " + (index + 1),
            puzzle, destination,
        )
    }
    val placed = games.flatMap { game -> game.puzzle.words.map { it.entry.letters } }.toSet()
    val remaining = rows.filterNot { englishLetters(cleanAnswer(it.answer)) in placed }
    return PuzzleBatch(games, remaining, placed.size)
}
