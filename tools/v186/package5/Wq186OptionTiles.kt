package com.kelimebulmacasi.a1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Lightweight accessible tiles without an experimental FlowRow dependency. */
@Composable
internal fun Wq186OptionTiles(
    options: List<String>,
    selected: Set<String>,
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    if (options.isEmpty()) return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { option ->
                    val active = option in selected
                    Surface(
                        onClick = { onSelect(option) },
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        color = if (active) Color(0xFFE6D8FC) else Color.White,
                        contentColor = Color(0xFF245042),
                        shape = RoundedCornerShape(13.dp),
                        border = BorderStroke(
                            if (active) 2.dp else 1.dp,
                            if (active) Color(0xFF428D80) else Color(0xFFD4DFD9)
                        )
                    ) {
                        Text(
                            option,
                            modifier = Modifier.padding(
                                PaddingValues(horizontal = 5.dp, vertical = 11.dp)
                            ),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                repeat(3 - row.size) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color.Transparent
                    ) { Text("") }
                }
            }
        }
    }
}
