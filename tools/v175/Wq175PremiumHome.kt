package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compact real-Android two-column home navigation: every card routes to the
 * existing working screen. Original glossy WebP game icons, not text or emoji.
 */
private data class Wq175HomeTile(
    val title:String,
    val subtitle:String,
    val icon:String,
    val tint:Color,
    val onClick:()->Unit,
    val artSlug:String?=null
)

private val Wq175Navy = Color(0xFF1E3742)
private val Wq175Muted = Color(0xFF536977)

@Composable
private fun Wq175HomeTileView(tile:Wq175HomeTile,small:Boolean=false) {
    val shape=RoundedCornerShape(22.dp)
    Surface(
        onClick=tile.onClick,
        modifier=Modifier.fillMaxWidth().heightIn(min=if(small)128.dp else 170.dp),
        shape=shape,color=tile.tint,
        border=BorderStroke(1.dp,Color(0xFFE2E2DD)),
        shadowElevation=3.dp
    ){
        Column(
            Modifier.fillMaxWidth().padding(
                horizontal=if(small)10.dp else 13.dp,
                vertical=if(small)9.dp else 12.dp),
            verticalArrangement=Arrangement.spacedBy(5.dp)
        ) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically) {
                if(tile.artSlug!=null)
                    Wq175ThemeIcon(tile.artSlug,Modifier.size(if(small)53.dp else 76.dp))
                else Wq175GlossIcon(tile.icon,Modifier.size(if(small)53.dp else 76.dp))
                Text("›",color=Wq175Navy,fontSize=23.sp,fontWeight=FontWeight.Bold)
            }
            Text(tile.title,color=Wq175Navy,
                fontSize=if(small)13.sp else 15.sp,
                lineHeight=if(small)15.sp else 18.sp,
                fontWeight=FontWeight.ExtraBold,
                maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(tile.subtitle,color=Wq175Muted,
                fontSize=if(small)10.sp else 11.sp,
                lineHeight=if(small)12.sp else 14.sp,
                maxLines=2,overflow=TextOverflow.Ellipsis)
        }
    }
}

/** Primary CTA styled for the midnight-slate premium home rather than old green. */
@Composable
internal fun Wq175HomeStartButton(onClick:()->Unit) {
    Button(
        onClick=onClick,
        modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),
        shape=RoundedCornerShape(18.dp),
        colors=ButtonDefaults.buttonColors(
            containerColor=Color(0xFF315574),contentColor=Color.White)
    ) {
        Text("Yeni Oyun",fontSize=16.sp,fontWeight=FontWeight.ExtraBold)
    }
}

@Composable
internal fun Wq175PremiumHomeGrid(
    session:AppSession,
    newGame:()->Unit,
    packages:()->Unit,
    arena:()->Unit,
    yds:()->Unit,
    ydsCenter:()->Unit,
    custom:()->Unit,
    learning:()->Unit,
    practice:()->Unit,
    assistant:()->Unit,
    verbs:()->Unit,
    progress:()->Unit,
    insights:()->Unit
) {
    val main = listOf(
        Wq175HomeTile("Hazır Paketler","Seviyeni seç, kelime dünyasını keşfet.",
            "packages",Color(0xFFF4F4DC),packages),
        Wq175HomeTile("Arena","Düello, ortak bulmaca ve 5 şıklı yarış.",
            "trophy",Color(0xFFF0E8FC),arena,artSlug="arena"),
        Wq175HomeTile("YDS Akademi","Konu, paragraf, deneme ve analiz.",
            "yds",Color(0xFFE7F3FF),ydsCenter,artSlug="yds_akademi"),
        Wq175HomeTile("Kelime Hafızam",
            "${session.learningDueCount()} kelime tekrar bekliyor.",
            "cards",Color(0xFFE7F8F7),learning,artSlug="kelime_hafizam"),
        Wq175HomeTile("Bulmacalarım","Oluştur, eşleştir ve kaydet.",
            "puzzle",Color(0xFFFFF1E1),custom,artSlug="bulmacalarim"),
        Wq175HomeTile("Günlük Planım","Hedef koy, çalış, ilerle.",
            "plan",Color(0xFFFFF6DD),practice,artSlug="gunluk_plan"),
        Wq175HomeTile("Yeni Oyun","Kendi kelime tahtanı oluştur.",
            "sentence",Color(0xFFE9F4EF),newGame,artSlug="yeni_oyun"),
        Wq175HomeTile("Konuşma Asistanı","Lena ile yazılı ve sesli sohbet.",
            "assistant",Color(0xFFF0EDFA),assistant)
    )
    val more = listOf(
        Wq175HomeTile("YDS Hazırlık","Kolay, orta ve zor bulmacalar.",
            "yds",Color(0xFFE9F1FD),yds),
        Wq175HomeTile("Fiiller","V1, V2, V3 ile pratik yap.",
            "verbs",Color(0xFFE7F4EE),verbs),
        Wq175HomeTile("İlerlemem",
            "${session.totalPoints} puan · ${session.finishedCount} bulmaca.",
            "trophy",Color(0xFFFFF1DC),progress),
        Wq175HomeTile("Öğrenme Merkezim","Tekrar, istatistik ve öneriler.",
            "cards",Color(0xFFECEFFD),insights)
    )
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(11.dp)) {
        Text("Keşfet ve öğren",fontWeight=FontWeight.ExtraBold,
            color=Wq175Navy,fontSize=20.sp)
        for(pair in main.chunked(2)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                pair.forEach{tile ->
                    Box(Modifier.weight(1f)) { Wq175HomeTileView(tile) }
                }
            }
        }
        Text("Diğer çalışmalar",fontWeight=FontWeight.Bold,
            color=Wq175Navy,fontSize=17.sp,
            modifier=Modifier.padding(top=8.dp))
        for(pair in more.chunked(2)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                pair.forEach{tile ->
                    Box(Modifier.weight(1f)){ Wq175HomeTileView(tile,small=true) }
                }
            }
        }
    }
}
