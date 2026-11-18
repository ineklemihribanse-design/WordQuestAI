package com.kelimebulmacasi.a1

import android.content.Context
import org.robolectric.RuntimeEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** New v0.8.1 preferences must be per profile and never require an API key. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class VoicePreferencesTest {
    @Test fun voiceChoiceRemainsPrivateToLearnerProfile() {
        val context = RuntimeEnvironment.getApplication()
        val preferences = PreferenceStore(context)
        val a = "test_voice_081_a"
        val b = "test_voice_081_b"
        preferences.save(a, WordQuestPreferences(
            pronunciationMode = PronunciationMode.Device,
            nativeVoiceUs = "sample-US-voice",
            nativeVoiceUk = "sample-GB-voice",
            allowOnlineVoice = true,
        ))
        preferences.save(b, WordQuestPreferences(pronunciationMode = PronunciationMode.ClassicOffline))
        assertEquals("sample-US-voice", preferences.load(a).nativeVoiceUs)
        assertEquals("sample-GB-voice", preferences.load(a).nativeVoiceUk)
        assertTrue(preferences.load(a).allowOnlineVoice)
        assertEquals(PronunciationMode.ClassicOffline, preferences.load(b).pronunciationMode)
        assertFalse(preferences.load(b).allowOnlineVoice)
        assertEquals("", preferences.load(b).nativeVoiceUs)
    }

    @Test fun textScaleAndGentleChimeSettingsStayIndependentForTwoProfiles() {
        val context = RuntimeEnvironment.getApplication()
        val store = PreferenceStore(context)
        val first = "sound_profile_first_082"
        val second = "sound_profile_second_082"
        store.save(first, WordQuestPreferences(textScale = 0.75f,
            correctSoundEnabled = false, correctSoundVolume = 0.12f,
            pronunciation = true))
        store.save(second, WordQuestPreferences(textScale = 1.35f,
            correctSoundEnabled = true, correctSoundVolume = 0.5f,
            pronunciation = false))
        val reopened = PreferenceStore(context)
        val a = reopened.load(first)
        val b = reopened.load(second)
        assertEquals(0.75f, a.textScale, 0.0001f)
        assertFalse(a.correctSoundEnabled)
        assertEquals(0.12f, a.correctSoundVolume, 0.0001f)
        assertTrue(a.pronunciation)
        assertEquals(1.35f, b.textScale, 0.0001f)
        assertTrue(b.correctSoundEnabled)
        assertEquals(0.5f, b.correctSoundVolume, 0.0001f)
        assertFalse(b.pronunciation)
    }

    @Test fun oldTextSizeSettingMigratesWithoutMakingTextLarger() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("wordquest_preferences", Context.MODE_PRIVATE)
        val profile = "legacy_text_size_082"
        prefs.edit().remove("$profile.textScale")
            .putString("$profile.text", AppTextSize.Large.name).commit()
        assertEquals(AppTextSize.Large.scale, PreferenceStore(context).load(profile).textScale, 0.0001f)
    }

    @Test fun defaultModeUsesInstalledVoicesWithoutInternetPermission() {
        val defaults = WordQuestPreferences()
        assertEquals(PronunciationMode.Device, defaults.pronunciationMode)
        assertFalse(defaults.allowOnlineVoice)
        assertTrue(defaults.autoPronunciation)
    }
}
