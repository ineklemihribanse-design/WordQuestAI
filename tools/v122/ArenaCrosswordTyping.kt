package com.kelimebulmacasi.a1

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Tap an intersection again to switch between available across/down words. */
internal fun nextCrosswordSelection(
    indexes: Collection<Int>?, solved: Set<Int>, currentlySelected: Int
): Int? {
    val options = indexes.orEmpty().filterNot { it in solved }
    if (options.isEmpty()) return null
    return if (currentlySelected in options && options.size > 1)
        options.first { it != currentlySelected }
    else options.first()
}

/**
 * The IME edits only unsolved squares: letters already revealed by intersecting
 * solved words are immutable, and the player's next keystroke skips them.
 */
internal fun crosswordFixedPattern(puzzle: Puzzle, solved: Set<Int>, wordIndex: Int): String {
    val fixed = mutableMapOf<Cell, Char>()
    puzzle.words.forEachIndexed { index, word ->
        if (index in solved) word.cells.forEachIndexed { position, cell ->
            fixed[cell] = word.entry.letters[position]
        }
    }
    val selected = puzzle.words.getOrNull(wordIndex) ?: return ""
    return selected.cells.map { fixed[it] ?: '.' }.joinToString("")
}

/** Keep a dot for an incomplete cell, rather than shifting later letters left. */
internal fun fillCrosswordGaps(pattern: String, typed: String): String {
    val incoming = englishLetters(typed)
    var used = 0
    return buildString(pattern.length) {
        pattern.forEach { fixed ->
            if (fixed != '.') append(fixed)
            else append(incoming.getOrNull(used++) ?: '.')
        }
    }
}

@Composable
internal fun ArenaCrosswordTypingStrip(
    puzzle: Puzzle, solved: Set<Int>, wordIndex: Int, typed: String
) {
    val selected = puzzle.words.getOrNull(wordIndex) ?: return
    val pattern = crosswordFixedPattern(puzzle, solved, wordIndex)
    val displayed = fillCrosswordGaps(pattern, typed)
    val missing = pattern.count { it == '.' }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            "SEÇİLİ KELİME · ${selected.number} ${if (selected.direction == Direction.Across) "YATAY" else "DİKEY"}",
            color = Color(0xFFE7C582), fontWeight = FontWeight.Bold, fontSize = 11.sp
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            displayed.forEachIndexed { index, letter ->
                val isFixed = pattern[index] != '.'
                Box(
                    Modifier.size(width = 34.dp, height = 37.dp)
                        .background(
                            if (isFixed) Color(0xFF3E7555) else Color(0xFFE7C582),
                            RoundedCornerShape(5.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (letter == '.') "·" else letter.toString(),
                        color = if (isFixed) Color.White else Color(0xFF112720),
                        fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                }
            }
        }
        Text(
            "Yalnızca boş kareleri doldur · ${englishLetters(typed).length}/$missing harf" +
                if (missing < selected.cells.size) " · ${selected.cells.size - missing} harf zaten açık" else "",
            color = Color(0xFFC5D8CD), fontSize = 11.sp
        )
    }
}


/**
 * Preserve the crossword while the soft keyboard is visible. In keyboard mode
 * the player can scroll a smaller board window rather than losing all of the
 * crossings. Automatically bring the selected word's first row into view.
 *
 * With the keyboard closed the original full-sized board remains unchanged.
 */
@Composable
internal fun ArenaCompactCrosswordBoard(
    puzzle: Puzzle,
    solved: Set<Int>,
    selected: Int,
    compact: Boolean,
    typed: String,
    onSelect: (Int) -> Unit
) {
    val positionedDraft = fillCrosswordGaps(crosswordFixedPattern(puzzle, solved, selected), typed)
    if (!compact) {
        ArenaPuzzleGrid(puzzle, solved, selected, positionedDraft, compact, onSelect)
        return
    }

    val scroll = rememberScrollState()
    val targetCells = puzzle.words.getOrNull(selected)?.cells.orEmpty()
    val firstRow = (0 until puzzle.rows).firstOrNull { row ->
        (0 until puzzle.columns).any { column -> Cell(row, column) in targetCells }
    } ?: 0

    LaunchedEffect(firstRow, scroll.maxValue) {
        if (scroll.maxValue > 0) {
            val target = (scroll.maxValue.toFloat() *
                firstRow / puzzle.rows.coerceAtLeast(1)).toInt()
            scroll.animateScrollTo(target.coerceIn(0, scroll.maxValue))
        }
    }

    Column(
        Modifier.fillMaxWidth()
            .height(138.dp)
            .clip(RoundedCornerShape(12.dp))
            .verticalScroll(scroll)
    ) {
        ArenaPuzzleGrid(puzzle, solved, selected, positionedDraft, compact, onSelect)
    }
}
