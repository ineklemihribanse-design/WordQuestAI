package com.kelimebulmacasi.a1

/**
 * Local game routing. Solo modes never create a Firebase room or a pretend peer.
 * Team's solo variant reuses the offline crossword engine as a cooperative-board
 * practice session with only one human solver.
 */
internal object Wq190ArenaSoloPolicy {
    val modes: Set<String> = setOf("duel", "team", "quiz")
    val supportedLevels: Set<String> = setOf("A1", "A2", "B1", "B2", "C1", "C2")

    fun canPlaySolo(mode: String) = mode in modes
    fun localEngineMode(requested: String): String =
        when(requested) {
            "team" -> "duel"
            "duel", "quiz" -> requested
            else -> "duel"
        }
    fun displayName(requested: String, solo: Boolean): String =
        when(requested) {
            "team" -> if (solo) "Ortak Bulmaca · Tek kişilik pratik" else "Ortak Bulmaca"
            "quiz" -> if (solo) "5 Şıklı · Tek kişilik" else "5 Şıklı Düello"
            else -> if (solo) "Bulmaca · Tek kişilik" else "Bulmaca Düellosu"
        }
    fun levelPool(raw: List<String>, primary: String): List<String> =
        raw.filter { it in supportedLevels }.distinct().take(6).ifEmpty {
            listOf(primary.takeIf { it in supportedLevels } ?: "A1")
        }
    fun roundLevel(raw: List<String>, primary: String, roundSeed: String): String {
        val pool = levelPool(raw, primary)
        return pool[Math.floorMod(roundSeed.hashCode(), pool.size)]
    }
}
