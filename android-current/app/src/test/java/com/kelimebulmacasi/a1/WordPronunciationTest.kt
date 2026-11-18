package com.kelimebulmacasi.a1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Resource manifest and OGG decoding are additionally checked by tools/check_offline_audio.py. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class WordPronunciationTest {
    @Test fun stableOfflineFilenameAndCaseNormalization() {
        assertEquals("6d5a45920a15adea", OfflineAccentPlayer.assetId(" WATER "))
        assertEquals("321f6b7e8bf7f29a", OfflineAccentPlayer.assetId("tomato"))
        assertEquals("5bac399344eb2e10", OfflineAccentPlayer.assetId("Can't"))
        assertEquals("turn off", OfflineAccentPlayer.normalize("  Turn   OFF  "))
        assertNotEquals(OfflineAccentPlayer.assetId("water"), OfflineAccentPlayer.assetId("tomato"))
    }

    @Test fun localesRemainIndependent() {
        assertEquals("en-US", EnglishVoice.American.locale.toLanguageTag())
        assertEquals("en-GB", EnglishVoice.British.locale.toLanguageTag())
        assertNotEquals(EnglishVoice.American.locale, EnglishVoice.British.locale)
    }
}
