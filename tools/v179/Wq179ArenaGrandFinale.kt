package com.kelimebulmacasi.a1

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

internal enum class Wq179ArenaOutcome { Victory, Defeat, Draw, Cooperation }
internal data class Wq179ArenaResultText(
    val outcome:Wq179ArenaOutcome,val headline:String,val subline:String,
    val badge:String
)

internal object Wq179ArenaResultCopy {
    fun make(cooperative:Boolean,isHost:Boolean,host:Int,guest:Int,
        hostName:String,guestName:String):Wq179ArenaResultText {
        val mine=if(isHost)host else guest
        val rival=if(isHost)guest else host
        return when {
            cooperative -> Wq179ArenaResultText(
                Wq179ArenaOutcome.Cooperation,"BİRLİKTE BAŞARDINIZ!",
                "İkiniz aynı takımda mücadele ettiniz. Toplam "+(host+guest)+" puan!",
                "TAKIM ZAFERİ")
            host==guest -> Wq179ArenaResultText(
                Wq179ArenaOutcome.Draw,"BERABERLİK!",
                "Müthiş rekabet! İkiniz de "+mine+" puan topladınız.",
                "DOSTÇA MÜCADELE")
            mine>rival -> Wq179ArenaResultText(
                Wq179ArenaOutcome.Victory,"KUPA SENİN!",
                abs(host-guest).toString()+" puan farkla rakibini geçtin. Harika oynadın!",
                "MAÇIN KAZANANI")
            else -> Wq179ArenaResultText(
                Wq179ArenaOutcome.Defeat,"BU MAÇ RAKİBİNİN!",
                (if(isHost)guestName else hostName)+" "+
                    abs(host-guest).toString()+" puan farkla kazandı. Rövanşta yeniden dene!",
                "RÖVANŞA HAZIR MISIN?")
        }
    }
}

@Composable
internal fun Wq179ArenaGrandFinale(
    match:PremiumOnlineMatch, myUid:String,
    hostName:String,guestName:String,hostPoints:Int,guestPoints:Int
){
    val isHost=myUid==match.hostUid
    val outcome=remember(match.code,myUid,hostPoints,guestPoints,match.cooperative){
        Wq179ArenaResultCopy.make(match.cooperative,isHost,hostPoints,guestPoints,
            hostName,guestName)
    }
    val together=outcome.outcome==Wq179ArenaOutcome.Cooperation||
        outcome.outcome==Wq179ArenaOutcome.Draw
    val winnerHost=hostPoints>guestPoints
    val heroId=if(winnerHost)match.hostAvatar else match.guestAvatar
    val intro=remember(match.code){Animatable(.76f)}
    LaunchedEffect(match.code){
        intro.snapTo(.76f)
        intro.animateTo(1.08f,tween(690,easing=FastOutSlowInEasing))
        intro.animateTo(1f,tween(260))
    }
    val glow=rememberInfiniteTransition(label="arena-final-glow")
    val halo by glow.animateFloat(initialValue=.6f,targetValue=1f,
        animationSpec=infiniteRepeatable(tween(1450),RepeatMode.Reverse),
        label="trophy-halo")
    val floating by glow.animateFloat(initialValue=0f,targetValue=1f,
        animationSpec=infiniteRepeatable(tween(3600,easing=LinearEasing),RepeatMode.Restart),
        label="celebration-confetti")
    val wide=LocalConfiguration.current.screenWidthDp>=600
    val purple=Color(0xFF49247B)
    val gold=Color(0xFFFFDE8B)
    val positive=outcome.outcome!=Wq179ArenaOutcome.Defeat
    val headingColor=if(positive)gold else Color(0xFFDCD1FF)
    val shape=RoundedCornerShape(30.dp)
    Column(
        Modifier.fillMaxWidth().shadow(10.dp,shape)
            .clip(shape).background(Brush.verticalGradient(listOf(
                Color(0xFF2A174F),Color(0xFF6034A3),Color(0xFF2C194F))))
            .border(2.dp,if(positive)Color(0xFFFFD47F) else Color(0xFFB2A4E7),shape)
            .padding(horizontal=14.dp,vertical=20.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(8.dp)
    ){
        Text("WORDQUEST · ARENA FİNALİ",fontSize=11.sp,
            letterSpacing=1.sp,color=Color(0xFFD4C5FF),fontWeight=FontWeight.Black)
        Text(outcome.headline,fontSize=if(wide)31.sp else 25.sp,
            lineHeight=34.sp,fontWeight=FontWeight.Black,
            color=headingColor,textAlign=TextAlign.Center)
        Box(Modifier.fillMaxWidth().height(if(wide)258.dp else 215.dp),
            contentAlignment=Alignment.Center){
            Canvas(Modifier.matchParentSize()){
                drawCircle(
                    Brush.radialGradient(listOf(
                        if(positive)Color(0x70FFC967) else Color(0x48AFA6FF),
                        Color.Transparent
                    )),radius=size.minDimension*.57f,
                    center=Offset(size.width*.5f,size.height*.47f))
                repeat(27){i->
                    val x=size.width*((i*37+19)%97)/100f
                    val initial=size.height*((i*41+17)%95)/100f
                    val y=(initial+floating*size.height)%size.height
                    val piece=if(i%3==0)Color(0xFFFFDA74) else
                        if(i%3==1)Color(0xFFCDBAFF) else Color(0xFFFCF6FF)
                    drawRoundRect(piece.copy(alpha=.7f),Offset(x,y),
                        Size(4f+(i%3)*2,8f+(i%4)*2))
                }
            }
            if(together){
                Row(Modifier.align(Alignment.Center),
                    horizontalArrangement=Arrangement.Center,
                    verticalAlignment=Alignment.CenterVertically) {
                    Wq2DPortrait(match.hostAvatar,
                        Modifier.size(if(wide)164.dp else 133.dp)
                            .graphicsLayer(scaleX=intro.value,scaleY=intro.value),
                        victory=outcome.outcome==Wq179ArenaOutcome.Cooperation)
                    Wq2DPortrait(match.guestAvatar,
                        Modifier.size(if(wide)164.dp else 133.dp)
                            .graphicsLayer(scaleX=intro.value,scaleY=intro.value),
                        victory=outcome.outcome==Wq179ArenaOutcome.Cooperation)
                }
            }else{
                Wq2DPortrait(heroId,
                    Modifier.align(Alignment.Center).size(if(wide)222.dp else 187.dp)
                        .graphicsLayer(scaleX=intro.value,scaleY=intro.value),
                    victory=true)
            }
            if(positive){
                Box(Modifier.align(Alignment.TopEnd).size(if(wide)70.dp else 59.dp)
                    .graphicsLayer(scaleX=.9f+.1f*halo,scaleY=.9f+.1f*halo)
                    .background(Brush.radialGradient(listOf(
                        Color(0xAAFFD774),Color(0x24FFD774),Color.Transparent)),CircleShape),
                    contentAlignment=Alignment.Center){
                    Text("🏆",fontSize=if(wide)46.sp else 38.sp)
                }
            }
        }
        Surface(shape=RoundedCornerShape(32.dp),
            color=if(positive)Color(0xFFFFE29C) else Color(0xFFD9D2FA)) {
            Text(outcome.badge,Modifier.padding(horizontal=18.dp,vertical=7.dp),
                color=purple,fontSize=12.sp,fontWeight=FontWeight.ExtraBold)
        }
        Text(outcome.subline,color=Color.White,fontSize=if(wide)17.sp else 14.sp,
            lineHeight=21.sp,fontWeight=FontWeight.SemiBold,
            textAlign=TextAlign.Center)
        Row(Modifier.fillMaxWidth().padding(top=8.dp),
            horizontalArrangement=Arrangement.spacedBy(9.dp)){
            Wq179FinalistScore(hostName,hostPoints,hostPoints>guestPoints,
                Modifier.weight(1f))
            Wq179FinalistScore(guestName,guestPoints,guestPoints>hostPoints,
                Modifier.weight(1f))
        }
    }
}

@Composable
private fun Wq179FinalistScore(name:String,points:Int,winning:Boolean,modifier:Modifier){
    Column(modifier.clip(RoundedCornerShape(14.dp))
        .background(if(winning)Color(0x50F7CA70) else Color(0x3CFFFFFF))
        .border(if(winning)1.5.dp else .6.dp,
            if(winning)Color(0xFFFFDA78) else Color(0x70FFFFFF),
            RoundedCornerShape(14.dp))
        .padding(horizontal=7.dp,vertical=11.dp),
        horizontalAlignment=Alignment.CenterHorizontally,
        verticalArrangement=Arrangement.spacedBy(4.dp)){
        Text(name,color=Color.White,maxLines=1,overflow=TextOverflow.Ellipsis,
            fontSize=13.sp,fontWeight=FontWeight.Bold)
        Text(points.toString(),color=if(winning)Color(0xFFFFDC7B) else Color.White,
            fontSize=29.sp,fontWeight=FontWeight.Black)
        Text(if(winning)"★ KAZANAN" else "PUAN",color=Color(0xFFEDE5FF),
            fontSize=10.sp,fontWeight=FontWeight.SemiBold)
    }
}
