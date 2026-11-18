package com.kelimebulmacasi.a1

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** v0.8.1: preferred exact-regional device voice, with a safe independent offline fallback. */
internal class WordPronunciation(context: Context) {
    private val bundled = OfflineAccentPlayer(context)
    private val appContext = context.applicationContext
    private var device: DeviceAccentPlayer? = null
    private var rate = 0.9f
    private var volume = 1f
    private var mode = PronunciationMode.Device
    private var usVoice = ""
    private var ukVoice = ""
    private var allowNetwork = false
    private var closed = false

    var voiceDetails by mutableStateOf("Cihazın İngilizce sesleri henüz denetlenmedi.")
        private set
    var accentDiagnostics by mutableStateOf("Ses kayıtları denetlenmedi.")
        private set
    var deviceVoices by mutableStateOf<List<DeviceVoiceOption>>(emptyList())
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private fun devicePlayer(): DeviceAccentPlayer {
        return device ?: DeviceAccentPlayer(appContext) { choices ->
            deviceVoices = choices
            val listedUk = choices.count { it.region == EnglishVoice.British && !it.systemDefault }
            val systemUk = choices.any { it.region == EnglishVoice.British && it.systemDefault }
            accentDiagnostics = "US: ${choices.count { it.region == EnglishVoice.American }} cihaz sesi · " +
                "UK: $listedUk listelenen ses" +
                (if (systemUk) " · en-GB sistem dili destekleniyor" else " · en-GB sistem desteği bildirilmiyor") +
                " · Çevrimdışı güvenlik kayıtları: ${bundled.dictionarySize} kelime × 2 aksan."
        }.also { device = it }
    }

    fun inspect() {
        if (closed) return
        if (!bundled.libraryAvailable) error = "Uygulamada çevrimdışı kayıt paketi bulunamadı."
        accentDiagnostics = "Çevrimdışı güvenlik kayıtları: ${bundled.dictionarySize} kelime × 2 aksan; " +
            "cihaz sesleri sorgulanıyor."
        devicePlayer().inspect()
    }

    fun configure(rate: Float, level: Float) {
        this.rate = rate.coerceIn(0.5f, 1.5f)
        this.volume = level.coerceIn(0f, 1f)
        bundled.configure(this.rate, this.volume)
    }

    fun configureEngine(preferences: WordQuestPreferences) {
        val changed = mode != preferences.pronunciationMode || usVoice != preferences.nativeVoiceUs ||
            ukVoice != preferences.nativeVoiceUk || allowNetwork != preferences.allowOnlineVoice
        mode = preferences.pronunciationMode
        usVoice = preferences.nativeVoiceUs
        ukVoice = preferences.nativeVoiceUk
        allowNetwork = preferences.allowOnlineVoice
        if (changed) stop()
    }

    fun pronounce(words: List<String>, preferred: EnglishVoice, replay: Boolean = false) {
        if (closed) return
        stop()
        error = null
        if (mode == PronunciationMode.ClassicOffline) {
            voiceDetails = "${preferred.locale.toLanguageTag()} · eski çevrimdışı eSpeak kaydı (robotik olabilir)"
            bundled.play(words, preferred, rate, volume, { error = it })
            return
        }
        voiceDetails = "${preferred.locale.toLanguageTag()} · telefonda kurulu ses motoru; " +
            "gerçek ses kalitesi cihaza bağlıdır"
        devicePlayer().play(words, preferred,
            if (preferred == EnglishVoice.American) usVoice else ukVoice,
            allowNetwork, rate, volume,
            fallback = { reason, remaining ->
                if (!closed) {
                    error = "$reason. Aynı aksanın eski çevrimdışı ses kaydı çalınıyor."
                    voiceDetails = "${preferred.locale.toLanguageTag()} · cihaz sesi başarısız; " +
                        "eski çevrimdışı kayıt kullanılıyor"
                    bundled.play(remaining, preferred, rate, volume, { error = it })
                }
            })
    }

    fun stop() {
        device?.stop()
        bundled.stop()
        error = null
    }

    fun close() {
        closed = true
        device?.close()
        bundled.close()
    }
}
