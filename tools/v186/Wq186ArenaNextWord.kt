package com.kelimebulmacasi.a1

/**
 * Choose an actionable crossword clue, never a failed/passed clue that
 * is waiting for the authoritative replay phase. No Firebase mutation.
 *
 * Eligibility must be supplied by PremiumOnlineLimits.canAttempt from the
 * current server snapshot; this helper only picks from that approved set.
 */
internal object Wq186ArenaNextWord {
    fun next(
        roomCode: String, playerUid: String,
        size: Int, solved: Set<Int>, current: Int,
        canAttempt: (Int) -> Boolean
    ): Int? {
        if (size <= 0) return null
        val legal = (0 until size).filter { index ->
            index != current && index !in solved && canAttempt(index)
        }.toSet()
        if (legal.isEmpty()) return null
        val unavailable = (0 until size).filterNot { it in legal }.toSet()
        return Wq179ArenaOrder.next(roomCode, playerUid,
            size, unavailable, current).takeIf { it in legal }
    }
}
