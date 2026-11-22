package com.kelimebulmacasi.a1

/**
 * Shared Arena clue/hint guard for normal and replay turns.
 * UI hints only when the server snapshot says this particular clue is legal.
 * Real quota spending still occurs in the authoritative Firebase transaction.
 */
internal object Wq186ArenaHintPolicy {
    fun canUse(
        myTurn: Boolean, countdown: Int?,
        solved: Boolean, alreadyUsed: Boolean,
        pattern: String, attemptAllowed: Boolean, remaining: Int
    ): Boolean = myTurn &&
        (countdown == null || countdown > 0) &&
        !solved && !alreadyUsed && attemptAllowed &&
        remaining > 0 && pattern.contains('.')
}
