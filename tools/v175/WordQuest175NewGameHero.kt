package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val MakerInk=Color(0xFF174C44)
private val MakerLime=Color(0xFFE7FA96)

@Composable
internal fun Wq175NewGameHero(level:String,profile:String,available:Int) {
    val teal=Color(0xFF176457)
    val isYds=level in listOf("Kolay","Orta","Zor")
    Box(Modifier.fillMaxWidth().heightIn(min=172.dp)
        .clip(RoundedCornerShape(26.dp))
        .background(Brush.linearGradient(listOf(
            Color(0xFF124C45),Color(0xFF2D8976),Color(0xFF91C490))))) {
        Canvas(Modifier.matchParentSize()) {
            val cell=size.minDimension/10f
            val ox=size.width*.64f
            val oy=size.height*.10f
            for(row in 0..3)for(col in 0..3){
                val value=(row*11+col*7)%6
                drawRoundRect(if(value<3)Color(0xA5F7FFA1)
                    else Color(0x48D4F5DB),
                    Offset(ox+col*cell*.72f,oy+row*cell*.72f),
                    Size(cell*.64f,cell*.64f),CornerRadius(cell*.10f))
            }
        }
        Row(Modifier.fillMaxWidth().padding(17.dp),
            verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f),
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("KELİME ATÖLYESİ",color=MakerLime,
                    fontSize=11.sp,fontWeight=FontWeight.Black)
                Text("Kendi bulmacanı\noluştur!",
                    fontSize=23.sp,lineHeight=27.sp,
                    color=Color.White,fontWeight=FontWeight.Black)
                Text((if(isYds)"YDS " else "CEFR ")+level+
                    " · "+available+" kelime hazır",
                    color=Color(0xFFF0FFF7),fontSize=12.sp)
            }
            Wq2DPortrait(
                Wq2DMascots.selected(androidx.compose.ui.platform.LocalContext.current,profile),
                Modifier.size(112.dp))
        }
    }
}

@Composable
internal fun Wq175NewGameSummary(
    level:String,topic:String?,kind:String?,count:Int?,
    available:Int,timedMinutes:Int
) {
    val shape=RoundedCornerShape(19.dp)
    Surface(Modifier.fillMaxWidth(),shape=shape,
        color=Color(0xFFF0FCF8),
        border=BorderStroke(1.dp,Color(0xFFBCE8D8))) {
        Column(Modifier.padding(14.dp),
            verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("OYUN ÖZETİ",fontSize=11.sp,
                fontWeight=FontWeight.Black,color=MakerInk)
            Text(level+" · "+(topic?:"Tüm konular")+" · "+
                (kind?:"Tüm kelime türleri"),
                fontSize=12.sp,color=Color(0xFF356E63))
            Text("Tahta: "+(count?.toString()?:"Seçilmedi")+" kelime"+
                "    Oyun: "+if(timedMinutes==0)"Klasik" else "$timedMinutes dk",
                color=MakerInk,fontSize=13.sp,fontWeight=FontWeight.Bold)
            LinearProgressIndicator(
                progress={if(available<=0)0f
                    else((count?:0).toFloat()/available).coerceIn(0f,1f)},
                modifier=Modifier.fillMaxWidth().height(5.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color=Color(0xFF3A9B7D),trackColor=Color(0xFFD5EDE5))
            Text("$available gerçek kelimeden seçim yapılır.",
                color=Color(0xFF5D8278),fontSize=11.sp)
        }
    }
}
