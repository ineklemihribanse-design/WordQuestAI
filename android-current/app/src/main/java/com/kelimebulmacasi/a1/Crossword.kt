package com.kelimebulmacasi.a1

data class Cell(val row: Int, val column: Int)

enum class Direction(val label: String, val dr: Int, val dc: Int) {
    Across("Yatay", 0, 1),
    Down("Dikey", 1, 0);

    val perpendicular get() = if (this == Across) Down else Across
}

fun englishLetters(text: String): String = buildString {
    for (character in text) {
        val letter = if (character == 'ı' || character == 'İ') 'I' else character.uppercaseChar()
        if (letter in 'A'..'Z') append(letter)
    }
}

data class Entry(val answer: String, val clue: String, val context: String = "") {
    val letters = englishLetters(answer)
    val lengthLabel = answer.split(' ', '-').filter { it.isNotBlank() }.joinToString("–") {
        englishLetters(it).length.toString()
    }
}

data class Placement(val start: Cell, val direction: Direction) {
    fun cell(offset: Int) = Cell(start.row + offset * direction.dr, start.column + offset * direction.dc)
}

data class Word(val number: Int, val entry: Entry, val placement: Placement) {
    val direction get() = placement.direction
    val start get() = placement.start
    val cells = entry.letters.indices.map(placement::cell)
}

class Puzzle(val rows: Int, val columns: Int, val words: List<Word>) {
    init {
        require(rows > 0 && columns > 0 && words.isNotEmpty())
        require(words.map { it.entry.letters }.distinct().size == words.size)
    }

    val solution: Map<Cell, Char> = buildMap {
        for (word in words) {
            for ((cell, letter) in word.cells.zip(word.entry.letters.asIterable())) {
                require(cell.row in 0 until rows && cell.column in 0 until columns)
                require(get(cell)?.let { it == letter } != false)
                put(cell, letter)
            }
        }
    }
    val wordsAt = words.indices.flatMap { index -> words[index].cells.map { it to index } }
        .groupBy({ it.first }, { it.second })
    val numbers = words.associate { it.start to it.number }

    init {
        require(wordsAt.values.all { indices -> indices.distinctBy { words[it].direction }.size == indices.size })
    }

    companion object {
        fun create(entries: List<Entry>, placements: List<Placement>): Puzzle {
            require(entries.size == placements.size)
            val firstRow = placements.minOf { it.start.row }
            val firstColumn = placements.minOf { it.start.column }
            val normalized = placements.map {
                it.copy(start = Cell(it.start.row - firstRow, it.start.column - firstColumn))
            }
            val numbers = normalized.map { it.start }.distinct()
                .sortedWith(compareBy(Cell::row, Cell::column))
                .withIndex().associate { (index, cell) -> cell to index + 1 }
            val words = entries.indices.map { Word(numbers.getValue(normalized[it].start), entries[it], normalized[it]) }
                .sortedWith(compareBy(Word::number, Word::direction))
            return Puzzle(
                words.maxOf { it.cells.last().row } + 1,
                words.maxOf { it.cells.last().column } + 1,
                words,
            )
        }
    }
}

val foodPuzzle = Puzzle(
    rows = 8,
    columns = 8,
    words = listOf(
        Word(1, Entry("MILK", "Süt"), Placement(Cell(0, 6), Direction.Down)),
        Word(2, Entry("WATER", "Su"), Placement(Cell(1, 0), Direction.Across)),
        Word(3, Entry("RICE", "Pirinç"), Placement(Cell(1, 4), Direction.Down)),
        Word(4, Entry("CAKE", "Kek"), Placement(Cell(3, 4), Direction.Across)),
        Word(5, Entry("EGG", "Yumurta"), Placement(Cell(3, 7), Direction.Down)),
        Word(6, Entry("APPLE", "Elma"), Placement(Cell(4, 0), Direction.Across)),
        Word(7, Entry("PEAR", "Armut"), Placement(Cell(4, 2), Direction.Down)),
        Word(8, Entry("BREAD", "Ekmek"), Placement(Cell(7, 1), Direction.Across)),
    ),
)

data class Award(val points: Int, val earnedAt: Long)

data class Progress(
    val grid: String = "",
    val hinted: Set<Int> = emptySet(),
    val awards: Map<Int, Award> = emptyMap(),
    val lockedWords: Set<Int> = emptySet(),
    val completed: Boolean = false,
    val updatedAt: Long = 0,
    val resetAt: Long = 0,
    val layout: List<Placement> = emptyList(),
    val layoutAt: Long = 0,
) {
    val started get() = grid.any { it in 'A'..'Z' }
    val points get() = awards.values.sumOf(Award::points)

    fun merge(other: Progress): Progress {
        val newest = when {
            resetAt > other.resetAt -> this
            resetAt < other.resetAt -> other
            layoutAt > other.layoutAt -> this
            layoutAt < other.layoutAt -> other
            updatedAt > other.updatedAt -> this
            updatedAt < other.updatedAt -> other
            layout != other.layout -> if (layout.toString() >= other.layout.toString()) this else other
            grid >= other.grid -> this
            else -> other
        }
        val combinedAwards = (awards.keys + other.awards.keys).associateWith { index ->
            listOfNotNull(awards[index], other.awards[index]).minWith(
                compareBy(Award::earnedAt, Award::points),
            )
        }
        val sameRound = resetAt == other.resetAt
        return newest.copy(
            hinted = if (sameRound) hinted + other.hinted else newest.hinted,
            lockedWords = if (sameRound) lockedWords + other.lockedWords else newest.lockedWords,
            awards = combinedAwards,
            completed = completed || other.completed,
        )
    }
}

data class CrosswordView(
    val wordIndex: Int,
    val cell: Cell,
    val letters: Map<Cell, Char>,
    val checked: Boolean,
    val solvedIndices: Set<Int>,
    val locked: Set<Cell>,
    val complete: Boolean,
    val hasErrors: Boolean,
    val points: Int,
) {
    fun isWrong(cell: Cell, puzzle: Puzzle) = checked && cell in letters &&
        letters[cell] != puzzle.solution[cell]
}

class Crossword(val puzzle: Puzzle, saved: Progress = Progress()) {
    private val letters = puzzle.solution.keys.mapNotNull { cell ->
        val letter = saved.grid.getOrNull(cell.row * puzzle.columns + cell.column)
        if (letter == null || letter !in 'A'..'Z') null else cell to letter
    }.toMap().toMutableMap()
    private val hinted = saved.hinted.filter { it in puzzle.words.indices }.toMutableSet()
    private val awards = saved.awards.filter { (index, award) ->
        index in puzzle.words.indices && award.points in setOf(5, 10)
    }.toMutableMap()
    private var completed = saved.completed
    private var updatedAt = saved.updatedAt
    private var resetAt = saved.resetAt
    private val firstWord = puzzle.words.indexOfFirst { it.direction == Direction.Across }.coerceAtLeast(0)
    private val solved = mutableSetOf<Int>()
    private val locked = mutableSetOf<Cell>()
    private var moveBackOnDelete = false
    private var lastGridTap: Cell? = null

    var wordIndex = firstWord
        private set
    var cell = puzzle.words[firstWord].start
        private set
    private var checked = false

    val word get() = puzzle.words[wordIndex]
    val complete get() = solved.size == puzzle.words.size

    init {
        for (index in saved.lockedWords.filter { it in puzzle.words.indices }) {
            puzzle.words[index].cells.forEach { position -> letters[position] = puzzle.solution.getValue(position) }
        }
        lockCorrectWords(saved.updatedAt.coerceAtLeast(1))
        val resume = if (firstWord in solved) puzzle.words.indices.firstOrNull { it !in solved } else firstWord
        selectWord(resume ?: firstWord)
    }

    fun view() = CrosswordView(
        wordIndex, cell, letters.toMap(), checked, solved.toSet(), locked.toSet(), complete,
        checked && letters.any { (position, letter) -> puzzle.solution[position] != letter },
        awards.values.sumOf(Award::points),
    )

    private fun lockCorrectWords(earnedAt: Long) {
        for ((index, candidate) in puzzle.words.withIndex()) {
            if (index !in solved && candidate.cells.all { letters[it] == puzzle.solution[it] }) {
                solved.add(index)
                locked.addAll(candidate.cells)
                awards.putIfAbsent(index, Award(if (index in hinted) 5 else 10, earnedAt))
            }
        }
        if (complete) completed = true
    }

    fun selectWord(index: Int) {
        lastGridTap = null
        wordIndex = index
        cell = word.cells.firstOrNull { it !in locked && letters[it] != puzzle.solution[it] }
            ?: word.cells.firstOrNull { it !in locked } ?: word.start
        moveBackOnDelete = false
    }

    fun selectCell(target: Cell) {
        val choices = puzzle.wordsAt[target] ?: return
        val selected = if (lastGridTap == target && choices.size == 2 && wordIndex in choices) {
            choices.first { it != wordIndex }
        } else {
            choices.firstOrNull { it !in solved && puzzle.words[it].direction == word.direction }
                ?: choices.firstOrNull { it !in solved } ?: choices.first()
        }
        lastGridTap = target
        wordIndex = selected
        val offset = word.cells.indexOf(target)
        cell = word.cells.drop(offset).firstOrNull { it !in locked }
            ?: word.cells.firstOrNull { it !in locked } ?: target
        moveBackOnDelete = false
    }

    fun selectLetter(target: Cell) {
        if (target in word.cells && target !in locked) {
            lastGridTap = null
            moveBackOnDelete = false
            cell = target
        }
    }

    fun moveToNextWord() {
        val next = (1..puzzle.words.size).asSequence()
            .map { (wordIndex + it) % puzzle.words.size }.firstOrNull { it !in solved }
        if (next != null) selectWord(next)
    }

    fun enter(text: String) {
        val input = englishLetters(text)
        if (input.isEmpty() || wordIndex in solved) return
        checked = false
        val cells = word.cells
        val start = cells.indexOf(cell)
        val positions = if (input.length == cells.size) cells else cells.drop(start).filter { it !in locked }
        if (positions.isEmpty()) return
        lastGridTap = null
        markUpdated()
        val letterCount = minOf(positions.size, input.length)
        for (index in 0 until letterCount) {
            val position = positions[index]
            if (position !in locked) letters[position] = input[index]
            lockCorrectWords(updatedAt)
            if (wordIndex in solved) {
                advanceAfter(position)
                return
            }
        }
        advanceAfter(positions[letterCount - 1])
    }

    private fun advanceAfter(written: Cell) {
        lockCorrectWords(updatedAt)
        val next = word.cells.drop(word.cells.indexOf(written) + 1).firstOrNull { it !in locked }
        moveBackOnDelete = next != null
        cell = next ?: written
    }

    fun delete() {
        if (wordIndex in solved) return
        lastGridTap = null
        checked = false
        val index = word.cells.indexOf(cell)
        if (moveBackOnDelete || cell in locked || cell !in letters) {
            cell = word.cells.take(index).lastOrNull { it !in locked && it in letters } ?: cell
        }
        moveBackOnDelete = false
        if (cell !in locked && letters.remove(cell) != null) markUpdated()
    }

    fun reveal(): Cell? {
        if (wordIndex in solved) return null
        lastGridTap = null
        val target = cell.takeIf { it !in locked } ?: word.cells.firstOrNull { it !in locked } ?: return null
        val correct = puzzle.solution.getValue(target)
        val revealed = letters[target] != correct
        if (revealed) hinted.addAll(puzzle.wordsAt.getValue(target))
        markUpdated()
        checked = false
        letters[target] = correct
        advanceAfter(target)
        return target.takeIf { revealed }
    }

    fun check() {
        checked = true
    }

    fun reset() {
        markUpdated()
        resetAt = updatedAt
        letters.clear()
        checked = false
        hinted.clear()
        solved.clear()
        locked.clear()
        selectWord(firstWord)
    }

    private fun markUpdated() {
        updatedAt = maxOf(System.currentTimeMillis(), updatedAt + 1)
    }

    fun progress() = Progress(
        grid = buildString {
            repeat(puzzle.rows) { row ->
                repeat(puzzle.columns) { column -> append(letters[Cell(row, column)] ?: '.') }
            }
        },
        hinted = hinted.toSet(),
        awards = awards.toMap(),
        lockedWords = solved.toSet(),
        completed = completed,
        updatedAt = updatedAt,
        resetAt = resetAt,
    )
}
