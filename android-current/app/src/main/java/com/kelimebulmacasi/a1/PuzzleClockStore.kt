package com.kelimebulmacasi.a1

import android.content.Context

/** Private per-profile timer metadata; no player progress or game records are modified. */
internal enum class PuzzleClockMode { Classic, Countdown, Stopwatch }

internal data class PuzzleClock(
    val mode: PuzzleClockMode,
    val startedAtMillis: Long,
    val minutes: Int,
) {
    fun elapsedSeconds(now: Long): Long = ((now - startedAtMillis).coerceAtLeast(0L) / 1000L)
    fun remainingSeconds(now: Long): Long =
        if (mode != PuzzleClockMode.Countdown) Long.MAX_VALUE
        else (minutes * 60L - elapsedSeconds(now)).coerceAtLeast(0L)
    fun expired(now: Long): Boolean = mode == PuzzleClockMode.Countdown && remainingSeconds(now) == 0L
}

internal class PuzzleClockStore(context: Context, profileKey: String) {
    private val store = context.applicationContext.getSharedPreferences(
        "wq186_game_clock_" + stableHash(profileKey).take(24), Context.MODE_PRIVATE,
    )

    fun save(id: String, mode: PuzzleClockMode, minutes: Int, now: Long = System.currentTimeMillis()) {
        require(minutes in 1..180)
        val key = stableHash(id).take(24)
        store.edit()
            .putString("$key.mode", mode.name)
            .putInt("$key.minutes", minutes)
            .putLong("$key.started", now)
            .apply()
    }

    fun load(id: String): PuzzleClock? {
        val key = stableHash(id).take(24)
        val mode = PuzzleClockMode.entries.firstOrNull {
            it.name == store.getString("$key.mode", null)
        } ?: return null
        val minutes = store.getInt("$key.minutes", 0)
        val started = store.getLong("$key.started", 0L)
        if (minutes !in 1..180 || started <= 0) return null
        return PuzzleClock(mode, started, minutes)
    }
}
