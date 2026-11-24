package com.kelimebulmacasi.a1

import java.io.IOException
import java.net.URI
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal enum class Wq210LenaService(val label: String, val model: String, val endpoint: String) {
    Groq("Groq", "openai/gpt-oss-20b", "https://api.groq.com/openai/v1/chat/completions"),
    Gemini("Google Gemini", "gemini-3.5-flash-lite", "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent"),
    OpenRouter("OpenRouter", "openrouter/free", "https://openrouter.ai/api/v1/chat/completions");
}
internal enum class Wq210ExtraKey(val label: String, val url: String, val prefix: String) {
    Groq("Groq", "https://console.groq.com/keys", "gsk_"),
    OpenRouter("OpenRouter", "https://openrouter.ai/keys", "sk-or-");
}
internal class Wq210HttpFailure(val status: Int, val retryAfterSeconds: Long?) : IOException("HTTP $status")
internal fun wq210CooldownMillis(status: Int, retryAfter: Long?): Long {
    val minimum = when (status) { 429 -> 60_000L; 401, 403, 402 -> 300_000L; else -> 20_000L }
    val requested = retryAfter?.coerceIn(0, 604800)?.times(1000) ?: 0L
    return maxOf(minimum, requested)
}
internal class Wq210LenaRouter(private val now: () -> Long = System::currentTimeMillis) {
    private val blockedUntil = mutableMapOf<Wq210LenaService, Long>()
    fun reset() = blockedUntil.clear()
    fun eligible(keys: Map<Wq210LenaService, String>): List<Wq210LenaService> =
        Wq210LenaService.entries.filter { !keys[it].isNullOrBlank() && (blockedUntil[it] ?: 0L) <= now() }

    suspend fun <T : Any> run(keys: Map<Wq210LenaService, String>, request: suspend (Wq210LenaService, String) -> T): T {
        val candidates = eligible(keys)
        if (candidates.isEmpty()) throw WordRequestException("Lena için etkin API anahtarı yok veya sağlayıcılar geçici bekleme süresinde. Ayarlar'daki anahtar ve kotaları kontrol et. Mesajın korunuyor.")
        val errors = mutableListOf<String>()
        for (service in candidates) {
            try {
                return request(service, keys.getValue(service))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Wq210HttpFailure) {
                blockedUntil[service] = now() + wq210CooldownMillis(e.status, e.retryAfterSeconds)
                errors.add("${service.label}: HTTP ${e.status}")
            } catch (e: IOException) {
                blockedUntil[service] = now() + 20_000
                errors.add("${service.label}: bağlantı/yanıt")
            } catch (e: org.json.JSONException) {
                blockedUntil[service] = now() + 20_000
                errors.add("${service.label}: okunamayan yanıt")
            }
        }
        throw WordRequestException("Lena şu anda yanıt alamadı. " + errors.joinToString(" · ") + ". Mesajın korunuyor; daha sonra tekrar dene.")
    }
}
internal fun wq210LenaBody(service: Wq210LenaService, request: JSONObject): JSONObject {
    val body = JSONObject(request.toString())
    if (service == Wq210LenaService.Gemini) {
        body.optJSONObject("generationConfig")?.put("maxOutputTokens", 512)
        return body
    }
    val chat = providerRequestBody(AiProvider.OpenAI, body)
    chat.put("model", service.model).put("max_completion_tokens", 768)
    if (service == Wq210LenaService.Groq) chat.put("reasoning_effort", "low")
    if (service == Wq210LenaService.OpenRouter) {
        chat.remove("max_completion_tokens")
        chat.put("max_tokens", 512)
        // Only the explicitly free router is used; no paid-model fallback.
        chat.put("provider", JSONObject().put("max_price", JSONObject().put("prompt", 0).put("completion", 0)))
    }
    return chat
}
internal suspend fun wq210LenaHttp(service: Wq210LenaService, secret: String, request: JSONObject): String =
    withContext(Dispatchers.IO) {
        val c = URI.create(service.endpoint).toURL().openConnection() as HttpsURLConnection
        try {
            c.requestMethod = "POST"
            c.connectTimeout = 3500
            c.readTimeout = 6000
            c.instanceFollowRedirects = false
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (service == Wq210LenaService.Gemini) c.setRequestProperty("x-goog-api-key", secret)
            else c.setRequestProperty("Authorization", "Bearer $secret")
            val payload = wq210LenaBody(service, request).toString().toByteArray(Charsets.UTF_8)
            c.setFixedLengthStreamingMode(payload.size)
            c.outputStream.use { it.write(payload) }
            val status = c.responseCode
            if (status !in 200..299) throw Wq210HttpFailure(status, c.getHeaderField("Retry-After")?.trim()?.toLongOrNull())
            val raw = c.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val text = StringBuilder()
                val buf = CharArray(4096)
                while (true) {
                    val n = reader.read(buf)
                    if (n < 0) break
                    if (text.length + n > 262144) throw IOException("oversize")
                    text.append(buf, 0, n)
                }
                text.toString()
            }
            if (service == Wq210LenaService.Gemini) raw else normalizeProviderResponse(AiProvider.OpenAI, raw)
        } finally { c.disconnect() }
    }
