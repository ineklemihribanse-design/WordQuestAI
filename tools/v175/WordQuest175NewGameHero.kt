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

/** Dynamic review panel above the existing real Start callback.
 * Every number comes from current game settings; no sample/fake progress.
 */
@Composable
internal fun Wq175NewGameSummary(
    level:String,topic:String?,kind:String?,count:Int?,
    available:Int,timedMinutes:Int
) {
    val ink=Color(0xFF193B42)
    val chosen=count?:0
    val gameType=if(timedMinutes==0) "Klasik" else "$timedMinutes dk"
    val ratio=if(available<=0)0f else (chosen.toFloat()/available).coerceIn(0f,1f)
    Surface(modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(25.dp),
        color=Color(0xFFF3FCF7),
        shadowElevation=4.dp,
        border=BorderStroke(1.dp,Color(0xFFA3D4C1))) {
        Column(Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement=Arrangement.spacedBy(13.dp)) {
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(12.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Box(Modifier.size(50.dp)
                    .background(Color(0xFFE7DBFF),RoundedCornerShape(15.dp)),
                    contentAlignment=Alignment.Center) {
                    Wq175GlossIcon("puzzle",Modifier.size(43.dp))
                }
                Column(Modifier.weight(1f),
                    verticalArrangement=Arrangement.spacedBy(3.dp)) {
                    Text("OYUN ÖZETİ",fontSize=11.sp,
                        fontWeight=FontWeight.Black,color=Color(0xFF26826C))
                    Text("Bulmacan hazır",fontSize=19.sp,
                        fontWeight=FontWeight.ExtraBold,color=ink)
                    Text((topic?:"Tüm konular")+" · "+(kind?:"Tüm kelime türleri"),
                        fontSize=12.sp,color=Color(0xFF5E7773),maxLines=2)
                }
            }
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                listOf(
                    Triple("SEVİYE",level,Color(0xFFF2EAFE)),
                    Triple("KELİME",if(chosen>0)"$chosen adet" else "Seçilmedi",Color(0xFFE3F8FA)),
                    Triple("SÜRE",gameType,Color(0xFFFFF1D8))
                ).forEach { (label,value,tint) ->
                    Surface(modifier=Modifier.weight(1f),
                        shape=RoundedCornerShape(14.dp),color=tint) {
                        Column(Modifier.padding(horizontal=9.dp,vertical=11.dp),
                            verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text(label,fontSize=9.sp,fontWeight=FontWeight.Bold,
                                color=Color(0xFF667077),maxLines=1)
                            Text(value,fontSize=13.sp,
                                fontWeight=FontWeight.ExtraBold,color=ink,
                                maxLines=2,lineHeight=16.sp)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.SpaceBetween) {
                Text("Seçilebilir kelime havuzu",
                    fontSize=12.sp,color=Color(0xFF43685E))
                Text("$available kelime",fontSize=12.sp,
                    fontWeight=FontWeight.Bold,color=Color(0xFF207D62))
            }
            LinearProgressIndicator(
                progress={ratio},
                modifier=Modifier.fillMaxWidth().height(7.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color=Color(0xFF32A58A),trackColor=Color(0xFFD8ECE4))
            Text("Seçimlerin hazır. Başlatınca kelimeler bu havuzdan alınır.",
                fontSize=11.sp,lineHeight=15.sp,color=Color(0xFF6A8179))
        }
    }
}
