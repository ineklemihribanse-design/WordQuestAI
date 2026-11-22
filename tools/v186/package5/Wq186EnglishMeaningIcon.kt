package com.kelimebulmacasi.a1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Native, scalable illustration of the approved Book ↔ Meaning / lightbulb
 * concept. Compose draws it at any screen density without external fonts.
 */
@Composable
internal fun Wq186EnglishMeaningIcon(modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(
            Brush.linearGradient(listOf(Color(0xFFF1E7FF),Color(0xFFBDF5DF)))
        ),contentAlignment=Alignment.Center
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal=5.dp),
            verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(2.dp)
        ) {
            Box(
                Modifier.weight(1f).height(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFFC18DEA)),
                contentAlignment=Alignment.Center
            ) { Text("Aa",fontSize=16.sp,fontWeight=FontWeight.Bold,color=Color.White) }
            Box(
                Modifier.weight(1f).height(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFF40A987)),
                contentAlignment=Alignment.Center
            ) { Text("✦",fontSize=21.sp,fontWeight=FontWeight.Black,color=Color(0xFFFFDB64)) }
        }
        Text("↔",modifier=Modifier.align(Alignment.BottomCenter),
            color=Color(0xFF8D4AB4),fontWeight=FontWeight.Black,fontSize=18.sp)
    }
}
