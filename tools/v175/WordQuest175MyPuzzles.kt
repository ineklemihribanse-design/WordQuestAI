package com.kelimebulmacasi.a1

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
        .background(Brush.horizontalGradient(listOf(
            Color(0xFF17624F), Color(0xFF44A17D), Color(0xFF80C5A1))))
        .padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("KENDİ BULMACA ATÖLYEN",
                    color = Color(0xFFE5FFB2), fontSize = 11.sp,
                    fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text("Tasarla, çöz,\nyeniden keşfet.",
                    color = Color.White, fontSize = 24.sp,
                    lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("Kelimelerinden özgün bulmacalar oluştur.",
                    color = Color(0xFFE1FAED), fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    val p = RoundedCornerShape(12.dp)
                    Column(Modifier.clip(p).background(Color(0x42FFFFFF))
                        .padding(horizontal=12.dp,vertical=7.dp)) {
                        Text(total.toString(),color=Color.White,fontWeight=FontWeight.Black)
                        Text("Kaydedilen",color=Color(0xFFF5FFF9),fontSize=10.sp)
                    }
                    Column(Modifier.clip(p).background(Color(0x42FFFFFF))
                        .padding(horizontal=12.dp,vertical=7.dp)) {
                        Text(completed.toString(),color=Color.White,fontWeight=FontWeight.Black)
                        Text("Tamamlanan",color=Color(0xFFF5FFF9),fontSize=10.sp)
                    }
                }
            }
            Wq175GlossIcon("puzzle",Modifier.size(112.dp))
        }
    }
}
