package com.kelimebulmacasi.a1

import org.json.JSONObject
import org.json.JSONArray

/** Only the selected provider receives the conversation and its own key. */
internal class Wq209LenaModelRoute(private val provider: AiProvider, configured: String) {
    var model: String = if (provider == AiProvider.Gemini &&
        configured == "gemini-3.1-flash-lite") "gemini-3.5-flash-lite" else configured
        private set

    fun onFailure(failure: Wq208ProviderFailure) {
        if (provider != AiProvider.Gemini || failure.httpStatus != 503 || failure.retryDelayMs == null) return
        model = if (model == "gemini-3.5-flash-lite") "gemini-3.1-flash-lite" else "gemini-3.5-flash-lite"
    }
}

/** Text-only tutor history is portable between Gemini models. Keep user text and
 * visible assistant answers, not model-specific thought signatures. */
internal fun wq209LenaTextHistory(request: JSONObject, turns: List<ConversationTurn>): JSONObject {
    val result = JSONObject(request.toString())
    val old = result.optJSONArray("contents") ?: return result
    val history = JSONArray()
    for (turn in turns.takeLast(8)) {
        history.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", turn.message))))
        history.put(JSONObject().put("role", "model").put("parts", JSONArray().put(JSONObject().put("text", turn.answer))))
    }
    if (old.length() > 0) history.put(old.getJSONObject(old.length()-1))
    result.put("contents", history)
    return result
}
