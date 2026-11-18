package com.kelimebulmacasi.a1

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class GameMode(val title: String) {
    Topics("Konuya Göre"),
    Kinds("Kelime Türüne Göre"),
    Yds("YDS Hazırlık"),
    Generated("Yeni Oyun"),
    Custom("Bulmacalarım"),
    Demo("İlk deneme"),
}

data class Destination(val mode: GameMode, val level: String, val category: String) {
    val path get() = if (mode == GameMode.Yds) "YDS · $level · $category" else "$level · $category"
}

class Game(
    val id: String,
    val mode: GameMode,
    val level: String,
    val category: String,
    val title: String,
    val wordCount: Int,
    val mixed: Boolean = false,
    val destination: Destination? = null,
    val progressId: String = id,
    val modifiedAt: Long = 0,
    loadPuzzle: () -> Puzzle,
) {
    val puzzle by lazy(loadPuzzle)

    constructor(
        id: String, mode: GameMode, level: String, category: String, title: String, puzzle: Puzzle,
        destination: Destination? = null,
        progressId: String = id, modifiedAt: Long = 0,
    ) : this(
        id, mode, level, category, title, puzzle.words.size, destination = destination,
        progressId = progressId, modifiedAt = modifiedAt, loadPuzzle = { puzzle },
    )

    val path get() = when (mode) {
        GameMode.Yds -> "YDS · $level · $category"
        GameMode.Custom -> destination?.path ?: "Bulmacalarım"
        GameMode.Generated -> destination?.path ?: level
        GameMode.Demo -> "A1 · İlk deneme"
        else -> "$level · $category"
    }
}

val originalGame = Game(
    "a1_food_v1", GameMode.Demo, "A1", "Yiyecek ve İçecek", "İlk deneme", foodPuzzle,
)

class Catalog(context: Context) {
    val games = listOf("puzzles.json", "expanded-puzzles.json").flatMap { name ->
        context.assets.open(name).bufferedReader(Charsets.UTF_8).use { reader ->
            val data = JSONArray(reader.readText())
            List(data.length()) { index -> parseGame(data.getJSONObject(index)) }
        }
    }
    val byId = games.associateBy(Game::id)
    internal val vocabulary by lazy {
        VocabularyBank(games, context.assets.open("vocabulary-extra.tsv").bufferedReader().use { it.readLines() })
    }

    private fun parseGame(item: JSONObject): Game {
        val words = item.getJSONArray("words")
        val mode = when (item.getString("mode")) {
            "topic" -> GameMode.Topics
            "kind" -> GameMode.Kinds
            "yds" -> GameMode.Yds
            else -> error("Bilinmeyen oyun modu")
        }
        val level = item.getString("level")
        val category = item.getString("category")
        val topic = if (mode == GameMode.Yds && category == GameMode.Yds.title) when (level) {
            "Kolay" -> "Akademik Kelimeler"
            "Orta" -> "Bağlam ve Bağlaçlar"
            "Zor" -> "İleri Akademik İngilizce"
            else -> category
        } else category
        return Game(
            item.getString("id"), mode, level, topic,
            item.getString("title"), words.length(), item.optBoolean("mixed"),
        ) { parsePuzzle(words) }
    }

    private fun parsePuzzle(words: JSONArray): Puzzle {
        val entries = List(words.length()) { index ->
            val data = words.getJSONArray(index)
            Entry(data.getString(0), data.getString(1), data.optString(2))
        }
        val placements = List(words.length()) { index ->
            val data = words.getJSONArray(index)
            Placement(
                Cell(data.getInt(3), data.getInt(4)),
                if (data.getString(5) == "A") Direction.Across else Direction.Down,
            )
        }
        return Puzzle.create(entries, placements)
    }
}
