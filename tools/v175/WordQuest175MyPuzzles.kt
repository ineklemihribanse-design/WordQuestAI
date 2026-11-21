package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

/** Visual-only, profile-backed counts: does not invent puzzle records. */
@Composable
internal fun Wq175MyPuzzlesHero(total: Int, completed: Int) {
    BoxWithConstraints(Modifier.fillMaxWidth().heightIn(min=215.dp)
        .clip(RoundedCornerShape(27.dp))
        .background(Brush.horizontalGradient(listOf(
            Color(0xFF146348),Color(0xFF33896B),Color(0xFF91CE9A))))) {
        Box(Modifier.align(Alignment.TopEnd).offset(x=40.dp,y=(-74).dp)
            .size(220.dp).background(Color.White.copy(alpha=.08f),CircleShape))
        val compact=maxWidth<330.dp
        Row(Modifier.fillMaxWidth().padding(17.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            Column(Modifier.weight(1f),
                verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("KENDİ BULMACA ATÖLYEN",
                    color=Color(0xFFEAF79B),fontSize=10.sp,
                    fontWeight=FontWeight.Black)
                Text("Tasarla, çöz,\nyeniden keşfet.",
                    color=Color.White,fontSize=if(compact)21.sp else 25.sp,
                    lineHeight=if(compact)25.sp else 29.sp,
                    fontWeight=FontWeight.ExtraBold)
                Text("Kelimelerinden özgün bulmacalar oluştur.",
                    color=Color(0xFFE5FFF0),fontSize=12.sp,lineHeight=16.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf(total to "Kaydedilen",completed to "Tamamlanan")
                        .forEach { (value,label) ->
                            Column(Modifier.clip(RoundedCornerShape(11.dp))
                                .background(Color.White.copy(alpha=.22f))
                                .padding(horizontal=10.dp,vertical=8.dp)) {
                                Text(value.toString(),color=Color.White,
                                    fontWeight=FontWeight.Black,fontSize=17.sp)
                                Text(label,color=Color(0xFFF3FFFA),fontSize=10.sp)
                            }
                        }
                }
            }
            Box(Modifier.width(if(compact)88.dp else 118.dp)
                .height(164.dp),contentAlignment=Alignment.Center) {
                Box(Modifier.size(116.dp)
                    .background(Color.White.copy(alpha=.11f),CircleShape))
                Wq175GlossIcon("mistakes",
                    Modifier.align(Alignment.Center).size(if(compact)85.dp else 110.dp))
                Wq175GlossIcon("notebook",
                    Modifier.align(Alignment.BottomEnd).size(47.dp))
            }
        }
    }
}

/** Real editor route; this primary CTA never claims to create a puzzle by itself. */
@Composable
internal fun Wq175PuzzleCreateCta(onClick:()->Unit) {
    Surface(onClick=onClick,modifier=Modifier.fillMaxWidth().heightIn(min=98.dp),
        shape=RoundedCornerShape(23.dp),color=Color(0xFF14644E),
        border=BorderStroke(2.dp,Color(0xFFB3DD76)),
        shadowElevation=6.dp) {
        Row(Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Wq175GlossIcon("mistakes",Modifier.size(69.dp))
            Column(Modifier.weight(1f),
                verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text("Yeni Bulmaca Oluştur",color=Color.White,
                    fontWeight=FontWeight.ExtraBold,fontSize=19.sp,
                    lineHeight=23.sp)
                Text("Kelime seç, soru türünü belirle ve oluştur.",
                    color=Color(0xFFE7FCE9),fontSize=12.sp,lineHeight=16.sp)
            }
            Text("›",fontWeight=FontWeight.Black,fontSize=30.sp,
                color=Color(0xFFD9EEA1))
        }
    }
}

private data class Wq175PuzzleSource(
    val title:String,val icon:String,val tint:Color,val click:()->Unit
)

/** Every option leads to a real existing editor, package list, or import UI.
 * PDF, camera and AI first import vocabulary into the user's learning library.
 */
@Composable
internal fun Wq175PuzzleSourceShortcuts(
    onManual:()->Unit,onPackage:()->Unit,onImport:()->Unit
) {
    val sources=listOf(
        Wq175PuzzleSource("Kendi Kelimelerinle","notebook",Color(0xFFE9F7E5),onManual),
        Wq175PuzzleSource("Hazır Paketten Seç","packages",Color(0xFFF0E7FF),onPackage),
        Wq175PuzzleSource("PDF’den İçe Aktar","pdf",Color(0xFFE7F3FF),onImport),
        Wq175PuzzleSource("Görsel / Kamera","camera",Color(0xFFFFF1E4),onImport),
        Wq175PuzzleSource("Yapay Zekâ","ai_teacher",Color(0xFFF4E8FF),onImport)
    )
    Column(Modifier.fillMaxWidth(),
        verticalArrangement=Arrangement.spacedBy(9.dp)) {
        Text("Bulmaca için kelime kaynağın",fontSize=17.sp,
            color=Color(0xFF224C42),fontWeight=FontWeight.ExtraBold)
        Row(Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            sources.forEach { source ->
                Surface(onClick=source.click,
                    modifier=Modifier.width(137.dp).heightIn(min=126.dp),
                    shape=RoundedCornerShape(19.dp),color=source.tint,
                    shadowElevation=3.dp,
                    border=BorderStroke(1.dp,Color(0x22A2A2A2))) {
                    Column(Modifier.fillMaxWidth().padding(10.dp),
                        verticalArrangement=Arrangement.spacedBy(5.dp),
                        horizontalAlignment=Alignment.CenterHorizontally) {
                        Wq175GlossIcon(source.icon,Modifier.size(61.dp))
                        Text(source.title,color=Color(0xFF24483E),
                            fontSize=12.sp,lineHeight=15.sp,
                            fontWeight=FontWeight.Bold,maxLines=2)
                    }
                }
            }
        }
        Text("PDF, kamera ve AI önce Kelime Hafızam’daki içe aktarma ekranını açar; "+
             "kelimeleri onayladıktan sonra bulmaca oluşturabilirsin.",
            fontSize=11.sp,lineHeight=15.sp,color=Color(0xFF68716E))
    }
}
