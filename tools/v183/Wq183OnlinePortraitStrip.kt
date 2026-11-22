package com.kelimebulmacasi.a1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Room-scoped, lightweight two-player portraits used in the online match
 * header and automatically kept on the finished/celebration screen.
 *
 * Uses the existing Wq2DPortrait image pipeline, which renders the bounded
 * opponent preview if present and falls back to Piko without blocking play.
 * A compact single row keeps all five quiz options below fully scrollable.
 */
@Composable
internal fun Wq183OnlinePortraitStrip(
    hostAvatar: String?,
    guestAvatar: String?,
    hostName: String,
    guestName: String,
    hostScore: Int,
    guestScore: Int,
    mode: String,
    isHost: Boolean
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF18362F))
            .border(1.dp, Color(0xFF526F65), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Wq183PlayerPortrait(hostAvatar, hostName, hostScore, isHost, Modifier.weight(1f))
        Text(
            if (mode == "team") "+" else "VS",
            color = Color(0xFFE7C582),
            fontWeight = FontWeight.Black,
            fontSize = 11.sp
        )
        Wq183PlayerPortrait(guestAvatar, guestName, guestScore, !isHost, Modifier.weight(1f))
    }
}

@Composable
private fun Wq183PlayerPortrait(
    avatar: String?,
    name: String,
    score: Int,
    isMe: Boolean,
    modifier: Modifier = Modifier
) {
    val safeId = if (Wq2DMascots.valid(avatar)) avatar!! else "piko"
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Wq2DPortrait(safeId, Modifier.size(42.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name.ifBlank { "Oyuncu" },
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                (if (isMe) "Sen · " else "") + "★ $score",
                color = Color(0xFFE7C582),
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}
