package com.kelimebulmacasi.a1

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Wq172Ink=Color(0xFF312158)
private val Wq172Gold=Color(0xFFFFCD68)
private val Wq172Stroke=Color(0xFFE7D8F9)

@Composable
private fun Arena172Icon(mode:String) {
    Canvas(Modifier.size(48.dp)) {
        val u=size.minDimension/100f
        when(mode) {
            "duel" -> {
                drawLine(Color(0xFFFFD36D),Offset(23*u,84*u),Offset(78*u,17*u),12*u)
                drawLine(Color.White,Offset(30*u,73*u),Offset(77*u,16*u),7*u)
                drawLine(Color(0xFFFFD36D),Offset(23*u,17*u),Offset(78*u,84*u),12*u)
                drawLine(Color.White,Offset(25*u,16*u),Offset(73*u,73*u),7*u)
            }
            "team" -> {
                drawRoundRect(Color(0xFF8050CE),Offset(10*u,33*u),Size(48*u,42*u),
                    cornerRadius=androidx.compose.ui.geometry.CornerRadius(10*u))
                drawCircle(Color(0xFF8050CE),11*u,Offset(35*u,28*u))
                drawRoundRect(Color(0xFFFFC05C),Offset(52*u,25*u),Size(35*u,42*u),
                    cornerRadius=androidx.compose.ui.geometry.CornerRadius(9*u))
                drawCircle(Color(0xFFFFC05C),10*u,Offset(69*u,22*u))
            }
            else -> {
                val bolt=Path().apply {
                    moveTo(58*u,4*u);lineTo(27*u,52*u);lineTo(49*u,52*u)
                    lineTo(39*u,96*u);lineTo(78*u,43*u);lineTo(55*u,43*u);close()
                }
                drawPath(bolt,Color(0xFFFFB735))
                drawPath(bolt,Color(0xFFFFDE88),style=Stroke(2*u))
            }
        }
    }
}

@Composable
private fun Arena172Card(
    title:String,subtitle:String,tag:String,mode:String,
    selected:Boolean,enabled:Boolean,onClick:()->Unit
) {
    val shape=RoundedCornerShape(23.dp)
    val colors=if(selected) listOf(Color(0xFFA57BE1),Color(0xFF6E39B5),Color(0xFF4B267C))
        else listOf(Color.White,Color(0xFFFFF6FC))
    Box(Modifier.width(205.dp).height(204.dp).shadow(5.dp,shape).clip(shape)
        .background(Brush.verticalGradient(colors))
        .border(if(selected)2.dp else 1.dp,
            if(selected)Wq172Gold else Color.White,shape)
        .clickable(enabled=enabled,onClick=onClick).padding(10.dp)) {
        Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Arena172Icon(mode)
            Text(title,color=if(selected)Color.White else Wq172Ink,
                fontSize=16.sp,fontWeight=FontWeight.ExtraBold,
                maxLines=1,textAlign=TextAlign.Center)
            Text(subtitle,color=if(selected)Color(0xFFF3E5FF) else Color(0xFF6D667C),
                fontSize=11.sp,lineHeight=13.sp,maxLines=2,textAlign=TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Box(Modifier.clip(RoundedCornerShape(28.dp))
                .background(if(selected)Color(0xFF8C5BC5) else Color(0xFFF4EAFD))
                .padding(horizontal=14.dp,vertical=7.dp)) {
                Text(if(enabled)tag else "Çevrimiçi gerekli",
                    color=if(selected)Color.White else Wq172Ink,
                    fontWeight=FontWeight.SemiBold,fontSize=11.sp)
            }
        }
        if(selected) Box(Modifier.align(Alignment.TopEnd).size(26.dp)
            .clip(CircleShape).background(Color(0xFF9465DA)),
            contentAlignment=Alignment.Center) {
            Text("✓",color=Color.White,fontWeight=FontWeight.Bold)
        }
    }
}

/** The three existing network game modes, not mock or disconnected controls. */
@Composable
internal fun Arena172AdventurePicker(mode:String,onMode:(String)->Unit,allowTeam:Boolean) {
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(9.dp)) {
        Text("Maceranı Seç!",color=Wq172Ink,fontSize=27.sp,fontWeight=FontWeight.ExtraBold)
        Text("Bugün nasıl bir mücadele istiyorsun?",
            color=Color(0xFF726A7D),fontSize=13.sp)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(vertical=8.dp),
            horizontalArrangement=Arrangement.spacedBy(11.dp)) {
            Arena172Card("Bulmaca Düellosu","Rakibine meydan oku!","♟  2 oyuncu",
                "duel",mode!="quiz" && mode!="team",true,{onMode("duel")})
            Arena172Card("Ortak Bulmaca","Birlikte çöz, birlikte kazan!","♟  2+ oyuncu",
                "team",mode=="team",allowTeam,{onMode("team")})
            Arena172Card("5 Şıklı Düello","Hızlı düşün, doğru seç!","⚡  Hızlı oyun",
                "quiz",mode=="quiz",true,{onMode("quiz")})
        }
    }
}

@Composable
private fun Arena172Picker(
    title:String,label:String,values:List<Pair<String,Int>>,
    enabled:Boolean=true,onValue:(Int)->Unit,modifier:Modifier=Modifier
) {
    var opened by remember { mutableStateOf(false) }
    Column(modifier.clip(RoundedCornerShape(16.dp))
        .background(Color(0xFFFAF5FF)).padding(9.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(title,color=Wq172Ink,fontSize=12.sp,fontWeight=FontWeight.Bold)
        Box {
            OutlinedButton(onClick={opened=true},enabled=enabled,
                modifier=Modifier.fillMaxWidth().heightIn(min=43.dp),
                contentPadding=PaddingValues(horizontal=3.dp)) {
                Text(label+" ⌄",color=Wq172Ink,fontSize=11.sp,maxLines=1,
                    overflow=TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded=opened,onDismissRequest={opened=false}) {
                values.forEach{ (text,value) ->
                    DropdownMenuItem(text={Text(text)},onClick={onValue(value);opened=false})
                }
            }
        }
    }
}

@Composable
private fun Arena172Hint(modifier:Modifier=Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp))
        .background(Color(0xFFECF9E8)).padding(11.dp),
        verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text("ⓘ  İpucu",color=Color(0xFF245C3D),fontSize=12.sp,
            fontWeight=FontWeight.Bold)
        Text("Zorlanırsan sınırlı sayıda ipucu kullan.",
            color=Color(0xFF5B6F5D),fontSize=11.sp,lineHeight=13.sp)
    }
}

/** On compact screens hint moves below the controls, no tiny tap targets. */
@Composable
internal fun Arena172MatchSettings(
    mode:String,seconds:Int,allowedTimes:List<Int>,onSeconds:(Int)->Unit,
    size:Int,sizes:List<Int>,onSize:(Int)->Unit
) {
    ArenaApprovedPanel("Maç Ayarları","⚙") {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact=maxWidth<560.dp
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Arena172Picker("◴  Süre",if(seconds==0)"Süresiz" else "$seconds sn",
                        allowedTimes.map { (if(it==0)"Süresiz" else "$it sn") to it },
                        onValue=onSeconds,modifier=Modifier.weight(1f))
                    Arena172Picker("🧩  Bulmaca",if(mode=="quiz")"12 soru" else "$size kelime",
                        sizes.map{ "$it kelime" to it },enabled=mode!="quiz",
                        onValue=onSize,modifier=Modifier.weight(1f))
                    if(!compact) Arena172Hint(Modifier.weight(1f))
                }
                if(compact) Arena172Hint(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Arena172ScorePlayer(
    name:String,points:Int,avatar:String,active:Boolean,modifier:Modifier
) {
    Row(modifier.heightIn(min=86.dp).clip(RoundedCornerShape(18.dp))
        .background(Brush.horizontalGradient(
            listOf(Color(0xFF51359B),Color(0xFF7045AB))))
        .border(1.dp,Wq172Gold,RoundedCornerShape(18.dp))
        .padding(horizontal=5.dp,vertical=8.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        ArenaMascotV2(avatar,Modifier.size(39.dp))
        Column(Modifier.weight(1f)) {
            Text(name,color=Color.White,fontSize=11.sp,maxLines=1,
                overflow=TextOverflow.Ellipsis)
            Text(points.toString(),color=Color.White,fontSize=22.sp,
                fontWeight=FontWeight.ExtraBold)
            if(active) Text("● SIRA ONDA",color=Wq172Gold,fontSize=8.sp)
        }
    }
}

/** Only the visual changes; countdown is the genuine match countdown. */
@Composable
internal fun Arena172LiveScoreboard(
    hostName:String,guestName:String,hostScore:Int,guestScore:Int,
    isHostTurn:Boolean,countdown:Int?,completed:Int,total:Int,cooperative:Boolean,
    hostAvatar:String,guestAvatar:String
) {
    val c=countdown?.coerceAtLeast(0)
    val clock=if(c==null)"∞" else (c/60).toString().padStart(2,'0')+":"+
        (c%60).toString().padStart(2,'0')
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
        .background(Brush.horizontalGradient(
            listOf(Color(0xFF4D45A6),Color(0xFF9470CE),Color(0xFF5545AC))))
        .padding(horizontal=5.dp,vertical=8.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        Arena172ScorePlayer(hostName,hostScore,hostAvatar,!cooperative&&isHostTurn,
            Modifier.weight(1f))
        Box(Modifier.size(92.dp),contentAlignment=Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val s=5.dp.toPx()
                drawCircle(Color(0xFF283377))
                drawCircle(Color(0xFF38E2FF),style=Stroke(width=s))
                drawArc(Color(0xFFA9FFFF),-87f,73f,false,style=Stroke(width=s+1f))
            }
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Text("◴",color=Color.White,fontSize=13.sp)
                Text(clock,color=Color.White,fontSize=18.sp,fontWeight=FontWeight.ExtraBold)
                Text(if(c==null)"Süresiz" else "Kalan Süre",
                    color=Color(0xFFD7EDFF),fontSize=9.sp)
                Text(completed.toString()+"/"+total,
                    color=Color(0xFFDCD9FF),fontSize=8.sp)
            }
        }
        Arena172ScorePlayer(guestName,guestScore,guestAvatar,!cooperative&&!isHostTurn,
            Modifier.weight(1f))
    }
}
