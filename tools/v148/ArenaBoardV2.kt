package com.kelimebulmacasi.a1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Crossword board with restrained luminous selection and no unnecessary overlays.
 * Stable crossing letters are mint; selected editable squares are lavender.
 * Remains scrollable horizontally on narrow Android screens.
 */
@Composable
internal fun ArenaBoardV2(
    puzzle: Puzzle,
    solved: Set<Int>,
    selected: Int,
    positionedDraft: String,
    compact: Boolean,
    onSelect: (Int) -> Unit
) {
    val chosen = puzzle.words.getOrNull(selected)
    val fixed=remember(puzzle,solved) {
        buildMap<Cell,Char> {
            puzzle.words.forEachIndexed { index, word ->
                if(index in solved) word.cells.forEachIndexed { position, cell ->
                    put(cell,word.entry.letters[position])
                }
            }
        }
    }
    val draft=remember(chosen,positionedDraft) {
        buildMap<Cell,Char> {
            chosen?.cells?.forEachIndexed { i, cell ->
                val ch=positionedDraft.getOrNull(i)
                if(ch != null && ch != '.') put(cell,ch)
            }
        }
    }
    val scroll=rememberScrollState()
    BoxWithConstraints(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF0EEFA)).padding(8.dp)
    ) {
        val columns=puzzle.columns.coerceAtLeast(1)
        val cellSize=((maxWidth - 16.dp)/columns).coerceIn(27.dp,44.dp)
        val focusColumn=(0 until puzzle.columns).firstOrNull { col ->
            (0 until puzzle.rows).any { row -> Cell(row,col) in (chosen?.cells ?: emptyList()) }
        } ?: 0
        val cellPixels=with(LocalDensity.current){(cellSize + 2.dp).toPx()}
        LaunchedEffect(selected,compact,scroll.maxValue) {
            if(scroll.maxValue>0) {
                val target=((focusColumn-1).coerceAtLeast(0)*cellPixels).toInt()
                scroll.animateScrollTo(target.coerceIn(0,scroll.maxValue))
            }
        }
        Column(Modifier.fillMaxWidth().horizontalScroll(scroll),
            verticalArrangement=Arrangement.spacedBy(2.dp)) {
            repeat(puzzle.rows) { r ->
                Row(horizontalArrangement=Arrangement.spacedBy(2.dp)) {
                    repeat(puzzle.columns) { c ->
                        val cell=Cell(r,c)
                        val indexes=puzzle.wordsAt[cell].orEmpty()
                        val active=chosen?.cells?.contains(cell)==true
                        val locked=fixed.containsKey(cell)
                        val letter=fixed[cell] ?: if(active) draft[cell] else null
                        val usable=indexes.isNotEmpty()
                        val base=when {
                            !usable -> Color(0xFFE1E1EC)
                            locked -> Color(0xFFBFE8D9)
                            active -> Color(0xFFDCD1FF)
                            else -> Color.White
                        }
                        val stroke=when {
                            active -> Color(0xFF715DD4)
                            locked -> Color(0xFF76BCAB)
                            else -> Color(0xFFE4E1EF)
                        }
                        Box(
                            Modifier.size(cellSize)
                                .then(if(active)Modifier.shadow(2.dp,RoundedCornerShape(5.dp))
                                      else Modifier)
                                .clip(RoundedCornerShape(5.dp))
                                .background(base)
                                .border(if(active)1.6.dp else .7.dp,stroke,
                                    RoundedCornerShape(5.dp))
                                .clickable(enabled=usable) {
                                    val next=nextCrosswordSelection(indexes,solved,selected)
                                    if(next!=null && next!=selected)onSelect(next)
                                },
                            contentAlignment=Alignment.Center
                        ) {
                            if(usable) {
                                puzzle.numbers[cell]?.let { n ->
                                    Text(n.toString(),Modifier.align(Alignment.TopStart)
                                            .padding(start=2.dp,top=1.dp),
                                        fontSize=8.sp,color=Color(0xFF77728E))
                                }
                                Text(letter?.toString().orEmpty(),
                                    color=if(locked)Color(0xFF256B5F) else Color(0xFF493887),
                                    fontSize=if(cellSize<32.dp)12.sp else 16.sp,
                                    fontWeight=FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
