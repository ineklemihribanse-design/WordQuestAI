package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Compact, editable form card. A saved choice takes just a heading and one summary line. */
@Composable
internal fun Wq186GameSettingsCard(
    heading: String,
    summary: String,
    expanded: Boolean,
    toggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFFFFCF7)),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(heading, color = Ink, fontWeight = FontWeight.Bold, fontFamily = Sans, fontSize = 16.sp)
                    Text(summary, color = Color(0xFF34574E), fontFamily = Sans, fontSize = 12.sp)
                }
                OutlinedButton(onClick = toggle, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(if (expanded) "Kapat" else "✎ Düzenle", fontSize = 12.sp, color = Ink)
                }
            }
            if (expanded) content()
        }
    }
}

/** Actual two-column button cards rather than scrollable FilterChip rows. */
@Composable
internal fun Wq186OptionTiles(
    options: List<String>,
    selected: Set<String>,
    enabled: Boolean,
    onChoose: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (pair in options.chunked(2)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (option in pair) {
                    val chosen = option in selected
                    OutlinedCard(
                        modifier = Modifier.weight(1f)
                            .clickable(enabled = enabled) { onChoose(option) },
                        shape = RoundedCornerShape(13.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (chosen) Color(0xFFE3F3DB) else Color.White,
                        ),
                        border = BorderStroke(if (chosen) 2.dp else 1.dp,
                            if (chosen) Color(0xFF39835C) else Color(0xFFB7C9C5)),
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(option, modifier = Modifier.weight(1f), fontFamily = Sans,
                                fontSize = 13.sp, color = Ink, fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium)
                            if (chosen) Text("✓", color = Color(0xFF246B46), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * The user's daily target stays small after configuration. Editing opens the
 * existing v175 banner, preserving its seven-day progress and saved goal.
 */
@Composable
internal fun Wq186CollapsibleDailyPlan(
    minutes: Int,
    completedMinutes: Int,
    remainingSeconds: Long,
    configured: Boolean,
    editor: @Composable () -> Unit,
) {
    var expanded by rememberSaveable(configured) { mutableStateOf(!configured) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFFFFCF7)),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Günlük çalışma hedefi", fontFamily = Sans,
                        fontWeight = FontWeight.Bold, color = Ink, fontSize = 16.sp)
                    Text("$minutes dk hedef · $completedMinutes dk tamamlandı",
                        fontFamily = Sans, color = Color(0xFF34574E), fontSize = 12.sp)
                    if (remainingSeconds > 0L) Text(
                        "Kalan ${remainingSeconds / 60} dk ${remainingSeconds % 60} sn",
                        fontFamily = Sans, color = Color(0xFF34574E), fontSize = 12.sp,
                    )
                }
                if (remainingSeconds == 0L) {
                    Text("✓", fontSize = 43.sp, fontWeight = FontWeight.Black,
                        color = Color(0xFF247447))
                }
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 5.dp),
                ) {
                    Text(if (expanded) "Kapat" else "✎ Düzenle",
                        fontFamily = Sans, color = Ink, fontSize = 12.sp)
                }
            }
        }
        if (expanded) {
            editor() // Existing v175 banner preserves all goal and streak behaviours.
            OutlinedButton(onClick = { expanded = false },
                modifier = Modifier.fillMaxWidth()) {
                Text("Ayarları tamamla", fontFamily = Sans)
            }
        }
    }
}
