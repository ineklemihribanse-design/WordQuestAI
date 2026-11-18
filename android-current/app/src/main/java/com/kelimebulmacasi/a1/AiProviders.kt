package com.kelimebulmacasi.a1

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.IOException
import java.net.URI
import java.net.SocketTimeoutException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Bring your own key. There is deliberately no developer key in the APK. */
internal enum class AiProvider(val label: String, val defaultModel: String, val homepage: String) {
    Gemini("Google Gemini", "gemini-3.1-flash-lite", "https://aistudio.google.com/apikey"),
    OpenAI("OpenAI", "gpt-4.1-mini", "https://platform.openai.com/api-keys"),
    Claude("Claude", "claude-sonnet-4-5", "https://platform.claude.com/settings/keys");
}

internal val aiModelPattern = Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,79}")

/** Android Keystore encrypts secrets before saving to account-specific SharedPreferences.
 *  These preferences are excluded from Android backup and must never be logged or sent to Firebase.
 */
internal class UserAiCredentials(context: Context, profile: String) {
    private val prefs = context.applicationContext.getSharedPreferences("wq_private_ai_keys", Context.MODE_PRIVATE)
    private val userPrefix = stableHash(profile).take(24)
    private val keystoreAlias = "wq_ai_credential_aes_v1"
    private fun field(provider: AiProvider, suffix: String) = "$userPrefix.${provider.name}.$suffix"

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(keystoreAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(keystoreAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256).build())
            generateKey()
        }
    }

    fun selected(): AiProvider = AiProvider.entries.firstOrNull {
        // Historical Claude choices are left encrypted on disk, never made active.
        it != AiProvider.Claude && it.name == prefs.getString("$userPrefix.selected", null)
    } ?: AiProvider.Gemini

    fun choose(provider: AiProvider) {
        prefs.edit().putString("$userPrefix.selected", provider.name).apply()
    }

    fun model(provider: AiProvider): String = prefs.getString(field(provider, "model"), null)
        ?.takeIf { aiModelPattern.matches(it) } ?: provider.defaultModel

    fun saveModel(provider: AiProvider, model: String) {
        require(aiModelPattern.matches(model)) { "Model adı harf, sayı, nokta, tire veya alt çizgi içermeli." }
        prefs.edit().putString(field(provider, "model"), model).apply()
    }

    fun key(provider: AiProvider): String {
        val encoded = prefs.getString(field(provider, "key"), null) ?: return ""
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            require(bytes.size > 28)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
        }.getOrDefault("")
    }

    fun hasKey(provider: AiProvider): Boolean = key(provider).isNotBlank()

    fun saveKey(provider: AiProvider, raw: String) {
        val value = validatePersonalApiKey(provider, raw)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val saved = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit().putString(field(provider, "key"), Base64.encodeToString(saved, Base64.NO_WRAP)).apply()
    }

    fun removeKey(provider: AiProvider) {
        prefs.edit().remove(field(provider, "key")).apply()
    }
}

internal fun validatePersonalApiKey(provider: AiProvider, raw: String): String {
    val key = raw.trim().trim('"', '\'').trim()
    if (key.length !in 12..4096 || key.any { it.isWhitespace() || Character.isISOControl(it) })
        throw WordRequestException("API anahtarı geçersiz görünüyor. Tamamını kopyalayın.")
    if (provider == AiProvider.Gemini && key.startsWith("sk-"))
        throw WordRequestException("Bu anahtar Gemini için değil. Ayarlardan doğru sağlayıcıyı seçin.")
    return key
}

/** Convert the existing Gemini-oriented request body into the selected provider's text-chat API. */
internal fun providerRequestBody(provider: AiProvider, geminiRequest: JSONObject): JSONObject {
    if (provider == AiProvider.Gemini) return geminiRequest
    val system = geminiRequest.optJSONObject("systemInstruction")?.optJSONArray("parts")
        ?.optJSONObject(0)?.optString("text").orEmpty()
    val source = geminiRequest.optJSONArray("contents") ?: JSONArray()
    val messages = JSONArray()
    if (provider == AiProvider.OpenAI) messages.put(JSONObject().put("role", "system").put("content", system))
    for (i in 0 until source.length()) {
        val item = source.optJSONObject(i) ?: continue
        val role = if (item.optString("role") == "model") "assistant" else "user"
        val parts = item.optJSONArray("parts") ?: JSONArray()
        val text = (0 until parts.length()).mapNotNull { parts.optJSONObject(it)?.optString("text") }
            .joinToString("\n").take(10_000)
        if (text.isNotBlank()) messages.put(JSONObject().put("role", role).put("content", text))
    }
    val maxTokens = geminiRequest.optJSONObject("generationConfig")?.optInt("maxOutputTokens", 1024)
        ?.coerceIn(128, 4096) ?: 1024
    return if (provider == AiProvider.OpenAI) JSONObject()
        .put("messages", messages).put("max_completion_tokens", maxTokens)
        .apply {
            if (geminiRequest.optJSONObject("generationConfig")?.optString("responseMimeType") == "application/json")
                put("response_format", JSONObject().put("type", "json_object"))
        }
    else JSONObject().put("system", system).put("messages", messages).put("max_tokens", maxTokens)
}

/** Read provider text and present the same shape to the existing parsing routines. */
internal fun normalizeProviderResponse(provider: AiProvider, response: String): String {
    if (provider == AiProvider.Gemini) return response
    val obj = JSONObject(response)
    val text = when (provider) {
        AiProvider.OpenAI -> obj.optJSONArray("choices")?.optJSONObject(0)
            ?.optJSONObject("message")?.optString("content").orEmpty()
        AiProvider.Claude -> obj.optJSONArray("content")?.let { parts ->
            (0 until parts.length()).mapNotNull { parts.optJSONObject(it)?.takeIf { p -> p.optString("type") == "text" }
                ?.optString("text") }.joinToString("\n")
        }.orEmpty()
        else -> ""
    }
    if (text.isBlank()) throw WordRequestException("AI sağlayıcısından okunabilir yanıt alınamadı.")
    return JSONObject().put("candidates", JSONArray().put(JSONObject().put("content",
        JSONObject().put("parts", JSONArray().put(JSONObject().put("text", text)))))).toString()
}

internal suspend fun generateWithProvider(provider: AiProvider, rawKey: String, model: String,
                                          originalRequest: JSONObject): String = withContext(Dispatchers.IO) {
    val key = validatePersonalApiKey(provider, rawKey)
    if (!aiModelPattern.matches(model)) throw WordRequestException("Model adını Ayarlar'dan kontrol edin.")
    val uri = when (provider) {
        AiProvider.Gemini -> "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        AiProvider.OpenAI -> "https://api.openai.com/v1/chat/completions"
        AiProvider.Claude -> "https://api.anthropic.com/v1/messages"
    }
    val request = providerRequestBody(provider, originalRequest)
    if (provider != AiProvider.Gemini) request.put("model", model)
    val connection = URI.create(uri).toURL().openConnection() as HttpsURLConnection
    try {
        connection.requestMethod = "POST"
        connection.connectTimeout = 15_000
        connection.readTimeout = 45_000
        connection.instanceFollowRedirects = false
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        when (provider) {
            AiProvider.Gemini -> connection.setRequestProperty("x-goog-api-key", key)
            AiProvider.OpenAI -> connection.setRequestProperty("Authorization", "Bearer $key")
            AiProvider.Claude -> {
                connection.setRequestProperty("x-api-key", key)
                connection.setRequestProperty("anthropic-version", "2026-10-21")
            }
        }
        val body = request.toString().toByteArray(Charsets.UTF_8)
        connection.setFixedLengthStreamingMode(body.size)
        connection.outputStream.use { it.write(body) }
        val status = connection.responseCode
        if (status !in 200..299) {
            if (provider == AiProvider.Gemini) {
                val detail = runCatching { connection.errorStream?.bufferedReader()?.use { it.readText().take(3000) } }
                    .getOrNull().orEmpty()
                throw geminiRequestError(status, detail)
            }
            val explanation = when (status) {
                401 -> "Anahtar kabul edilmedi. Ayarlardan değiştirin."
                403 -> "API erişimine izin verilmedi. Sağlayıcı hesabınızı kontrol edin."
                404 -> "Seçtiğiniz model bulunamadı. Model adını kontrol edin."
                429 -> "API kota veya hız sınırına ulaşıldı."
                in 500..599 -> "AI sağlayıcısı geçici olarak yanıt veremiyor."
                else -> "İstek reddedildi. Sağlayıcı ve model ayarını kontrol edin."
            }
            throw WordRequestException("${provider.label}: $explanation (HTTP $status)", invalidKey = status == 401)
        }
        val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            val result = StringBuilder()
            val chars = CharArray(4096)
            while (true) {
                val n = reader.read(chars)
                if (n < 0) break
                if (result.length + n > 262144) throw WordRequestException("AI yanıtı fazla uzun; daha kısa istekte bulunun.")
                result.append(chars, 0, n)
            }
            result.toString()
        }
        normalizeProviderResponse(provider, response)
    } catch (_: SocketTimeoutException) {
        throw WordRequestException("AI isteği zaman aşımına uğradı. İnternet bağlantınızı kontrol edin.")
    } finally {
        connection.disconnect()
    }
}
