package com.kelimebulmacasi.a1

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Default: complete grid visible on phone/tablet. Manual zoom: scrollable large cells. */
@Composable
internal fun ArenaBoardV2(
    puzzle:Puzzle, solved:Set<Int>, selected:Int,
    positionedDraft:String, compact:Boolean, onSelect:(Int)->Unit
){
    val chosen=puzzle.words.getOrNull(selected)
    val fixed=remember(puzzle,solved){
        buildMap<Cell,Char>{
            puzzle.words.forEachIndexed{index,w->
                if(index in solved)w.cells.forEachIndexed{pos,cell->put(cell,w.entry.letters[pos])}
            }
        }
    }
    val draft=remember(chosen,positionedDraft){
        buildMap<Cell,Char>{
            chosen?.cells?.forEachIndexed{i,cell->
                positionedDraft.getOrNull(i)?.let{letter->
                    if(letter!='.')put(cell,letter)
                }
            }
        }
    }
    var zoomed by rememberSaveable(puzzle.hashCode()){mutableStateOf(false)}
    val horizontal=rememberScrollState()
    val vertical=rememberScrollState()
    val density=LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp))
        .background(Color(0xFFF2EEFA)).padding(8.dp)){
        val cols=puzzle.columns.coerceAtLeast(1)
        val rows=puzzle.rows.coerceAtLeast(1)
        val gap=2.dp
        val visibleHeight=(LocalConfiguration.current.screenHeightDp.dp*.42f)
            .coerceIn(210.dp,450.dp)
        val maxHeight=if(compact)132.dp else visibleHeight
        val fitWidth=(maxWidth-12.dp-gap*cols)/cols
        val fitHeight=(maxHeight-gap*rows)/rows
        val fitted=minOf(fitWidth,fitHeight).coerceIn(9.dp,42.dp)
        val cell=if(zoomed)36.dp else fitted
        val actualHeight=(cell+gap)*rows
        val viewport=if(zoomed)maxHeight else minOf(maxHeight,actualHeight+gap)
        val selectedRows=chosen?.cells.orEmpty().map{it.row}
        val selectedCols=chosen?.cells.orEmpty().map{it.column}
        LaunchedEffect(selected,zoomed,compact,horizontal.maxValue,vertical.maxValue){
            if(zoomed){
                val x=with(density){((selectedCols.firstOrNull()?:0)*(cell+gap)).roundToPx()}
                val y=with(density){((selectedRows.firstOrNull()?:0)*(cell+gap)).roundToPx()}
                horizontal.scrollTo((x-60).coerceIn(0,horizontal.maxValue))
                vertical.scrollTo((y-45).coerceIn(0,vertical.maxValue))
            }else {
                horizontal.scrollTo(0)
                vertical.scrollTo(0)
            }
        }
        Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.SpaceBetween){
                Text(if(compact)"Klavye açık · seçili kelime" else
                    if(zoomed)"BÜYÜTÜLMÜŞ TAHTA" else "TÜM TAHTA",
                    color=Color(0xFF6D57A8),fontWeight=FontWeight.Bold,fontSize=10.sp)
                TextButton(onClick={zoomed=!zoomed},
                    contentPadding=PaddingValues(horizontal=7.dp,vertical=1.dp)){
                    Text(if(zoomed)"Küçült ⤢" else "Büyüt ⤢",fontSize=12.sp,
                        fontWeight=FontWeight.Bold)
                }
            }
            Box(Modifier.fillMaxWidth().height(viewport).clip(RoundedCornerShape(11.dp))){
                Column(Modifier.fillMaxWidth()
                    .horizontalScroll(horizontal).verticalScroll(vertical),
                    verticalArrangement=Arrangement.spacedBy(gap)){
                    repeat(rows){row->
                        Row(horizontalArrangement=Arrangement.spacedBy(gap)){
                            repeat(cols){col->
                                val place=Cell(row,col)
                                val indexes=puzzle.wordsAt[place].orEmpty()
                                val usable=indexes.isNotEmpty()
                                val active=chosen?.cells?.contains(place)==true
                                val locked=place in fixed
                                val bg=when{
                                    !usable->Color(0xFFD9D9E6)
                                    locked->Color(0xFFC0EEE0)
                                    active->Color(0xFFDCD0FF)
                                    else->Color.White
                                }
                                Box(Modifier.size(cell)
                                    .clip(RoundedCornerShape(if(cell<18.dp)2.dp else 5.dp))
                                    .background(bg)
                                    .border(if(active)1.4.dp else .6.dp,
                                        if(active)Color(0xFF7254D6) else Color(0xFFE6E2F2),
                                        RoundedCornerShape(if(cell<18.dp)2.dp else 5.dp))
                                    .clickable(enabled=usable){
                                        val next=nextCrosswordSelection(indexes,solved,selected)
                                        if(next!=null && next!=selected)onSelect(next)
                                    },contentAlignment=Alignment.Center){
                                    if(usable){
                                        puzzle.numbers[place]?.let{number->
                                            Text(number.toString(),Modifier.align(Alignment.TopStart),
                                                fontSize=when{
                                                    cell<18.dp->5.sp
                                                    cell<26.dp->6.sp
                                                    else->8.sp
                                                },lineHeight=8.sp,color=Color(0xFF7C7294))
                                        }
                                        Text((fixed[place]?:if(active)draft[place] else null)
                                            ?.toString().orEmpty(),color=Color(0xFF493887),
                                            fontSize=when{
                                                cell<15.dp->8.sp
                                                cell<24.dp->11.sp
                                                cell<32.dp->13.sp
                                                else->16.sp
                                            },fontWeight=FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if(zoomed) Text("Büyütülmüş tahtayı yatay ve dikey kaydırabilirsin.",
                color=Color(0xFF776A93),fontSize=10.sp)
        }
    }
}
