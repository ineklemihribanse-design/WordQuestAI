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
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
            .background(Brush.horizontalGradient(listOf(
                AcademyNavy, Color(0xFF284D88), Color(0xFF4E6EBA))))
            .heightIn(min = 200.dp)) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Color(0x22FFE9AD), radius = size.width * 0.45f,
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * .92f, size.height * .34f))
                drawCircle(Color(0x1BFFFFFF), radius = size.width * 0.24f,
                    center = androidx.compose.ui.geometry.Offset(
                        size.width * .80f, size.height * .88f))
            }
            Row(Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("YDS AKADEMİ",
                        color = AcademyGold, fontSize = 12.sp,
                        fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    Text("İlerle, öğren,\nbaşarını gör.",
                        color = Color.White, fontSize = 27.sp, lineHeight = 31.sp,
                        fontWeight = FontWeight.ExtraBold)
                    Text("Konu çalışmaları, soru bankası ve süreli denemeler bir arada.",
                        color = Color(0xFFDFEBFF), fontSize = 12.sp, lineHeight = 17.sp)
                }
                Wq175GlossIcon("yds", Modifier.size(100.dp))
            }
        }
        Text("Çalışma yolunu seç", color = AcademyNavy,
            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Wq175AcademyTile("Konu & Kelime Hazırlığı",
            "YDS kelime paketleri, zorluk seviyeleri ve bulmacalar",
            "packages", Color(0xFFE7F3FF), Color(0xFF2D75B8), prep)
        Wq175AcademyTile("Sınav Merkezi",
            "Paragraf, çeviri, yakın anlam, denemeler ve yanlış analizi",
            "yds", Color(0xFFFFF0DB), Color(0xFF98601B), exams)
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
