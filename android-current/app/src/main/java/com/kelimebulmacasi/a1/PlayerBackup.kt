package com.kelimebulmacasi.a1

import java.io.StringReader
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

internal data class PlayerBackup(
    val games: List<Game>,
    val progress: Map<String, Progress>,
    val lastGame: String?,
    val lastGameAt: Long,
    val learning: LearningArchive? = null,
    val yds: Map<String, Pair<Int, Int>> = emptyMap(),
    val daily: Map<String, DailyDay> = emptyMap(),
)

private const val BACKUP_FORMAT = "kelime-bulmacasi-progress"
private val gameKey = Regex("[A-Za-z0-9_:-]{1,100}")

internal fun decodeBackupFile(bytes: ByteArray): PlayerBackup {
    val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF").trimStart()
    return if (text.startsWith('<')) decodeOldPreferences(text) else decodeBackup(text)
}

private fun decodeOldPreferences(xml: String): PlayerBackup {
    require(!xml.contains("<!DOCTYPE", ignoreCase = true))
    val parser = XmlPullParserFactory.newInstance().newPullParser()
    parser.setInput(StringReader(xml))
    require(parser.nextTag() == XmlPullParser.START_TAG && parser.name == "map")
    val games = JSONArray()
    val progress = JSONObject()
    var lastGame: String? = null
    var lastGameAt = 0L
    var demo: String? = null
    var recognized = false
    while (parser.next() != XmlPullParser.END_DOCUMENT) {
        if (parser.eventType != XmlPullParser.START_TAG || parser.depth != 2) continue
        val name = parser.getAttributeValue(null, "name") ?: continue
        when (parser.name) {
            "string" -> {
                val value = parser.nextText()
                when {
                    name.startsWith("custom:") -> {
                        val game = JSONObject(value)
                        require(game.getString("id") == name.removePrefix("custom:"))
                        games.put(game)
                        recognized = true
                    }
                    name.startsWith("progress:") -> {
                        progress.put(name.removePrefix("progress:"), JSONObject(value))
                        recognized = true
                    }
                    name == "last_game" -> lastGame = value
                    name == originalGame.id -> demo = value
                }
            }
            "long" -> if (name == "last_game_at") lastGameAt = parser.getAttributeValue(null, "value")?.toLongOrNull() ?: 0
            "boolean" -> if (name == "imported_demo" || name == "imported_guest") recognized = true
        }
    }
    if (!recognized && demo != null) {
        require(demo.length == foodPuzzle.rows * foodPuzzle.columns && demo.all { it in 'A'..'Z' || it == '.' })
        if (demo.any { it in 'A'..'Z' }) {
            val record = Crossword(foodPuzzle, Progress(grid = demo, updatedAt = 1)).progress()
            progress.put(originalGame.id, record.toJson())
            lastGame = originalGame.id
        }
        recognized = true
    }
    require(recognized)
    return decodeBackup(
        JSONObject().put("format", BACKUP_FORMAT).put("version", 1).put("games", games)
            .put("progress", progress).put("lastGame", lastGame ?: JSONObject.NULL).put("lastGameAt", lastGameAt).toString(),
    )
}

internal fun PlayerBackup.encode(): String = JSONObject()
    .put("format", BACKUP_FORMAT)
    .put("version", 1)
    .put("games", JSONArray(games.map { it.toJson() }))
    .put("progress", JSONObject().apply { progress.forEach { (id, record) -> put(id, record.toJson()) } })
    .put("lastGame", lastGame ?: JSONObject.NULL)
    .put("lastGameAt", lastGameAt)
    .apply {
        learning?.let { put("learning", it.toJson()) }
        if (yds.isNotEmpty()) put("yds", JSONObject().apply {
            for ((id, counts) in yds) put(id, JSONArray().put(counts.first).put(counts.second))
        })
        if (daily.isNotEmpty()) put("daily", JSONObject().apply {
            daily.forEach { (date, day) ->
                put(date, JSONArray().put(day.goalMinutes).put(day.activeSeconds)
                    .put(JSONArray(day.completed.sorted())))
            }
        })
    }
    .toString()

internal fun decodeBackup(text: String): PlayerBackup {
    val data = JSONObject(text.removePrefix("\uFEFF"))
    require(data.getString("format") == BACKUP_FORMAT && data.getInt("version") == 1)
    val items = data.getJSONArray("games")
    val games = List(items.length()) { index ->
        val item = items.getJSONObject(index)
        val words = item.getJSONArray("words")
        val id = item.getString("id")
        require(id.startsWith("custom_") || id.startsWith("round_"))
        require(words.length() in 6..(if (id.startsWith("round_")) MAX_BOARD_WORDS else 20))
        repeat(words.length()) { word ->
            val row = words.getJSONArray(word)
            require(englishLetters(row.getString(0)).length in 2..22 &&
                row.getInt(3) in 0..MAX_BOARD_SIDE && row.getInt(4) in 0..MAX_BOARD_SIDE)
        }
        item.toCustomGame().also { require(gameKey.matches(it.id) && gameKey.matches(it.progressId)) }
    }
    val records = data.getJSONObject("progress")
    val progress = records.keys().asSequence().associateWith { id ->
        require(gameKey.matches(id))
        records.getJSONObject(id).toProgress().also {
            require(it.grid.length <= 4_000 && it.awards.keys.all { word -> word in 0 until MAX_BOARD_WORDS })
        }
    }
    val lastGame = if (data.isNull("lastGame")) null else data.optString("lastGame").takeIf(gameKey::matches)
    val ydsData = data.optJSONObject("yds")
    val allowedYdsIds = YdsContent.questions.map { it.id }.toSet()
    val yds = ydsData?.keys()?.asSequence()?.mapNotNull { id ->
        if (id !in allowedYdsIds) return@mapNotNull null
        val row = ydsData.optJSONArray(id) ?: return@mapNotNull null
        if (row.length() != 2) return@mapNotNull null
        val right = row.optInt(0, -1)
        val total = row.optInt(1, -1)
        if (total !in 0..100_000 || right !in 0..total) return@mapNotNull null
        id to (right to total)
    }?.toMap().orEmpty()
    val dailyObject = data.optJSONObject("daily")
    val daily = dailyObject?.keys()?.asSequence()?.take(367)?.mapNotNull { date ->
        if (runCatching { java.time.LocalDate.parse(date) }.isFailure) return@mapNotNull null
        val entry = dailyObject.optJSONArray(date) ?: return@mapNotNull null
        if (entry.length() != 3) return@mapNotNull null
        val goal = entry.optInt(0, -1)
        val seconds = entry.optLong(1, -1L)
        val completedArray = entry.optJSONArray(2) ?: return@mapNotNull null
        if (goal !in listOf(15, 30, 60) || seconds !in 0L..86_400L || completedArray.length() > 250)
            return@mapNotNull null
        val completed = (0 until completedArray.length()).map { completedArray.optString(it) }.toSet()
        if (completed.any { !Regex("[A-Z_]{1,30}:[a-f0-9]{12}").matches(it) }) return@mapNotNull null
        date to DailyDay(goal, seconds, completed)
    }?.toMap().orEmpty()
    return PlayerBackup(games, progress, lastGame, data.optLong("lastGameAt"),
        data.optJSONObject("learning")?.toLearningArchive(), yds, daily)
}
