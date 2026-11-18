package com.kelimebulmacasi.a1

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.Locale

/**
 * Device-independent US/GB recordings bundled with the app.
 * Does NOT query or start the Android TextToSpeech service, and never substitutes one accent for another.
 * Both regions are generated using separately identified eSpeak profiles, not a single TTS model.
 */
internal class OfflineAccentPlayer(context: Context) {
    private val assets = context.applicationContext.assets
    private val main = Handler(Looper.getMainLooper())
    private val queue = ArrayDeque<String>()
    private var current: MediaPlayer? = null
    private var generation = 0
    private var accent = EnglishVoice.American
    private var rate = 0.9f
    private var level = 1f
    private var onProblem: (String) -> Unit = {}
    private var onFinished: () -> Unit = {}
    private var closed = false
    private val known: Map<String, String> by lazy {
        runCatching {
            val json = assets.open("pronunciation/manifest.json")
                .bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
                .getJSONObject("words")
            buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val word = keys.next()
                    put(word, json.getString(word))
                }
            }
        }.getOrDefault(emptyMap())
    }

    val dictionarySize: Int get() = known.size
    val libraryAvailable: Boolean get() = known.isNotEmpty()

    fun contains(word: String): Boolean = known.containsKey(normalize(word))

    fun play(
        words: List<String>, selected: EnglishVoice, speed: Float, volume: Float,
        error: (String) -> Unit, completed: () -> Unit = {},
    ) {
        check(Looper.myLooper() == Looper.getMainLooper()) { "Audio player must be used on main thread" }
        stop()
        if (closed) return
        onProblem = error
        onFinished = completed
        accent = selected
        rate = speed.coerceIn(0.5f, 1.5f)
        level = volume.coerceIn(0f, 1f)
        val filtered = words.map(::normalize).filter(String::isNotBlank).take(80)
        if (filtered.isEmpty()) { completed(); return }
        for (word in filtered) {
            if (!contains(word)) {
                error("'$word' için ${selected.locale.toLanguageTag()} çevrimdışı ses kaydı yok. Diğer aksana geçilmedi.")
                return
            }
        }
        queue.addAll(filtered)
        playNext()
    }

    fun configure(speed: Float, volume: Float) {
        rate = speed.coerceIn(0.5f, 1.5f)
        level = volume.coerceIn(0f, 1f)
        current?.let { player ->
            runCatching {
                player.setVolume(level, level)
                if (player.isPlaying) player.playbackParams = PlaybackParams().setSpeed(rate).setPitch(1f)
            }
        }
    }

    fun stop() {
        generation++
        queue.clear()
        current?.let { runCatching { it.reset() }; runCatching { it.release() } }
        current = null
        onFinished = {}
        onProblem = {}
    }

    fun close() { stop(); closed = true }

    private fun playNext() {
        if (closed || current != null) return
        if (queue.isEmpty()) {
            val finished = onFinished
            onFinished = {}
            finished()
            return
        }
        val word = queue.removeFirst()
        val filename = known[word]
        if (filename == null) { fail(word, "kayıt listesinde yok"); return }
        val token = generation
        val player = MediaPlayer()
        current = player
        try {
            val descriptor = assets.openFd("pronunciation/${accent.locale.toLanguageTag().lowercase(Locale.US)}/$filename.ogg")
            descriptor.use {
                player.setDataSource(it.fileDescriptor, it.startOffset, it.length)
            }
            player.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            player.setVolume(level, level)
            player.setOnPreparedListener { mp ->
                if (generation == token && current === mp && !closed) {
                    try {
                        mp.start()
                        mp.playbackParams = PlaybackParams().setSpeed(rate).setPitch(1f)
                    } catch (exc: Exception) { fail(word, exc.message ?: "çalınamadı") }
                }
            }
            player.setOnCompletionListener { mp ->
                if (generation == token && current === mp) {
                    current = null
                    runCatching { mp.release() }
                    main.post { if (generation == token) playNext() }
                }
            }
            player.setOnErrorListener { mp, _, _ ->
                if (generation == token && current === mp) fail(word, "çözümleme başarısız")
                true
            }
            player.prepareAsync()
        } catch (exc: Exception) {
            fail(word, exc.message ?: "ses dosyası açılamadı")
        }
    }

    private fun fail(word: String, detail: String) {
        val problem = onProblem
        val region = accent.locale.toLanguageTag()
        stop()
        problem("'$word' için $region çevrimdışı kayıt çalınamadı ($detail). Başka aksan kullanılmadı.")
    }

    companion object {
        fun normalize(text: String): String = text.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ")
        fun assetId(text: String): String = MessageDigest.getInstance("SHA-1")
            .digest(normalize(text).toByteArray(Charsets.UTF_8))
            .take(8).joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
