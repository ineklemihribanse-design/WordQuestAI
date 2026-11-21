package com.kelimebulmacasi.a1

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AcademyNavy = Color(0xFF102A56)
private val AcademyGold = Color(0xFFFFDA7B)

@Composable
internal fun Wq175YdsAcademy(
    back: () -> Unit, prep: () -> Unit, exams: () -> Unit
) {
    BackHandler(onBack = back)
    Column(Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(
            Color(0xFFF8FBFF), Color(0xFFEAF2FF), Color(0xFFFDFBFF))))
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = back) {
            Text("‹  Geri", color = AcademyNavy, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(29.dp))
            .heightIn(min=202.dp)) {
            Wq175ThemeBanner("yds_akademi",Modifier.matchParentSize())
            Row(Modifier.fillMaxWidth().padding(19.dp),
                verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.fillMaxWidth(.67f),
                    verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("WORDQUEST AI · YDS",color=AcademyGold,
                        fontSize=11.sp,fontWeight=FontWeight.Black)
                    Text("YDS Akademi",color=Color.White,
                        fontSize=27.sp,lineHeight=31.sp,
                        fontWeight=FontWeight.ExtraBold)
                    Text("Kelimelerden tam denemeye, bütün çalışmaların tek yerde.",
                        color=Color(0xFFF1F4FF),fontSize=12.sp,lineHeight=17.sp)
                    Text("Hedef: 2.000 doğrulanmış soru",
                        color=AcademyGold,fontSize=10.sp,fontWeight=FontWeight.Bold)
                }
            }
        }
        Text("Çalışma alanları",color=AcademyNavy,
            fontSize=20.sp,fontWeight=FontWeight.ExtraBold)
        val modules=listOf(
            Wq175AcademyModule("Kelime","YDS kelimeleri","cards",
                Color(0xFFE6F7F1),prep),
            Wq175AcademyModule("Dilbilgisi","Gramer konuları","grammar",
                Color(0xFFF0E8FC),exams),
            Wq175AcademyModule("Paragraf","Okuma ve anlama","paragraph",
                Color(0xFFFFF2D7),exams),
            Wq175AcademyModule("Çeviri","İngilizce–Türkçe","translate",
                Color(0xFFE3F1FF),exams),
            Wq175AcademyModule("Yakın Anlam","Anlam ilişkileri","synonym",
                Color(0xFFFFE4F1),exams),
            Wq175AcademyModule("Cümle Tamamlama","Eksik ifadeler","completion",
                Color(0xFFE1F5E9),exams),
            Wq175AcademyModule("Mini Deneme","Kısa testler","timer",
                Color(0xFFE3F1FF),exams),
            Wq175AcademyModule("Tam Deneme","80 soru · 180 dk","exam",
                Color(0xFFDFE5FF),exams),
            Wq175AcademyModule("Yanlış Analizi","Hataları incele","review",
                Color(0xFFFFEECF),exams)
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cols=if(maxWidth<390.dp ||
                LocalDensity.current.fontScale>1.25f)2 else 3
            Column(verticalArrangement=Arrangement.spacedBy(11.dp)) {
                modules.chunked(cols).forEach { group ->
                    Row(Modifier.fillMaxWidth(),
                        horizontalArrangement=Arrangement.spacedBy(11.dp)) {
                        group.forEach { module ->
                            Box(Modifier.weight(1f)) {
                                Wq175AcademyModuleCard(module)
                            }
                        }
                        repeat(cols-group.size){Spacer(Modifier.weight(1f))}
                    }
                }
            }
        }
        Text("Kelime çalışmalarında hazırlık paketleri, diğer alanlarda sınav merkezi açılır. " +
            "Soruları ve denemeleri oradan seçebilirsin.",
            color=Color(0xFF586D87),fontSize=11.sp,lineHeight=16.sp)
        Surface(shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF1F5FF),
            border = BorderStroke(1.dp, Color(0xFFD7E3FA))) {
            Column(Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Öğrenme planı", color = AcademyNavy,
                    fontWeight = FontWeight.Bold)
                Text("Kelime → konu çalışması → soru çözümü → yanlışları tekrar et. " +
                    "Sınav geçmişin ve önceki puanların korunur.",
                    color = Color(0xFF556D90), fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

private data class Wq175AcademyModule(
    val title:String,val subtitle:String,val icon:String,
    val background:Color,val action:()->Unit
)

@Composable
private fun Wq175AcademyModuleCard(module:Wq175AcademyModule) {
    Surface(onClick=module.action,
        modifier=Modifier.fillMaxWidth().heightIn(min=144.dp),
        shape=RoundedCornerShape(23.dp),
        color=module.background,
        shadowElevation=4.dp,
        border=BorderStroke(1.dp,Color(0xFFE2DFDA))) {
        Column(Modifier.fillMaxWidth().padding(11.dp),
            verticalArrangement=Arrangement.spacedBy(5.dp)) {
            Wq175GlossIcon(module.icon,Modifier.size(69.dp))
            Text(module.title,color=AcademyNavy,
                fontSize=15.sp,lineHeight=17.sp,
                fontWeight=FontWeight.ExtraBold,
                maxLines=2,overflow=TextOverflow.Ellipsis)
            Text(module.subtitle,color=Color(0xFF586A81),
                fontSize=11.sp,lineHeight=14.sp,
                maxLines=2,overflow=TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Wq175AcademyTile(
    title: String, detail: String, icon: String,
    pale: Color, ink: Color, action: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Row(Modifier.fillMaxWidth().shadow(4.dp, shape)
        .clip(shape).background(Brush.horizontalGradient(listOf(Color.White,pale)))
        .clickable(onClick = action).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Wq175GlossIcon(icon, Modifier.size(69.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = AcademyNavy,
                fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            Text(detail, color = Color(0xFF62718B),
                lineHeight = 17.sp, fontSize = 12.sp)
        }
        Text("›", fontSize = 28.sp, color = ink, fontWeight = FontWeight.Bold)
    }
}

/** The 2,000-word tier is a CONTENT TARGET, never fake current progress. */
@Composable
internal fun Wq175PackageIntro(mode: GameMode, level: String) {
    val yds = mode == GameMode.Yds
    val bg = if (yds) listOf(AcademyNavy,Color(0xFF4270AA))
        else listOf(Color(0xFF0A827F),Color(0xFF6DC6C4))
    val title = if (yds) "YDS Kelime Paketleri" else "Hazır Paketler"
    val subtitle = if (yds) "Seviyene göre özgün bulmaca ve kelime çalışmaları"
        else "A1–C2 seviyeleri için zenginleşen kelime kütüphanesi"
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(23.dp))
        .background(Brush.horizontalGradient(bg))
        .padding(horizontal = 17.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = Color.White,
                fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
            Text(subtitle, color = Color(0xFFE8FAFF),
                fontSize = 12.sp, lineHeight = 17.sp)
            if (!yds) {
                Text("İçerik hedefi: Her seviyede 2.000 kelime / ifade",
                    color = Color(0xFFDEFDE9), fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }
        Wq175GlossIcon(if (yds) "yds" else "packages", Modifier.size(88.dp))
    }
}
