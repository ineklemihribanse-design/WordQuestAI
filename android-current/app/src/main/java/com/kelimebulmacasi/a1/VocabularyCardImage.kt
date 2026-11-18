package com.kelimebulmacasi.a1

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Optional, user-requested, per-word illustration with the learner's saved Gemini key.
 * Never send the user's name, profile ID, learning history, or conversation text to the model.
 * Image creation requires a tap and may consume the user's provider quota.
 */
internal object VocabularyGeminiImageService {
    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/interactions"

    suspend fun generate(apiKey: String, word: String, clue: String): ByteArray =
        withContext(Dispatchers.IO) {
            val concept = word.trim().take(72)
            val translation = clue.trim().take(108)
            require(concept.isNotBlank() && translation.isNotBlank()) { "Bu kelime için görsel oluşturulamaz." }
            val instruction = """
                Create exactly one tasteful, premium editorial illustration for a vocabulary flashcard.
                English concept: "$concept". Turkish meaning or context: "$translation".
                Depict the real-world referent, scene, action or feeling accurately.
                If a person is appropriate, depict realistic age-appropriate human beings.
                For a non-person concept, depict relevant realistic objects or environments, not robots.
                Avoid cartoon mascots, text, typography, watermarks, UI elements, or logos.
                Natural photographic lighting, refined navy/cream palette, center composition,
                vertical portrait illustration with space around the subject.
                No additional explanation: return the image only.
            """.trimIndent()
            // Nano Banana 2.1: Interactions API, 1K image, no server-side history.
            val body = JSONObject()
                .put("model", "gemini-nano-banana-2.1")
                .put("input", instruction)
                .put("store", false)
                .put("response_format", JSONObject()
                    .put("type", "image")
                    .put("image_size", "1K")).toString()

            val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", apiKey)
                setRequestProperty("Api-Revision", "2026-05-20")
            }
            try {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                when (connection.responseCode) {
                    429 -> error("Görsel kotası doldu. Daha sonra tekrar dene.")
                    401, 403 -> error("Gemini görsel iznini ve API anahtarını kontrol et.")
                    in 400..599 -> error("Gemini şu anda görsel oluşturamadı.")
                }
                val response = connection.inputStream.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        if (output.size() + n > 10_000_000) error("Görsel yanıtı çok büyük.")
                        output.write(buffer, 0, n)
                    }
                    output.toByteArray().toString(Charsets.UTF_8)
                }
                val interaction = JSONObject(response)
                val image = interaction.optJSONObject("output_image")
                var encoded = image?.optString("data").orEmpty()
                if (encoded.isEmpty()) {
                    val steps = interaction.optJSONArray("steps")
                        ?: error("Model görsel döndürmedi.")
                    for (stepIndex in 0 until steps.length()) {
                        val step = steps.optJSONObject(stepIndex) ?: continue
                        if (step.optString("type") != "model_output") continue
                        val blocks = step.optJSONArray("content") ?: continue
                        for (blockIndex in 0 until blocks.length()) {
                            val block = blocks.optJSONObject(blockIndex) ?: continue
                            if (block.optString("type") == "image") {
                                encoded = block.optString("data")
                                if (encoded.isNotEmpty()) break
                            }
                        }
                        if (encoded.isNotEmpty()) break
                    }
                }
                if (encoded.isEmpty() || encoded.length > 10_000_000) {
                    error("Model geçerli görsel döndürmedi.")
                }
                val original = Base64.decode(encoded, Base64.DEFAULT)
                if (original.size !in 1..7_500_000) error("Görsel boyutu uygun değil.")
                val bitmap = BitmapFactory.decodeByteArray(original, 0, original.size)
                    ?: error("Görsel okunamadı.")
                val scale = minOf(1f, 900f / max(bitmap.width, bitmap.height))
                val resized = if (scale < 1f)
                    Bitmap.createScaledBitmap(bitmap,
                        (bitmap.width * scale).toInt().coerceAtLeast(1),
                        (bitmap.height * scale).toInt().coerceAtLeast(1), true)
                    else bitmap
                val buffer = java.io.ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.JPEG, 84, buffer)
                if (resized !== bitmap) resized.recycle()
                bitmap.recycle()
                buffer.toByteArray().also { require(it.size in 1..2_500_000) }
            } finally {
                connection.disconnect()
            }
        }
}

internal object VocabularyVisualCache {
    private fun cacheFile(context: Context, profile: String, word: String, clue: String): File {
        val fingerprint = (profile + "|" + word.lowercase() + "|" + clue.lowercase())
            .toByteArray(Charsets.UTF_8)
        val hash = MessageDigest.getInstance("SHA-256").digest(fingerprint).joinToString("") {
            "%02x".format(it.toInt() and 255)
        }
        return File(File(context.cacheDir, "vocabulary_visuals_v1"), "$hash.jpg")
    }
    suspend fun load(context: Context, profile: String, word: String, clue: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val file = cacheFile(context, profile, word, clue)
            file.takeIf { it.isFile && it.length() in 1..2_500_000L }?.readBytes()
        }
    suspend fun save(context: Context, profile: String, word: String, clue: String, data: ByteArray) =
        withContext(Dispatchers.IO) {
            if (data.size !in 1..2_500_000) return@withContext
            val file = cacheFile(context, profile, word, clue)
            file.parentFile?.mkdirs()
            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeBytes(data)
            if (!temp.renameTo(file)) {
                file.writeBytes(data)
                temp.delete()
            }
            // Bound disk use; OS may also clear cache at any time.
            file.parentFile?.listFiles { item -> item.extension == "jpg" }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(60)?.forEach { it.delete() }
        }
}
