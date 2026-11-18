package com.kelimebulmacasi.a1

import android.content.Context
import java.util.Locale

internal enum class AppTextSize(val label: String, val scale: Float) {
    Standard("Standart", 1f),
    Large("Büyük", 1.18f),
    ExtraLarge("Çok büyük", 1.35f),
}

internal enum class EnglishVoice(val label: String, val locale: Locale) {
    American("Amerikan", Locale.US),
    British("Britanya", Locale.UK),
}

internal enum class PronunciationMode(val label: String) {
    Device("Cihazın en kaliteli mevcut sesleri"),
    ClassicOffline("Eski çevrimdışı kayıtlar (robotik)"),
}

internal data class WordQuestPreferences(
    val textSize: AppTextSize = AppTextSize.Standard,
    val textScale: Float = textSize.scale,
    val correctSoundEnabled: Boolean = true,
    val correctSoundVolume: Float = 0.28f,
    val pronunciation: Boolean = true,
    val autoPronunciation: Boolean = true,
    val voice: EnglishVoice = EnglishVoice.American,
    val speechRate: Float = 0.9f,
    val speechVolume: Float = 1f,
    val showIpa: Boolean = true,
    val showTurkishApprox: Boolean = false,
    val pronunciationMode: PronunciationMode = PronunciationMode.Device,
    val nativeVoiceUs: String = "",
    val nativeVoiceUk: String = "",
    val allowOnlineVoice: Boolean = false,
)

internal class PreferenceStore(context: Context) {
    private val store = context.getSharedPreferences("wordquest_preferences", Context.MODE_PRIVATE)

    fun load(profile: String): WordQuestPreferences = WordQuestPreferences(
        textSize = AppTextSize.entries.find { it.name == store.getString("$profile.text", null) } ?: AppTextSize.Standard,
        textScale = if (store.contains("$profile.textScale")) {
            store.getFloat("$profile.textScale", 1f).coerceIn(0.75f, 1.4f)
        } else {
            (AppTextSize.entries.find { it.name == store.getString("$profile.text", null) }
                ?: AppTextSize.Standard).scale
        },
        correctSoundEnabled = store.getBoolean("$profile.correctSound", true),
        correctSoundVolume = store.getFloat("$profile.correctSoundVolume", 0.28f).coerceIn(0f, 1f),
        pronunciation = store.getBoolean("$profile.audio", true),
        autoPronunciation = store.getBoolean("$profile.auto", true),
        voice = EnglishVoice.entries.find { it.name == store.getString("$profile.voice", null) } ?: EnglishVoice.American,
        speechRate = store.getFloat("$profile.speechRate", 0.9f).coerceIn(0.5f, 1.5f),
        speechVolume = store.getFloat("$profile.speechVolume", 1f).coerceIn(0f, 1f),
        showIpa = store.getBoolean("$profile.ipa", true),
        showTurkishApprox = store.getBoolean("$profile.ipaTurkish", false),
        pronunciationMode = PronunciationMode.entries.find {
            it.name == store.getString("$profile.audioMode", null)
        } ?: PronunciationMode.Device,
        nativeVoiceUs = store.getString("$profile.nativeUs", "").orEmpty(),
        nativeVoiceUk = store.getString("$profile.nativeUk", "").orEmpty(),
        allowOnlineVoice = store.getBoolean("$profile.onlineVoice", false),
    )

    fun save(profile: String, preferences: WordQuestPreferences) {
        store.edit()
            .putString("$profile.text", preferences.textSize.name)
            .putFloat("$profile.textScale", preferences.textScale.coerceIn(0.75f, 1.4f))
            .putBoolean("$profile.correctSound", preferences.correctSoundEnabled)
            .putFloat("$profile.correctSoundVolume", preferences.correctSoundVolume.coerceIn(0f, 1f))
            .putBoolean("$profile.audio", preferences.pronunciation)
            .putBoolean("$profile.auto", preferences.autoPronunciation)
            .putString("$profile.voice", preferences.voice.name)
            .putFloat("$profile.speechRate", preferences.speechRate)
            .putFloat("$profile.speechVolume", preferences.speechVolume)
            .putBoolean("$profile.ipa", preferences.showIpa)
            .putBoolean("$profile.ipaTurkish", preferences.showTurkishApprox)
            .putString("$profile.audioMode", preferences.pronunciationMode.name)
            .putString("$profile.nativeUs", preferences.nativeVoiceUs)
            .putString("$profile.nativeUk", preferences.nativeVoiceUk)
            .putBoolean("$profile.onlineVoice", preferences.allowOnlineVoice)
            .apply()
    }
}
