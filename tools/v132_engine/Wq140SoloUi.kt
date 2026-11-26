package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val Wq140Gold=Color(0xFFFFD875)
internal val Wq140Cream=Color(0xFFFFF4FF)
internal val Wq140Soft=Color(0xFFE2CFFF)
internal val Wq140Success=Color(0xFF9AF0BB)
private val Panel140=Color(0xF0251347)
private val Light140=Color(0xF038205E)
private val Border140=Color(0xFF9A70D7)
internal val LocalWq140ArenaViewport=compositionLocalOf<Float?>{null}
internal object Wq140ArenaFit {
    fun cell(width:Float,height:Float,rows:Int,cols:Int,gap:Float):Float {
        val r=rows.coerceAtLeast(1);val c=cols.coerceAtLeast(1)
        return minOf((width-8f-(c-1)*gap)/c,(height-8f-(r-1)*gap)/r,60f).coerceAtLeast(.5f)
    }
}
@Composable internal fun Wq140SoloBanner(tag:String,heading:String,detail:String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel140)
        .border(1.dp,Border140,RoundedCornerShape(16.dp)).padding(horizontal=12.dp,vertical=7.dp),
        verticalArrangement=Arrangement.spacedBy(2.dp)) {
        Text(tag.uppercase(),color=Wq140Gold,fontWeight=FontWeight.Bold,fontSize=11.sp)
        Text(heading,color=Wq140Cream,fontWeight=FontWeight.Black,fontSize=20.sp,lineHeight=23.sp)
        Text(detail,color=Wq140Soft,fontSize=12.sp)
    }
}
@Composable internal fun Wq140SoloPanel(content:@Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel140)
        .border(1.dp,Border140,RoundedCornerShape(16.dp)).padding(9.dp),
        verticalArrangement=Arrangement.spacedBy(6.dp),content=content)
}
@Composable internal fun Wq140SoloScore(done:Int,total:Int,score:Int,remainingSeconds:Int?) {
    Wq140SoloPanel {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("İlerleme $done/$total",color=Wq140Cream,fontWeight=FontWeight.Bold,fontSize=13.sp)
            Text("★ $score puan"+(remainingSeconds?.let{" · $it sn"}?:""),color=Wq140Gold,
                fontWeight=FontWeight.Black,fontSize=if(remainingSeconds!=null&&remainingSeconds in 1..10)18.sp else 13.sp)
        }
        LinearProgressIndicator(progress={if(total<=0)0f else (done.toFloat()/total).coerceIn(0f,1f)},
            modifier=Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(5.dp)),color=Wq140Gold,trackColor=Light140)
    }
}
@Composable internal fun Wq140SoloGoldButton(label:String,onClick:()->Unit,enabled:Boolean=true) {
    Button(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(12.dp),
        colors=ButtonDefaults.buttonColors(containerColor=Wq140Gold,contentColor=Color(0xFF28103E),
            disabledContainerColor=Color(0xFF534365),disabledContentColor=Wq140Soft),
        contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)) {
        Text(label,fontWeight=FontWeight.Black,fontSize=13.sp)
    }
}
@Composable internal fun Wq140SoloOutlineButton(label:String,onClick:()->Unit,modifier:Modifier=Modifier) {
    OutlinedButton(onClick=onClick,modifier=modifier,shape=RoundedCornerShape(12.dp),
        colors=ButtonDefaults.outlinedButtonColors(containerColor=Light140,contentColor=Wq140Gold),
        border=BorderStroke(1.dp,Border140),contentPadding=PaddingValues(horizontal=7.dp,vertical=5.dp)) {
        Text(label,fontSize=12.sp,fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center)
    }
}
@Composable internal fun Wq140SoloClueHeader(puzzle:Puzzle,selected:Int) {
    val w=puzzle.words.getOrNull(selected)?:return
    Wq140SoloPanel {
        Text("AKTİF İPUCU · "+w.number+" "+(if(w.direction==Direction.Across)"YATAY" else "DİKEY")+" · "+w.cells.size+" HARF",
            color=Wq140Gold,fontWeight=FontWeight.Black,fontSize=11.sp)
        Text(w.entry.clue,color=Wq140Cream,fontWeight=FontWeight.Bold,fontSize=15.sp)
    }
}
@Composable internal fun Wq140SoloQuizAnswer(letter:Char,answer:String,chosen:Boolean,correct:Boolean,hasAnswered:Boolean,onPick:()->Unit) {
    val fill=when{hasAnswered&&correct->Color(0xFF195638);hasAnswered&&chosen->Color(0xFF742C43);else->Light140}
    val accent=when{hasAnswered&&correct->Wq140Success;hasAnswered&&chosen->Color(0xFFFFADBA);else->Wq140Gold}
    Surface(onClick=onPick,enabled=!hasAnswered,modifier=Modifier.fillMaxWidth().heightIn(min=46.dp),
        color=fill,shape=RoundedCornerShape(12.dp),border=BorderStroke(if(chosen)2.dp else 1.dp,accent.copy(alpha=.75f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal=9.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(31.dp).clip(RoundedCornerShape(8.dp)).background(accent),contentAlignment=Alignment.Center) {
                Text(letter.toString(),fontWeight=FontWeight.Black,color=Color(0xFF251347),fontSize=14.sp)
            }
            Text(answer,Modifier.weight(1f),color=Wq140Cream,fontWeight=FontWeight.SemiBold,fontSize=15.sp)
            if(hasAnswered&&(correct||chosen))Text(if(correct)"✓" else "×",color=accent,fontWeight=FontWeight.Black,fontSize=20.sp)
        }
    }
}
