package com.kelimebulmacasi.a1

import java.util.UUID
import kotlin.random.Random

internal val englishLevels = listOf("A1", "A2", "B1", "B2", "C1", "C2")
internal val ydsLevels = listOf("Kolay", "Orta", "Zor")
internal const val MIN_NEW_GAME_WORDS = 8

internal data class WordSense(val entry: Entry, val level: String, val topic: String?, val kind: String?)

internal class VocabularyBank(games: List<Game>, extra: List<String>) {
    private val levelBanks = buildList {
        for (game in games) {
            if (game.mode != GameMode.Topics && game.mode != GameMode.Kinds) continue
            for (word in game.puzzle.words) add(WordSense(
                word.entry, game.level, game.category.takeIf { game.mode == GameMode.Topics },
                game.category.takeIf { game.mode == GameMode.Kinds },
            ))
        }
        for (line in extra) {
            if (line.isBlank() || line.startsWith('#')) continue
            val fields = line.split('\t')
            require(fields.size == 5 && fields[0] in englishLevels)
            val answer = fields[3].trim().uppercase()
            require(isEnglishAnswer(answer) && fields[4].isNotBlank())
            add(WordSense(Entry(answer, fields[4]), fields[0], fields[1], fields[2]))
        }
    }.distinct().groupBy(WordSense::level)

    private val ydsBanks = games.filter { it.mode == GameMode.Yds }.groupBy(Game::level).mapValues { (_, byLevel) ->
        byLevel.flatMap { game -> game.puzzle.words.map { WordSense(it.entry, game.level, game.category, null) } }.distinct()
    }

    private fun senses(level: String) = levelBanks[level] ?: ydsBanks[level].orEmpty()

    fun categories(level: String, mode: GameMode): List<String> = senses(level).mapNotNull {
        when (mode) {
            GameMode.Topics, GameMode.Yds -> it.topic
            GameMode.Kinds -> it.kind
            else -> null
        }
    }.distinct().sorted()

    fun pool(level: String, topic: String? = null, kind: String? = null): List<Entry> {
        val levelSenses = senses(level)
        val matchingKind = kind?.let { value -> levelSenses.filter { it.kind == value }.map { it.entry.letters }.toSet() }
        return levelSenses.filter {
            (topic == null || topic == it.topic) && (kind == null || kind == it.kind ||
                (topic != null && it.kind == null && it.entry.letters in matchingKind.orEmpty()))
        }
            .map(WordSense::entry).filter { it.letters.length in 2..22 && it.clue.isNotBlank() }
            .distinctBy(Entry::letters)
    }
}

internal suspend fun generateGame(
    pool: List<Entry>, size: Int, level: String, topic: String?, kind: String?,
    previous: Set<String> = emptySet(), random: Random = Random.Default,
): Game? {
    val candidates = pool.distinctBy(Entry::letters)
    require(size in MIN_NEW_GAME_WORDS..MAX_BOARD_WORDS && candidates.size >= size)
    repeat(8) { attempt ->
        val shuffled = candidates.shuffled(random)
        val entries = (if (attempt == 0) shuffled.sortedBy { it.letters in previous } else shuffled).take(size)
        val puzzle = customPuzzle(entries, random.nextInt()) ?: return@repeat
        val label = listOfNotNull(topic, kind).joinToString(" · ").ifBlank { "Karışık" }
        val destination = when {
            level in ydsLevels -> Destination(GameMode.Yds, level, label)
            topic != null -> Destination(GameMode.Topics, level, topic)
            kind != null -> Destination(GameMode.Kinds, level, kind)
            else -> null
        }
        val title = if (label == "Karışık") "$level · Karışık" else label
        return Game("round_${UUID.randomUUID()}", GameMode.Generated, level, label, title, puzzle, destination)
    }
    return null
}
