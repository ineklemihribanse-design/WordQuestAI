package com.kelimebulmacasi.a1

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink174=Color(0xFF281150)
private val Royal174=Color(0xFF6A20C1)
private val Gold174=Color(0xFFFFCC38)
private val Lilac174=Color(0xFFF3E9FF)
private val Border174=Color(0xFFE5D2F9)

/**
 * Resolution-independent arena colosseum. Decorative rendering is kept behind
 * the real, tappable mode cards; no flattened screenshot or fake controls.
 */
@Composable
private fun Wq174TitleTrophy() {
    Canvas(Modifier.size(39.dp)) {
        val u=size.minDimension/100f
        drawRoundRect(Brush.verticalGradient(listOf(
            Color(0xFFFFE981),Color(0xFFFFC53F),Color(0xFFEAA32B))),
            Offset(25*u,13*u),Size(50*u,48*u),CornerRadius(9*u))
        drawArc(Color(0xFFFFD55D),90f,180f,false,
            Offset(9*u,18*u),Size(33*u,38*u),
            style=Stroke(8*u))
        drawArc(Color(0xFFFFD55D),-90f,180f,false,
            Offset(58*u,18*u),Size(33*u,38*u),
            style=Stroke(8*u))
        drawRoundRect(Color(0xFFFFC54A),Offset(46*u,59*u),
            Size(8*u,24*u),CornerRadius(3*u))
        drawRoundRect(Color(0xFFFFD45C),Offset(30*u,82*u),
            Size(40*u,11*u),CornerRadius(4*u))
    }
}

@Composable
private fun Wq174ArenaColosseum(modifier:Modifier=Modifier) {
    Canvas(modifier) {
        val w=size.width
        val h=size.height
        val purple=Brush.verticalGradient(
            listOf(Color(0xFF5F1EAE),Color(0xFFB18AE9),Color(0xFFE6C4FB)),
            endY=h)
        drawRect(purple)
        drawCircle(Color(0x44FFDA7B),w*.42f,Offset(w*.5f,h*.70f))
        // Receding colonnade and gold-trimmed balcony arches.
        for(side in 0..1) {
            val x=if(side==0)w*.018f else w*.89f
            drawRoundRect(Color(0x88F7DDFF),Offset(x,h*.20f),
                Size(w*.092f,h*.68f),CornerRadius(w*.03f))
            drawRoundRect(Color(0xFFDFB9EF),Offset(x-w*.017f,h*.19f),
                Size(w*.126f,h*.047f),CornerRadius(5f))
            drawRoundRect(Color(0xFFFFDBA3),Offset(x-w*.016f,h*.25f),
                Size(w*.120f,h*.012f),CornerRadius(3f))
            for(a in 0..3) {
                val y=h*(.31f+a*.125f)
                drawRoundRect(Color(0x779A55C1),Offset(x+w*.02f,y),
                    Size(w*.052f,h*.075f),CornerRadius(16f))
            }
            drawLine(Color(0xFFFDE49E),Offset(x+w*.046f,h*.17f),
                Offset(x+w*.046f,h*.82f),2.5.dp.toPx())
        }
        drawOval(Brush.radialGradient(listOf(Color(0x80FFF2AE),
            Color(0x00FFF1B0)),center=Offset(w*.5f,h*.92f),radius=w*.6f),
            Offset(-w*.03f,h*.80f),Size(w*1.06f,h*.20f))
        // Two purple victory banners, mirrored at edges.
        for(side in 0..1) {
            val x=if(side==0)w*.09f else w*.81f
            val flag=Path().apply{
                moveTo(x,h*.24f);lineTo(x+w*.10f,h*.24f)
                lineTo(x+w*.095f,h*.39f);lineTo(x+w*.05f,h*.43f)
                lineTo(x,h*.39f);close()
            }
            drawPath(flag,Color(0x99A341CF))
            drawPath(flag,Color(0xFFFFD76B),style=Stroke(2.dp.toPx()))
        }
    }
}

@Composable
private fun Wq174ModeSymbol(kind:String,modifier:Modifier=Modifier) {
    Canvas(modifier) {
        val unit=size.minDimension/100f
        when(kind) {
            "duel" -> {
                // Crossed polished steel blades and 3D gold guards.
                drawLine(Color(0xFF704292),Offset(24*unit,87*unit),
                    Offset(75*unit,12*unit),14*unit)
                drawLine(Color(0xFF704292),Offset(77*unit,87*unit),
                    Offset(25*unit,12*unit),14*unit)
                drawLine(Color(0xFFFFBE3A),Offset(24*unit,82*unit),
                    Offset(72*unit,14*unit),10*unit)
                drawLine(Color(0xFFFFD46C),Offset(76*unit,82*unit),
                    Offset(28*unit,14*unit),10*unit)
                val a=Path().apply{
                    moveTo(22*unit,10*unit);lineTo(33*unit,15*unit)
                    lineTo(77*unit,65*unit);lineTo(68*unit,72*unit);close()
                }
                val b=Path().apply{
                    moveTo(78*unit,10*unit);lineTo(67*unit,15*unit)
                    lineTo(23*unit,65*unit);lineTo(32*unit,72*unit);close()
                }
                drawPath(a,Brush.linearGradient(listOf(Color.White,Color(0xFFB9C2E8))))
                drawPath(b,Brush.linearGradient(listOf(Color.White,Color(0xFFB9C2E8))))
                drawLine(Color(0xFFFFDB66),Offset(59*unit,58*unit),
                    Offset(80*unit,76*unit),9*unit)
                drawLine(Color(0xFFFFDB66),Offset(41*unit,58*unit),
                    Offset(20*unit,76*unit),9*unit)
            }
            "team" -> {
                val p1=Path().apply{
                    moveTo(9*unit,26*unit);lineTo(28*unit,26*unit)
                    cubicTo(22*unit,5*unit,52*unit,5*unit,48*unit,26*unit)
                    lineTo(52*unit,26*unit);lineTo(52*unit,52*unit)
                    cubicTo(70*unit,45*unit,68*unit,77*unit,52*unit,68*unit)
                    lineTo(52*unit,88*unit);lineTo(9*unit,88*unit);close()
                }
                val p2=Path().apply{
                    moveTo(54*unit,14*unit);lineTo(70*unit,14*unit)
                    cubicTo(67*unit,0f,95*unit,0f,91*unit,14*unit)
                    lineTo(96*unit,14*unit);lineTo(96*unit,70*unit)
                    lineTo(79*unit,70*unit)
                    cubicTo(80*unit,88*unit,56*unit,84*unit,61*unit,70*unit)
                    lineTo(54*unit,70*unit);close()
                }
                drawPath(p2,Brush.verticalGradient(
                    listOf(Color(0xFFFFF0A3),Color(0xFFFFB639),Color(0xFFD8851A))))
                drawPath(p1,Brush.verticalGradient(
                    listOf(Color(0xFFC5A4FF),Color(0xFF8B40DF),Color(0xFF6331B2))))
                drawPath(p1,Color(0xFFE6CCFF),style=Stroke(2*unit))
                drawPath(p2,Color(0xFFFFF4C6),style=Stroke(2*unit))
            }
            else -> {
                val p=Path().apply{
                    moveTo(60*unit,5*unit);lineTo(17*unit,57*unit)
                    lineTo(49*unit,57*unit);lineTo(38*unit,98*unit)
                    lineTo(90*unit,42*unit);lineTo(57*unit,42*unit);close()
                }
                drawPath(p,Brush.verticalGradient(
                    listOf(Color(0xFFFFF7B5),Color(0xFFFFCE4B),Color(0xFFF19A18))))
                drawPath(p,Color(0xFFF7AA21),style=Stroke(2.5f*unit))
                drawLine(Color.White,Offset(57*unit,15*unit),
                    Offset(32*unit,50*unit),3*unit)
            }
        }
    }
}

@Composable
private fun Wq174ArenaModeCard(
    title:String,subtitle:String,kind:String,
    selected:Boolean,enabled:Boolean,onClick:()->Unit
) {
    val shape=RoundedCornerShape(24.dp)
    val gradient=if(selected)Brush.horizontalGradient(
        listOf(Color(0xFF5113B6),Color(0xFF8425DA),Color(0xFFB431E7)))
        else Brush.horizontalGradient(listOf(Color(0xFFFFF9FF),Color(0xFFFFF0F4)))
    Box(Modifier.fillMaxWidth().heightIn(min=114.dp)
        .shadow(if(selected)10.dp else 5.dp,shape)
        .clip(shape).background(gradient)
        .border(if(selected)3.dp else 1.5.dp,
            if(selected)Gold174 else Color.White,shape)
        .clickable(enabled=enabled,onClick=onClick)
        .padding(horizontal=14.dp,vertical=12.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(13.dp)) {
            Wq174ModeSymbol(kind,Modifier.size(76.dp))
            Box(Modifier.width(1.dp).height(64.dp).background(
                if(selected)Color(0x66FFFFFF) else Color(0x33B07DBC)))
            Column(Modifier.weight(1f),
                verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(title,fontSize=20.sp,lineHeight=22.sp,
                    fontWeight=FontWeight.ExtraBold,
                    color=if(selected)Color.White else Ink174,
                    maxLines=2)
                Text(if(enabled)subtitle else "Arkadaşınla Oyna modunda kullanılabilir",
                    fontSize=12.sp,lineHeight=15.sp,maxLines=3,
                    color=if(selected)Color(0xFFF8ECFF) else Color(0xFF5D4E70))
            }
        }
        if(selected)Box(Modifier.align(Alignment.TopEnd).size(27.dp)
            .background(Gold174,CircleShape),contentAlignment=Alignment.Center) {
            Text("✓",fontSize=17.sp,color=Ink174,fontWeight=FontWeight.ExtraBold)
        }
    }
}

@Composable
internal fun Wq174ArenaHeroAndModes(
    selectedAvatar:String,mode:String,solo:Boolean,onMode:(String)->Unit
) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))) {
        Wq174ArenaColosseum(Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=20.dp),
            verticalArrangement=Arrangement.spacedBy(12.dp),
            horizontalAlignment=Alignment.CenterHorizontally) {
            Row(verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                Wq174TitleTrophy()
                Text("Arena",fontSize=33.sp,color=Color.White,
                    fontWeight=FontWeight.Black)
            }
            Text("Arkadaşlarınla yarış, birlikte çöz, hızlı ol!",
                color=Color(0xFFFFF4FF),fontSize=13.sp,textAlign=TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Wq174ArenaModeCard("Bulmaca Düellosu","İki oyuncu rekabet eder.",
                "duel",selected=mode=="duel",enabled=true){onMode("duel")}
            Wq174ArenaModeCard("Ortak Bulmaca",
                "Aynı bulmacayı birlikte çözersiniz.",
                "team",selected=mode=="team",enabled=!solo){onMode("team")}
            Wq174ArenaModeCard("5 Şıklı Düello",
                "Hızlı çoktan seçmeli yarışma.",
                "quiz",selected=mode=="quiz",enabled=true){onMode("quiz")}
        }
    }
}

@Composable
private fun Wq174SettingIcon(kind:String) {
    Box(Modifier.size(39.dp).clip(RoundedCornerShape(12.dp))
        .background(Color(0xFFEEDCFF)),contentAlignment=Alignment.Center) {
        when(kind){
            "level"->Text("ABC",fontSize=13.sp,color=Royal174,fontWeight=FontWeight.Black)
            "topic"->Text("▱",fontSize=23.sp,color=Royal174,fontWeight=FontWeight.Bold)
            "time"->Text("◴",fontSize=27.sp,color=Royal174,fontWeight=FontWeight.Bold)
            "count"->Text("☷",fontSize=25.sp,color=Royal174,fontWeight=FontWeight.Bold)
            else->Text("✦",fontSize=22.sp,color=Color(0xFFB68704))
        }
    }
}

@Composable
private fun Wq174SettingRow(
    kind:String,label:String,value:String,choices:List<String>,
    enabled:Boolean=true,onSelect:(String)->Unit
) {
    var expanded by remember{mutableStateOf(false)}
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
        .background(Color(0xFFFFF9FC))
        .border(1.dp,Border174,RoundedCornerShape(18.dp))
        .padding(horizontal=9.dp,vertical=5.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Wq174SettingIcon(kind)
        Text(label,color=Ink174,fontSize=13.sp,fontWeight=FontWeight.Bold,
            modifier=Modifier.weight(.85f),maxLines=2)
        Box(Modifier.weight(1.2f)) {
            Surface(onClick={expanded=true},enabled=enabled,
                modifier=Modifier.fillMaxWidth().heightIn(min=43.dp),
                shape=RoundedCornerShape(23.dp),
                color=Color(0xFFF8F3FF),
                border=androidx.compose.foundation.BorderStroke(
                    1.dp,Color(0xFFD6BBF3))) {
                Row(Modifier.padding(horizontal=11.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text(value,maxLines=1,overflow=TextOverflow.Ellipsis,
                        modifier=Modifier.weight(1f),fontSize=12.sp,
                        fontWeight=FontWeight.ExtraBold,color=Ink174)
                    Text("⌄",fontSize=16.sp,color=Royal174)
                }
            }
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                choices.forEach{option->
                    DropdownMenuItem(text={Text(option,color=Ink174,fontSize=13.sp)},
                        onClick={onSelect(option);expanded=false})
                }
            }
        }
    }
}

/** All choices are real upstream callbacks; no invented match state. */
@Composable
internal fun Wq174UnifiedMatchSettings(
    solo:Boolean,onSolo:(Boolean)->Unit,mode:String,onMode:(String)->Unit,
    level:String,onLevel:(String)->Unit,
    topic:String,topics:List<String>,onTopic:(String)->Unit,
    seconds:Int,allowedTimes:List<Int>,onSeconds:(Int)->Unit,
    size:Int,sizes:List<Int>,onSize:(Int)->Unit
) {
    LaunchedEffect(level,mode,topics) {
        if(topics.isNotEmpty() && topic !in topics)onTopic(topics.first())
    }
    Column(Modifier.fillMaxWidth().shadow(8.dp,RoundedCornerShape(29.dp))
        .clip(RoundedCornerShape(29.dp))
        .background(Color(0xFFFFF8FE))
        .border(1.dp,Color(0xFFD9B9FD),RoundedCornerShape(29.dp))) {
        Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(
            listOf(Color(0xFF4F149F),Color(0xFF872CDD))))
            .padding(horizontal=18.dp,vertical=14.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Text("⚙",color=Color.White,fontSize=24.sp)
            Spacer(Modifier.width(9.dp))
            Text("Maç Ayarları",color=Color.White,
                fontSize=22.sp,fontWeight=FontWeight.Black)
        }
        Column(Modifier.padding(13.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("Oyun Biçimi",color=Ink174,fontWeight=FontWeight.ExtraBold,
                fontSize=13.sp)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))
                .background(Color(0xFFF3EAFE))
                .border(1.dp,Border174,RoundedCornerShape(23.dp))) {
                listOf(false to "Arkadaşınla Oyna",
                    true to "Tek Başına Oyna").forEach{(isSolo,label)->
                    val checked=solo==isSolo
                    val bg=if(checked)Brush.horizontalGradient(
                        listOf(Color(0xFF6321BA),Color(0xFF9F38EA)))
                    else Brush.horizontalGradient(listOf(
                        Color(0xFFF8F3FF),Color(0xFFF8F3FF)))
                    Box(Modifier.weight(1f).heightIn(min=48.dp)
                        .clip(RoundedCornerShape(23.dp)).background(bg)
                        .clickable{
                            if(isSolo && mode=="team")onMode("duel")
                            onSolo(isSolo)
                        },contentAlignment=Alignment.Center) {
                        Text(label,fontSize=12.sp,fontWeight=FontWeight.Bold,
                            color=if(checked)Color.White else Ink174,
                            textAlign=TextAlign.Center,
                            modifier=Modifier.padding(horizontal=4.dp))
                    }
                }
            }
            Wq174SettingRow("level","Seviye Seçiniz",
                level.ifBlank{"Seçiniz"},PremiumArenaRules.allowedLevels,
                onSelect=onLevel)
            Wq174SettingRow("topic","Konu Seçiniz",
                topic.ifBlank{"Seçiniz"},topics,
                enabled=topics.isNotEmpty(),onSelect=onTopic)
            Wq174SettingRow("time","Süre",
                if(seconds==0)"Süresiz" else "$seconds saniye",
                allowedTimes.map{if(it==0)"Süresiz" else "$it saniye"},
                onSelect={v->onSeconds(if(v=="Süresiz")0 else v.substringBefore(' ').toInt())})
            Wq174SettingRow("count","Bulmaca Sayısı",
                if(mode=="quiz")"12 soru" else "$size kelime",
                (if(mode=="quiz")listOf(12) else sizes).map {
                    "$it "+if(mode=="quiz")"soru" else "kelime"
                },
                enabled=mode!="quiz",
                onSelect={v -> v.substringBefore(' ').toIntOrNull()?.let { n -> onSize(n) }})
            // Existing match rules control hint budget; this is deliberately
            // informative rather than a nonfunctional dropdown.
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFFFF5DB)).padding(11.dp),
                verticalAlignment=Alignment.CenterVertically){
                Wq174SettingIcon("hint")
                Spacer(Modifier.width(9.dp))
                Text("İpucu",modifier=Modifier.weight(1f),
                    color=Ink174,fontWeight=FontWeight.Bold)
                Text("3 hak · maç kurallarına bağlı",color=Color(0xFF6B438D),
                    fontSize=12.sp,fontWeight=FontWeight.SemiBold)
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(
                    Color(0xFFEBD9FF),Color(0xFFF7EFFF))))
                .padding(13.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                Text("Maç Kuralları",color=Ink174,fontSize=14.sp,
                    fontWeight=FontWeight.ExtraBold)
                Text("• Seçtiğin seviye ve konuya uygun sorular gelir.\n"+
                    "• Süre bittiğinde cevaplar mevcut kurallarla kontrol edilir.\n"+
                    "• İpucu kullanımı mevcut puanlama sistemine bağlıdır.",
                    color=Color(0xFF493568),fontSize=11.sp,lineHeight=17.sp)
            }
        }
    }
}
