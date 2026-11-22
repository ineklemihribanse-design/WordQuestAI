package com.kelimebulmacasi.a1

import kotlin.random.Random

/**
 * Paket 6: deterministic competition rules shared by both devices.
 * No player-controlled word index is accepted by this object.
 */
internal object Wq186ArenaFairPlay {
    private val levels = mapOf("A1" to 1, "A2" to 2, "B1" to 3, "B2" to 4, "C1" to 5, "C2" to 6)

    fun wordPoints(length: Int, level: String): Int {
        val tier = levels[level] ?: 1
        return length.coerceIn(2, 35) * 2 + tier * 4
    }

    fun totalScore(
        solvedBy: Map<Int, String>, playerUid: String,
        wordLength: (Int) -> Int, wordLevel: (Int) -> String,
        mistakes: Map<Int, Map<String, Int>>
    ): Int = solvedBy.entries.sumOf { (index, winner) ->
        if (winner == playerUid) wordPoints(wordLength(index), wordLevel(index)) else 0
    } - mistakes.values.sumOf { it[playerUid] ?: 0 } * 3

    /** Stable pseudo-random permutation, independently reproduced on both devices. */
    fun order(room: String, count: Int): List<Int> {
        if (count <= 0) return emptyList()
        return (0 until count).shuffled(Random(("p6|"+room).hashCode().toLong()))
    }

    /** Failed words are deferred to the last round, never deliberately selected early. */
    fun nextWord(
        room: String, count: Int, solved: Set<Int>, deferred: Set<Int>,
        current: Int, finalRound: Boolean
    ): Int? {
        val candidates = order(room, count).filter { it !in solved && (finalRound || it !in deferred) }
        if (candidates.isEmpty()) return null
        val at = candidates.indexOf(current)
        return candidates[if (at < 0) 0 else (at+1) % candidates.size]
    }

    fun finalRoundNeeded(count: Int, solved: Set<Int>, deferred: Set<Int>): Boolean =
        (0 until count).none { it !in solved && it !in deferred } &&
            deferred.any { it !in solved }

    fun hintPosition(room: String, word: Int, used: Set<Int>, pattern: String): Int? {
        val candidates = pattern.indices.filter { pattern[it] == '.' && it !in used }
        if (candidates.isEmpty()) return null
        val random = Random(("p6|hint|"+room+"|"+word+"|"+used.sorted().joinToString()).hashCode().toLong())
        return candidates[random.nextInt(candidates.size)]
    }

    fun canSkip(used: Int, limit: Int = 2): Boolean = limit in 0..5 && used in 0 until limit
    fun canRetry(used: Int, limit: Int): Boolean = limit in 0..5 && used in 0 until limit
    fun hintsLeft(used: Int, limit: Int): Int = (limit.coerceIn(0, 20) - used.coerceAtLeast(0)).coerceAtLeast(0)

    val allowedEmojis = setOf("👏", "🔥", "🎉", "😄", "👍", "💪", "🤝", "🏆")
    fun validEmoji(value: String) = value in allowedEmojis
    const val DISCONNECT_GRACE_MS = 90_000L
}
