package com.kelimebulmacasi.a1

import android.content.Context
import android.os.SystemClock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Uses a monotonic clock and a wall-clock fallback after device reboot.
 * Backgrounding the app does not pause the timer; old progress is not changed. */
internal data class Wq175TimedRound(
    val elapsedStartMs: Long, val wallStartMs: Long, val durationMs: Long
)
internal object Wq175TimedGames {
    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(
        "wordquest_timed_crossword_v175", Context.MODE_PRIVATE
    )
    private fun key(id: String) = "round_" + stableHash(id).take(30)
    fun start(context: Context, gameId: String, minutes: Int) {
        require(minutes in listOf(3, 5, 10))
        val duration = minutes * 60_000L
        val raw = SystemClock.elapsedRealtime().toString() + ":" +
            System.currentTimeMillis().toString() + ":" + duration.toString()
        prefs(context).edit().putString(key(gameId), raw).apply()
    }
    fun read(context: Context, gameId: String): Wq175TimedRound? {
        val raw = prefs(context).getString(key(gameId), null) ?: return null
        val numbers = raw.split(':').mapNotNull { it.toLongOrNull() }
        if (numbers.size != 3 || numbers[2] !in 1L..3_600_000L) return null
        return Wq175TimedRound(numbers[0], numbers[1], numbers[2])
    }
    fun remainingMs(round: Wq175TimedRound,
        elapsedNow: Long = SystemClock.elapsedRealtime(),
        wallNow: Long = System.currentTimeMillis()): Long {
        val elapsed = if (elapsedNow >= round.elapsedStartMs)
            elapsedNow - round.elapsedStartMs
        else (wallNow - round.wallStartMs).coerceAtLeast(0L)
        return (round.durationMs - elapsed).coerceAtLeast(0L)
    }
    fun clear(context: Context, gameId: String) {
        prefs(context).edit().remove(key(gameId)).apply()
    }
}

@Composable
internal fun Wq175GameModeSelector(timed: Boolean, onTimed: (Boolean) -> Unit,
    minutes: Int, onMinutes: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Oyun Biçimi", color = Color(0xFF173F34), fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            listOf(false to "Klasik", true to "Süreye Karşı").forEach { (enabled, label) ->
                Surface(onClick = { onTimed(enabled) },
                    modifier = Modifier.weight(1f).heightIn(min = 58.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = if (timed == enabled) Color(0xFF205E52) else Color(0xFFF8FCF8),
                    border = BorderStroke(1.dp, if (timed == enabled)
                        Color(0xFF205E52) else Color(0xFFD7E5DC))) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(label, color = if (timed == enabled) Color.White
                            else Color(0xFF244D42), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (timed) {
            Text("Süre dolduğunda bulmaca sona erer. Süre arka planda da işler.",
                color = Color(0xFF52776A), fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 10).forEach { n ->
                    FilterChip(selected = minutes == n, onClick = { onMinutes(n) },
                        label = { Text(n.toString() + " dakika") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE0F2D2)))
                }
            }
        }
    }
}

@Composable
internal fun Wq175CountdownBadge(remaining: Long) {
    val seconds = ((remaining + 999L) / 1000L).coerceAtLeast(0L)
    val urgent = seconds <= 30
    val background = if (urgent) Color(0xFFFFE8E5) else Color(0xFFDBF5F0)
    val foreground = if (urgent) Color(0xFFA42D37) else Color(0xFF0E665E)
    Row(Modifier.fillMaxWidth().background(background, RoundedCornerShape(18.dp))
        .padding(horizontal = 17.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(if (urgent) "Son saniyeler" else "Süreye Karşı",
            color = foreground, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("%02d:%02d".format(seconds / 60, seconds % 60),
            color = foreground, fontSize = 25.sp, fontWeight = FontWeight.Black)
    }
}
