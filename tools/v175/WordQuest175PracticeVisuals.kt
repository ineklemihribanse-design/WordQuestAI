package com.kelimebulmacasi.a1

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType

internal data class Wq175RoomTint(val pale:Color,val accent:Color,val ink:Color)

internal fun wq175RoomTint(kind:PracticeKind):Wq175RoomTint=when(kind){
    PracticeKind.Dictation -> Wq175RoomTint(
        Color(0xFFFFE9DF),Color(0xFFE57B69),Color(0xFF803B42))
    PracticeKind.Sentence -> Wq175RoomTint(
        Color(0xFFDDF8EF),Color(0xFF239B8D),Color(0xFF206157))
    PracticeKind.Grammar -> Wq175RoomTint(
        Color(0xFFEDE7FF),Color(0xFF8754CC),Color(0xFF4E327B))
    PracticeKind.Story -> Wq175RoomTint(
        Color(0xFFE1F0FF),Color(0xFF4489CE),Color(0xFF254B80))
    PracticeKind.Speak -> Wq175RoomTint(
        Color(0xFFFFE4F0),Color(0xFFD568A1),Color(0xFF833A63))
}

/** Scalable illustrated shapes that never use blurry emoji placeholders. */
@Composable
internal fun Wq175PracticeIcon(kind:PracticeKind,modifier:Modifier=Modifier){
    val ink=wq175RoomTint(kind).accent
    Canvas(modifier) {
        val u=size.minDimension/100f
        when(kind) {
            PracticeKind.Dictation->{
                drawArc(ink,185f,170f,false,Offset(19*u,14*u),
                    Size(62*u,61*u),style=Stroke(9*u))
                drawRoundRect(ink,Offset(12*u,50*u),
                    Size(18*u,35*u),CornerRadius(7*u))
                drawRoundRect(ink,Offset(70*u,50*u),
                    Size(18*u,35*u),CornerRadius(7*u))
                for(i in 0..2)drawLine(ink.copy(alpha=.9f),
                    Offset((40+i*10)*u,(51-i*6)*u),
                    Offset((40+i*10)*u,(61+i*5)*u),4*u)
            }
            PracticeKind.Sentence->{
                for(i in 0..2){
                    val x=(8+i*29)*u
                    drawRoundRect(ink.copy(alpha=if(i==1)1f else .65f),
                        Offset(x,26*u),Size(25*u,47*u),CornerRadius(7*u))
                    drawLine(Color.White,Offset(x+6*u,49*u),
                        Offset(x+19*u,49*u),3*u)
                }
            }
            PracticeKind.Grammar->{
                drawRoundRect(Color.White,Offset(16*u,10*u),
                    Size(54*u,74*u),CornerRadius(9*u))
                drawRoundRect(ink,Offset(16*u,10*u),
                    Size(54*u,74*u),CornerRadius(9*u),style=Stroke(5*u))
                drawLine(ink,Offset(27*u,33*u),Offset(56*u,33*u),3*u)
                drawLine(ink,Offset(27*u,47*u),Offset(50*u,47*u),3*u)
                drawCircle(ink,15*u,Offset(72*u,73*u),style=Stroke(6*u))
                drawLine(ink,Offset(82*u,83*u),Offset(95*u,96*u),8*u)
            }
            PracticeKind.Story->{
                val left=Path().apply {
                    moveTo(50*u,21*u);quadraticTo(30*u,7*u,9*u,23*u)
                    lineTo(9*u,82*u);quadraticTo(32*u,70*u,50*u,84*u)
                    close()
                }
                val right=Path().apply{
                    moveTo(50*u,21*u);quadraticTo(70*u,7*u,91*u,23*u)
                    lineTo(91*u,82*u);quadraticTo(68*u,70*u,50*u,84*u)
                    close()
                }
                drawPath(left,Color.White)
                drawPath(right,Color(0xFFF3ECFF))
                drawPath(left,ink,style=Stroke(4*u))
                drawPath(right,ink,style=Stroke(4*u))
                drawLine(ink,Offset(50*u,22*u),Offset(50*u,85*u),4*u)
            }
            PracticeKind.Speak->{
                drawRoundRect(ink,Offset(36*u,12*u),
                    Size(28*u,49*u),CornerRadius(15*u))
                drawArc(ink,0f,180f,false,Offset(24*u,38*u),
                    Size(52*u,44*u),style=Stroke(7*u))
                drawLine(ink,Offset(50*u,80*u),Offset(50*u,92*u),7*u)
                drawLine(ink,Offset(32*u,92*u),Offset(68*u,92*u),7*u)
            }
        }
    }
}

@Composable
internal fun Wq175DailyPlanBanner(
    minutes:Int,done:Int,activeMinutes:Int,remaining:Long,due:Int,
    fraction:Float,activeDays:Int,onMinutes:(Int)->Unit
){
    val shape=RoundedCornerShape(24.dp)
    var customGoal by rememberSaveable { mutableStateOf("") }
    val customValue = customGoal.toIntOrNull()
    Column(Modifier.fillMaxWidth().shadow(5.dp,shape)
        .clip(shape).background(Brush.linearGradient(listOf(
            Color(0xFFF1F9E7),Color(0xFFFFF4E9),Color(0xFFF5F0FF))))
        .border(1.dp,Color(0xFFE7E9DA),shape)
        .padding(15.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("Günlük çalışma hedefim",fontSize=22.sp,
            fontWeight=FontWeight.ExtraBold,color=Color(0xFF205242))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(8.dp)){
            listOf(15,30,60).forEach{m->
                FilterChip(selected=minutes==m,onClick={onMinutes(m)},
                    label={Text(m.toString()+" dk",fontWeight=FontWeight.Bold)})
            }
            if(minutes !in listOf(15,30,60)){
                FilterChip(selected=true,onClick={customGoal=minutes.toString()},
                    label={Text(minutes.toString()+" dk kişisel",fontWeight=FontWeight.Bold)})
            }
        }
        Row(Modifier.fillMaxWidth(),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(value=customGoal,
                onValueChange={customGoal=it.filter(Char::isDigit).take(3)},
                label={Text("Kişisel hedef (5–180 dk)")},
                singleLine=true,modifier=Modifier.weight(1f),
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
            Button(onClick={if(customValue!=null && customValue in 5..180) onMinutes(customValue)},
                enabled=customValue!=null && customValue in 5..180) {
                Text("Uygula")
            }
        }
        Text("Son 7 günde "+activeDays+"/7 aktif gün · "+
             done.toString()+" etkinlik · "+activeMinutes+" dk çalışma",
            color=Color(0xFF376457),fontSize=12.sp)
        LinearProgressIndicator(progress={fraction.coerceIn(0f,1f)},
            modifier=Modifier.fillMaxWidth().height(8.dp)
                .clip(RoundedCornerShape(5.dp)),
            color=Color(0xFF408F70),trackColor=Color(0xFFE1DAEA))
        Text("Kalan: "+(remaining/60)+" dk "+(remaining%60)+
            " sn · "+due+" kelime tekrar bekliyor",
            color=Color(0xFF666B73),fontSize=12.sp)
        Text("Çalışma süresi bu sayfa açıkken sayılır.",
            color=Color(0xFF8A8490),fontSize=10.sp)
    }
}

@Composable
internal fun Wq175PracticeTile(
    kind:PracticeKind,onClick:()->Unit
){
    val c=wq175RoomTint(kind)
    val shape=RoundedCornerShape(20.dp)
    Row(Modifier.fillMaxWidth().heightIn(min=86.dp)
        .shadow(3.dp,shape).clip(shape)
        .background(Brush.horizontalGradient(listOf(Color.White,c.pale)))
        .border(1.dp,c.accent.copy(alpha=.24f),shape)
        .clickable(onClick=onClick)
        .padding(11.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(12.dp)){
        Box(Modifier.size(58.dp).background(c.pale,RoundedCornerShape(16.dp)),
            contentAlignment=Alignment.Center){
            Wq175PracticeIcon(kind,Modifier.size(45.dp))
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
            Text(kind.title,fontWeight=FontWeight.ExtraBold,
                fontSize=17.sp,color=c.ink)
            Text(kind.detail,fontSize=12.sp,lineHeight=15.sp,
                color=Color(0xFF65716F))
        }
        Text("›",fontSize=28.sp,color=c.accent,fontWeight=FontWeight.Bold)
    }
}

@Composable
internal fun Wq175PracticeQuestionHeader(kind:PracticeKind,number:Int){
    val c=wq175RoomTint(kind)
    val suppliedHero=when(kind){
        PracticeKind.Dictation -> "dinle_ve_yaz"
        PracticeKind.Grammar -> "dilbilgisi"
        else -> null
    }
    val shape=RoundedCornerShape(22.dp)
    Box(Modifier.fillMaxWidth().heightIn(min=102.dp).clip(shape)
        .background(Brush.horizontalGradient(listOf(c.pale,Color.White)))
        .border(1.dp,c.accent.copy(alpha=.23f),shape)) {
        if(suppliedHero!=null) {
            Wq175ThemeIllustration(suppliedHero,
                Modifier.align(Alignment.CenterEnd).width(117.dp)
                    .height(102.dp).alpha(.23f))
        }
        Row(Modifier.fillMaxWidth().padding(13.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Box(Modifier.size(57.dp)
                .background(c.pale,RoundedCornerShape(16.dp)),
                contentAlignment=Alignment.Center){
                Wq175PracticeIcon(kind,Modifier.size(43.dp))
            }
            Column(Modifier.weight(1f),
                verticalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(kind.title,color=c.ink,fontSize=19.sp,
                    lineHeight=23.sp,fontWeight=FontWeight.ExtraBold)
                Text("Alıştırma "+(number+1),color=c.accent,fontSize=12.sp,
                    fontWeight=FontWeight.Bold)
                Text(kind.detail,color=Color(0xFF68727C),fontSize=11.sp,
                    lineHeight=15.sp,maxLines=2)
            }
        }
    }
}
