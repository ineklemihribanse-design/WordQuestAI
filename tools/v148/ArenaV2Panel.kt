package com.kelimebulmacasi.a1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Online-only surface, leaving all older offline/learning screens unchanged. */
@Composable
internal fun ArenaV2Panel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier=Modifier.fillMaxWidth()
            .background(Color.White,RoundedCornerShape(17.dp))
            .border(1.dp,Color(0xFFE4E1EF),RoundedCornerShape(17.dp))
            .padding(14.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp),
        content=content
    )
}
