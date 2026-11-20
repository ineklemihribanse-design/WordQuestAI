package com.kelimebulmacasi.a1

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

private val Navy174=Color(0xFF23114A)
private val Indigo174=Color(0xFF5530A6)
private val Pale174=Color(0xFFF2F6FF)

@Composable
private fun Wq174MemorySparkles(modifier:Modifier=Modifier) {
    Canvas(modifier) {
        val w=size.width
        val h=size.height
        drawCircle(Brush.radialGradient(listOf(
            Color(0x55D7ACFF),Color(0x00D7ACFF)),
            center=Offset(w*.66f,h*.55f),radius=w*.45f),w*.38f,
            Offset(w*.66f,h*.55f))
        for(i in 0..5) {
            val x=w*(.41f+i*.105f)
            val y=h*(if(i%2==0).36f else .69f)
            val r=if(i%2==0)7.dp.toPx() else 5.dp.toPx()
            drawLine(Color(0xFFFFD87D),Offset(x-r,y),
                Offset(x+r,y),2.dp.toPx())
            drawLine(Color(0xFFFFD87D),Offset(x,y-r),
                Offset(x,y+r),2.dp.toPx())
        }
        for(i in 0..4){
            val x=w*(i*.25f-.12f)
            val radius=w*.16f
            drawCircle(Color(0x77FFFFFF),radius,
                Offset(x,h*.98f))
        }
    }
}

/** Always uses user's selected 2D portrait, no hard-coded Tomba. */
@Composable
internal fun Wq174MemoryHero(
    selected:String,onPick:()->Unit,isCompact:Boolean
) {
    val shape=RoundedCornerShape(25.dp)
    BoxWithConstraints(Modifier.fillMaxWidth()
        .height(if(isCompact)250.dp else 295.dp)
        .clip(shape)
        .background(Brush.linearGradient(listOf(
            Color(0xFFF5FAFF),Color(0xFFE7EEFF),Color(0xFFF2DFFF))))){
        val compact=maxWidth<490.dp
        Wq174MemorySparkles(Modifier.matchParentSize())
        Column(Modifier.align(Alignment.TopStart)
            .fillMaxWidth(if(compact) 0.54f else 0.58f)
            .padding(start=15.dp,top=18.dp),
            verticalArrangement=Arrangement.spacedBy(11.dp)){
            Text("Kelime\nHafızam",fontSize=if(compact)32.sp else 38.sp,
                lineHeight=if(compact)34.sp else 42.sp,
                color=Navy174,fontWeight=FontWeight.Black)
            Box(Modifier.width(51.dp).height(4.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF298BE6)))
            Text("Kelimelerini öğren,\ntekrar et ve kalıcı hâle getir.",
                color=Color(0xFF5E6083),fontSize=12.sp,lineHeight=17.sp)
        }
        // One interactive avatar selector; large art is decorative only.
        Wq2DCompactSelector(selected,onClick=onPick,
            modifier=Modifier.align(Alignment.TopEnd)
                .padding(top=9.dp,end=9.dp))
        Wq2DPortrait(selected,
            Modifier.align(Alignment.BottomEnd)
                .padding(end=6.dp,bottom=5.dp)
                .size(if(compact)163.dp else 220.dp))
    }
}

@Composable
private fun Wq174LearningIcon(kind:String,modifier:Modifier=Modifier) {
    Canvas(modifier) {
        val u=size.minDimension/100f
        when(kind) {
            "card" -> {
                drawRoundRect(Color(0xFFFFC33D),Offset(18*u,24*u),
                    Size(58*u,59*u),CornerRadius(12*u))
                drawRoundRect(Brush.verticalGradient(
                    listOf(Color(0xFFF4E3FF),Color(0xFFA88CFF))),
                    Offset(25*u,14*u),Size(56*u,62*u),CornerRadius(12*u))
                drawRoundRect(Color(0xFF6940C6),Offset(31*u,20*u),
                    Size(44*u,49*u),CornerRadius(8*u),style=Stroke(2*u))
                drawLine(Color.White,Offset(42*u,43*u),
                    Offset(65*u,43*u),4*u)
                drawLine(Color.White,Offset(42*u,53*u),
                    Offset(62*u,53*u),4*u)
            }
            "write" -> {
                drawRoundRect(Color(0xFFB4A1F9),Offset(10*u,13*u),
                    Size(66*u,69*u),CornerRadius(11*u))
                drawRoundRect(Color(0xFFFAF7FF),Offset(16*u,18*u),
                    Size(56*u,62*u),CornerRadius(9*u))
                for(i in 0..2) drawLine(Color(0xFF9B7ED8),
                    Offset(27*u,(40+i*12)*u),
                    Offset(63*u,(40+i*12)*u),3*u)
                drawLine(Color(0xFFF9C531),
                    Offset(75*u,23*u),Offset(49*u,64*u),12*u)
                drawLine(Color(0xFFFCE2A0),
                    Offset(76*u,25*u),Offset(53*u,59*u),4*u)
            }
            "type" -> {
                val p=Path().apply{
                    moveTo(9*u,17*u);lineTo(27*u,17*u)
                    cubicTo(23*u,2*u,50*u,2*u,47*u,17*u)
                    lineTo(52*u,17*u);lineTo(52*u,42*u)
                    cubicTo(68*u,35*u,67*u,61*u,52*u,55*u)
                    lineTo(52*u,85*u);lineTo(9*u,85*u);close()
                }
                val g=Path().apply{
                    moveTo(53*u,21*u);lineTo(73*u,21*u)
                    cubicTo(71*u,5*u,95*u,7*u,91*u,21*u)
                    lineTo(95*u,21*u);lineTo(95*u,81*u)
                    lineTo(78*u,81*u)
                    cubicTo(79*u,94*u,58*u,95*u,61*u,81*u)
                    lineTo(53*u,81*u);close()
                }
                drawPath(g,Brush.verticalGradient(
                    listOf(Color(0xFFFFE57B),Color(0xFFFFB32F))))
                drawPath(p,Brush.verticalGradient(
                    listOf(Color(0xFFC79EFF),Color(0xFF803DDD))))
            }
            else -> {
                drawCircle(Color(0xFF7741C9),42*u,
                    Offset(50*u,50*u))
                drawCircle(Color(0xFFFFEFFD),32*u,
                    Offset(50*u,50*u))
                drawCircle(Color(0xFF9C62E3),23*u,
                    Offset(50*u,50*u))
                drawCircle(Color(0xFFFFCC41),12*u,
                    Offset(50*u,50*u))
                drawLine(Color(0xFFFFC82F),Offset(74*u,24*u),
                    Offset(48*u,52*u),6*u)
                drawCircle(Color(0xFFFFE590),5*u,Offset(74*u,24*u))
            }
        }
    }
}

@Composable
internal fun Wq174MemoryStats(total:Int,due:Int,learned:Int) {
    val items=listOf(
        Triple("Kelimem",total,Color(0xFFDDEEFF)),
        Triple("Tekrar",due,Color(0xFFEEE0FF)),
        Triple("Öğrenilen",learned,Color(0xFFFFF1D8))
    )
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))
        .background(Color.White.copy(alpha=.88f))
        .padding(5.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){
        items.forEachIndexed{ index,(label,value,bg)->
            val color=when(index){
                0->Color(0xFF3185DE)
                1->Color(0xFF9255E4)
                else->Color(0xFFF4AE24)
            }
            Column(Modifier.weight(1f).heightIn(min=103.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(
                    Color.White,bg)))
                .border(1.dp,color.copy(alpha=.48f),RoundedCornerShape(20.dp))
                .padding(vertical=11.dp),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(3.dp)) {
                Text(when(index){0->"▤";1->"↻";else->"★"},
                    color=color,fontSize=24.sp,fontWeight=FontWeight.Bold)
                Text("$value",color=Navy174,
                    fontSize=24.sp,fontWeight=FontWeight.ExtraBold)
                Text(label,color=Color(0xFF535A7B),fontSize=12.sp)
            }
        }
    }
}

@Composable
internal fun Wq174GoldButton(label:String,enabled:Boolean,onClick:()->Unit) {
    val shape=RoundedCornerShape(30.dp)
    val gradient=if(enabled)Brush.verticalGradient(
        listOf(Color(0xFFFFE57C),Color(0xFFFFCA31),Color(0xFFFFA914)))
        else Brush.verticalGradient(
            listOf(Color(0xFFE5E7EA),Color(0xFFD8DBE1)))
    Row(Modifier.fillMaxWidth().heightIn(min=54.dp)
        .shadow(if(enabled)6.dp else 0.dp,shape)
        .clip(shape).background(gradient)
        .border(1.5.dp,if(enabled)Color(0xFFFFEB9C)
            else Color(0xFFD0D1D4),shape)
        .clickable(enabled=enabled,onClick=onClick)
        .padding(10.dp),
        horizontalArrangement=Arrangement.Center,
        verticalAlignment=Alignment.CenterVertically) {
        if(enabled) {
            Text("▶",color=Color(0xFF58278B),fontSize=17.sp)
            Spacer(Modifier.width(10.dp))
        }
        Text(label,fontWeight=FontWeight.ExtraBold,fontSize=16.sp,
            color=if(enabled)Navy174 else Color(0xFF9096A0))
    }
}

@Composable
internal fun Wq174MemoryModeCard(
    title:String,subtitle:String,mode:String,enabled:Boolean,
    modifier:Modifier=Modifier,onClick:()->Unit
) {
    val palette=when(mode){
        "card"-> listOf(Color(0xFFFCFEFF),Color(0xFFDCEEFF))
        "write"->listOf(Color(0xFFFFF9FF),Color(0xFFEBDFFF))
        "type"->listOf(Color(0xFFFFFEF8),Color(0xFFFFEEDD))
        else->listOf(Color(0xFFF7FAFF),Color(0xFFDCEAFD))
    }
    val shape=RoundedCornerShape(22.dp)
    Column(modifier.heightIn(min=151.dp)
        .shadow(5.dp,shape)
        .clip(shape).background(Brush.verticalGradient(palette))
        .border(1.dp,Color.White,shape)
        .clickable(enabled=enabled,onClick=onClick)
        .padding(12.dp),
        verticalArrangement=Arrangement.spacedBy(6.dp)){
        Wq174LearningIcon(mode,Modifier.size(52.dp))
        Text(title,color=Navy174,fontSize=15.sp,
            lineHeight=17.sp,fontWeight=FontWeight.ExtraBold,maxLines=2)
        Text(subtitle,color=Color(0xFF5D5B83),
            fontSize=11.sp,lineHeight=15.sp,maxLines=3)
    }
}
