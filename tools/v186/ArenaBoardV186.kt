package com.kelimebulmacasi.a1

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * v1.0.86: Zoom/pan instead of shrinking the cells when the keyboard appears.
 * The single logical cell address (row,column) is shared by solutions,
 * persisted hint letter and the player's current draft.
 */
@Composable
internal fun ArenaBoardV2(
    puzzle:Puzzle, solved:Set<Int>, selected:Int,
    positionedDraft:String, compact:Boolean, zoomed:Boolean=false,
    hintIndex:Int=-1, hintChar:Char?=null, onSelect:(Int)->Unit
) {
    val chosen=puzzle.words.getOrNull(selected)
    val fixed=remember(puzzle,solved){
        buildMap<Cell,Char>{
            puzzle.words.forEachIndexed { index, word ->
                if(index in solved) word.cells.forEachIndexed { pos, coordinate ->
                    word.entry.letters.getOrNull(pos)?.let { put(coordinate,it) }
                }
            }
        }
    }
    val draft=remember(chosen,positionedDraft) {
        buildMap<Cell,Char> {
            chosen?.cells?.forEachIndexed { index, coordinate ->
                positionedDraft.getOrNull(index)?.takeIf{it!='.'}?.let{
                    put(coordinate,it)
                }
            }
        }
    }
    val horizontal=rememberScrollState()
    val vertical=rememberScrollState()
    val density=LocalDensity.current
    val config=LocalConfiguration.current
    BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical=2.dp)) {
        val cols=puzzle.columns.coerceAtLeast(1)
        val rows=puzzle.rows.coerceAtLeast(1)
        val gap=if(maxWidth<500.dp)1.dp else 1.5.dp
        // Invariant: cell size is independent from compact/IME visibility.
        val cell=Wq186ArenaGridPolicy.cellDp(maxWidth.value,cols,zoomed).dp
        val boardWidth=cell*cols+gap*(cols-1)+8.dp
        val boardHeight=cell*rows+gap*(rows-1)+8.dp
        val viewport=Wq186ArenaGridPolicy.viewportDp(
            config.screenHeightDp.toFloat(),boardHeight.value,compact).dp
        val leftInset=((maxWidth-boardWidth)/2f).coerceAtLeast(0.dp)
        val hintCell=chosen?.cells?.getOrNull(hintIndex)
        val selectedRows=chosen?.cells.orEmpty().map{it.row}
        val selectedCols=chosen?.cells.orEmpty().map{it.column}
        LaunchedEffect(puzzle,selected,zoomed,compact,cell,
            horizontal.maxValue,vertical.maxValue) {
            if(selectedCols.isNotEmpty() && selectedRows.isNotEmpty()){
                val midpointCol=(selectedCols.minOrNull()!!+
                    selectedCols.maxOrNull()!!)/2f
                val midpointRow=(selectedRows.minOrNull()!!+
                    selectedRows.maxOrNull()!!)/2f
                val positionX=with(density) { ((cell+gap)*midpointCol).roundToPx() }
                val positionY=with(density) { ((cell+gap)*midpointRow).roundToPx() }
                val halfViewportX=with(density) { (maxWidth/2).roundToPx() }
                val halfViewportY=with(density) { (viewport/2).roundToPx() }
                horizontal.scrollTo((positionX-halfViewportX)
                    .coerceIn(0,horizontal.maxValue))
                vertical.scrollTo((positionY-halfViewportY)
                    .coerceIn(0,vertical.maxValue))
            } else {
                horizontal.scrollTo(0)
                vertical.scrollTo(0)
            }
        }
        Box(Modifier.fillMaxWidth().height(viewport)
            .clip(RoundedCornerShape(12.dp))
            .horizontalScroll(horizontal)
            .verticalScroll(vertical)){
            Column(Modifier.padding(start=leftInset).width(boardWidth)
                .background(Color(0xFFF2EDFA),RoundedCornerShape(12.dp))
                .padding(4.dp),
                verticalArrangement=Arrangement.spacedBy(gap)){
                repeat(rows){ row ->
                    Row(horizontalArrangement=Arrangement.spacedBy(gap)){
                        repeat(cols){ col ->
                            val address=Cell(row,col)
                            val candidates=puzzle.wordsAt[address].orEmpty()
                            val usable=candidates.isNotEmpty()
                            val active=chosen?.cells?.contains(address)==true
                            val alreadySolved=address in fixed
                            val hinted=active && address==hintCell && hintChar!=null
                            val background=when {
                                !usable -> Color(0xFFD9D9E6)
                                alreadySolved -> Color(0xFFC0EEE0)
                                hinted -> Color(0xFFFFE6A9)
                                active -> Color(0xFFDCD0FF)
                                else -> Color.White
                            }
                            Box(Modifier.size(cell)
                                .clip(RoundedCornerShape(if(cell<18.dp)2.dp else 5.dp))
                                .background(background)
                                .border(if(active)1.5.dp else .6.dp,
                                    if(active)Color(0xFF7254D6)
                                    else Color(0xFFE6E2F2),
                                    RoundedCornerShape(if(cell<18.dp)2.dp else 5.dp))
                                .clickable(enabled=usable){
                                    nextCrosswordSelection(candidates,solved,selected)
                                        ?.takeIf{it!=selected}?.let(onSelect)
                                },contentAlignment=Alignment.Center) {
                                if(usable){
                                    puzzle.numbers[address]?.let{number->
                                        Text(number.toString(),
                                            Modifier.align(Alignment.TopStart),
                                            fontSize=(cell.value*.22f)
                                                .coerceIn(6f,9f).sp,
                                            lineHeight=9.sp,
                                            color=Color(0xFF7C7294))
                                    }
                                    // Solved > hint > draft. All use the same Cell.
                                    val visibleLetter = fixed[address] ?:
                                        (if(hinted)hintChar else if(active)draft[address] else null)
                                    Text(visibleLetter?.toString().orEmpty(),
                                        fontSize=(cell.value*.50f)
                                            .coerceIn(11f,21f).sp,
                                        lineHeight=(cell.value*.62f)
                                            .coerceIn(13f,24f).sp,
                                        color=Color(0xFF493887),
                                        fontWeight=FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
