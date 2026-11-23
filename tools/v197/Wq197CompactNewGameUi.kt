package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The same new-game banner content in a compact height to make room for
 * all four selections without shrinking the crossword or changing state.
 */
@Composable
internal fun Wq197CompactHero(level: String, profile: String, available: Int) {
    val yds = level in ydsLevels
    Row(
        Modifier.fillMaxWidth().height(88.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(Brush.horizontalGradient(listOf(
                Color(0xFF164D43), Color(0xFF388872), Color(0xFF83B695)
            ))).padding(horizontal = 13.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("KELİME ATÖLYESİ", fontSize = 9.sp,
                fontWeight = FontWeight.Bold, color = Color(0xFFF0FAB8))
            Text("Kendi bulmacanı oluştur!", fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text((if (yds) "YDS " else "CEFR ") + level +
                " · " + available + " kelime hazır",
                fontSize = 11.sp, color = Color(0xFFF4FFF3), maxLines = 1)
        }
        Wq2DPortrait(
            Wq2DMascots.selected(androidx.compose.ui.platform.LocalContext.current, profile),
            Modifier.size(69.dp)
        )
    }
}

/** All selection pills reflow: CEFR is six in a row on tablets, three per
 * row on phones; no sideways scrolling, 40dp visual height, touch target kept.
 */
@Composable
internal fun Wq197OptionGrid(
    options: List<String>,
    selected: Set<String>,
    enabled: Boolean,
    choose: (String) -> Unit,
) {
    if (options.isEmpty()) return
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val longest = options.maxOfOrNull { it.length } ?: 0
        val columns = Wq197LayoutPolicy.columns(maxWidth.value, options.size, longest)
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            options.chunked(columns).forEach { group ->
                Row(Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    group.forEach { name ->
                        val active = name in selected
                        OutlinedButton(
                            onClick = { choose(name) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                            shape = RoundedCornerShape(22.dp),
                            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (active) LightLime else White,
                                contentColor = Forest,
                            ),
                            border = BorderStroke(1.dp,
                                if (active) Color(0xFFC3D89C) else Color(0xFFD7DED5)),
                        ) {
                            Text(
                                (if (active) "✓ " else "") + name,
                                fontFamily = Sans,
                                fontSize = if (columns >= 5) 11.sp else 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                lineHeight = 14.sp,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    repeat(columns - group.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** CEFR and YDS stay multi-select; a small switch chooses which group to
 * display, so large blank YDS rows never push the rest off the first screen.
 */
@Composable
internal fun Wq197LevelSelector(
    levels: List<String>,
    enabled: Boolean,
    chooseLevel: (String) -> Unit,
) {
    val yds = levels.firstOrNull() in ydsLevels
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Wq196LevelBadge(Modifier.size(47.dp))
                Column(Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text("Seviye", fontFamily = Serif, color = Ink,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Birden fazla CEFR veya YDS seviyesi seçebilirsin",
                        fontFamily = Sans, fontSize = 11.sp, lineHeight = 14.sp,
                        color = Forest, maxLines = 2)
                }
            }
            Wq197OptionGrid(
                listOf("CEFR", "YDS"), setOf(if (yds) "YDS" else "CEFR"), enabled,
            ) { label ->
                if ((label == "YDS") != yds) {
                    chooseLevel(if (label == "YDS") ydsLevels.first() else "A1")
                }
            }
            if (!yds) {
                Wq197OptionGrid(englishLevels, levels.toSet(), enabled, chooseLevel)
            } else {
                Wq197OptionGrid(
                    ydsLevels.map { "YDS $it" },
                    levels.map { "YDS $it" }.toSet(), enabled,
                ) { label -> chooseLevel(label.removePrefix("YDS ")) }
            }
        }
    }
}

/** Remaining topic/kind choices open over the page rather than stretching
 * it for dozens of rows. Multiple selections remain possible in the dialog.
 */
@Composable
internal fun Wq197FilterDialog(
    title: String,
    options: List<String>,
    selected: Set<String>,
    enabled: Boolean,
    choose: (String) -> Unit,
    dismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(title, fontFamily = Serif, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Wq197OptionGrid(options, selected, enabled, choose)
            }
        },
        confirmButton = {
            TextButton(onClick = dismiss) { Text("Tamam", color = Forest) }
        }
    )
}
