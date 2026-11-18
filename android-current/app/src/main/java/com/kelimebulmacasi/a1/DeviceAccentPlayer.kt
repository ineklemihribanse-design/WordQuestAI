package com.kelimebulmacasi.a1

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.ArrayDeque
import java.util.Locale

/** A selectable voice that the phone's current speech engine actually reports as installed. */
internal data class DeviceVoiceOption(
    val id: String,
    val region: EnglishVoice,
    val quality: Int,
    val networkRequired: Boolean,
    val systemDefault: Boolean = false,
) {
    val description: String get() = if (systemDefault)
        "Android varsayılan Britanya sesi · en-GB (çalarken doğrulanır)" else buildString {
        append(id.substringAfterLast('.'))
        append(" · ")
        append(if (quality >= Voice.QUALITY_HIGH) "yüksek kalite" else "standart kalite")
        append(if (networkRequired) " · internet gerekli" else " · çevrimdışı")
    }
}

/**
 * Pronounces English using exactly matching en-US / en-GB voices installed on the device.
 * It never calls ACTION_INSTALL_TTS_DATA and never silently substitutes the other accent.
 * Initialization is asynchronous; queued words play after init succeeds, otherwise fallback
 * is requested from the bundled recording library.
 */
internal class DeviceAccentPlayer(
    context: Context,
    private val voicesChanged: (List<DeviceVoiceOption>) -> Unit,
) {
    companion object {
        const val SYSTEM_BRITISH_VOICE = "wordquest.system.en-GB"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val appContext = context.applicationContext
    private var engine: TextToSpeech? = null
    private var initStarted = false
    private var initialized = false
    private var ready = false
    private var closed = false
    private var token = 0L
    private var index = 0
    private var currentId: String? = null
    private val remaining = ArrayDeque<String>()
    private var request: Request? = null

    private data class Request(
        val words: List<String>,
        val accent: EnglishVoice,
        val voiceId: String,
        val allowNetwork: Boolean,
        val speed: Float,
        val volume: Float,
        val fallback: (String, List<String>) -> Unit,
    )

    fun inspect() { startIfNeeded() }

    fun play(
        words: List<String>, accent: EnglishVoice, voiceId: String, allowNetwork: Boolean,
        speed: Float, volume: Float, fallback: (String, List<String>) -> Unit,
    ) {
        require(Looper.myLooper() == Looper.getMainLooper())
        stop()
        if (closed || words.isEmpty()) return
        request = Request(
            words.take(80), accent, voiceId, allowNetwork,
            speed.coerceIn(0.5f, 1.5f), volume.coerceIn(0f, 1f), fallback,
        )
        startIfNeeded()
        if (initialized) begin()
    }

    private fun startIfNeeded() {
        if (closed || initStarted) return
        initStarted = true
        try {
            engine = TextToSpeech(appContext) { status ->
                handler.post {
                    if (!closed) {
                        ready = status == TextToSpeech.SUCCESS
                        initialized = true
                        if (ready) {
                            engine?.setOnUtteranceProgressListener(listener())
                            publishVoices()
                        } else voicesChanged(emptyList())
                        if (request != null) begin()
                    }
                }
            }
        } catch (_: Exception) {
            initialized = true
            ready = false
            voicesChanged(emptyList())
            if (request != null) begin()
        }
    }

    private fun installedVoices(): List<Voice> = try {
        engine?.voices.orEmpty().filter { voice ->
            // Preserve the original American selection exactly. Some Android engines
            // report British English with the nonstandard UK/GBR region alias.
            (voice.locale.language == "en" && voice.locale.country == "US" ||
                BritishLocalePolicy.isBritish(voice.locale)) &&
                TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features.orEmpty()
        }.sortedWith(compareBy<Voice>({ it.isNetworkConnectionRequired }, { -it.quality }, { it.name }))
    } catch (_: Exception) { emptyList() }

    private fun britishSystemLanguageAvailable(tts: TextToSpeech): Boolean = runCatching {
        tts.isLanguageAvailable(Locale.UK) >= TextToSpeech.LANG_COUNTRY_AVAILABLE
    }.getOrDefault(false)

    /**
     * Some TTS engines can speak en-GB without exposing any en-GB entries in getVoices().
     * Querying support does NOT change the American voice or global device settings.
     */
    private fun publishVoices() {
        val options = installedVoices().map {
            DeviceVoiceOption(it.name, if (BritishLocalePolicy.isBritish(it.locale))
                EnglishVoice.British else EnglishVoice.American,
                it.quality, it.isNetworkConnectionRequired)
        }.toMutableList()
        if (engine?.let(::britishSystemLanguageAvailable) == true) {
            options.add(DeviceVoiceOption(SYSTEM_BRITISH_VOICE, EnglishVoice.British,
                Voice.QUALITY_NORMAL, networkRequired = false, systemDefault = true))
        }
        voicesChanged(options)
    }

    private fun selectBritishSystemVoice(tts: TextToSpeech, allowNetwork: Boolean): Boolean {
        if (!britishSystemLanguageAvailable(tts)) return false
        val outcome = runCatching { tts.setLanguage(Locale.UK) }
            .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        if (outcome < TextToSpeech.LANG_COUNTRY_AVAILABLE) return false
        // setLanguage may choose a different region. Verify the resulting accent,
        // never silently allow US English on the UK button.
        val active = runCatching { tts.voice }.getOrNull()
        if (active != null) {
            return BritishLocalePolicy.isBritish(active.locale) &&
                (allowNetwork || !active.isNetworkConnectionRequired)
        }
        // A few older engines return null from getVoice(); validate the engine's
        // reported synthesis language instead (some use ISO3 codes: eng / GBR).
        @Suppress("DEPRECATION")
        val language = runCatching { tts.language }.getOrNull()
        return BritishLocalePolicy.isBritish(language)
    }

    private fun begin() {
        val next = request ?: return
        val tts = engine
        if (!ready || tts == null) {
            unavailable("Cihazın ses motoru açılamadı", next.words)
            return
        }
        val choices = installedVoices().filter {
            (it.locale.country == next.accent.locale.country ||
                (next.accent == EnglishVoice.British && BritishLocalePolicy.isBritish(it.locale))) &&
                (next.allowNetwork || !it.isNetworkConnectionRequired)
        }
        // A manual selection must be honoured, otherwise fall back to local recordings.
        val candidates = if (next.voiceId.isNotBlank()) choices.filter { it.name == next.voiceId } else choices
        val selected = candidates.firstOrNull { candidate ->
            runCatching {
                tts.setVoice(candidate) == TextToSpeech.SUCCESS &&
                    tts.voice?.name == candidate.name &&
                    (tts.voice?.locale?.country == next.accent.locale.country ||
                        (next.accent == EnglishVoice.British &&
                            BritishLocalePolicy.isBritish(tts.voice?.locale)))
            }.getOrDefault(false)
        }
        // UK ONLY: the system language may be usable even when getVoices() returns
        // zero British entries. US keeps its original explicit-voice path unchanged.
        val britishSystemSelected = selected == null && next.accent == EnglishVoice.British &&
            (next.voiceId.isBlank() || next.voiceId == SYSTEM_BRITISH_VOICE) &&
            selectBritishSystemVoice(tts, next.allowNetwork)
        if (selected == null && !britishSystemSelected) {
            val reason = if (next.voiceId.isNotBlank())
                "Seçilen ${next.accent.label} cihaz sesi artık kullanılamıyor"
            else "${next.accent.label} için uygun ${if (next.allowNetwork) "" else "çevrimdışı "}cihaz sesi bulunamadı"
            unavailable(reason, next.words)
            return
        }
        remaining.clear()
        remaining.addAll(next.words)
        tts.setSpeechRate(next.speed)
        index = 0
        speakNext()
    }

    private fun speakNext() {
        val next = request ?: return
        val tts = engine ?: return
        if (remaining.isEmpty()) {
            currentId = null
            request = null
            return
        }
        val word = remaining.removeFirst()
        val id = "wordquest-${token}-${index++}"
        currentId = id
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, next.volume)
        }
        val result = runCatching { tts.speak(word, TextToSpeech.QUEUE_FLUSH, params, id) }
            .getOrDefault(TextToSpeech.ERROR)
        if (result != TextToSpeech.SUCCESS) {
            unavailable("Cihaz sesi '$word' kelimesini okuyamadı", listOf(word) + remaining.toList())
        }
    }

    private fun listener() = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit
        override fun onDone(utteranceId: String?) = finish(utteranceId, false)
        @Deprecated("Old Android engines may call this overload")
        override fun onError(utteranceId: String?) = finish(utteranceId, true)
        override fun onError(utteranceId: String?, errorCode: Int) = finish(utteranceId, true)
        override fun onStop(utteranceId: String?, interrupted: Boolean) = Unit
    }

    private fun finish(id: String?, error: Boolean) {
        handler.post {
            if (closed || id == null || currentId != id) return@post
            if (error) unavailable("Cihaz sesi okuma sırasında hata verdi", remaining.toList())
            else speakNext()
        }
    }

    private fun unavailable(reason: String, remainingWords: List<String>) {
        val callback = request?.fallback
        stop()
        if (remainingWords.isNotEmpty()) callback?.invoke(reason, remainingWords)
    }

    fun stop() {
        token++
        request = null
        currentId = null
        remaining.clear()
        runCatching { engine?.stop() }
    }

    fun close() {
        stop()
        closed = true
        runCatching { engine?.shutdown() }
        engine = null
    }
}
