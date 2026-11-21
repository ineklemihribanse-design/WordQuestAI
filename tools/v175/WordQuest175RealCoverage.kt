package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Show measured word availability, not arbitrary '2000/2000' badges.
 * The displayed numbers are de-duplicated puzzle-ready words in the active catalog.
 * They are not an independent CEFR certification.
 */
@Composable
internal fun Wq175RealVocabularyCoverage(session: AppSession, level: String) {
    var available by remember(level, session.profileKey) { mutableStateOf<Int?>(null) }
    var loadError by remember(level, session.profileKey) { mutableStateOf(false) }
    LaunchedEffect(level, session.profileKey) {
        available = null
        loadError = false
        runCatching {
            withContext(Dispatchers.Default) {
                session.vocabulary().pool(level)
                    .map { it.letters }
                    .distinct()
                    .size
            }
        }.onSuccess { available = it }.onFailure { loadError = true }
    }
    val shape = RoundedCornerShape(19.dp)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape, color = Color(0xFFF0FCFA),
        border = BorderStroke(1.dp, Color(0xFFBFE3DD))
    ) {
        Column(Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Wq175GlossIcon("packages",Modifier.size(56.dp))
                Column(Modifier.weight(1f),
                    verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text("$level kelime kütüphanesi",
                        color=Color(0xFF145D5A),
                        fontWeight=FontWeight.ExtraBold,fontSize=16.sp)
                    Text("Güncel kelime havuzun",fontSize=11.sp,
                        color=Color(0xFF5A827D))
                }
            }
            when {
                loadError -> Text("Kelime havuzu okunamadı. Paketleri yeniden aç.",
                    color = Color(0xFFAB4A4A), fontSize = 12.sp)
                available == null -> LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
                else -> {
                    val actual = available ?: 0
                    val remaining = (2000 - actual).coerceAtLeast(0)
                    val percent = (actual / 2000f).coerceIn(0f,1f)
                    Text("$actual farklı kelime · hedef: 2.000",
                        color = Color(0xFF1D665E), fontWeight = FontWeight.Bold)
                    LinearProgressIndicator(
                        progress = { percent },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Color(0xFF269589), trackColor = Color(0xFFD4EAE6)
                    )
                    Text(if(remaining>0) "Hedef için $remaining kelime daha gerekiyor."
                         else "Sayısal hedef karşılandı; dil seviyesi ve anlam kalite incelemesi sürüyor.",
                        color = Color(0xFF506D69), fontSize = 12.sp)
                    Text("Sayılar uygulamanın gerçek kelime havuzundan alınır; resmi CEFR onayı değildir.",
                        color = Color(0xFF728681), fontSize = 11.sp)
                }
            }
        }
    }
}
