package com.kelimebulmacasi.a1

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale

/**
 * AI free-form responses: offline recordings when the complete English text is covered;
 * otherwise Android TTS only with a verified exact regional voice, never a silent fallback.
 * Puzzle/vocabulary voice always uses [OfflineAccentPlayer] without Android TTS.
 */
internal class ConversationAudio(
    context: Context,
    private val onPlaying: (Long?) -> Unit,
    private val onError: (String) -> Unit,
    private val englishVoice: () -> EnglishVoice = { EnglishVoice.American },
    private val settings: () -> Pair<Float, Float> = { 1f to 1f },
    private val pronunciationPreferences: () -> WordQuestPreferences = { WordQuestPreferences() },
    private val onActualSpeechRange: (Long, String) -> Unit = { _, _ -> },
) {
    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val offline = OfflineAccentPlayer(context)
    private var engine: TextToSpeech? = null
    private var initialized = false
    private var ready = false
    private var closed = false
    private var pending: ConversationTurn? = null
    private var sequence = 0L
    private var utterance: String? = null
    private var activeTurnId: Long? = null
    private var utteranceText = ""

    fun play(turn: ConversationTurn) {
        if (closed) return
        stop()
        val text = turn.answer.replace(Regex("[*_`#]"), "").trim()
        if (text.isEmpty()) return
        activeTurnId = turn.id
        val (rate, volume) = settings()
        // If all English words have bundled recordings, no device TTS service is initialized.
        if (turn.language == ConversationLanguage.English &&
            pronunciationPreferences().pronunciationMode == PronunciationMode.ClassicOffline) {
            val words = Regex("[A-Za-z]+(?:'[A-Za-z]+)?").findAll(text)
                .map { it.value.lowercase(Locale.US) }.toList()
            if (words.isNotEmpty() && words.size <= 80 && words.all(offline::contains)) {
                onPlaying(turn.id)
                offline.play(words, englishVoice(), rate, volume,
                    error = { onPlaying(null); onError(it) },
                    completed = { onPlaying(null) })
                return
            }
        }
        // Truly open-ended AI text cannot be represented by a finite offline word library.
        // Never claim a downloaded accent is available based on setLanguage() alone.
        if (!initialized) {
            pending = turn
            if (engine == null) initialize()
            return
        }
        val tts = engine
        if (!ready || tts == null) {
            onError("Serbest konuşma ses motoru kullanılamıyor; bulmacalardaki US/UK çevrimdışı sesler çalışmaya devam eder.")
            return
        }
        if (turn.language == ConversationLanguage.English) {
            if (selectExactEnglishVoice(tts, englishVoice(), pronunciationPreferences()) == null) {
                onError("${englishVoice().locale.toLanguageTag()} için uzun AI yanıtını okuyabilecek uygun cihaz sesi bulunamadı. Kelime bulmacalarının çevrimdışı sesleri kullanılabilir.")
                return
            }
        } else {
            val locale = Locale.forLanguageTag(turn.language.tag)
            if (tts.setLanguage(locale) < TextToSpeech.LANG_AVAILABLE) {
                onError("${turn.language.label} sesli okuması cihazdaki motor tarafından desteklenmiyor.")
                return
            }
        }
        tts.setSpeechRate(rate.coerceIn(0.5f, 1.5f))
        val id = (++sequence).toString()
        utterance = id
        utteranceText = text.take(TextToSpeech.getMaxSpeechInputLength())
        if (tts.speak(utteranceText, TextToSpeech.QUEUE_FLUSH,
                Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f)) }, id) == TextToSpeech.SUCCESS) {
            // onStart() reports actual playback; speak() SUCCESS only queues audio.
        } else {
            utterance = null
            onError("AI yanıtı sesli okunamadı. Bulmacaların çevrimdışı sesleri cihaz TTS sistemini kullanmaz.")
        }
    }

    private fun initialize() {
        engine = TextToSpeech(appContext) { status ->
            main.post {
                if (!closed) {
                    initialized = true
                    ready = status == TextToSpeech.SUCCESS
                    if (ready) engine?.setOnUtteranceProgressListener(listener())
                    val next = pending
                    pending = null
                    next?.let(::play)
                }
            }
        }
    }

    fun stop() {
        pending = null
        utterance = null
        activeTurnId = null
        utteranceText = ""
        offline.stop()
        engine?.stop()
        onPlaying(null)
    }

    fun close() {
        closed = true
        stop()
        offline.close()
        engine?.shutdown()
        engine = null
    }

    private fun selectExactEnglishVoice(tts: TextToSpeech, accent: EnglishVoice,
                                        preferences: WordQuestPreferences): Voice? {
        val locale = accent.locale
        val selectedName = if (accent == EnglishVoice.American) preferences.nativeVoiceUs
            else preferences.nativeVoiceUk
        val voices = tts.voices.orEmpty().filter {
            it.locale.language == "en" && it.locale.country == locale.country &&
                TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty() &&
                (preferences.allowOnlineVoice || !it.isNetworkConnectionRequired) &&
                (selectedName.isBlank() || selectedName == it.name)
        }.sortedWith(compareBy<Voice>({ it.isNetworkConnectionRequired }, { -it.quality }, { it.name }))
        return voices.firstOrNull { candidate ->
            tts.setVoice(candidate) == TextToSpeech.SUCCESS &&
                tts.voice?.name == candidate.name &&
                tts.voice?.locale?.country == locale.country
        }
    }

    private fun listener() = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            main.post {
                val id = activeTurnId
                if (!closed && utteranceId == utterance && id != null) onPlaying(id)
            }
        }
        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            // This is WORD/TEXT timing, not actual phoneme/viseme timing.
            main.post {
                val id = activeTurnId
                val text = utteranceText
                if (!closed && utteranceId == utterance && id != null &&
                    start >= 0 && end > start && end <= text.length) {
                    onActualSpeechRange(id, text.substring(start, end))
                }
            }
        }
        override fun onDone(utteranceId: String?) { finish(utteranceId, failed = false) }
        @Deprecated("Android uses this callback on some TTS engines.")
        override fun onError(utteranceId: String?) { finish(utteranceId, failed = true) }
        override fun onError(utteranceId: String?, errorCode: Int) { finish(utteranceId, failed = true) }
        override fun onStop(utteranceId: String?, interrupted: Boolean) { finish(utteranceId, failed = false) }
    }

    private fun finish(id: String?, failed: Boolean) {
        main.post {
            if (!closed && id != null && id == utterance) {
                utterance = null
                activeTurnId = null
                utteranceText = ""
                onPlaying(null)
                if (failed) onError("Yanıt sesli okunamadı. Cihaz TTS motorunun ses kısıtlaması olabilir.")
            }
        }
    }
}
