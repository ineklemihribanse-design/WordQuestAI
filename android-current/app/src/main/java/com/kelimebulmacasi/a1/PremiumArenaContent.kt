package com.kelimebulmacasi.a1

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom

/** Curated offline A1–C2 pack. Older A1/A2 boards retain their stable IDs. */
internal data class ArenaLexeme(
    val answer: String, val meaning: String, val level: String, val category: String,
)

internal data class PremiumArenaBoard(val id: String, val level: String, val category: String, val puzzle: Puzzle)
internal data class PremiumQuestion(val prompt: String, val options: List<String>, val correct: Int, val answer: String)

internal object PremiumArenaRules {
    val allowedLevels = listOf("A1", "A1–A2", "A2", "B1", "B2", "C1", "C2")
    val allowedSizes = listOf(20, 30, 40, 50)
    val allowedTimes = listOf(0, 20, 30, 45)
    const val allTopics = "Tüm konular"

    fun matchesLevel(candidate: String, selected: String): Boolean = when (selected) {
        "A1" -> candidate == "A1"
        "A2" -> candidate == "A2"
        "A1–A2" -> candidate == "A1" || candidate == "A2" || candidate == "A1–A2"
        else -> candidate == selected
    }

    fun crosswordLevel(candidate: String, selected: String): Boolean = candidate == selected

    fun canUseTopic(candidate: String, requested: String): Boolean =
        requested == allTopics || candidate == requested

    /** Rotates unseen items first, but can reuse a pack if the finite pool has been exhausted. */
    fun <T> prioritizeUnseen(items: List<T>, recent: Set<String>, id: (T) -> String): List<T> =
        items.filterNot { id(it) in recent }.ifEmpty { items }
}

internal class PremiumArenaContent(private val context: Context) {
    private val random = kotlin.random.Random(SecureRandom().nextLong())
    private val prefs = context.getSharedPreferences("wordquest_premium_rotation", Context.MODE_PRIVATE)
    private val vocabulary: List<ArenaLexeme> by lazy {
        val data = JSONArray(context.assets.open("arena-vocabulary-v1.json").bufferedReader().use { it.readText() })
        List(data.length()) { index ->
            val e = data.getJSONObject(index)
            ArenaLexeme(e.getString("answer"), e.getString("meaning"), e.getString("level"), e.getString("category"))
        }
    }
    private val puzzles: JSONArray by lazy {
        JSONArray(context.assets.open("arena-crosswords-v1.json").bufferedReader().use { it.readText() })
    }

    val wordCount: Int get() = vocabulary.size
    val boardCount: Int get() = puzzles.length()
    fun availableSizes(level: String, topic: String = PremiumArenaRules.allTopics): List<Int> =
        PremiumArenaRules.allowedSizes.filter { count -> (0 until puzzles.length()).any { index ->
            val board = puzzles.getJSONObject(index)
            board.getString("level") == level && board.getJSONArray("words").length() == count &&
                PremiumArenaRules.canUseTopic(board.getString("category"), topic)
        } }

    fun topics(level: String, crossword: Boolean = false): List<String> =
        listOf(PremiumArenaRules.allTopics) + if (crossword) {
            (0 until puzzles.length()).map { puzzles.getJSONObject(it) }
                .filter { it.getString("level") == level }.map { it.getString("category") }.distinct().sorted()
        } else if (level == "A1–A2") emptyList() else
            vocabulary.filter { it.level == level }.map { it.category }.distinct().sorted()

    private fun history(key: String): List<String> = prefs.getString(key, "")
        .orEmpty().split('|').filter(String::isNotBlank)

    private fun record(key: String, ids: List<String>, cap: Int) {
        val combined = (ids + history(key)).distinct().take(cap)
        prefs.edit().putString(key, combined.joinToString("|")).apply()
    }

    fun nextPuzzle(level: String, size: Int, topic: String, profile: String): PremiumArenaBoard? {
        require(level in PremiumArenaRules.allowedLevels && size in PremiumArenaRules.allowedSizes)
        val eligible = (0 until puzzles.length()).map { puzzles.getJSONObject(it) }.filter { b ->
            b.getString("level") == level && b.getJSONArray("words").length() == size &&
                PremiumArenaRules.canUseTopic(b.getString("category"), topic)
        }
        if (eligible.isEmpty()) return null
        val key = "board.$profile.$level.$size.$topic"
        val available = PremiumArenaRules.prioritizeUnseen(eligible, history(key).toSet()) { it.getString("id") }
        val picked = available[random.nextInt(available.size)]
        val words = picked.getJSONArray("words")
        val entries = (0 until words.length()).map { i ->
            val row = words.getJSONArray(i)
            Entry(row.getString(0), row.getString(1), row.optString(2))
        }
        val placement = (0 until words.length()).map { i ->
            val row = words.getJSONArray(i)
            Placement(Cell(row.getInt(3), row.getInt(4)), if (row.getString(5) == "A") Direction.Across else Direction.Down)
        }
        val board = PremiumArenaBoard(picked.getString("id"), level, picked.getString("category"), Puzzle.create(entries, placement))
        record(key, listOf(board.id), 50)
        return board
    }

    /** Same seed and content pack produce the exact same five-option questions on both devices. */
    fun questionsForSeed(level: String, topic: String, seed: Int, size: Int = 12): List<PremiumQuestion> {
        if (level !in PremiumArenaRules.allowedLevels || size !in 1..30) return emptyList()
        val random = kotlin.random.Random(seed)
        val eligible = vocabulary.filter { PremiumArenaRules.matchesLevel(it.level, level) }
        val targets = eligible.filter { PremiumArenaRules.canUseTopic(it.category, topic) }
            .distinctBy { it.answer }.shuffled(random)
        return targets.mapNotNull { target ->
            val distractors = eligible.asSequence()
                .filter { it.answer != target.answer && it.meaning.trim().lowercase() != target.meaning.trim().lowercase() &&
                    it.meaning.substringBefore(',').trim().lowercase() != target.meaning.substringBefore(',').trim().lowercase() }
                .distinctBy { it.answer }.toList().shuffled(random).take(4)
            if (distractors.size != 4) null else {
                val choices = (distractors.map { it.answer } + target.answer).shuffled(random)
                PremiumQuestion("${target.meaning} — İngilizcesi hangisidir?", choices, choices.indexOf(target.answer), target.answer)
            }
        }.take(size)
    }

    /** Resolve the server-selected board ID without consuming the local recent-history rotation. */
    fun puzzleById(id: String): PremiumArenaBoard? {
        if (!Regex("[A-Za-z0-9_.:-]{1,100}").matches(id)) return null
        val picked = (0 until puzzles.length()).asSequence().map { puzzles.getJSONObject(it) }
            .firstOrNull { it.getString("id") == id } ?: return null
        val words = picked.getJSONArray("words")
        val entries = (0 until words.length()).map { index ->
            val row = words.getJSONArray(index)
            Entry(row.getString(0), row.getString(1), row.optString(2))
        }
        val placements = (0 until words.length()).map { index ->
            val row = words.getJSONArray(index)
            Placement(Cell(row.getInt(3), row.getInt(4)), if (row.getString(5) == "A") Direction.Across else Direction.Down)
        }
        return PremiumArenaBoard(id, picked.getString("level"), picked.getString("category"), Puzzle.create(entries, placements))
    }

    fun nextQuestions(level: String, topic: String, profile: String, size: Int = 12): List<PremiumQuestion> {
        require(level in PremiumArenaRules.allowedLevels && size in 1..30)
        val all = vocabulary.filter { PremiumArenaRules.matchesLevel(it.level, level) }
        val filtered = all.filter { PremiumArenaRules.canUseTopic(it.category, topic) }
        val key = "questions.$profile.$level.$topic"
        val unseen = PremiumArenaRules.prioritizeUnseen(filtered, history(key).toSet()) { it.level + ':' + it.answer }
        val result = mutableListOf<PremiumQuestion>()
        val selected = mutableSetOf<String>()
        for (target in unseen.shuffled(random)) {
            if (result.size == size) break
            if (!selected.add(target.answer)) continue
            val correctMeaning = target.meaning.trim().lowercase()
            val distractors = all.filter { it.answer != target.answer && it.answer !in selected &&
                it.meaning.trim().lowercase() != correctMeaning &&
                it.meaning.substringBefore(',').trim().lowercase() != target.meaning.substringBefore(',').trim().lowercase()
            }.distinctBy { it.answer }.shuffled(random).take(4)
            if (distractors.size != 4) continue
            val choices = (distractors.map { it.answer } + target.answer).shuffled(random)
            result.add(PremiumQuestion("${target.meaning} — İngilizcesi hangisidir?", choices, choices.indexOf(target.answer), target.answer))
        }
        record(key, result.map { it.answer }.map { word ->
            // A word can be listed at both levels; store entries from the selected pool.
            val found = filtered.firstOrNull { it.answer == word }
            (found?.level ?: level) + ':' + word
        }, 250)
        return result
    }
}
