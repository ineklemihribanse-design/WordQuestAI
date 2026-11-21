package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Five independently tappable answers in an accessible light surface.
 * All score writes, timeouts, and correct/wrong sound effects remain in the caller.
 */
@Composable
internal fun Wq182QuizAnswer(
    letter:Char, answer:String, chosen:Boolean,
    correct:Boolean, hasAnswered:Boolean, onPick:()->Unit
){
    val fill = when {
        hasAnswered && correct -> Color(0xFFE0F7E9)
        hasAnswered && chosen -> Color(0xFFFFEBE5)
        else -> Color(0xFFFFFAFF)
    }
    val accent=when {
        hasAnswered && correct -> Color(0xFF207349)
        hasAnswered && chosen -> Color(0xFFA34339)
        else -> Color(0xFF6544A5)
    }
    Surface(onClick=onPick,enabled=!hasAnswered,
        modifier=Modifier.fillMaxWidth().heightIn(min=61.dp),
        shape=RoundedCornerShape(18.dp),color=fill,
        border=BorderStroke(if(chosen)2.dp else 1.dp,accent.copy(alpha=if(chosen)1f else .36f)),
        shadowElevation=if(hasAnswered)0.dp else 3.dp){
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=9.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(13.dp)){
            Surface(shape=RoundedCornerShape(12.dp),color=accent,
                modifier=Modifier.size(40.dp)){
                Box(contentAlignment=Alignment.Center){
                    Text(letter.toString(),color=Color.White,
                        fontWeight=FontWeight.Black,fontSize=17.sp)
                }
            }
            Text(answer,modifier=Modifier.weight(1f),color=Color(0xFF28233C),
                fontSize=16.sp,lineHeight=21.sp,fontWeight=FontWeight.SemiBold,
                maxLines=4,overflow=TextOverflow.Ellipsis)
            if(hasAnswered && (correct || chosen))
                Text(if(correct)"✓" else "×",fontSize=21.sp,color=accent,
                    fontWeight=FontWeight.Black)
        }
    }
}

/** Compact explanation behind an i icon, never a large permanent text block. */
@Composable
internal fun Wq182Info(title:String,detail:String) {
    var open by remember { mutableStateOf(false) }
    TextButton(onClick={open=true},
        contentPadding=PaddingValues(horizontal=7.dp,vertical=1.dp)) {
        Text("ⓘ",fontSize=19.sp,color=Color(0xFF6645A0),
            fontWeight=FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        Text(title,color=Color(0xFF493375),fontSize=12.sp)
    }
    if(open) AlertDialog(
        onDismissRequest={open=false},
        title={Text(title)},
        text={Text(detail)},
        confirmButton={TextButton(onClick={open=false}){Text("Anladım")}}
    )
}
