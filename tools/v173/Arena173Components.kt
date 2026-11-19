package com.kelimebulmacasi.a1

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val v173Ink=Color(0xFF302056)
private val v173Violet=Color(0xFF7442B4)
private val v173Gold=Color(0xFFFFCA57)
private val v173Stroke=Color(0xFFE2D7F3)

@Composable
private fun Arena173Icon(kind:String) {
    val context=LocalContext.current
    val iconAtlas:Bitmap?=remember(context) {
        runCatching {
            context.assets.open("arena/icons/wq173_mode_icons.webp").use {
                BitmapFactory.decodeStream(it)
            }
        }.getOrNull()
    }
    val tile=remember(iconAtlas,kind) {
        val i=when(kind) {"duel"->0;"team"->1;else->2}
        if(iconAtlas!=null && iconAtlas.width==144 && iconAtlas.height==48)
            runCatching{Bitmap.createBitmap(iconAtlas,i*48,0,48,48)}.getOrNull()
        else null
    }
    if(tile!=null) {
        Image(tile.asImageBitmap(),contentDescription=when(kind){
            "duel"->"Çapraz kılıçlar";"team"->"Mor ve altın yapboz";else->"Altın şimşek"
        },modifier=Modifier.size(67.dp),contentScale=ContentScale.Fit)
        return
    }
    // Keep the fully original vector drawing as a reliable offline fallback.
    Canvas(Modifier.size(67.dp)) {
        val u=size.minDimension/100f
        when(kind) {
            "duel" -> {
                // Beveled metal sword shafts, gold guards and soft 3D shadows.
                drawLine(Color(0xFF68499B),Offset(22*u,83*u),Offset(80*u,18*u),15*u)
                drawLine(Color(0xFF6F518F),Offset(78*u,83*u),Offset(20*u,18*u),15*u)
                drawLine(Color(0xFFE1A53F),Offset(17*u,87*u),Offset(78*u,20*u),12*u)
                drawLine(Color(0xFFF6C861),Offset(83*u,87*u),Offset(22*u,20*u),12*u)
                val bladeA=Path().apply{
                    moveTo(19*u,18*u);lineTo(25*u,13*u);lineTo(75*u,67*u)
                    lineTo(65*u,73*u);close()
                }
                val bladeB=Path().apply{
                    moveTo(81*u,18*u);lineTo(75*u,13*u);lineTo(25*u,67*u)
                    lineTo(35*u,73*u);close()
                }
                drawPath(bladeA,Color(0xFFD9DAEF))
                drawPath(bladeB,Color(0xFFF1F2FF))
                drawLine(Color.White,Offset(24*u,17*u),Offset(68*u,67*u),2*u)
                drawLine(Color.White,Offset(76*u,16*u),Offset(32*u,67*u),2*u)
                drawLine(Color(0xFFFFC755),Offset(58*u,56*u),Offset(79*u,76*u),9*u)
                drawLine(Color(0xFFFFC755),Offset(42*u,56*u),Offset(21*u,76*u),9*u)
                drawCircle(Color(0xFFEEB247),7*u,Offset(78*u,82*u))
                drawCircle(Color(0xFFEEB247),7*u,Offset(22*u,82*u))
            }
            "team" -> {
                // Two connected puzzle pieces with glossy beveled faces.
                val violet=Path().apply{
                    moveTo(18*u,36*u);lineTo(34*u,36*u)
                    cubicTo(28*u,19*u,49*u,16*u,50*u,35*u)
                    lineTo(58*u,36*u);lineTo(58*u,52*u)
                    cubicTo(74*u,46*u,75*u,65*u,58*u,65*u)
                    lineTo(58*u,82*u);lineTo(18*u,82*u);close()
                }
                val gold=Path().apply{
                    moveTo(53*u,25*u);lineTo(67*u,25*u)
                    cubicTo(66*u,9*u,85*u,12*u,83*u,25*u)
                    lineTo(89*u,25*u);lineTo(89*u,66*u)
                    lineTo(74*u,66*u)
                    cubicTo(76*u,81*u,58*u,80*u,60*u,66*u)
                    lineTo(53*u,66*u);close()
                }
                drawPath(gold,Color(0xFFDA9138))
                drawPath(gold,Brush.verticalGradient(
                    listOf(Color(0xFFFFEB8B),Color(0xFFFFBC4A),Color(0xFFDC8C26))))
                drawPath(violet,Color(0xFF6540B2))
                drawPath(violet,Brush.verticalGradient(
                    listOf(Color(0xFFBF93FF),Color(0xFF8E55DA),Color(0xFF6A3BAC))))
                drawPath(violet,Color(0xFFD7B4FF),style=Stroke(1.8f*u))
                drawPath(gold,Color(0xFFFFECB0),style=Stroke(1.4f*u))
            }
            else -> {
                val shadow=Path().apply{
                    moveTo(59*u,6*u);lineTo(23*u,54*u);lineTo(48*u,54*u)
                    lineTo(36*u,96*u);lineTo(83*u,41*u);lineTo(57*u,41*u);close()
                }
                drawPath(shadow,Color(0xFFBE761D))
                val bolt=Path().apply{
                    moveTo(57*u,4*u);lineTo(25*u,50*u);lineTo(49*u,50*u)
                    lineTo(38*u,89*u);lineTo(78*u,40*u);lineTo(55*u,40*u);close()
                }
                drawPath(bolt,Brush.verticalGradient(listOf(
                    Color(0xFFFFED9D),Color(0xFFFFC84C),Color(0xFFEE9A20))))
                drawPath(bolt,Color(0xFFFFF1B5),style=Stroke(2*u))
                drawLine(Color(0xFFFFF5C5),Offset(55*u,17*u),Offset(39*u,46*u),2.5f*u)
            }
        }
    }
}

@Composable
private fun Arena173GameCard(
    title:String,subtitle:String,caption:String,kind:String,
    selected:Boolean,enabled:Boolean,cardWidth:androidx.compose.ui.unit.Dp=225.dp,
    onClick:()->Unit
) {
    val shape=RoundedCornerShape(25.dp)
    val gradient=if(selected) Brush.verticalGradient(listOf(
        Color(0xFFA476DC),Color(0xFF7846B9),Color(0xFF512582)))
        else Brush.verticalGradient(listOf(Color.White,Color(0xFFFFF8FC)))
    Box(Modifier.width(cardWidth).height(213.dp)
        .shadow(if(selected)9.dp else 5.dp,shape)
        .clip(shape).background(gradient)
        .border(if(selected)2.dp else 1.dp,
            if(selected)v173Gold else Color.White,shape)
        .clickable(enabled=enabled,onClick=onClick).padding(12.dp)) {
        Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Arena173Icon(kind)
            Text(title,fontSize=18.sp,fontWeight=FontWeight.ExtraBold,
                color=if(selected)Color.White else v173Ink,textAlign=TextAlign.Center,
                maxLines=1)
            Text(subtitle,fontSize=12.sp,
                color=if(selected)Color(0xFFF4EAFB) else Color(0xFF776B8B),
                lineHeight=14.sp,maxLines=2,textAlign=TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Box(Modifier.clip(RoundedCornerShape(30.dp))
                .background(if(selected)Color(0xFF9864C7) else Color(0xFFF4EAF9))
                .padding(horizontal=12.dp,vertical=7.dp)) {
                Text(if(enabled)caption else "Çevrimiçi mod",
                    fontSize=12.sp,fontWeight=FontWeight.Bold,
                    color=if(selected)Color.White else v173Ink)
            }
        }
        if(selected) Box(Modifier.align(Alignment.TopEnd).size(25.dp)
            .clip(CircleShape).background(Color(0xFF9864D8)),
            contentAlignment=Alignment.Center) {
            Text("✓",color=Color.White,fontWeight=FontWeight.Bold)
        }
    }
}

@Composable
internal fun Arena173AdventurePicker(mode:String,onMode:(String)->Unit,solo:Boolean) {
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(9.dp)){
        Text("Maceranı Seç!",fontSize=28.sp,fontWeight=FontWeight.ExtraBold,color=v173Ink)
        Text("Bugün nasıl bir mücadele istiyorsun?",fontSize=13.sp,
            color=Color(0xFF716B78))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Tablet: three equal cards across the row. Phone: swipeable,
            // full-sized cards instead of shrinking into bad tap targets.
            val cardWidth=if(maxWidth>=720.dp)(maxWidth-22.dp)/3 else 225.dp
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(11.dp)) {
                Arena173GameCard("Bulmaca Düellosu","Rakibine meydan oku!",
                    "2 oyuncu","duel",mode=="duel" || (solo && mode!="quiz"),
                    true,cardWidth=cardWidth) {
                    onMode("duel")
                }
                Arena173GameCard("Ortak Bulmaca","Birlikte çöz, birlikte kazan!",
                    "2+ oyuncu","team",mode=="team",!solo,cardWidth=cardWidth) {
                    onMode("team")
                }
                Arena173GameCard("5 Şıklı Düello","Hızlı düşün, doğru seç!",
                    "Hızlı oyun","quiz",mode=="quiz",true,cardWidth=cardWidth) {
                    onMode("quiz")
                }
            }
        }
    }
}

/** Accessible dropdown field: chosen value is always visible on the button. */
@Composable
private fun Arena173Dropdown(
    label:String,current:String,entries:List<String>,onChoose:(String)->Unit,
    enabled:Boolean=true,modifier:Modifier=Modifier
) {
    var open by remember {mutableStateOf(false)}
    Column(modifier,verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Text(label,fontSize=12.sp,color=v173Ink,fontWeight=FontWeight.Bold)
        Box {
            OutlinedButton(onClick={open=true},enabled=enabled,
                modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),
                shape=RoundedCornerShape(16.dp),
                contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)) {
                Text(current,fontSize=12.sp,color=v173Ink,maxLines=1,
                    overflow=TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
                Text("⌄",fontSize=16.sp,color=v173Violet)
            }
            DropdownMenu(expanded=open,onDismissRequest={open=false}) {
                entries.forEach {entry ->
                    DropdownMenuItem(text={
                        Text(entry,color=v173Ink,fontSize=13.sp)
                    },onClick={onChoose(entry);open=false})
                }
            }
        }
    }
}

@Composable
private fun Arena173IntDropdown(
    label:String,current:Int,options:List<Int>,onChoose:(Int)->Unit,
    suffix:String,enabled:Boolean=true,modifier:Modifier=Modifier
) {
    Arena173Dropdown(label,if(current==0)"Süresiz" else "$current $suffix",
        options.map{if(it==0)"Süresiz" else "$it $suffix"},
        onChoose={selected ->
            val value=if(selected=="Süresiz")0 else
                selected.substringBefore(' ').toIntOrNull()
            if(value!=null) onChoose(value)
        },enabled=enabled,modifier=modifier)
}

/** One unified section; uses original online/offline callbacks and rule datasets. */
@Composable
internal fun Arena173UnifiedMatchSettings(
    solo:Boolean,onSolo:(Boolean)->Unit,mode:String,onMode:(String)->Unit,
    level:String,onLevel:(String)->Unit,
    topic:String,topics:List<String>,onTopic:(String)->Unit,
    seconds:Int,allowedTimes:List<Int>,onSeconds:(Int)->Unit,
    size:Int,sizes:List<Int>,onSize:(Int)->Unit
) {
    Column(Modifier.fillMaxWidth().shadow(6.dp,RoundedCornerShape(25.dp))
        .clip(RoundedCornerShape(25.dp)).background(Color.White)
        .border(1.dp,v173Stroke,RoundedCornerShape(25.dp))
        .padding(15.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Maç Ayarları",fontSize=20.sp,fontWeight=FontWeight.ExtraBold,color=v173Ink)
        Text("Oyun Biçimi",fontWeight=FontWeight.Bold,fontSize=13.sp,color=v173Ink)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            ArenaApprovedOption("Arkadaşınla Oyna",!solo,Modifier.weight(1f),
                symbol=null,description="Oda oluştur, arkadaşını davet et",compact=true) {
                onSolo(false)
            }
            ArenaApprovedOption("Tek Başına Oyna",solo,Modifier.weight(1f),
                symbol=null,description="Hemen oyuna başla",compact=true) {
                if(mode=="team") onMode("duel")
                onSolo(true)
            }
        }
        Text("Maç Kurulumu",fontWeight=FontWeight.Bold,fontSize=13.sp,color=v173Ink)
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val narrow=maxWidth<490.dp
            if(narrow) {
                Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Arena173Dropdown("İngilizce Seviyesi",
                        if(level.isBlank())"Seviye Seçiniz" else "Seviye Seçiniz · $level",
                        PremiumArenaRules.allowedLevels,onLevel)
                    Arena173Dropdown("Konu / Paket",
                        if(topic.isBlank())"Konu Seçiniz" else "Konu Seçiniz · $topic",
                        topics,onTopic)
                }
            } else {
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Arena173Dropdown("İngilizce Seviyesi",
                        if(level.isBlank())"Seviye Seçiniz" else "Seviye Seçiniz · $level",
                        PremiumArenaRules.allowedLevels,onLevel,modifier=Modifier.weight(1f))
                    Arena173Dropdown("Konu / Paket",
                        if(topic.isBlank())"Konu Seçiniz" else "Konu Seçiniz · $topic",
                        topics,onTopic,modifier=Modifier.weight(1f))
                }
            }
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val narrow=maxWidth<490.dp
            Column(verticalArrangement=Arrangement.spacedBy(9.dp)) {
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Arena173IntDropdown("Süre",seconds,allowedTimes,onSeconds,"sn",
                        modifier=Modifier.weight(1f))
                    Arena173IntDropdown("Bulmaca",if(mode=="quiz")12 else size,
                        if(mode=="quiz")listOf(12) else sizes,onSize,
                        if(mode=="quiz")"soru" else "kelime",
                        enabled=mode!="quiz",modifier=Modifier.weight(1f))
                }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFFEDF9E8)).padding(11.dp),
                    verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("İpucu",color=Color(0xFF21613F),
                        fontWeight=FontWeight.Bold,fontSize=13.sp)
                    Text("Zorlanırsan sınırlı sayıda ipucu kullan.",
                        color=Color(0xFF5D765D),fontSize=11.sp)
                }
            }
        }
    }
}
