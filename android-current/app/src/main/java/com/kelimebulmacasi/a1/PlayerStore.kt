package com.kelimebulmacasi.a1

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

internal fun Progress.toJson(): JSONObject = JSONObject()
    .put("grid", grid)
    .put("hinted", JSONArray(hinted.sorted()))
    .put("awards", JSONObject().apply {
        awards.forEach { (index, award) -> put(index.toString(), JSONArray(listOf(award.points, award.earnedAt))) }
    })
    .put("lockedWords", JSONArray(lockedWords.sorted()))
    .put("completed", completed)
    .put("updatedAt", updatedAt)
    .put("resetAt", resetAt)
    .apply {
        if (layoutAt > 0) put("layoutAt", layoutAt)
        if (layout.isNotEmpty()) put("layout", JSONArray(layout.map { place ->
            JSONArray(listOf(place.start.row, place.start.column, place.direction.name))
        }))
    }

internal fun JSONObject.toProgress(): Progress {
    fun indices(key: String): Set<Int> {
        val values = optJSONArray(key) ?: return emptySet()
        return (0 until values.length()).map { values.optInt(it, -1) }.filter { it >= 0 }.toSet()
    }
    val scores = optJSONObject("awards") ?: JSONObject()
    return Progress(
        grid = optString("grid"),
        hinted = indices("hinted"),
        awards = scores.keys().asSequence().mapNotNull { key ->
            val index = key.toIntOrNull() ?: return@mapNotNull null
            val data = scores.optJSONArray(key) ?: return@mapNotNull null
            val points = data.optInt(0)
            if (index >= 0 && points in setOf(5, 10)) index to Award(points, data.optLong(1)) else null
        }.toMap(),
        lockedWords = indices("lockedWords"),
        completed = optBoolean("completed"),
        updatedAt = optLong("updatedAt"),
        resetAt = optLong("resetAt"),
        layout = optJSONArray("layout")?.takeIf { it.length() in 6..MAX_BOARD_WORDS }?.let { list ->
            runCatching {
                List(list.length()) { index ->
                    val item = list.getJSONArray(index)
                    val row = item.getInt(0)
                    val column = item.getInt(1)
                    require(row in 0..MAX_BOARD_SIDE && column in 0..MAX_BOARD_SIDE)
                    Placement(Cell(row, column), Direction.valueOf(item.getString(2)))
                }
            }.getOrDefault(emptyList())
        }.orEmpty(),
        layoutAt = optLong("layoutAt"),
    )
}

internal fun Game.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .apply {
        if (mode == GameMode.Generated) {
            put("mode", mode.name)
            put("level", level)
            put("category", category)
        }
        if (progressId != id) put("progressId", progressId)
        if (modifiedAt > 0) put("modifiedAt", modifiedAt)
        destination?.let {
            put("destination", JSONObject().put("mode", it.mode.name).put("level", it.level).put("category", it.category))
        }
    }
    .put("words", JSONArray().apply {
        for (word in puzzle.words) {
            put(JSONArray(listOf(
                word.entry.answer, word.entry.clue, word.entry.context, word.start.row,
                word.start.column, word.direction.name,
            )))
        }
    })

internal fun JSONObject.toCustomGame(): Game {
    val id = getString("id")
    val generated = id.startsWith("round_")
    val words = getJSONArray("words")
    require(words.length() in 6..(if (generated) MAX_BOARD_WORDS else 20))
    val data = (0 until words.length()).map { words.getJSONArray(it) }
    val entries = data.map { Entry(it.getString(0), it.getString(1), it.getString(2)) }
    val places = data.map {
        val row = it.getInt(3)
        val column = it.getInt(4)
        require(row in 0..MAX_BOARD_SIDE && column in 0..MAX_BOARD_SIDE)
        Placement(Cell(row, column), Direction.valueOf(it.getString(5)))
    }
    val destination = optJSONObject("destination")?.let {
        runCatching { Destination(GameMode.valueOf(it.getString("mode")), it.getString("level"), it.getString("category")) }
            .getOrNull()
    }
    return Game(
        id, if (generated) GameMode.Generated else GameMode.Custom,
        if (generated) getString("level") else "", if (generated) getString("category") else GameMode.Custom.title,
        getString("title"), Puzzle.create(entries, places), destination,
        progressId = optString("progressId", id), modifiedAt = optLong("modifiedAt"),
    )
}

class PlayerStore(context: Context, uid: String? = null, secondLocalProfile: Boolean = false) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        when {
            uid != null -> "player_v2_account_$uid"
            secondLocalProfile -> "player_v2_local_2"
            else -> "player_v2_guest"
        },
        Context.MODE_PRIVATE,
    )
    private var savedGames: List<Game>? = null

    init {
        if (uid == null && !secondLocalProfile && !preferences.getBoolean("imported_demo", false)) {
            val original = context.getSharedPreferences("crossword_progress", Context.MODE_PRIVATE)
                .getString("a1_food_v1", "").orEmpty()
            val editor = preferences.edit().putBoolean("imported_demo", true)
            if (original.any { it in 'A'..'Z' }) {
                val progress = Crossword(foodPuzzle, Progress(grid = original, updatedAt = 1)).progress()
                editor.putString("progress:a1_food_v1", progress.toJson().toString())
                editor.putString("last_game", originalGame.id)
            }
            editor.apply()
        }
    }

    var lastGame: String?
        get() = preferences.getString("last_game", null)
        set(value) { recordLastGame(value, System.currentTimeMillis()) }

    val lastGameAt: Long get() = preferences.getLong("last_game_at", 0)

    fun recordLastGame(id: String?, openedAt: Long) {
        if (openedAt >= lastGameAt) {
            preferences.edit().putString("last_game", id).putLong("last_game_at", openedAt).apply()
        }
    }

    fun find(id: String): Progress? = preferences.getString("progress:$id", null)
        ?.let { runCatching { JSONObject(it).toProgress() }.getOrNull() }

    fun read(id: String): Progress = find(id) ?: Progress()

    fun save(id: String, progress: Progress) {
        preferences.edit().putString("progress:$id", progress.toJson().toString()).apply()
    }

    fun progress(): Map<String, Progress> = preferences.all.keys.filter { it.startsWith("progress:") }
        .associate { key -> key.removePrefix("progress:").let { it to read(it) } }

    fun savedGames(): List<Game> = savedGames ?: preferences.all.keys.filter { it.startsWith("custom:") }
        .mapNotNull { key ->
            preferences.getString(key, null)?.let { runCatching { JSONObject(it).toCustomGame() }.getOrNull() }
        }.sortedWith(compareBy(Game::title, Game::id)).also { savedGames = it }

    fun saveCustom(games: List<Game>): Boolean {
        val editor = preferences.edit()
        val changed = writeNewerGames(editor, games)
        editor.apply()
        if (changed) savedGames = null
        return changed
    }

    fun importGuest(guest: PlayerStore) {
        if (preferences.getBoolean("imported_guest", false)) return
        val editor = preferences.edit().putBoolean("imported_guest", true)
        val previous = lastGame
        writeBackup(editor, PlayerBackup(guest.savedGames(), guest.progress(), guest.lastGame.takeIf { previous == null }, guest.lastGameAt))
    }

    internal fun importBackup(backup: PlayerBackup) = writeBackup(preferences.edit(), backup)

    private fun writeBackup(editor: SharedPreferences.Editor, backup: PlayerBackup) {
        writeNewerGames(editor, backup.games)
        val progress = backup.progress.mapValues { (id, record) -> read(id).merge(record) }
        progress.forEach { (id, record) -> editor.putString("progress:$id", record.toJson().toString()) }
        if (backup.lastGame != null && (lastGame == null || backup.lastGameAt > lastGameAt)) {
            editor.putString("last_game", backup.lastGame).putLong("last_game_at", backup.lastGameAt)
        }
        editor.apply()
        savedGames = null
    }

    private fun writeNewerGames(editor: SharedPreferences.Editor, games: List<Game>): Boolean {
        val existing = savedGames().associateBy(Game::id).toMutableMap()
        var changed = false
        for (game in games) {
            val current = existing[game.id]
            if (current != null && current.modifiedAt > game.modifiedAt) continue
            val json = game.toJson().toString()
            if (current != null && current.modifiedAt == game.modifiedAt && current.toJson().toString() >= json) continue
            editor.putString("custom:" + game.id, json)
            existing[game.id] = game
            changed = true
        }
        return changed
    }
}
