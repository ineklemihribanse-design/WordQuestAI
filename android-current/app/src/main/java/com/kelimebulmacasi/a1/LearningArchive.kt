package com.kelimebulmacasi.a1

import org.json.JSONArray
import org.json.JSONObject

internal data class ArchivedLearningEvent(
    val id: String, val key: String, val gameId: String,
    val kind: LearningKind, val at: Long, val assisted: Boolean, val attempt: String = "",
    val order: Long? = null,
)

internal data class LearningArchive(
    val cards: List<LearningCard>,
    val events: List<ArchivedLearningEvent>,
)

internal fun LearningArchive.toJson(): JSONObject = JSONObject()
    .put("cards", JSONArray(cards.map { card ->
        JSONObject().put("key", card.key).put("answer", card.answer)
            .put("clue", card.clue).put("context", card.context).put("pinned", card.pinned)
            .apply { if (card.dueAt > 0) put("dueAt", card.dueAt) }
    }))
    .put("events", JSONArray(events.map { event ->
        JSONArray(listOf(event.id, event.key, event.gameId, event.kind.name, event.at, event.assisted, event.attempt))
            .apply { event.order?.let { put(it) } }
    }))

internal fun JSONObject.toLearningArchive(): LearningArchive {
    val data = getJSONArray("cards")
    val entries = getJSONArray("events")
    require(data.length() <= 50_000 && entries.length() <= 200_000)
    val cards = List(data.length()) { i ->
        val item = data.getJSONObject(i)
        val answer = item.getString("answer")
        val clue = item.getString("clue")
        val context = item.optString("context")
        require(answer.length in 2..80 && clue.length in 1..400 && context.length <= 2_000)
        val key = learningWordKey(Entry(answer, clue, context))
        require(item.getString("key") == key)
        LearningCard(key, answer, clue, context, dueAt = item.optLong("dueAt").coerceAtLeast(0),
            pinned = item.optBoolean("pinned"))
    }
    val cardKeys = cards.map { it.key }.toSet()
    require(cardKeys.size == cards.size)
    val events = List(entries.length()) { i ->
        val row = entries.getJSONArray(i)
        require(row.length() in 6..8)
        val id = row.getString(0)
        val key = row.getString(1)
        val gameId = row.getString(2)
        require(id.length in 8..100 && key in cardKeys && gameId.length in 1..150)
        val kind = LearningKind.valueOf(row.getString(3))
        val attempt = row.optString(6)
        require(attempt.isEmpty() || (kind == LearningKind.WRONG_CHECK &&
            attempt.length <= 80 && attempt.all { it in 'A'..'Z' }))
        ArchivedLearningEvent(id, key, gameId, kind,
            row.getLong(4).also { require(it > 0) }, row.getBoolean(5), attempt,
            if (row.length() == 8) row.getLong(7).also { require(it in 1..1_000_000) } else null)
    }
    require(events.map { it.id }.toSet().size == events.size)
    return LearningArchive(cards, events)
}
